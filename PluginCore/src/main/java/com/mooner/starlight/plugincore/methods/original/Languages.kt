package com.mooner.starlight.plugincore.methods.original

import com.mooner.starlight.plugincore.Session
import com.mooner.starlight.plugincore.language.Language

class Languages {
    companion object {
        fun get(id: String): Language? {
            return Session.getLanguageManager().getLanguage(id) as Language?
        }
    }
}