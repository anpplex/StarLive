package com.starlive.app.redeem

import android.util.Log
import com.starlive.app.BuildConfig
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object RedeemClient {
    private const val TAG = "StarLive"

    data class ExchangeOk(
        val packId: String,
        val title: String,
        val version: Int,
        val sha256: String,
        val downloadUrl: String,
        val alreadyBound: Boolean,
    )

    fun exchange(baseUrl: String, code: String, deviceId: String): Result<ExchangeOk> {
        return runCatching {
            val root = baseUrl.trimEnd('/')
            val url = URL("$root/api/v1/starlive/exchange")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            try {
                val body = JSONObject()
                    .put("code", code.trim())
                    .put("device_id", deviceId)
                    .put("app_version", BuildConfig.VERSION_NAME)
                    .toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val codeHttp = conn.responseCode
                val text = (if (codeHttp in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.readText().orEmpty()
                val parsed = RedeemExchangeParser.parse(codeHttp, text)
                ExchangeOk(
                    packId = parsed.packId,
                    title = parsed.title,
                    version = parsed.version,
                    sha256 = parsed.sha256,
                    downloadUrl = parsed.downloadUrl,
                    alreadyBound = parsed.alreadyBound,
                )
            } finally {
                conn.disconnect()
            }
        }.onFailure { Log.w(TAG, "exchange failed", it) }
    }

    fun downloadTo(url: String, dest: File, expectedSha256: String = ""): Result<File> {
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 120_000
                instanceFollowRedirects = true
            }
            try {
                if (conn.responseCode !in 200..299) {
                    throw RuntimeException("下载失败 HTTP ${conn.responseCode}")
                }
                dest.parentFile?.mkdirs()
                BufferedInputStream(conn.inputStream).use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
            } finally {
                conn.disconnect()
            }
            if (expectedSha256.isNotBlank()) {
                val actual = sha256Hex(dest)
                if (!actual.equals(expectedSha256, ignoreCase = true)) {
                    dest.delete()
                    throw RuntimeException("主题包校验失败（SHA-256 不匹配）")
                }
            }
            dest
        }.onFailure { Log.w(TAG, "download failed", it) }
    }

    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { b -> "%02x".format(b) }
    }
}
