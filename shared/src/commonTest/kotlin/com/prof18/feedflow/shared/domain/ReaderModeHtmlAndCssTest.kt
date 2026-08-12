package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.ReaderModeDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderModeHtmlAndCssTest {

    @Test
    fun `reader mode css hides images that failed to load`() {
        val css = readerModeCss(colors = null, fontSize = 18, lineHeight = 0)

        assertTrue(css.contains("img.__feedflow_image_load_failed"))
        assertTrue(css.contains("display: none !important;"))
    }

    @Test
    fun `reader mode css uses an adaptive wider reading column`() {
        val css = readerModeCss(colors = null, fontSize = 18, lineHeight = 0)

        assertTrue(css.contains("--reader-column: 880px"))
        assertTrue(css.contains("--reader-gutter: clamp(18px, 4vw, 48px)"))
        assertTrue(css.contains("margin: 0"))
        assertTrue(!css.contains("max-width: 700px"))
    }

    @Test
    fun `reader mode css preserves image aspect ratios`() {
        val css = readerModeCss(colors = null, fontSize = 18, lineHeight = 0)
        val heroCss = css.substringAfter(".__hero {").substringBefore("}")

        assertTrue(css.contains("height: auto !important"))
        assertTrue(css.contains("object-fit: contain"))
        assertTrue(css.contains("max-height: none"))
        assertTrue(!heroCss.contains("object-fit: cover"))
        assertTrue(!heroCss.contains("height: 50vw"))
    }

    @Test
    fun `reader mode html marks failed images as hidden`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p>Content</p><img src=\"https://example.com/missing.jpg\" width=\"1080\" height=\"1920\" />",
            fontSize = 18,
        )

        assertTrue(html.contains("image.addEventListener(\"error\""))
        assertTrue(html.contains("image.complete && image.naturalWidth === 0"))
        assertTrue(html.contains("__feedflow_image_load_failed"))
        assertTrue(html.contains("aria-hidden"))
    }

    @Test
    fun `reader mode html hydrates lazy images and hides tracking pixels`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<picture><source data-srcset=\"https://example.com/image.webp\" />" +
                "<img data-src=\"https://example.com/image.jpg\" /></picture>",
            fontSize = 18,
        )

        assertTrue(html.contains("data-lazy-srcset"))
        assertTrue(html.contains("image.setAttribute(\"src\", lazySource)"))
        assertTrue(html.contains("__feedflow_tracking_image"))
        assertTrue(html.contains("__feedflow_content_image"))
        assertTrue(html.contains("viewport-fit=cover"))
        assertTrue(!html.contains("user-scalable=no"))
    }

    @Test
    fun `readerLineHeightToCss maps steps`() {
        assertEquals("1.5", readerLineHeightToCss(0))
        assertEquals("1.6", readerLineHeightToCss(ReaderModeDefaults.LINE_HEIGHT))
        assertEquals("2.5", readerLineHeightToCss(10))
        assertEquals("3.0", readerLineHeightToCss(15))
    }

    @Test
    fun `readerModeCss includes line height rule`() {
        val defaultCss = readerModeCss(null, 18, lineHeight = ReaderModeDefaults.LINE_HEIGHT)
        assertTrue(defaultCss.contains("line-height: 1.6"))

        val spacedCss = readerModeCss(null, 18, lineHeight = 5)
        assertTrue(spacedCss.contains("line-height: 2.0"))
    }

    @Test
    fun `getReaderModeStyledHtml includes line height`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p>Content</p>",
            fontSize = 18,
            lineHeight = 3,
        )

        assertTrue(html.contains("line-height: 1.8"))
    }

    @Test
    fun `getReaderModeStyledHtml includes an escaped article title`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p>Content</p>",
            fontSize = 18,
            title = "Title <with> & symbols",
            imageUrl = "https://example.com/hero.jpg",
            leadingContent = "<div id=\"spacer\"></div>",
            siteName = "Example & Site",
        )

        assertTrue(html.contains("<h1>Title &lt;with&gt; &amp; symbols</h1>"))
        assertTrue(html.contains("<img class=\"__hero\" src=\"https://example.com/hero.jpg\""))
        assertTrue(html.contains("<h4>Example &amp; Site</h4>"))
        assertTrue(html.indexOf("id=\"spacer\"") < html.indexOf("<h1>"))
    }

    @Test
    fun `feed hero is not duplicated when content has a leading image`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p><img src=\"https://example.com/hero.jpg\" style=\"float: left\">Article body</p>",
            fontSize = 18,
            imageUrl = "https://example.com/hero.jpg",
            siteName = "Example Site",
        )

        assertTrue(html.contains("<h4>Example Site</h4><p><img"))
        assertTrue(html.contains("float: left"))
        assertTrue(!html.contains("class=\"__hero\""))
    }

    @Test
    fun `feed hero is not injected when content has a different leading image`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p><img src=\"https://example.com/diagram.jpg\">Article body</p>",
            fontSize = 18,
            imageUrl = "https://example.com/hero.jpg",
        )

        assertTrue(html.contains("https://example.com/diagram.jpg"))
        assertTrue(!html.contains("class=\"__hero\""))
    }
}
