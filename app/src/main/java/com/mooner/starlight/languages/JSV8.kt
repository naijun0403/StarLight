package com.mooner.starlight.languages

import android.graphics.Color
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar
import coil.load
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8Value
import com.eclipsesource.v8.utils.V8ObjectUtils
import com.mooner.starlight.R
import com.mooner.starlight.plugincore.config.*
import com.mooner.starlight.plugincore.language.*
import com.mooner.starlight.plugincore.logger.Logger
import com.mooner.starlight.plugincore.method.Method
import com.mooner.starlight.plugincore.models.Message
import com.mooner.starlight.plugincore.project.Project
import com.mooner.starlight.plugincore.utils.Icon


class JSV8: Language() {
    override val id: String
        get() = "JS_V8"
    override val name: String
        get() = "자바스크립트 (V8)"
    override val fileExtension: String
        get() = "js"
    override val loadIcon: (ImageView) -> Unit = { imageView ->
        imageView.load(R.drawable.ic_v8)
    }
    override val requireRelease: Boolean
        get() = true
    override val defaultCode: String
        get() = """
            function onMessage(event) {
                
            }
        """.trimIndent()

    override val configObjectList: List<ConfigObject> = config {
        category {
            title = "JS-V8"
            items = items {
                toggle {
                    id = "toggle_test"
                    name = "토글 테스트"
                    defaultValue = false
                    icon = Icon.ADD
                }
                slider {
                    id = "slider_test"
                    name = "슬라이더 테스트"
                    max = 5
                    defaultValue = 2
                    icon = Icon.ADD
                    dependency = "toggle_test"
                }
                toggle {
                    id = "toggle_test2"
                    name = "토글 테스트2"
                    defaultValue = true
                    icon = Icon.ADD
                    dependency = "toggle_test"
                }
                string {
                    id = "string_test"
                    name = "인풋 테스트"
                    hint = "테스트으으"
                    icon = Icon.ADD
                }
                spinner {
                    id = "spinner_test"
                    name = "스피너 테스트"
                    items = listOf(
                        "테스트1",
                        "테스트2",
                        "테스트3"
                    )
                    icon = Icon.ADD
                }
                button {
                    id = "button_test"
                    name = "버튼 테스트"
                    onClickListener = {
                        Logger.d("JSV8_Config", "onClickListener")
                    }
                    backgroundColor = Color.parseColor("#ffa361")
                    icon = Icon.ADD
                }
                custom {
                    id = "custom_test"
                    onInflate = {
                        val imageView = ImageView(it.context).apply {
                            layoutParams = ActionBar.LayoutParams(1440, 1440).apply { gravity = Gravity.CENTER }
                            maxWidth = 500
                            maxHeight = 500
                            x = 0f
                            y = 0f
                        }
                        (it as LinearLayout).addView(imageView)
                        imageView.load(R.drawable.splash_anim)
                    }
                }
            }
        }
    }


    override fun onConfigUpdated(updated: Map<String, Any>) {
        Logger.i("JSV8", "updated: $updated")
    }

    override fun compile(code: String, methods: List<Method>, project: Project?): Any {
        val v8 = V8.createV8Runtime()
        try {
            v8.apply {
                for (methodBlock in methods) {
                    addClass(
                        methodBlock.name,
                        methodBlock.getInstance(project!!),
                        methodBlock.functions.map { it.name }.toTypedArray(),
                        methodBlock.functions.map { it.args }.toTypedArray()
                    )
                }
                executeScript(code)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            v8.locker.release()
        }
        return v8
    }

    override fun release(engine: Any) {
        val v8 = engine as V8
        if (!v8.isReleased && v8.locker.hasLock()) {
            v8.release(false)
        }
    }

    override fun callFunction(engine: Any, functionName: String, args: Array<Any>, onError: (e: Exception) -> Unit) {
        val v8 = engine as V8
        v8.locker.acquire()
        var messageArg: Message? = null

        try {
            if (args.find { it is Message }.also { if (it != null) messageArg = it as Message } != null) {
                val msgParams: V8Value = V8ObjectUtils.toV8Object(v8,
                    hashMapOf(
                        "message" to messageArg!!.message,
                        "room" to mapOf(
                            "name" to messageArg!!.room.name,
                            "isGroupChat" to messageArg!!.room.isGroupChat,
                            "send" to messageArg!!.room::send
                        ),
                        "sender" to mapOf(
                            "name" to messageArg!!.sender.name,
                            "profileBase64" to messageArg!!.sender.profileBase64,
                            "profileHash" to messageArg!!.sender.profileHash
                        ),
                        "packageName" to messageArg!!.packageName
                    )
                )
                v8.executeJSFunction(functionName, V8Array(v8).push(msgParams))
            } else {
                v8.executeJSFunction(functionName, *args)
            }
        } catch (e: Exception) {
            onError(e)
        } finally {
            v8.locker.release()
        }
    }

    override fun eval(code: String): Any {
        val engine = compile(code, listOf(), null) as V8
        try {
            engine.locker.acquire()
            return engine.executeScript(code)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            release(engine)
        }
    }

    private fun V8.addClass(name: String, clazz: Any, methods: Array<String>, args: Array<Array<Class<*>>>) {
        val obj = V8Object(this)
        this.add(name, obj)

        for ((i, method) in methods.withIndex()) {
            obj.registerJavaMethod(
                clazz,
                method,
                method,
                args[i]
            )
        }
        obj.close()
    }
}