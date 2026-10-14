package id.lapor.lh.feature.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.lapor.lh.R
import id.lapor.lh.core.ui.LaporLHMotion
import id.lapor.lh.core.ui.LocalDarkTheme

@Composable
fun LoginRoute(viewModel: LoginViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LoginScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
    )
}

private val dimmedPhoto = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.7f, 0.7f, 0.7f, 1f) })

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val canSubmit = !uiState.isLoading && uiState.email.isNotBlank() && uiState.password.isNotBlank()
    val submit = {
        if (canSubmit) {
            focusManager.clearFocus()
            onSubmit()
        }
    }

    val scrollState = rememberScrollState()
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible) snapshotFlow { scrollState.maxValue }.collect { scrollState.scrollTo(it) }
    }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Image(
            painter = painterResource(R.drawable.login_header),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = if (LocalDarkTheme.current) dimmedPhoto else null,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f),
        )
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 24.dp)
                    .imePadding(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            ) {
                Column(
                    modifier =
                        Modifier
                            .verticalScroll(scrollState)
                            .navigationBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, top = 28.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = colorResource(R.color.logo_background),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(painterResource(R.drawable.ic_logo_mark), contentDescription = null, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (uiState.sessionEnded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.login_session_ended),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        label = { Text(stringResource(R.string.login_email)) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(R.string.login_password)) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text =
                                        stringResource(
                                            if (passwordVisible) R.string.login_hide_password else R.string.login_show_password,
                                        ),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
                    )
                    AnimatedVisibility(
                        visible = uiState.errorRes != null,
                        enter = expandVertically(LaporLHMotion.spatial()) + fadeIn(LaporLHMotion.effects()),
                        exit = shrinkVertically(LaporLHMotion.spatial()) + fadeOut(LaporLHMotion.effects()),
                    ) {
                        var shownRes by remember { mutableIntStateOf(uiState.errorRes ?: 0) }
                        uiState.errorRes?.let { shownRes = it }
                        Text(
                            text = if (shownRes != 0) stringResource(shownRes) else "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = submit,
                        enabled = canSubmit,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.login_submit))
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    CaseStudyNotice(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CaseStudyNotice(modifier: Modifier = Modifier) {
    val organization = stringResource(R.string.bpdlh_full_name)
    val wordmarkTint = if (LocalDarkTheme.current) ColorFilter.tint(MaterialTheme.colorScheme.onSurface) else null
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = stringResource(R.string.login_case_study_for),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { contentDescription = organization },
        ) {
            Image(painterResource(R.drawable.bpdlh_leaves), contentDescription = null, modifier = Modifier.height(30.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.bpdlh_wordmark),
                contentDescription = null,
                colorFilter = wordmarkTint,
                modifier = Modifier.height(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_prototype_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
