package com.prof18.feedflow.core.model

private const val MillisPerHour = 60L * 60L * 1_000L
private const val FlashHours = 3L
private const val DailyHours = 12L
private const val StandardHours = 24L
private const val SlowHours = 72L
private const val TimelessHours = 168L

enum class FlowPace(val windowHours: Long) {
    FLASH(FlashHours),
    DAILY(DailyHours),
    STANDARD(StandardHours),
    SLOW(SlowHours),
    TIMELESS(TimelessHours),
    ;

    val windowMillis: Long
        get() = windowHours * MillisPerHour

    fun includes(pubDateMillis: Long?, nowMillis: Long): Boolean =
        pubDateMillis?.let { publishedAt ->
            publishedAt >= nowMillis - windowMillis
        } ?: (this == TIMELESS)

    fun freshness(pubDateMillis: Long?, nowMillis: Long): Float {
        if (pubDateMillis == null) return if (this == TIMELESS) 1f else 0f

        val age = (nowMillis - pubDateMillis).coerceAtLeast(0L)
        return (1f - age.toFloat() / windowMillis.toFloat()).coerceIn(0f, 1f)
    }
}
