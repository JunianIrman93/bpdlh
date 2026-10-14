package id.lapor.lh.feature.auth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lapor.lh.BuildConfig
import id.lapor.lh.R
import id.lapor.lh.feature.auth.domain.Role
import id.lapor.lh.feature.report.ui.SectionCard
import id.lapor.lh.feature.report.ui.displayDateTime

@Composable
fun AccountRoute(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccountScreen(uiState = uiState, onBack = onBack, onLogout = viewModel::logout)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountScreen(
    uiState: AccountUiState,
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val user = uiState.user ?: return@Scaffold
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(initialsOf(user.fullName), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                        SecondaryText(user.email)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = stringResource(user.role.labelRes()),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
            SectionCard(title = stringResource(R.string.account_section_sync), isError = false, icon = R.drawable.ic_status_syncing) {
                val lastSyncAt = uiState.lastSyncAt
                Text(
                    text =
                        if (lastSyncAt == null) {
                            stringResource(R.string.account_never_synced)
                        } else {
                            stringResource(R.string.account_last_sync, lastSyncAt.displayDateTime())
                        },
                    style = MaterialTheme.typography.bodyLarge,
                )
                SecondaryText(
                    if (uiState.unsentCount > 0) {
                        stringResource(R.string.account_unsent, uiState.unsentCount)
                    } else {
                        stringResource(R.string.account_all_sent)
                    },
                )
            }
            SectionCard(title = stringResource(R.string.account_section_app), isError = false, icon = R.drawable.ic_info) {
                Text(stringResource(R.string.account_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge)
            }
            OutlinedButton(
                onClick = { confirmLogout = true },
                enabled = !uiState.isLoggingOut,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (uiState.isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(painterResource(R.drawable.ic_logout), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.account_logout))
                }
            }
        }
    }

    if (confirmLogout) {
        val unsent = uiState.unsentCount
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.logout_title)) },
            text = {
                Text(
                    if (unsent > 0) {
                        stringResource(R.string.logout_message_unsent, unsent)
                    } else {
                        stringResource(R.string.logout_message)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) {
                    Text(
                        text = stringResource(if (unsent > 0) R.string.logout_confirm_delete else R.string.account_logout),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SecondaryText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun initialsOf(name: String): String =
    name
        .split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

private fun Role.labelRes(): Int =
    when (this) {
        Role.FIELD_OFFICER -> R.string.account_role_field_officer
        Role.REVIEWER -> R.string.account_role_reviewer
    }
