package com.prof18.feedflow.core.model

enum class ReleaseState {
    ACTIVE,
    PENDING_UNDO,
    RELEASED,
}

data class ReadingProgress(
    val fraction: Float,
    val updatedAtMillis: Long,
) {
    val normalizedFraction: Float = fraction.coerceIn(0f, 1f)
}

enum class ReadingProgressLabel {
    JUST_STARTED,
    GETTING_INTO_IT,
    HALFWAY_THROUGH,
    NEARLY_FINISHED,
    FINISHED,
}

fun Float.toReadingProgressLabel(): ReadingProgressLabel = when {
    this >= FINISHED_PROGRESS -> ReadingProgressLabel.FINISHED
    this >= NEARLY_FINISHED_PROGRESS -> ReadingProgressLabel.NEARLY_FINISHED
    this >= HALFWAY_PROGRESS -> ReadingProgressLabel.HALFWAY_THROUGH
    this >= GETTING_STARTED_PROGRESS -> ReadingProgressLabel.GETTING_INTO_IT
    else -> ReadingProgressLabel.JUST_STARTED
}

private const val FINISHED_PROGRESS = 0.95f
private const val NEARLY_FINISHED_PROGRESS = 0.75f
private const val HALFWAY_PROGRESS = 0.4f
private const val GETTING_STARTED_PROGRESS = 0.1f
