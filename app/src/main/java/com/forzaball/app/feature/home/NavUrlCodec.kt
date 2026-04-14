package com.forzaball.app.feature.home

import android.util.Base64

internal object NavUrlCodec {
    fun encode(url: String): String =
        Base64.encodeToString(
            url.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )

    fun decode(payload: String): String =
        String(
            Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING),
            Charsets.UTF_8,
        )
}
