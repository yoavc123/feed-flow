package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.ReaderModeDefaults

// Last export: 2025-12-21T11:48:48.756Z
fun getReaderModeStyledHtml(
    colors: ReaderColors?,
    content: String,
    fontSize: Int,
    lineHeight: Int = ReaderModeDefaults.LINE_HEIGHT,
    title: String? = null,
    imageUrl: String? = null,
    leadingContent: String = "",
    siteName: String? = null,
): String {
    val titleTag = if (title != null) {
        "<h1>${title.escapeHtml()}</h1>"
    } else {
        ""
    }

    val subtitleTag = if (!siteName.isNullOrBlank()) {
        "<h4>${siteName.escapeHtml()}</h4>"
    } else {
        ""
    }
    val contentWithSubtitle = subtitleTag + content

    val processedContent = if (imageUrl != null && !hasLeadingImage(content)) {
        val heroTag = "<img class=\"__hero\" src=\"${imageUrl.escapeHtml()}\" alt=\"\" />"
        val h4CloseIndex = contentWithSubtitle.indexOf("</h4>", ignoreCase = true)
        if (h4CloseIndex >= 0) {
            val insertAt = h4CloseIndex + "</h4>".length
            contentWithSubtitle.substring(0, insertAt) + heroTag + contentWithSubtitle.substring(insertAt)
        } else {
            heroTag + contentWithSubtitle
        }
    } else {
        contentWithSubtitle
    }

    // language=html
    return """
    <html lang="en" dir='auto'>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <style>
      ${readerModeCss(colors, fontSize, lineHeight)}
    </style>
    </head>
    <body>
    $leadingContent
    $titleTag
    <div id="container">
        <div id="__content">
            $processedContent
        </div>
    </div>
    <script>
        document.addEventListener("DOMContentLoaded", function () {
            // Get the title from the first h1 (which we inject)
            var firstH1 = document.querySelector("h1");
            if (firstH1) {
                var titleText = firstH1.textContent.trim().toLowerCase();
                // Check all h1 and h2 elements for duplicates
                document.querySelectorAll("h1, h2").forEach(function(el) {
                    // Skip the first h1 (our injected title)
                    if (el === firstH1) return;
                    var elText = el.textContent.trim().toLowerCase();
                    // Hide if text matches the title
                    if (elText === titleText) {
                        el.style.display = 'none';
                    }
                });
            }

          function hideImage(image, className) {
              image.classList.add(className);
              image.setAttribute("aria-hidden", "true");
          }

          function firstImageAttribute(element, names) {
              for (var index = 0; index < names.length; index++) {
                  var value = element.getAttribute(names[index]);
                  if (value) return value;
              }
              return "";
          }

          document.querySelectorAll("picture source").forEach(function(source) {
              var lazySourceSet = firstImageAttribute(source, ["data-srcset", "data-lazy-srcset"]);
              if (lazySourceSet) source.setAttribute("srcset", lazySourceSet);
          });

          document.querySelectorAll("img").forEach(function(image) {
              var lazySource = firstImageAttribute(
                  image,
                  ["data-src", "data-lazy-src", "data-original", "data-original-src", "data-cfsrc"]
              );
              var lazySourceSet = firstImageAttribute(image, ["data-srcset", "data-lazy-srcset"]);
              if (lazySource) image.setAttribute("src", lazySource);
              if (lazySourceSet) image.setAttribute("srcset", lazySourceSet);
              image.setAttribute("decoding", "async");

              function normalizeLoadedImage() {
                  var declaredWidth = parseInt(image.getAttribute("width") || "0", 10);
                  var declaredHeight = parseInt(image.getAttribute("height") || "0", 10);
                  var actualWidth = image.naturalWidth || declaredWidth;
                  var actualHeight = image.naturalHeight || declaredHeight;
                  var isTrackingImage = actualWidth > 0 && actualHeight > 0 &&
                      actualWidth <= 2 && actualHeight <= 2;

                  if (isTrackingImage) {
                      hideImage(image, "__feedflow_tracking_image");
                  } else if (image.classList.contains("__hero") || actualWidth >= 240 || actualHeight >= 240) {
                      image.classList.add("__feedflow_content_image");
                  }
              }

              image.addEventListener("load", normalizeLoadedImage);
              image.addEventListener("error", function() {
                  hideImage(image, "__feedflow_image_load_failed");
              });

              if (image.complete && image.naturalWidth === 0) {
                  hideImage(image, "__feedflow_image_load_failed");
              } else if (image.complete) {
                  normalizeLoadedImage();
              }
          });

          document.body.addEventListener("click", function(event) {
              let anchor = event.target.closest("a");
              if (anchor) {
                  let url = anchor.href || anchor.getAttribute("href");
                  if (url && window.kmpJsBridge && window.kmpJsBridge.callNative) {
                      event.preventDefault();
                      window.kmpJsBridge.callNative(
                       "urlInterceptor",
                        url,
                        {}
                      );
                  }
                  return;
              }

              let image = event.target.closest("img");
              if (!image) return;

              let imageUrl = image.currentSrc ||
                  image.getAttribute("src") ||
                  image.getAttribute("data-src") ||
                  image.getAttribute("data-lazy-src") ||
                  image.getAttribute("data-original") ||
                  "";
              if (!imageUrl) return;

              // Validate URL for security - only allow http(s) URLs
              let isValidUrl = imageUrl.startsWith("http://") || imageUrl.startsWith("https://");
              let isLocalhost = imageUrl.includes("localhost") ||
                               imageUrl.includes("127.0.0.1") ||
                               imageUrl.includes("0.0.0.0") ||
                               imageUrl.includes("::1");

              if (!isValidUrl || isLocalhost) {
                  return;
              }

              event.preventDefault();
              if (window.kmpJsBridge && window.kmpJsBridge.callNative) {
                  window.kmpJsBridge.callNative(
                   "imageInterceptor",
                    imageUrl,
                    {}
                  );
              } else {
                  let encodedUrl = encodeURIComponent(imageUrl);
                  window.location.href = "feedflow-image://?src=" + encodedUrl;
              }
          });
        });
    </script>
    </body>
    </html>
        """
        .trimIndent()
}

