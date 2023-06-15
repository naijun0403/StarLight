/*
 * PluginConfigActivity.kt created by Minki Moon(mooner1022)
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.starlight.ui.plugins.config

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.google.android.material.snackbar.Snackbar
import dev.mooner.starlight.databinding.ActivityPluginConfigBinding
import dev.mooner.starlight.plugincore.Session
import dev.mooner.starlight.plugincore.Session.pluginManager
import dev.mooner.starlight.plugincore.config.ButtonConfigObject
import dev.mooner.starlight.plugincore.config.ConfigImpl
import dev.mooner.starlight.plugincore.config.TypedString
import dev.mooner.starlight.plugincore.config.config
import dev.mooner.starlight.plugincore.config.data.InMemoryConfig
import dev.mooner.starlight.plugincore.config.data.PrimitiveTypedString
import dev.mooner.starlight.plugincore.plugin.Plugin
import dev.mooner.starlight.plugincore.utils.Icon
import dev.mooner.starlight.ui.config.ConfigAdapter
import dev.mooner.starlight.utils.bindFadeImage
import dev.mooner.starlight.utils.restartApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

class PluginConfigActivity: AppCompatActivity() {

    companion object {
        private const val EXTRA_PLUGIN_NAME = "pluginName"
        private const val EXTRA_PLUGIN_ID = "pluginId"
        private const val PLUGIN_CONFIG_FILE_NAME = "config-plugin.json"
    }

    private val changedData: MutableMap<String, MutableMap<String, Any>> = hashMapOf()
    private lateinit var savedData: MutableMap<String, MutableMap<String, PrimitiveTypedString>>
    private var configAdapter: ConfigAdapter? = null

    private lateinit var binding: ActivityPluginConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPluginConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fabProjectConfig = binding.fabPluginConfig

        val pluginName = intent.getStringExtra(EXTRA_PLUGIN_NAME)!!
        val pluginId = intent.getStringExtra(EXTRA_PLUGIN_ID)!!
        val plugin = pluginManager.getPluginById(pluginId)?: error("Failed to find plugin with id: $pluginId, name: $pluginName")

        val configFile = File(plugin.getDataFolder(), PLUGIN_CONFIG_FILE_NAME)
        savedData = try {
            Session.json.decodeFromString(configFile.readText())
        } catch (e: Exception) {
            mutableMapOf()
        }

        configAdapter = ConfigAdapter.Builder(this) {
            bind(binding.configRecyclerView)
            onConfigChanged { parentId, id, view, data ->
                if (parentId in changedData)
                    changedData[parentId]!![id] = data
                else
                    changedData[parentId] = hashMapOf(id to data)

                if (parentId in savedData)
                    savedData[parentId]!![id] = PrimitiveTypedString.from(data)
                else
                    savedData[parentId] = hashMapOf(id to PrimitiveTypedString.from(data))

                if (!fabProjectConfig.isShown) {
                    fabProjectConfig.show()
                }
                plugin.onConfigChanged(id, view, data)
            }
            structure { getConfig(plugin) }
            savedData(savedData)
            lifecycleOwner(this@PluginConfigActivity)
        }.build()

        fabProjectConfig.setOnClickListener { view ->
            if (configAdapter?.hasError == true) {
                Snackbar.make(view, "올바르지 않은 설정이 있습니다. 확인 후 다시 시도해주세요.", Snackbar.LENGTH_SHORT).show()
                fabProjectConfig.hide()
                return@setOnClickListener
            }
            configFile.writeText(Session.json.encodeToString(savedData))
            plugin.onConfigUpdated(ConfigImpl(savedData), changedData.mapValues { it.value.keys.toSet() })
            plugin.onConfigUpdated(changedData)
            Snackbar.make(view, "설정 저장 완료!", Snackbar.LENGTH_SHORT).show()
            fabProjectConfig.hide()
        }

        binding.scroll.bindFadeImage(binding.imageViewLogo)

        binding.leave.setOnClickListener { finish() }

        binding.pluginName.text = pluginName
    }

    private fun getConfig(plugin: Plugin) = plugin.getConfigStructure() + config {
        category {
            id = "cautious"
            title = "위험"
            textColor = color { "#FF865E" }
            items {
                button {
                    id = "delete_plugin"
                    title = "플러그인 제거"
                    type = ButtonConfigObject.Type.FLAT
                    setOnClickListener { view ->
                        MaterialDialog(binding.root.context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                            setCommonAttrs()
                            title(text = "정말 [${plugin.info.fullName}](을)를 삭제할까요?")
                            message(text = "주의: 모든 설정과 하위 디렉토리가 함께 삭제되며, 되돌릴 수 없습니다.")
                            positiveButton(text = "확인") { dialog ->
                                pluginManager.removePlugin(plugin.info.id)
                                Snackbar.make(view, "플러그인을 삭제했습니다.\n앱을 재시작할까요?", Snackbar.LENGTH_LONG)
                                    .setAction("확인") {
                                        restartApplication()
                                    }
                                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                    .show()
                                dialog.dismiss()
                            }
                            negativeButton(text = "취소") { dialog ->
                                dialog.dismiss()
                            }
                        }
                    }
                    icon = Icon.DELETE_SWEEP
                    //backgroundColor = Color.parseColor("#B8DFD8")
                    iconTintColor = color { "#FF5C58" }
                }
            }
        }
    }
}