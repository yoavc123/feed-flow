package com.prof18.feedflow.core.model

enum class ArticleInteractionType {
    OPENED,
    RELEASED,
    SAVED,
    SHARED,
}

data class ArticleInteraction(
    val feedItemId: String,
    val feedSourceId: String,
    val type: ArticleInteractionType,
    val occurredAtMillis: Long,
)

data class ReadingHistoryItem(
    val feedItemId: String,
    val url: String,
    val title: String?,
    val summary: String?,
    val author: String?,
    val feedSourceId: String,
    val feedSourceTitle: String,
    val articleText: String?,
    val openedAtMillis: Long,
)
