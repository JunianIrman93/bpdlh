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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    onOpenReport: (Report) -> Unit,
    viewModel: ReportListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportListScreen(
        uiState = uiState,
        onFilterChange = viewModel::onFilterChange,
        onCreateReport = onCreateReport,
        onOpenReport = onOpenReport,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportListScreen(
    uiState: ReportListUiState,
    onFilterChange: (ReportStatus?) -> Unit,
    onCreateReport: () -> Unit,
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
            if (uiState.hasAnyReport) {
                FilterRow(selected = uiState.filter, onFilterChange = onFilterChange)
            }
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
                    stringResource(R.string.reports_attachment_count, report.attachments.size),
                )
            Text(
                text = details.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
