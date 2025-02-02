/*
 * GlobalConfig.kt created by Minki Moon(mooner1022)
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.starlight.plugincore.config

import dev.mooner.configdsl.DataMap
import dev.mooner.configdsl.MutableDataMap
import dev.mooner.starlight.plugincore.Session.json
import dev.mooner.starlight.plugincore.config.data.MutableConfig
import dev.mooner.starlight.plugincore.config.data.category.MutableConfigCategory
import dev.mooner.starlight.plugincore.event.EventHandler
import dev.mooner.starlight.plugincore.event.Events
import dev.mooner.starlight.plugincore.utils.decodeLegacyData
import dev.mooner.starlight.plugincore.utils.getStarLightDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.encodeToString
import java.io.File

object GlobalConfig: MutableConfig {

    private const val FILE_NAME = "config-general.json"
    private const val DEFAULT_CATEGORY = "general"

    private val cachedCategories: MutableMap<String, MutableConfigCategory> = hashMapOf()
    private val mData           : MutableDataMap by lazy(::loadFromFile)
    private val flushScope      = CoroutineScope(Dispatchers.IO)
    private val file            = File(getStarLightDirectory(), FILE_NAME)
    private val fileAccessMutex = Mutex()

    override fun getData(): DataMap =
        mData

    override fun getMutableData(): MutableDataMap =
        mData

    override operator fun get(id: String): MutableConfigCategory =
        category(id)

    override fun contains(id: String): Boolean = categoryOrNull(id) != null

    fun getDefaultCategory(): MutableConfigCategory =
        getOrCreateCategory(DEFAULT_CATEGORY)

    override fun category(id: String): MutableConfigCategory =
        getOrCreateCategory(id)

    override fun categoryOrNull(id: String): MutableConfigCategory? =
        getCategoryOrNull(id)

    override fun edit(block: MutableConfig.() -> Unit) {
        this.apply(block)
        push()
    }

    override fun push() {
        flushScope.launch {
            fileAccessMutex.lock()
            try {
                val str = json.encodeToString(mData)
                with(file) {
                    if (!exists())
                        mkdirs()
                    if (!isFile)
                        deleteRecursively()
                    writeText(str)
                }
            } finally {
                fileAccessMutex.unlock()
            }
            EventHandler.fireEvent(Events.Config.GlobalConfigUpdate())
        }
    }

    fun invalidateCache() {
        mData.clear()
        loadFromFile().forEach(mData::put)
    }

    private fun getCategoryOrNull(id: String): MutableConfigCategory? =
        cachedCategories[id] ?: mData[id]?.let(::MutableConfigCategory)

    private fun getOrCreateCategory(id: String): MutableConfigCategory {
        return getCategoryOrNull(id) ?: let {
            mData[id] = mutableMapOf()
            MutableConfigCategory(mData[id]!!).also {
                cachedCategories[id] = it
            }
        }
    }

    private fun loadFromFile(): MutableDataMap {
        return if (!file.exists() || !file.isFile) {
            file.parentFile?.mkdirs()
            file.createNewFile()
            hashMapOf()
        } else {
            val raw = file.readText()
            if (raw.isBlank())
                hashMapOf()
            else {
                try {
                    json.decodeLegacyData(raw)
                } catch (e: Exception) {
                    json.decodeFromString(raw)
                }
            }
        }
    }
}