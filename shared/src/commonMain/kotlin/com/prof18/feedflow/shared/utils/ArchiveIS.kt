package com.prof18.feedflow.shared.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

fun getArchiveISUrl(
    articleUrl: String,
    clock: Clock = Clock.System,
): String {
    val currentYear = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    return "https://archive.is/$currentYear/$articleUrl"
}
