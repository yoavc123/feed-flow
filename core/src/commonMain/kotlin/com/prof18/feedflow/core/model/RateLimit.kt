package com.prof18.feedflow.core.model

enum class RateLimit(
    val maxItems: Int?,
    val windowHours: Long?,
) {
    NONE(maxItems = null, windowHours = null),
    ONE_PER_HOUR(maxItems = 1, windowHours = 1),
    THREE_PER_DAY(maxItems = 3, windowHours = 24),
    ONE_PER_DAY(maxItems = 1, windowHours = 24),
}
