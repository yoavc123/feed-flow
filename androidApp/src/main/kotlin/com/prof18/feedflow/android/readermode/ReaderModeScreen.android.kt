package com.prof18.feedflow.android.readermode

import android.webkit.CookieManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.openShareSheet
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ReadingProgressLabel
import com.prof18.feedflow.core.model.ShownContentSource
import com.prof18.feedflow.core.model.toReadingProgressLabel
import com.prof18.feedflow.shared.domain.ReaderColors
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtml
import com.prof18.feedflow.shared.domain.readerLineHeightJs
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.getArchiveISUrl
import com.prof18.feedflow.shared.utils.isValidUrl
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun ReaderModeScreen(
    readerModeState: ReaderModeState,
    fontSize: Int,
    onUpdateFontSize: (Int) -> Unit,
    lineHeight: Int,
    onUpdateLineHeight: (Int) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    navigateBack: () -> Unit,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    onNavigateToPrevious: () -> Unit,
    onNavigateToNext: () -> Unit,
    onToggleContentSource: () -> Unit,
    onReadingProgress: (Float) -> Unit,
    initialReadingProgress: Float?,
    isDetailFullscreen: Boolean = false,
    onToggleDetailFullscreen: (() -> Unit)? = null,
) {
    val browserManager = koinInject<BrowserManager>()

    val context = LocalContext.current
    val navigator = rememberWebViewNavigator()
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var toolbarVisible by rememberSaveable { mutableStateOf(true) }
    val articleId = (readerModeState as? ReaderModeState.Success)?.readerModeData?.id?.id
    var readingProgress by rememberSaveable(articleId) { mutableStateOf(initialReadingProgress ?: 0f) }

    LaunchedEffect(articleId, initialReadingProgress) {
        toolbarVisible = true
        if (initialReadingProgress != null) {
            readingProgress = initialReadingProgress
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
        ) { contentPadding ->
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                AnimatedVisibility(
                    visible = toolbarVisible && readerModeState !is ReaderModeState.Loading,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(1f)
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            bottom = contentPadding.calculateBottomPadding() + 12.dp,
                        ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (readerModeState is ReaderModeState.Success) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                tonalElevation = 2.dp,
                            ) {
                                Text(
                                    text = readingProgress.toReadingProgressLabel().label(),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        ReaderModeFloatingToolbar(
                            readerModeState = readerModeState,
                            fontSize = fontSize,
                            openInBrowser = { url ->
                                if (isValidUrl(url)) {
                                    browserManager.openUrlWithFavoriteBrowser(url, context)
                                }
                            },
                            onShareClick = { url, title ->
                                context.openShareSheet(
                                    title = title,
                                    url = url,
                                )
                            },
                            onArchiveClick = { articleUrl ->
                                val archiveUrl = getArchiveISUrl(articleUrl)
                                if (isValidUrl(archiveUrl)) {
                                    browserManager.openUrlWithFavoriteBrowser(archiveUrl, context)
                                }
                            },
                            onCommentsClick = { commentsUrl ->
                                if (isValidUrl(commentsUrl)) {
                                    browserManager.openUrlWithFavoriteBrowser(commentsUrl, context)
                                }
                            },
                            onFontSizeChange = { newFontSize ->
                                navigator.evaluateJavaScript(
                                    """
        document.getElementById("container").style.fontSize = "$newFontSize" + "px";
                                    """.trimIndent(),
                                )
                                onUpdateFontSize(newFontSize)
                            },
                            lineHeight = lineHeight,
                            onLineHeightChange = { newLineHeight ->
                                navigator.evaluateJavaScript(readerLineHeightJs(newLineHeight))
                                onUpdateLineHeight(newLineHeight)
                            },
                            onBookmarkClick = onBookmarkClick,
                            onToggleContentSource = onToggleContentSource,
                            canNavigatePrevious = canNavigatePrevious,
                            canNavigateNext = canNavigateNext,
                            onNavigateToPrevious = onNavigateToPrevious,
                            onNavigateToNext = onNavigateToNext,
                        )
                    }
                }

                when (readerModeState) {
                    is ReaderModeState.HtmlNotAvailable -> {
                        if (readerModeState.url.isBlank()) {
                            ReaderContentUnavailable(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding),
                            )
                        } else {
                            FallbackWebView(
                                url = readerModeState.url,
                                contentPadding = contentPadding,
                                navigator = navigator,
                            )
                        }
                    }

                    ReaderModeState.Loading -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(contentPadding)
                                .fillMaxSize(),
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is ReaderModeState.Success -> {
                        ReaderMode(
                            readerModeState = readerModeState,
                            openInBrowser = { url ->
                                if (isValidUrl(url)) {
                                    browserManager.openUrlWithFavoriteBrowser(url, context)
                                }
                            },
                            onImageClick = { imageUrl ->
                                if (imageUrl.isNotBlank()) {
                                    fullscreenImageUrl = imageUrl
                                }
                            },
                            contentPadding = contentPadding,
                            navigator = navigator,
                            onExpandToolbar = { toolbarVisible = true },
                            onCollapseToolbar = { toolbarVisible = false },
                            onReadingProgress = { progress ->
                                readingProgress = progress
                                onReadingProgress(progress)
                            },
                            initialReadingProgress = initialReadingProgress,
                            modifier = Modifier.testTag(
                                ReaderModeE2eIds.article(readerModeState.readerModeData.id.id),
                            ),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = toolbarVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .zIndex(1f),
                ) {
                    ReaderModeToolbar(
                        navigateBack = {
                            if (navigator.canGoBack) {
                                navigator.navigateBack()
                            } else {
                                navigateBack()
                            }
                        },
                        isDetailFullscreen = isDetailFullscreen,
                        onToggleDetailFullscreen = onToggleDetailFullscreen,
                    )
                }
            }
        }

        fullscreenImageUrl?.let { imageUrl ->
            FullScreenImageOverlay(
                imageUrl = imageUrl,
                onDismiss = { fullscreenImageUrl = null },
            )
        }
    }
}

