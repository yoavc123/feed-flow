package com.prof18.feedflow.core.model

data class FeedSource(
    val id: String,
    val url: String,
    val title: String,
    val category: FeedSourceCategory?,
    val lastSyncTimestamp: Long?,
    val logoUrl: String?,
    val websiteUrl: String?,
    val fetchFailed: Boolean,
    val articleOpenMode: ArticleOpenMode,
    val isHiddenFromTimeline: Boolean,
    val isPinned: Boolean,
    val isNotificationEnabled: Boolean,
    val isHideImagesEnabled: Boolean,
    val pinnedPosition: Int = 0,
    val position: Int = 0,
    val flowPace: FlowPace? = null,
    val mutedUntilMillis: Long? = null,
    val voiceStatus: VoiceStatus = VoiceStatus.AUTOMATIC,
    val sourcePresentation: SourcePresentation = SourcePresentation.STANDARD,
    val rateLimit: RateLimit = RateLimit.NONE,
) {
    fun websiteUrlFallback(): String? =
        websiteUrl ?: url.toWebsiteBaseUrl()

    fun isMuted(nowMillis: Long): Boolean =
        mutedUntilMillis?.let { it > nowMillis } == true

    fun isVoice(): Boolean = when (voiceStatus) {
        VoiceStatus.VOICE -> true
        VoiceStatus.NOT_VOICE -> false
        VoiceStatus.AUTOMATIC -> flowPace == FlowPace.SLOW || flowPace == FlowPace.TIMELESS
    }
}

private fun String.toWebsiteBaseUrl(): String? {
    val trimmedUrl = trim()
    if (trimmedUrl.isEmpty()) {
        return null
    }
    val normalizedUrl = if (trimmedUrl.startsWith("http://", ignoreCase = true) ||
        trimmedUrl.startsWith("https://", ignoreCase = true)
    ) {
        trimmedUrl
    } else {
        "https://$trimmedUrl"
    }
    return WEBSITE_URL_REGEX.find(normalizedUrl)?.value
}

private val WEBSITE_URL_REGEX = Regex("^https?://[^/]+", RegexOption.IGNORE_CASE)
