package id.lapor.lh.feature.report.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lapor.lh.R
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
    onCreateReport: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenReport: (Report) -> Unit,
) {
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
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
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
                when {
                    !uiState.isLoaded -> {
                        Unit
                    }

                    uiState.reports.isEmpty() -> {
                        EmptyState(
                            text =
                                stringResource(
                                    if (uiState.hasAnyReport) R.string.reports_empty_filtered else R.string.reports_empty,
                                ),
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.reports, key = { it.id }) { report ->
                                ReportCard(report = report, onClick = { onOpenReport(report) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncBanner(
    uiState: ReportListUiState,
    onSyncNow: () -> Unit,
) {
    val offline = !uiState.isOnline
    val pending = uiState.pendingCount
    val message =
        when {
            offline && pending > 0 -> stringResource(R.string.sync_offline_pending, pending)
            offline -> stringResource(R.string.sync_offline)
            uiState.isSyncing -> stringResource(R.string.sync_in_progress)
            pending > 0 -> stringResource(R.string.sync_pending, pending)
            else -> return
        }
    val showSyncNow = !offline && !uiState.isSyncing && !uiState.isRefreshing
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (offline) colors.surfaceVariant else colors.secondaryContainer,
        contentColor = if (offline) colors.onSurfaceVariant else colors.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 48.dp).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            if (uiState.isSyncing && !offline) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(text = message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (showSyncNow) {
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
            )
        }
    }
}

@Composable
private fun ReportCard(
    report: Report,
    onClick: () -> Unit,
) {
    val draft = report.draft
    OutlinedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = draft.title.ifBlank { stringResource(R.string.reports_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                StatusBadge(status = report.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            val details =
                listOfNotNull(
                    draft.category?.let { stringResource(it.labelRes()) },
                    draft.activityDate?.display(),
                    if (report.isSummaryOnly) null else stringResource(R.string.reports_attachment_count, report.attachments.size),
                )
            Text(
                text = details.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (report.status == ReportStatus.SYNCING) {
                Spacer(modifier = Modifier.height(12.dp))
                UploadProgress(report = report)
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(modifier = Modifier.fillParentMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
