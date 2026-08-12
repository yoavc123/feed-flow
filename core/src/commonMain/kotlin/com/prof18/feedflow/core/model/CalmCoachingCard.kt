package com.prof18.feedflow.core.model

enum class CalmCoachingType {
    FLOODING_SOURCE,
    REPEATED_RELEASES,
    FREQUENTLY_OPENED,
    SUGGESTED_STREAM,
}

data class CalmCoachingCard(
    val id: String,
    val type: CalmCoachingType,
    val feedSource: FeedSource? = null,
    val evidenceCount: Int,
)
