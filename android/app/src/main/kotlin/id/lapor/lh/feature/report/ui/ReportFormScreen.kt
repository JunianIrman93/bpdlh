package id.lapor.lh.feature.report.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import id.lapor.lh.R
import id.lapor.lh.feature.report.domain.Attachment
import id.lapor.lh.feature.report.domain.AttachmentKind
import id.lapor.lh.feature.report.domain.Category
import id.lapor.lh.feature.report.domain.ReportField
import id.lapor.lh.feature.report.domain.ReportLocation
import id.lapor.lh.feature.report.domain.ReportRules
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

private val documentMimeTypes = ReportRules.mimeTypesByKind.getValue(AttachmentKind.DOCUMENT).toTypedArray()
private val fileMimeTypes = arrayOf("text/plain", "text/csv", "text/comma-separated-values")

@Composable
fun ReportFormRoute(
    onExit: () -> Unit,
    viewModel: ReportFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                ReportFormEvent.Exit -> onExit()
                is ReportFormEvent.Message -> snackbar.showSnackbar(context.getString(event.res))
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.saveDraft() }
    BackHandler { viewModel.saveAndExit() }

    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { viewModel.onPhotoCaptured(it) }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { viewModel.addAttachment(AttachmentKind.IMAGE, it.toString()) }
        }
    val document =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.addAttachment(AttachmentKind.DOCUMENT, it.toString()) }
        }
    val file =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.addAttachment(AttachmentKind.FILE, it.toString()) }
        }
    val locationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[Manifest.permission.ACCESS_FINE_LOCATION] ==
                true
            ) {
                viewModel.captureLocation()
            } else {
                viewModel.onLocationPermissionDenied()
            }
        }

    ReportFormScreen(
        uiState = uiState,
        snackbar = snackbar,
        actions =
            ReportFormActions(
                onBack = viewModel::saveAndExit,
                onTitleChange = viewModel::onTitleChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                onCategoryChange = viewModel::onCategoryChange,
                onActivityDateChange = viewModel::onActivityDateChange,
                onCaptureLocation = { viewModel.captureLocation() },
                onRequestLocationPermission = {
                    locationPermission.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
                onOpenAppSettings = { context.openAppSettings() },
                onOpenLocationSettings = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                onTakePhoto = { camera.launch(Uri.parse(viewModel.newCameraUri())) },
                onPickImage = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onPickDocument = { document.launch(documentMimeTypes) },
                onPickFile = { file.launch(fileMimeTypes) },
                onRemoveAttachment = viewModel::removeAttachment,
                onSaveDraft = viewModel::saveAndExit,
                onSubmit = viewModel::submit,
                onDeleteDraft = viewModel::deleteDraft,
            ),
    )
}

