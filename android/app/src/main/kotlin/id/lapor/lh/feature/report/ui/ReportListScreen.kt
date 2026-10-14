package id.lapor.lh.feature.report.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lapor.lh.R
import id.lapor.lh.core.ui.LaporLHMotion
import id.lapor.lh.core.ui.LocalDarkTheme
import id.lapor.lh.feature.report.domain.Report
import id.lapor.lh.feature.report.domain.ReportStatus

@Composable
fun ReportListRoute(
    onCreateReport: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenReport: (Report) -> Unit,
    viewModel: ReportListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportListScreen(
        uiState = uiState,
        onFilterChange = viewModel::onFilterChange,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onCreateReport = onCreateReport,
        onOpenAccount = onOpenAccount,
        onOpenReport = onOpenReport,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportListScreen(
    uiState: ReportListUiState,
    onFilterChange: (ReportStatus?) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onCreateReport: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenReport: (Report) -> Unit,
) {
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, uiState.hasMore, uiState.reports.size) {
        if (nearEnd && uiState.hasMore) onLoadMore()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.reports_title), style = MaterialTheme.typography.titleLarge)
                        uiState.userName?.let {
                            Text(
                                text = stringResource(R.string.reports_greeting, it),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAccount) {
                        Icon(painterResource(R.drawable.ic_account), contentDescription = stringResource(R.string.account_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateReport,
                expanded = fabExpanded,
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_add),
                        contentDescription = if (fabExpanded) null else stringResource(R.string.reports_new),
                    )
                },
                text = { Text(stringResource(R.string.reports_new)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SyncBanner(uiState = uiState, onSyncNow = onRefresh)
            if (uiState.hasAnyReport) {
                FilterRow(selected = uiState.filter, onFilterChange = onFilterChange)
            }
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                val content =
                    when {
                        !uiState.isLoaded || uiState.isInitialLoad -> ListContent.LOADING
                        uiState.reports.isEmpty() -> ListContent.EMPTY
                        else -> ListContent.REPORTS
                    }
                AnimatedContent(
                    targetState = content,
                    transitionSpec = { fadeIn(LaporLHMotion.effects()) togetherWith fadeOut(LaporLHMotion.effects()) },
                    label = "reportListContent",
                ) { shown ->
                    when (shown) {
                        ListContent.LOADING -> {
                            SkeletonList()
                        }

                        ListContent.EMPTY -> {
                            EmptyState(
                                title =
                                    stringResource(
                                        if (uiState.hasAnyReport) R.string.reports_empty_filtered_title else R.string.reports_empty_title,
                                    ),
                                text =
                                    stringResource(
                                        if (uiState.hasAnyReport) R.string.reports_empty_filtered else R.string.reports_empty,
                                    ),
                            )
                        }

                        ListContent.REPORTS -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(uiState.reports, key = { it.id }) { report ->
                                    ReportCard(
                                        report = report,
                                        onClick = { onOpenReport(report) },
                                        modifier =
                                            Modifier.animateItem(
                                                fadeInSpec = LaporLHMotion.effects(),
                                                placementSpec = LaporLHMotion.spatial(),
                                                fadeOutSpec = LaporLHMotion.effects(),
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class ListContent {
    LOADING,
    EMPTY,
    REPORTS,
}

private const val SKELETON_CARD_COUNT = 5
private const val LOAD_MORE_THRESHOLD = 3
private const val SHIMMER_DURATION_MS = 1200

@Composable
private fun SkeletonList() {
    val description = stringResource(R.string.reports_loading)
    val brush = shimmerBrush()
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .clearAndSetSemantics { contentDescription = description },
    ) {
        repeat(SKELETON_CARD_COUNT) { SkeletonCard(brush) }
    }
}

@Composable
private fun SkeletonCard(brush: Brush) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                SkeletonBlock(brush, MaterialTheme.shapes.medium, Modifier.size(44.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBlock(brush, MaterialTheme.shapes.small, Modifier.fillMaxWidth(0.75f).height(16.dp))
                    SkeletonBlock(brush, MaterialTheme.shapes.small, Modifier.fillMaxWidth(0.45f).height(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(brush, MaterialTheme.shapes.small, Modifier.width(88.dp).height(24.dp))
                Spacer(modifier = Modifier.weight(1f))
                SkeletonBlock(brush, MaterialTheme.shapes.small, Modifier.width(96.dp).height(12.dp))
            }
        }
    }
}

@Composable
internal fun SkeletonBlock(
    brush: Brush,
    shape: Shape,
    modifier: Modifier,
) {
    Box(modifier = modifier.clip(shape).background(brush))
}

@Composable
internal fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceBright
    val sweep = with(LocalDensity.current) { 320.dp.toPx() }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing)),
        label = "shimmerProgress",
    )
    val start = -sweep + progress * sweep * 4
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(start, 0f),
        end = Offset(start + sweep, sweep / 4),
    )
}

private data class BannerState(
    val offline: Boolean,
    val syncing: Boolean,
    val pending: Int,
    val canSyncNow: Boolean,
)

@Composable
private fun SyncBanner(
    uiState: ReportListUiState,
    onSyncNow: () -> Unit,
) {
    val offline = !uiState.isOnline
    val syncing = uiState.isSyncing && !offline
    val state =
        if (offline || syncing || uiState.pendingCount > 0) {
            BannerState(
                offline = offline,
                syncing = syncing,
                pending = uiState.pendingCount,
                canSyncNow = !offline && !uiState.isSyncing && !uiState.isRefreshing,
            )
        } else {
            null
        }
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (fadeIn(LaporLHMotion.effects()) togetherWith fadeOut(LaporLHMotion.effects()))
                .using(SizeTransform(clip = false) { _, _ -> LaporLHMotion.spatial() })
        },
        contentKey = { it?.copy(pending = 0, canSyncNow = false) },
        label = "syncBanner",
    ) { banner ->
        if (banner != null) BannerContent(banner, onSyncNow)
    }
}

@Composable
private fun BannerContent(
    banner: BannerState,
    onSyncNow: () -> Unit,
) {
    val message =
        when {
            banner.offline && banner.pending > 0 -> stringResource(R.string.sync_offline_pending, banner.pending)
            banner.offline -> stringResource(R.string.sync_offline)
            banner.syncing -> stringResource(R.string.sync_in_progress)
            else -> stringResource(R.string.sync_pending, banner.pending)
        }
    val colors = MaterialTheme.colorScheme
    val (container, content) =
        when {
            banner.offline -> colors.surfaceContainerHighest to colors.onSurfaceVariant
            banner.syncing -> colors.primaryContainer to colors.onPrimaryContainer
            else -> colors.secondaryContainer to colors.onSecondaryContainer
        }
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 52.dp).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            if (banner.syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            } else {
                Icon(
                    painterResource(if (banner.offline) R.drawable.ic_offline else R.drawable.ic_status_waiting),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (banner.canSyncNow) {
                TextButton(onClick = onSyncNow) { Text(stringResource(R.string.sync_now)) }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: ReportStatus?,
    onFilterChange: (ReportStatus?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onFilterChange(null) },
                label = { Text(stringResource(R.string.reports_filter_all)) },
            )
        }
        items(ReportStatus.entries) { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onFilterChange(status) },
                label = { Text(stringResource(status.labelRes())) },
                leadingIcon = {
                    Icon(
                        painterResource(status.iconRes()),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

@Composable
private fun ReportCard(
    report: Report,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = report.draft
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, LaporLHMotion.spatial(), label = "cardScale")
    OutlinedCard(
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        interactionSource = interactionSource,
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                CategoryTile(category = draft.category, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.title.ifBlank { stringResource(R.string.reports_untitled) },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = draft.category?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.reports_no_category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status = report.status)
                Spacer(modifier = Modifier.weight(1f))
                draft.activityDate?.let { MetaItem(icon = R.drawable.ic_calendar, text = it.display()) }
                if (!report.isSummaryOnly) {
                    Spacer(modifier = Modifier.width(12.dp))
                    MetaItem(icon = R.drawable.ic_file, text = report.attachments.size.toString())
                }
            }
            AnimatedVisibility(
                visible = report.status == ReportStatus.SYNCING,
                enter = expandVertically(LaporLHMotion.spatial()) + fadeIn(LaporLHMotion.effects()),
                exit = shrinkVertically(LaporLHMotion.spatial()) + fadeOut(LaporLHMotion.effects()),
            ) {
                UploadProgress(report = report, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun MetaItem(
    @DrawableRes icon: Int,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(
    title: String,
    text: String,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillParentMaxSize().padding(32.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(96.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_empty), contentDescription = null, modifier = Modifier.size(44.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
