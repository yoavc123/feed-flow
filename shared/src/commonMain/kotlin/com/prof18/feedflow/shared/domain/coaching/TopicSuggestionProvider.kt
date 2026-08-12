package com.prof18.feedflow.shared.domain.coaching

interface TopicSuggestionProvider {
    val isAvailable: Boolean

    suspend fun suggestTopics(titles: List<String>): List<String>
}

object UnavailableTopicSuggestionProvider : TopicSuggestionProvider {
    override val isAvailable: Boolean = false

    override suspend fun suggestTopics(titles: List<String>): List<String> = emptyList()
}