private class ReportFormActions(
    val onBack: () -> Unit,
    val onTitleChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onCategoryChange: (Category) -> Unit,
    val onActivityDateChange: (LocalDate) -> Unit,
    val onCaptureLocation: () -> Unit,
    val onRequestLocationPermission: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onOpenLocationSettings: () -> Unit,
    val onTakePhoto: () -> Unit,
    val onPickImage: () -> Unit,
    val onPickDocument: () -> Unit,
    val onPickFile: () -> Unit,
    val onRemoveAttachment: (String) -> Unit,
    val onSaveDraft: () -> Unit,
    val onSubmit: () -> Unit,
    val onDeleteDraft: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFormScreen(
    uiState: ReportFormUiState,
    snackbar: SnackbarHostState,
    actions: ReportFormActions,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.isNew) R.string.form_title_new else R.string.form_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (uiState.isLoaded) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            OutlinedButton(
                                onClick = actions.onSaveDraft,
                                enabled = !uiState.isBusy,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { Text(stringResource(R.string.form_save_draft)) }
                            Button(
                                onClick = actions.onSubmit,
                                enabled = !uiState.isBusy && !uiState.isAddingAttachment && !uiState.isLocating,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { Text(stringResource(R.string.form_submit)) }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (!uiState.isLoaded) return@Scaffold
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextFields(uiState, actions)
            CategoryField(uiState, actions.onCategoryChange)
            DateField(uiState, actions.onActivityDateChange)
            LocationSection(uiState, actions)
            AttachmentSection(uiState, actions)
            if (uiState.isExisting) {
                TextButton(
                    onClick = { confirmDelete = true },
                    enabled = !uiState.isBusy,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(R.string.form_delete_draft), color = MaterialTheme.colorScheme.error) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.form_delete_confirm_title)) },
            text = { Text(stringResource(R.string.form_delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDeleteDraft()
                }) { Text(stringResource(R.string.form_delete_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun TextFields(
    uiState: ReportFormUiState,
    actions: ReportFormActions,
) {
    val draft = uiState.draft
    val titleError = uiState.errors[ReportField.TITLE]
    OutlinedTextField(
        value = draft.title,
        onValueChange = { if (it.length <= ReportRules.TITLE_MAX) actions.onTitleChange(it) },
        label = { Text(stringResource(R.string.form_field_title)) },
        singleLine = true,
        isError = titleError != null,
        supportingText = {
            Text(
                titleError?.let { stringResource(it.messageRes()) }
                    ?: stringResource(R.string.form_counter, draft.title.trim().length, ReportRules.TITLE_MAX),
            )
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    val descriptionError = uiState.errors[ReportField.DESCRIPTION]
    OutlinedTextField(
        value = draft.description,
        onValueChange = { if (it.length <= ReportRules.DESCRIPTION_MAX) actions.onDescriptionChange(it) },
        label = { Text(stringResource(R.string.form_field_description)) },
        minLines = 4,
        isError = descriptionError != null,
        supportingText = {
            Text(
                descriptionError?.let { stringResource(it.messageRes()) }
                    ?: stringResource(R.string.form_counter, draft.description.trim().length, ReportRules.DESCRIPTION_MAX),
            )
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CategoryField(
    uiState: ReportFormUiState,
    onCategoryChange: (Category) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val error = uiState.errors[ReportField.CATEGORY]
    PickerField(
        label = stringResource(R.string.form_field_category),
        icon = uiState.draft.category.iconRes(),
        value =
            uiState.draft.category
                ?.let { stringResource(it.labelRes()) }
                .orEmpty(),
        error = error?.let { stringResource(it.messageRes()) },
        onClick = { open = true },
    )
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.form_field_category)) },
            text = {
                Column {
                    Category.entries.forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .selectable(
                                        selected = uiState.draft.category == category,
                                        role = Role.RadioButton,
                                        onClick = {
                                            onCategoryChange(category)
                                            open = false
                                        },
                                    ),
                        ) {
                            RadioButton(selected = uiState.draft.category == category, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(category.labelRes()), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun DateField(
    uiState: ReportFormUiState,
    onActivityDateChange: (LocalDate) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val error = uiState.errors[ReportField.ACTIVITY_DATE]
    PickerField(
        label = stringResource(R.string.form_field_date),
        icon = R.drawable.ic_calendar,
        value =
            uiState.draft.activityDate
                ?.display()
                .orEmpty(),
        error = error?.let { stringResource(it.messageRes()) },
        onClick = { open = true },
    )
    if (open) {
        DateDialog(uiState.draft.activityDate, onActivityDateChange, onClose = { open = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(
    selected: LocalDate?,
    onActivityDateChange: (LocalDate) -> Unit,
    onClose: () -> Unit,
) {
    val today = remember { ReportRules.today(System.currentTimeMillis()) }
    val earliest = ReportRules.earliestActivityDate(today)
    val state =
        remember {
            DatePickerState(
                locale = indonesian,
                initialSelectedDateMillis = (selected ?: today).toUtcMillis(),
                yearRange = earliest.year..today.year,
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            val date = utcTimeMillis.toUtcDate()
                            return !date.isBefore(earliest) && !date.isAfter(today)
                        }

                        override fun isSelectableYear(year: Int): Boolean = year in earliest.year..today.year
                    },
            )
        }
    DatePickerDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onActivityDateChange(it.toUtcDate()) }
                    onClose()
                },
            ) { Text(stringResource(R.string.action_continue)) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    stringResource(R.string.form_date_dialog_title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            headline = {
                Text(
                    state.selectedDateMillis
                        ?.toUtcDate()
                        ?.display()
                        .orEmpty(),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                )
            },
            showModeToggle = false,
        )
    }
}

@Composable
private fun PickerField(
    label: String,
    @DrawableRes icon: Int,
    value: String,
    error: String?,
    onClick: () -> Unit,
) {
    val description = listOf(label, value, error.orEmpty()).filter { it.isNotEmpty() }.joinToString(", ")
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        )
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
                    .semantics { contentDescription = description },
        )
    }
}

@Composable
private fun LocationSection(
    uiState: ReportFormUiState,
    actions: ReportFormActions,
) {
    val context = LocalContext.current
    var showRationale by rememberSaveable { mutableStateOf(false) }
    val location = uiState.draft.location
    val error = uiState.errors[ReportField.LOCATION]
    SectionCard(title = stringResource(R.string.form_section_location), isError = error != null, icon = R.drawable.ic_location) {
        if (location != null) LocationDetails(location)
        when (uiState.locationProblem) {
            LocationProblem.DENIED -> Notice(stringResource(R.string.form_location_denied), isError = true)
            LocationProblem.DISABLED -> Notice(stringResource(R.string.form_location_disabled), isError = true)
            LocationProblem.TIMEOUT -> Notice(stringResource(R.string.form_location_timeout), isError = true)
            null -> Unit
        }
        if (error != null && uiState.locationProblem == null) Notice(stringResource(error.messageRes()), isError = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(
                onClick = {
                    if (context.hasFineLocation()) actions.onCaptureLocation() else showRationale = true
                },
                enabled = !uiState.isLocating,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.height(48.dp),
            ) {
                if (uiState.isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.form_location_searching))
                } else {
                    Icon(painterResource(R.drawable.ic_location), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(if (location == null) R.string.form_location_capture else R.string.form_location_retake))
                }
            }
            when (uiState.locationProblem) {
                LocationProblem.DENIED -> {
                    TextButton(onClick = actions.onOpenAppSettings) { Text(stringResource(R.string.form_open_settings)) }
                }

                LocationProblem.DISABLED -> {
                    TextButton(onClick = actions.onOpenLocationSettings) { Text(stringResource(R.string.form_open_settings)) }
                }

                else -> {
                    Unit
                }
            }
        }
    }
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.form_location_rationale_title)) },
            text = { Text(stringResource(R.string.form_location_rationale_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    actions.onRequestLocationPermission()
                }) { Text(stringResource(R.string.action_continue)) }
            },
            dismissButton = { TextButton(onClick = { showRationale = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
internal fun LocationDetails(location: ReportLocation) {
    Text(
        text = String.format(Locale.US, "%.6f, %.6f", location.latitude, location.longitude),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.form_location_accuracy, location.accuracyM.toInt(), location.capturedAt.displayDateTime()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (location.accuracyM > ReportRules.POOR_ACCURACY_M) Notice(stringResource(R.string.form_location_poor_accuracy), isError = false)
    if (location.isMock) Notice(stringResource(R.string.form_location_mock), isError = false)
}

@Composable
private fun AttachmentSection(
    uiState: ReportFormUiState,
    actions: ReportFormActions,
) {
    val context = LocalContext.current
    val error = uiState.errors[ReportField.ATTACHMENTS]
    val canAdd = !uiState.isAddingAttachment && uiState.attachments.size < ReportRules.MAX_ATTACHMENTS
    SectionCard(title = stringResource(R.string.form_section_attachments), isError = error != null, icon = R.drawable.ic_file) {
        Text(
            text =
                stringResource(
                    R.string.form_attachment_summary,
                    uiState.attachments.size,
                    ReportRules.MAX_ATTACHMENTS,
                    Formatter.formatShortFileSize(context, uiState.attachments.sumOf { it.sizeBytes }),
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (error != null) Notice(stringResource(error.messageRes()), isError = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AttachButton(stringResource(R.string.form_attach_photo), R.drawable.ic_camera, canAdd, actions.onTakePhoto, Modifier.weight(1f))
            AttachButton(stringResource(R.string.form_attach_image), R.drawable.ic_image, canAdd, actions.onPickImage, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AttachButton(
                stringResource(R.string.form_attach_document),
                R.drawable.ic_document,
                canAdd,
                actions.onPickDocument,
                Modifier.weight(1f),
            )
            AttachButton(stringResource(R.string.form_attach_file), R.drawable.ic_file, canAdd, actions.onPickFile, Modifier.weight(1f))
        }
        if (uiState.isAddingAttachment) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.form_attachment_saving), style = MaterialTheme.typography.bodyMedium)
            }
        }
        uiState.attachments.forEach { attachment ->
            AttachmentRow(attachment = attachment, onRemove = { actions.onRemoveAttachment(attachment.id) })
        }
    }
}

@Composable
private fun AttachButton(
    label: String,
    @DrawableRes icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier.height(52.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun AttachmentRow(
    attachment: Attachment,
    onRemove: (() -> Unit)?,
) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (attachment.isImage && attachment.localPath.isNotEmpty()) {
            AsyncImage(
                model = File(attachment.localPath),
                contentDescription = attachment.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(attachment.kind.iconRes()), contentDescription = null)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(attachment.fileName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = Formatter.formatShortFileSize(context, attachment.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onRemove != null) {
            val description = stringResource(R.string.form_attachment_remove_description, attachment.fileName)
            IconButton(onClick = onRemove) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = description)
            }
        }
    }
}

@Composable
internal fun SectionCard(
    title: String,
    isError: Boolean,
    @DrawableRes icon: Int? = null,
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border =
            BorderStroke(
                1.dp,
                if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
internal fun Notice(
    text: String,
    isError: Boolean,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal val Attachment.isImage: Boolean
    get() = kind == AttachmentKind.PHOTO || kind == AttachmentKind.IMAGE

@DrawableRes
private fun AttachmentKind.iconRes(): Int =
    when (this) {
        AttachmentKind.PHOTO -> R.drawable.ic_camera
        AttachmentKind.IMAGE -> R.drawable.ic_image
        AttachmentKind.DOCUMENT -> R.drawable.ic_document
        AttachmentKind.FILE -> R.drawable.ic_file
    }

private fun LocalDate.toUtcMillis(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

private fun Long.toUtcDate(): LocalDate = LocalDate.ofEpochDay(Math.floorDiv(this, 86_400_000L))

private fun Context.hasFineLocation(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}