private fun String.escapeHtml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

private const val LEADING_IMAGE_SCAN_WINDOW = 1000

private fun hasLeadingImage(content: String): Boolean {
    val window = if (content.length > LEADING_IMAGE_SCAN_WINDOW) {
        content.substring(0, LEADING_IMAGE_SCAN_WINDOW)
    } else {
        content
    }
    return window.indexOf("<img", ignoreCase = true) >= 0
}

internal fun readerModeCss(colors: ReaderColors?, fontSize: Int, lineHeight: Int): String {
    val fontSizeCss = "${fontSize}px"
    val lineHeightCss = readerLineHeightToCss(lineHeight)
    val textColor = colors?.textColor ?: "inherit"
    val linkColor = colors?.linkColor ?: "inherit"
    val backgroundColor = colors?.backgroundColor ?: "transparent"
    val borderColor = colors?.borderColor ?: "transparent"
    // language=css
    return """
:root {
    --reader-text: $textColor;
    --reader-link: $linkColor;
    --reader-bg: $backgroundColor;
    --reader-border: $borderColor;
    --reader-column: 880px;
    --reader-gutter: clamp(18px, 4vw, 48px);
}

* {
    box-sizing: border-box;
}

html {
    overflow-x: hidden;
    background: var(--reader-bg);
    -webkit-text-size-adjust: 100%;
}

body {
    margin: 0;
    overflow-x: hidden;
    overflow-wrap: break-word;
    font: -apple-system-body;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
    font-size: $fontSizeCss;
    line-height: $lineHeightCss;
    padding: 0 0 128px;
    color: var(--reader-text);
    background: var(--reader-bg);
}

#container {
    width: 100%;
}

body > h1,
#__content {
    width: 100%;
    max-width: calc(var(--reader-column) + var(--reader-gutter) + var(--reader-gutter));
    margin-left: auto;
    margin-right: auto;
    padding-left: var(--reader-gutter);
    padding-right: var(--reader-gutter);
}

body > h1 {
    margin-top: 0;
    margin-bottom: 0.55em;
    font-size: clamp(1.9em, 7vw, 2.75em);
    letter-spacing: -0.025em;
}

#__content {
    line-height: $lineHeightCss;
    overflow-x: hidden;
    padding-bottom: 32px;
}

#__content > :first-child {
    margin-top: 0;
}

h1, h2, h3, h4, h5, h6 {
    line-height: 1.22;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
    font-weight: 750;
    overflow-wrap: anywhere;
}

h1 {
    font-size: 1.85em;
}

h2 {
    margin-top: 1.65em;
    font-size: 1.45em;
}

h3 {
    margin-top: 1.5em;
    font-size: 1.2em;
}

p {
    margin-top: 0;
    margin-bottom: 1.1em;
}

ul, ol {
    padding-inline-start: 1.5em;
}

li + li {
    margin-top: 0.35em;
}

img, svg, picture, iframe, object, video {
    max-width: 100% !important;
}

img {
    height: auto !important;
}

.__hero {
    display: block;
    width: 100%;
    height: auto !important;
    max-height: none;
    margin: 1.25em auto 1.5em;
    object-fit: contain;
    border-radius: 12px;
}

img.__feedflow_content_image {
    display: block;
    width: auto !important;
    height: auto !important;
    max-width: 100% !important;
    max-height: none !important;
    margin: 1.25em auto;
    float: none !important;
    object-fit: contain;
    border-radius: 10px;
    cursor: zoom-in;
}

img.__feedflow_image_load_failed,
img.__feedflow_tracking_image,
img[width="1"][height="1"] {
    display: none !important;
}

pre {
    max-width: 100%;
    overflow-x: auto;
    white-space: pre;
    background-color: var(--reader-bg);
    border: 1px solid var(--reader-border);
    border-radius: 10px;
    padding: 14px 16px;
    margin: 1.25em 0;
    font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
    line-height: 1.4286;
    font-size: 0.9em;
}

table {
    display: block;
    width: max-content;
    min-width: 100%;
    max-width: 100%;
    overflow-x: auto;
    border-collapse: collapse;
    margin: 1.25em 0;
}

th, td {
    padding: 0.65em 0.8em;
    border-bottom: 1px solid var(--reader-border);
    text-align: start;
}

blockquote {
    margin: 1.5em 0;
    padding: 0.25em 0 0.25em 1.1em;
    border-inline-start: 3px solid var(--reader-border);
    font-style: italic;
}

blockquote p {
    margin: 0.5em 0;
}

blockquote p:first-child {
    margin-top: 0;
}

blockquote p:last-child {
    margin-bottom: 0;
}

blockquote cite {
    display: block;
    text-align: right;
    margin-top: 1em;
    font-style: normal;
    font-weight: 600;
    opacity: 0.7;
}

blockquote cite:before {
    content: "\2014 ";
}

a:link, a:visited {
    color: var(--reader-link);
    text-decoration-thickness: 0.08em;
    text-underline-offset: 0.15em;
}

figure {
    max-width: 100%;
    margin: 1.5em 0;
}

figure > img,
figure > picture,
picture > img {
    display: block;
    margin-left: auto;
    margin-right: auto;
}

figcaption, cite {
    opacity: 0.68;
    font-size: 0.82em;
    line-height: 1.4;
}

.__subtitle {
    font-weight: bold;
    vertical-align: baseline;
    opacity: 0.5;
}

.__subtitle .__icon {
    width: 1.2em;
    height: 1.2em;
    object-fit: cover;
    overflow: hidden;
    border-radius: 3px;
    margin-right: 0.3em;
    position: relative;
    top: 0.3em;
}

.__subtitle .__separator {
    opacity: 0.5;
}

#__footer {
    margin: 2.5em 0 4em;
}

#__footer > .label {
    font-size: small;
    opacity: 0.5;
    text-align: center;
    margin-bottom: 0.66em;
    font-weight: 500;
}

#__footer > button {
    padding: 0.5em;
    text-align: center;
    font-weight: 500;
    min-height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    font-size: 1em;
    border: none;
    border-radius: 0.5em;
}

iframe {
    display: block;
    width: 100%;
    max-width: 100%;
    height: auto !important;
    min-height: 240px;
    max-height: 80vh;
    aspect-ratio: 16 / 9;
    margin: 1.5em 0;
    border: 0;
    border-radius: 10px;
}

video {
    display: block;
    width: 100%;
    height: auto !important;
    margin: 1.5em auto;
    border-radius: 10px;
}

code {
    padding: 2px 4px;
    border-radius: 3px;
    line-height: 1.4em;
    background-color: var(--reader-bg);
    border: 1px solid var(--reader-border);
    font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
    font-size: 0.9em;
    color: var(--reader-text);
}

pre code {
    letter-spacing: -.027em;
    font-size: 1em;
    background-color: transparent;
    border: none;
    padding: 0;
}

hr {
    height: 1px;
    margin: 2em 0;
    border: 0;
    background: var(--reader-border);
}

@media screen and (max-width: 480px) {
    blockquote {
        padding-inline-start: 0.85em;
    }

    pre {
        padding: 12px;
    }
}

    """.trimIndent()
}

