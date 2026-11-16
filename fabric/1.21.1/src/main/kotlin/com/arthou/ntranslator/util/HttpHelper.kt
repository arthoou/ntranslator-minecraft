package com.arthou.ntranslator.util

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI

object HttpHelper {
    fun requireLocal(uri: String): URI {
        val target = URI(uri)
        require(target.scheme in listOf("http", "https") &&
            target.host in listOf("127.0.0.1", "[::1]", "localhost") &&
            target.rawUserInfo == null && target.rawQuery == null && target.rawFragment == null
        ) { "Only loopback LibreTranslate addresses are supported." }
        return if (target.host == "localhost") {
            URI(target.scheme, null, "127.0.0.1", target.port, target.path, null, null)
        } else {
            target
        }
    }

    fun get(uri: String): JsonElement = request(uri, null)

    fun post(uri: String, body: JsonObject): JsonElement = request(uri, body)

    private fun request(uri: String, body: JsonObject?): JsonElement {
        val connection = requireLocal(uri).toURL().openConnection(Proxy.NO_PROXY) as HttpURLConnection
        try {
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                val data = body.toString().toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setFixedLengthStreamingMode(data.size)
                connection.outputStream.use { it.write(data) }
            }
            val status = connection.responseCode
            check(status in 200..299) { "LibreTranslate returned HTTP $status." }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { JsonParser.parseReader(it) }
        } finally {
            connection.disconnect()
        }
    }
}
