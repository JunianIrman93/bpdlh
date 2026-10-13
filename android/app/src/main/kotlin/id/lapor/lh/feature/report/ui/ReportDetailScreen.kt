package id.lapor.lh.feature.report.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lapor.lh.R
import id.lapor.lh.feature.report.domain.Report
import id.lapor.lh.feature.report.domain.ReportStatus
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ReportDetailRoute(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: ReportDetailViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val detailLoad by viewModel.detailLoad.collectAsStateWithLifecycle()
    ReportDetailScreen(
        report = report,
        detailLoad = detailLoad,
        onReloadDetail = viewModel::loadDetail,
        onBack = onBack,
        onCancelQueued = viewModel::cancelQueued,
        onRetry = viewModel::retry,
        onEdit = { id -> viewModel.edit { onEdit(id) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDetailScreen(
    report: Report?,
    detailLoad: DetailLoadState,
    onReloadDetail: () -> Unit,
    onBack: () -> Unit,
    onCancelQueued: () -> Unit,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (report == null) return@Scaffold
        val draft = report.draft
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = draft.title.ifBlank { stringResource(R.string.reports_untitled) },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                StatusBadge(status = report.status)
            }
            val details =
                listOfNotNull(draft.category?.let { stringResource(it.labelRes()) }, draft.activityDate?.display())
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (draft.description.isNotBlank()) Text(draft.description, style = MaterialTheme.typography.bodyLarge)

            if (report.status == ReportStatus.SYNCING) UploadProgress(report = report)
            if (report.failure != null) {
                when (report.status) {
                    ReportStatus.FAILED -> {
                        SectionCard(title = stringResource(R.string.detail_last_error), isError = true) {
                            Text(stringResource(report.failure.messageRes()))
                        }
                    }

                    ReportStatus.WAITING_NETWORK -> {
                        Notice(stringResource(R.string.detail_retry_pending), isError = false)
                    }

                    else -> {
                        Unit
                    }
                }
            }
            if (report.reviewNote != null) {
                SectionCard(
                    title = stringResource(R.string.detail_review_note),
                    isError = report.status == ReportStatus.REJECTED,
                ) { Text(report.reviewNote) }
            }

            if (report.isSummaryOnly) {
                ServerDetailCard(state = detailLoad, onReload = onReloadDetail)
            } else {
                LocationCard(report = report, snackbar = snackbar)

                SectionCard(title = stringResource(R.string.form_section_attachments), isError = false) {
                    if (report.attachments.isEmpty()) {
                        Notice(stringResource(R.string.detail_no_attachments), isError = false)
                    } else {
                        report.attachments.forEach { AttachmentRow(attachment = it, onRemove = null) }
                    }
                }
            }

            Actions(report = report, onCancelQueued = onCancelQueued, onRetry = onRetry, onEdit = onEdit)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ServerDetailCard(
    state: DetailLoadState,
    onReload: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.detail_server_title), isError = false) {
        if (state == DetailLoadState.LOADING || state == DetailLoadState.IDLE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Notice(stringResource(R.string.detail_server_loading), isError = false)
            }
        } else {
            val message =
                if (state == DetailLoadState.NETWORK_ERROR) R.string.detail_server_offline else R.string.detail_server_failed
            Notice(stringResource(message), isError = false)
            OutlinedButton(onClick = onReload, modifier = Modifier.height(48.dp)) {
                Text(stringResource(R.string.detail_server_reload))
            }
        }
    }
}

@Composable
private fun LocationCard(
    report: Report,
    snackbar: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noMapApp = stringResource(R.string.detail_no_map_app)
    val location = report.draft.location
    SectionCard(title = stringResource(R.string.form_section_location), isError = false) {
        if (location == null) {
            Notice(stringResource(R.string.detail_no_location), isError = false)
        } else {
            LocationDetails(location)
            TextButton(
                onClick = {
                    val coordinates = String.format(Locale.US, "%.6f,%.6f", location.latitude, location.longitude)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$coordinates?q=$coordinates"))
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        scope.launch { snackbar.showSnackbar(noMapApp) }
                    }
                },
                modifier = Modifier.height(48.dp),
            ) { Text(stringResource(R.string.detail_open_map)) }
        }
    }
}

@Composable
private fun Actions(
    report: Report,
    onCancelQueued: () -> Unit,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit,
) {
    when (report.status) {
        ReportStatus.WAITING_NETWORK -> {
            OutlinedButton(onClick = onCancelQueued, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(stringResource(R.string.detail_cancel_queue))
            }
        }

        ReportStatus.FAILED -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onEdit(report.id) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(R.string.detail_edit))
                }
                Button(onClick = onRetry, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(R.string.detail_retry))
                }
            }
        }

        ReportStatus.DRAFT -> {
            Button(onClick = { onEdit(report.id) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(stringResource(R.string.detail_edit))
            }
        }

        else -> {
            Unit
        }
    }
}
