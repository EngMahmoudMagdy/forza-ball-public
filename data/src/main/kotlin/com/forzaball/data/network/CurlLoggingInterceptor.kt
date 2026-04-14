package com.forzaball.data.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import kotlin.text.Charsets

/**
 * Logs each request as a copy-pastable curl command. Compatible with OkHttp 5 (ok2curl targets OkHttp 4 only).
 * Buffers the body once when present and replaces it so [chain.proceed] still receives a readable body.
 */
internal class CurlLoggingInterceptor(
    private val logLine: (String) -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val bodyBytes: ByteArray?
        val bodyContentType = original.body?.contentType()
        if (original.body != null) {
            val buffer = Buffer()
            original.body!!.writeTo(buffer)
            bodyBytes = buffer.readByteArray()
        } else {
            bodyBytes = null
        }

        val request = if (bodyBytes != null) {
            val newBody = bodyBytes.toRequestBody(bodyContentType)
            original.newBuilder().method(original.method, newBody).build()
        } else {
            original
        }

        logLine(buildCurl(request, bodyBytes))

        return chain.proceed(request)
    }

    private fun buildCurl(request: Request, bodyBytes: ByteArray?): String = buildString {
        append("curl -X ").append(request.method)
        append(" '").append(escapeSingleQuoted(request.url.toString())).append("'")

        for (i in 0 until request.headers.size) {
            val name = request.headers.name(i)
            val value = request.headers.value(i)
            append(" \\\n  -H '")
            append(escapeSingleQuoted("$name: $value"))
            append("'")
        }

        if (bodyBytes?.isNotEmpty() == true) {
            val charset = request.body?.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            val payload = String(bodyBytes, charset)
            append(" \\\n  --data-binary '").append(escapeSingleQuoted(payload)).append("'")
        }
    }

    private fun escapeSingleQuoted(s: String): String = s.replace("'", "'\\''")
}
