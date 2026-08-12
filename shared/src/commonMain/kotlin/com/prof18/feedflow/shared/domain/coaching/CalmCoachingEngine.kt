package com.prof18.feedflow.shared.domain.coaching

import com.prof18.feedflow.core.model.ArticleInteraction
import com.prof18.feedflow.core.model.ArticleInteractionType
import com.prof18.feedflow.core.model.CalmCoachingCard
import com.prof18.feedflow.core.model.CalmCoachingType
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource

class CalmCoachingEngine {

    fun createCards(
        feedItems: List<FeedItem>,
        feedSources: List<FeedSource>,
        interactions: List<ArticleInteraction>,
    ): List<CalmCoachingCard> {
        val sourceById = feedSources.associateBy(FeedSource::id)
        val cards = mutableListOf<CalmCoachingCard>()

        feedItems.groupingBy { it.feedSource.id }.eachCount()
            .filterValues { it >= FLOODING_ITEM_COUNT }
            .forEach { (sourceId, count) ->
                sourceById[sourceId]?.let { source ->
                    cards += sourceCard(CalmCoachingType.FLOODING_SOURCE, source, count)
                }
            }

        val releasesBySource = interactions.countBySource(ArticleInteractionType.RELEASED)
        releasesBySource.filterValues { it >= REPEATED_RELEASE_COUNT }.forEach { (sourceId, count) ->
            sourceById[sourceId]?.let { source ->
                cards += sourceCard(CalmCoachingType.REPEATED_RELEASES, source, count)
            }
        }

        val opensBySource = interactions.countBySource(ArticleInteractionType.OPENED)
        opensBySource.filterValues { it >= FREQUENT_OPEN_COUNT }.forEach { (sourceId, count) ->
            sourceById[sourceId]?.let { source ->
                cards += sourceCard(CalmCoachingType.FREQUENTLY_OPENED, source, count)
            }
        }

        val uncategorizedFrequentlyOpened = feedSources.count { source ->
            source.category == null && (opensBySource[source.id] ?: 0) >= STREAM_SOURCE_OPEN_COUNT
        }
        if (uncategorizedFrequentlyOpened >= SUGGESTED_STREAM_SOURCE_COUNT) {
            cards += CalmCoachingCard(
                id = CalmCoachingType.SUGGESTED_STREAM.name,
                type = CalmCoachingType.SUGGESTED_STREAM,
                evidenceCount = uncategorizedFrequentlyOpened,
            )
        }

        return cards
            .distinctBy(CalmCoachingCard::id)
            .sortedWith(compareBy({ it.type.ordinal }, { it.feedSource?.title.orEmpty() }, { it.id }))
            .take(MAX_CARDS)
    }

    private fun List<ArticleInteraction>.countBySource(type: ArticleInteractionType): Map<String, Int> =
        asSequence()
            .filter { it.type == type }
            .groupingBy(ArticleInteraction::feedSourceId)
            .eachCount()

    private fun sourceCard(type: CalmCoachingType, source: FeedSource, count: Int) = CalmCoachingCard(
        id = "${type.name}:${source.id}",
        type = type,
        feedSource = source,
        evidenceCount = count,
    )

    private companion object {
        const val FLOODING_ITEM_COUNT = 12
        const val REPEATED_RELEASE_COUNT = 5
        const val FREQUENT_OPEN_COUNT = 7
        const val STREAM_SOURCE_OPEN_COUNT = 3
        const val SUGGESTED_STREAM_SOURCE_COUNT = 3
        const val MAX_CARDS = 3
    }
}
