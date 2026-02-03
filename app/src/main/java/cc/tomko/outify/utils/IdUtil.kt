package cc.tomko.outify.utils

fun canonicalIdFromUri(uriOrId: String): String =
    uriOrId.substringAfterLast(":").trim()
