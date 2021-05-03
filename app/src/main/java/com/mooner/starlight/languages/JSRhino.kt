package com.mooner.starlight.languages

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.mooner.starlight.R
import com.mooner.starlight.core.ApplicationSession
import com.mooner.starlight.plugincore.language.Language
import com.mooner.starlight.plugincore.language.LanguageConfig
import com.mooner.starlight.plugincore.language.MethodBlock
import com.mooner.starlight.plugincore.language.SliderLanguageConfig
import com.mooner.starlight.plugincore.methods.original.Languages
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import javax.script.Invocable

class JSRhino: Language() {
    companion object {
        private const val CONF_OPTIMIZATION = "optimizationLevel"
        private const val CONF_LANG_VERSION = "js_version"
        private const val LANG_DEF_VERSION = Context.VERSION_ES6
    }

    override val id: String
        get() = "JS_RHINO"
    override val name: String
        get() = "자바스크립트 (라이노)"
    override val fileExtension: String
        get() = "js"
    override val icon: Drawable
        get() = ContextCompat.getDrawable(ApplicationSession.context, R.drawable.ic_js)!!
    override val requireRelease: Boolean
        get() = true

    override val configList: List<LanguageConfig>
        get() = listOf(
            SliderLanguageConfig(
                objectId = CONF_OPTIMIZATION,
                objectName = "최적화 레벨",
                max = 10,
                defaultValue = 1
            )
        )

    override fun onConfigChanged(changed: Map<String, Any>) {

    }

    override fun compile(code: String, methods: Array<MethodBlock>): Any {
        val context = Context.enter().apply {
            optimizationLevel = -1
            languageVersion = with(getLanguageConfig()) {
                if (containsKey(CONF_LANG_VERSION)) {
                    val version = get(CONF_LANG_VERSION)
                    if (version is Int) {
                        version
                    } else {
                        LANG_DEF_VERSION
                    }
                } else LANG_DEF_VERSION
            }
        }
        val shared = context.initStandardObjects()
        val scope = context.newObject(shared)
        //val engine = ScriptEngineManager().getEngineByName("rhino")!!
        for(methodBlock in methods) {
            ScriptableObject.putProperty(scope, methodBlock.blockName, Context.javaToJS(methodBlock.instance, scope))
            //engine.put(methodBlock.blockName, methodBlock.instance)
        }
        //engine.eval(code)
        context.evaluateString(scope, code, name, 1, null)
        //scope.put()
        return scope
    }

    override fun release(engine: Any) {
        super.release(engine)
        try {
            Context.exit()
        } catch (e: IllegalStateException) {}
    }

    override fun execute(engine: Any, methodName: String, args: Array<Any>) {
        ScriptableObject.callMethod(engine as Scriptable, methodName, args)
        //val invocable = engine as Invocable
        //invocable.invokeFunction(methodName, args)
    }

    override fun eval(code: String): Any {
        val context = Context.enter().apply {
            optimizationLevel = -1
            languageVersion = with(getLanguageConfig()) {
                if (containsKey(CONF_LANG_VERSION)) {
                    val version = get(CONF_LANG_VERSION)
                    if (version is Int) {
                        version
                    } else {
                        LANG_DEF_VERSION
                    }
                } else LANG_DEF_VERSION
            }
        }
        val shared = context.initSafeStandardObjects()
        val scope = context.newObject(shared)
        val result = context.evaluateString(scope, code, "eval", 1, null)
        Context.exit()
        return result
        //val engine = ScriptEngineManager().getEngineByName("rhino")!!
        //return engine.eval(code)
    }
}