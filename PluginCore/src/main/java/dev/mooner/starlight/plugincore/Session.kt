/*
 * Session.kt created by Minki Moon(mooner1022)
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.starlight.plugincore

import android.os.Build
import android.os.Environment
import dev.mooner.starlight.plugincore.api.ApiManager
import dev.mooner.starlight.plugincore.config.GlobalConfig
import dev.mooner.starlight.plugincore.language.LanguageManager
import dev.mooner.starlight.plugincore.library.LibraryLoader
import dev.mooner.starlight.plugincore.library.LibraryManager
import dev.mooner.starlight.plugincore.library.LibraryManagerApi
import dev.mooner.starlight.plugincore.logger.Logger
import dev.mooner.starlight.plugincore.plugin.PluginLoader
import dev.mooner.starlight.plugincore.plugin.PluginManager
import dev.mooner.starlight.plugincore.project.ProjectLoader
import dev.mooner.starlight.plugincore.project.ProjectManager
import dev.mooner.starlight.plugincore.utils.NetworkUtil
import dev.mooner.starlight.plugincore.widget.WidgetManager
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.properties.Delegates

object Session {

    @JvmStatic
    private var isDuringInit = false

    @JvmStatic
    private var mIsInitComplete = false
    val isInitComplete get() = mIsInitComplete

    private val mJson: ThreadLocal<Json> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ThreadLocal.withInitial {
            Json {
                isLenient = true
                ignoreUnknownKeys = true
                prettyPrint = true
            }
        }
    } else {
        object : ThreadLocal<Json>() {
            override fun initialValue(): Json {
                return Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                    prettyPrint = true
                }
            }
        }
    }
    val json: Json
        get() = mJson.get()!!

    @Suppress("DEPRECATION")
    val globalConfig: GlobalConfig =
        GlobalConfig(File(Environment.getExternalStorageDirectory(), "StarLight/"))

    val languageManager: LanguageManager = LanguageManager()
    val pluginLoader: PluginLoader       = PluginLoader()
    val pluginManager: PluginManager     = PluginManager()
    val widgetManager: WidgetManager     = WidgetManager()
    //val eventManager: EventHandler       = EventHandler()

    private var mProjectLoader: ProjectLoader by Delegates.notNull()
    private var mProjectManager: ProjectManager by Delegates.notNull()
    val projectManager get() = mProjectManager
    val projectLoader get() = mProjectLoader

    private var mLibraryLoader: LibraryLoader? = null
    private var mLibraryManager: LibraryManager? = null
    private val libraryLoader: LibraryLoader? get() = mLibraryLoader
    val libraryManager: LibraryManager? get() = mLibraryManager

    private var mApiManager: ApiManager by Delegates.notNull()
    val apiManager get() = mApiManager

    const val isDebugging: Boolean = true

    fun init(baseDir: File) {
        if (isInitComplete || isDuringInit) {
            Logger.w("Session", "Rejecting re-init of Session")
            return
        }
        isDuringInit = true

        val preStack = Thread.currentThread().stackTrace[2]
        if (!preStack.className.startsWith("dev.mooner.starlight")) {
            throw IllegalAccessException("Illegal access to internal function init() from $preStack")
        }

        val projectDir = File(baseDir, "projects/")

        mProjectManager = ProjectManager(projectDir)
        mProjectLoader = ProjectLoader(projectDir)
        mApiManager = ApiManager()

        if (globalConfig.category("beta_features").getBoolean("load_external_dex_libs", false)) {
            mLibraryLoader = LibraryLoader()
            mLibraryManager = LibraryManager(libraryLoader!!.loadLibraries(baseDir).toMutableSet())
            apiManager.addApi(LibraryManagerApi())
        }
        isDuringInit = false
        mIsInitComplete = true
    }

    fun shutdown() {
        val preStack = Thread.currentThread().stackTrace[2]
        if (!preStack.className.startsWith("dev.mooner.starlight")) {
            throw IllegalAccessException("Illegal access to internal function shutdown()")
        }

        //globalConfig.push()

        NetworkUtil.purge()
        apiManager.purge()

        languageManager.purge()
        widgetManager.purge()
        pluginLoader.purge()
        pluginManager.purge()
        projectManager.purge()

        mJson.remove()
    }
}