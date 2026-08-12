package com.prof18.feedflow.shared.domain.coaching

import com.prof18.feedflow.core.model.ArticleInteraction
import com.prof18.feedflow.core.model.ArticleInteractionType
import com.prof18.feedflow.core.model.CalmCoachingType
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalmCoachingEngineTest {
    private val engine = CalmCoachingEngine()

    @Test
    fun `flooding source is suggested deterministically`() {
        val source = FeedSourceGenerator.feedSource(id = "flood", title = "Flood")

        val cards = engine.createCards(
            feedItems = FeedItemGenerator.feedItemsForSource(source, count = 12),
            feedSources = listOf(source),
            interactions = emptyList(),
        )

        assertEquals(CalmCoachingType.FLOODING_SOURCE, cards.single().type)
        assertEquals(12, cards.single().evidenceCount)
    }

    @Test
    fun `release and open thresholds create source coaching`() {
        val source = FeedSourceGenerator.feedSource(id = "source")
        val interactions = List(5) { interaction(source.id, ArticleInteractionType.RELEASED, it) } +
            List(7) { interaction(source.id, ArticleInteractionType.OPENED, it + 10) }

        val cards = engine.createCards(emptyList(), listOf(source), interactions)

        assertEquals(
            listOf(CalmCoachingType.REPEATED_RELEASES, CalmCoachingType.FREQUENTLY_OPENED),
            cards.map { it.type },
        )
    }

    @Test
    fun `three frequently opened uncategorized sources suggest a stream`() {
        val sources = List(3) { FeedSourceGenerator.feedSource(id = "source-$it") }
        val interactions = sources.flatMapIndexed { sourceIndex, source ->
            List(3) { interaction(source.id, ArticleInteractionType.OPENED, sourceIndex * 10 + it) }
        }

        val cards = engine.createCards(emptyList(), sources, interactions)

        assertTrue(cards.any { it.type == CalmCoachingType.SUGGESTED_STREAM })
    }

    @Test
    fun `unavailable topic provider omits suggestions`() = kotlinx.coroutines.test.runTest {
        assertEquals(false, UnavailableTopicSuggestionProvider.isAvailable)
        assertTrue(UnavailableTopicSuggestionProvider.suggestTopics(listOf("Local title")).isEmpty())
    }

    private fun interaction(sourceId: String, type: ArticleInteractionType, timestamp: Int) = ArticleInteraction(
        feedItemId = "item-$sourceId-$timestamp",
        feedSourceId = sourceId,
        type = type,
        occurredAtMillis = timestamp.toLong(),
    )
}
