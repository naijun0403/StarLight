package com.mooner.starlight.plugincore.utils

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.stream.Collectors

class Utils {
    companion object {
        fun InputStream.readString(): String {
            return BufferedReader(InputStreamReader(this))
                .lines()
                .parallel()
                .collect(Collectors.joining("\n"))
        }
    }
}