@Composable
private fun ReaderContentUnavailable(modifier: Modifier = Modifier) {
    val strings = LocalFeedFlowStrings.current
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = strings.readerModeNoContentTitle,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = strings.readerModeNoContentMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun FallbackWebView(
    url: String,
    contentPadding: PaddingValues,
    navigator: WebViewNavigator,
) {
    val state = rememberWebViewState(url)
    var showPageLoader by remember { mutableStateOf(true) }

    LaunchedEffect(url) {
        showPageLoader = true
    }
    LaunchedEffect(state.loadingState) {
        showPageLoader = state.loadingState !is LoadingState.Finished
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        WebView(
            modifier = Modifier.fillMaxSize(),
            state = state,
            navigator = navigator,
        )

        if (showPageLoader) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun ReaderMode(
    readerModeState: ReaderModeState.Success,
    openInBrowser: (String) -> Unit,
    onImageClick: (String) -> Unit,
    contentPadding: PaddingValues,
    navigator: WebViewNavigator,
    onExpandToolbar: () -> Unit,
    onCollapseToolbar: () -> Unit,
    onReadingProgress: (Float) -> Unit,
    initialReadingProgress: Float?,
    modifier: Modifier = Modifier,
) {
    val bodyColor = MaterialTheme.colorScheme.onSurface.toArgb().toHexString().substring(2)
    val linkColor = MaterialTheme.colorScheme.primary.toArgb().toHexString().substring(2)
    val backgroundArgb = MaterialTheme.colorScheme.surface.toArgb()
    val backgroundColor = "#${backgroundArgb.toHexString().substring(2)}"
    val borderColor = "#${MaterialTheme.colorScheme.outlineVariant.toArgb().toHexString().substring(2)}"

    val colors = ReaderColors(
        textColor = "#$bodyColor",
        linkColor = "#$linkColor",
        backgroundColor = backgroundColor,
        borderColor = borderColor,
    )

    val latestOpenInBrowser by rememberUpdatedState(openInBrowser)
    val latestOpenImage by rememberUpdatedState(onImageClick)
    val latestExpand by rememberUpdatedState(onExpandToolbar)
    val latestCollapse by rememberUpdatedState(onCollapseToolbar)

    val spacerHeightDp = contentPadding.calculateTopPadding().value.toInt() + READER_TOP_CLEARANCE_DP

    val content = getReaderModeStyledHtml(
        colors = colors,
        content = readerModeState.readerModeData.content,
        fontSize = readerModeState.readerModeData.fontSize,
        lineHeight = readerModeState.readerModeData.lineHeight,
        title = readerModeState.readerModeData.title.takeIf {
            readerModeState.readerModeData.shownContentSource == ShownContentSource.FEED
        },
        imageUrl = readerModeState.readerModeData.imageUrl,
        leadingContent = "<div id=\"__feedflow_top_spacer\" style=\"height: ${spacerHeightDp}px;\"></div>",
        siteName = readerModeState.readerModeData.siteName,
    )

    val jsBridge = rememberWebViewJsBridge()
    LaunchedEffect(jsBridge) {
        jsBridge.register(
            object : IJsMessageHandler {
                override fun handle(
                    message: JsMessage,
                    navigator: WebViewNavigator?,
                    callback: (String) -> Unit,
                ) {
                    if (message.params.isNotBlank()) {
                        latestOpenInBrowser(message.params)
                    }
                }

                override fun methodName(): String = "urlInterceptor"
            },
        )
        jsBridge.register(
            object : IJsMessageHandler {
                override fun handle(
                    message: JsMessage,
                    navigator: WebViewNavigator?,
                    callback: (String) -> Unit,
                ) {
                    val imageUrl = message.params
                    if (imageUrl.isNotBlank() && isValidImageUrl(imageUrl)) {
                        latestOpenImage(imageUrl)
                    }
                }

                override fun methodName(): String = "imageInterceptor"
            },
        )
    }

    val state = rememberWebViewStateWithHTMLData(
        data = content,
        baseUrl = readerModeState.readerModeData.baseUrl,
    )
    var progressRestored by remember(readerModeState.readerModeData.id.id) { mutableStateOf(false) }
    LaunchedEffect(state.loadingState, initialReadingProgress) {
        val progress = initialReadingProgress
        if (!progressRestored && state.loadingState is LoadingState.Finished && progress != null && progress > 0f) {
            navigator.evaluateJavaScript(
                "window.scrollTo(0, Math.max(0, " +
                    "(document.documentElement.scrollHeight - window.innerHeight) * $progress));",
            )
            progressRestored = true
        }
    }

    val density = LocalDensity.current
    val thresholdPx = with(density) { 6.dp.toPx() }

    var scrollY by remember { mutableIntStateOf(0) }
    var scrollRange by remember { mutableIntStateOf(0) }
    var scrollExtent by remember { mutableIntStateOf(0) }
    var scrollEventCount by remember { mutableIntStateOf(0) }

    val layoutDir = LocalLayoutDirection.current
    Box(modifier = modifier.fillMaxSize()) {
        WebView(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = contentPadding.calculateLeftPadding(layoutDir))
                .padding(end = contentPadding.calculateRightPadding(layoutDir)),
            state = state,
            navigator = navigator,
            webViewJsBridge = jsBridge,
            onCreated = { webView ->
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                webView.setBackgroundColor(backgroundArgb)
                webView.isVerticalScrollBarEnabled = false
                webView.setOnScrollChangeListener { _, _, newScrollY, _, oldScrollY ->
                    val delta = newScrollY - oldScrollY
                    when {
                        delta.toFloat() > thresholdPx -> latestCollapse()
                        delta.toFloat() < -thresholdPx -> latestExpand()
                    }
                    scrollY = newScrollY
                    @Suppress("DEPRECATION")
                    val contentHeightPx = (webView.contentHeight * webView.scale).toInt()
                    val viewportHeightPx = webView.height
                    scrollRange = contentHeightPx
                    scrollExtent = viewportHeightPx
                    val scrollableRange = (contentHeightPx - viewportHeightPx).coerceAtLeast(1)
                    onReadingProgress((newScrollY.toFloat() / scrollableRange).coerceIn(0f, 1f))
                    if (!webView.canScrollVertically(1)) {
                        latestExpand()
                    }
                    scrollEventCount++
                }
            },
        )

        if (scrollRange > scrollExtent) {
            ScrollbarOverlay(
                scrollY = scrollY,
                scrollRange = scrollRange,
                scrollExtent = scrollExtent,
                scrollEventCount = scrollEventCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(8.dp)
                    .padding(end = 2.dp),
            )
        }
    }
}

@Composable
private fun ReadingProgressLabel.label(): String = when (this) {
    ReadingProgressLabel.JUST_STARTED -> LocalFeedFlowStrings.current.readingProgressJustStarted
    ReadingProgressLabel.GETTING_INTO_IT -> LocalFeedFlowStrings.current.readingProgressGettingIntoIt
    ReadingProgressLabel.HALFWAY_THROUGH -> LocalFeedFlowStrings.current.readingProgressHalfway
    ReadingProgressLabel.NEARLY_FINISHED -> LocalFeedFlowStrings.current.readingProgressNearlyFinished
    ReadingProgressLabel.FINISHED -> LocalFeedFlowStrings.current.readingProgressFinished
}

@Suppress("MagicNumber")
@Composable
private fun ScrollbarOverlay(
    scrollY: Int,
    scrollRange: Int,
    scrollExtent: Int,
    scrollEventCount: Int,
    modifier: Modifier = Modifier,
) {
    val thumbAlpha = remember { Animatable(0f) }
    LaunchedEffect(scrollEventCount) {
        if (scrollEventCount == 0) return@LaunchedEffect
        thumbAlpha.snapTo(1f)
        delay(1500.milliseconds)
        thumbAlpha.animateTo(0f, tween(durationMillis = 300))
    }

    BoxWithConstraints(modifier = modifier) {
        val containerHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val scrollableRange = (scrollRange - scrollExtent).toFloat().coerceAtLeast(1f)
        val thumbHeightPx = (scrollExtent.toFloat() / scrollRange.toFloat()) * containerHeightPx
        val thumbMaxOffset = containerHeightPx - thumbHeightPx
        val thumbOffsetPx = (scrollY.toFloat() / scrollableRange) * thumbMaxOffset

        val thumbHeightDp = with(LocalDensity.current) { thumbHeightPx.toDp() }
        val thumbOffsetDp = with(LocalDensity.current) { thumbOffsetPx.toDp() }

        Box(
            modifier = Modifier
                .offset(y = thumbOffsetDp)
                .fillMaxWidth()
                .height(thumbHeightDp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f * thumbAlpha.value)),
        )
    }
}

private fun isValidImageUrl(url: String): Boolean {
    val isHttpUrl = url.startsWith("http://") || url.startsWith("https://")
    val isLocalhost = url.contains("localhost", ignoreCase = true) ||
        url.contains("127.0.0.1") ||
        url.contains("0.0.0.0") ||
        url.contains("::1")
    return isHttpUrl && !isLocalhost
}

private const val READER_TOP_CLEARANCE_DP = 56