private const val LINE_HEIGHT_BASE_TENTHS = 15
private const val LINE_HEIGHT_STEP_TENTHS = 1
private const val LINE_HEIGHT_TENTHS_DIVISOR = 10
private const val LINE_HEIGHT_DESKTOP_ROUNDING_OFFSET = 5

// step 0 -> "1.5", default step 1 -> "1.6", step 15 -> "3.0". Integer tenths avoids float/locale issues.
internal fun readerLineHeightToCss(step: Int): String {
    val tenths = LINE_HEIGHT_BASE_TENTHS + step * LINE_HEIGHT_STEP_TENTHS
    return "${tenths / LINE_HEIGHT_TENTHS_DIVISOR}.${tenths % LINE_HEIGHT_TENTHS_DIVISOR}"
}

fun readerLineHeightToTextLineHeightSp(fontSize: Int, step: Int): Int =
    (
        fontSize * (LINE_HEIGHT_BASE_TENTHS + step * LINE_HEIGHT_STEP_TENTHS) +
            LINE_HEIGHT_DESKTOP_ROUNDING_OFFSET
        ) / LINE_HEIGHT_TENTHS_DIVISOR

// Live update injected into the reader WebView (Android & iOS use the same rule string).
fun readerLineHeightJs(step: Int): String {
    val lineHeight = readerLineHeightToCss(step)
    return """
        (function() {
          var styleId = "__feedflow_line_height_style";
          var style = document.getElementById(styleId);
          if (!style) {
            style = document.createElement("style");
            style.id = styleId;
            document.head.appendChild(style);
          }
          style.textContent = "body, #__content { line-height: $lineHeight; }";
        })();
    """.trimIndent()
}

data class ReaderColors(
    val textColor: String,
    val linkColor: String,
    val backgroundColor: String,
    val borderColor: String? = null,
)
