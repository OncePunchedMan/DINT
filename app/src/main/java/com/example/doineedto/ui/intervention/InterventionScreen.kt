package com.example.doineedto.ui.intervention

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.doineedto.R
import com.example.doineedto.data.ReasonValidator
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun InterventionScreen(
    viewModel: InterventionViewModel,
    remainingMillis: Long,
    canContinue: Boolean,
    onKeepLockedReasonSelected: (String) -> Unit,
    onKeepLocked: () -> Unit,
    onContinue: () -> Unit,
    onEmergencySkip: () -> Unit,
) {
    val reason by viewModel.reason.collectAsStateWithLifecycle()
    val isRepeatedDistractionReason by viewModel.isRepeatedDistractionReason.collectAsStateWithLifecycle()
    val hardModeEnabled = viewModel.hardModeEnabled
    val hideLockOutcomes = viewModel.hideLockOutcomes

    val secondsLeft = (remainingMillis / 1000f).roundToLong()
    val allCuratedReasons = remember { (continueReasons + keepLockedReasons).toSet() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isReasonValid = remember(reason, allCuratedReasons) {
        ReasonValidator.isReasonValid(reason, allCuratedReasons)
    }
    val canSubmit = canContinue && isReasonValid && !isRepeatedDistractionReason
    var showMoreContinueReasons by remember { mutableStateOf(false) }
    var showMoreHiddenReasons by remember { mutableStateOf(false) }
    var longPressedReason by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = hardModeEnabled) {}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PromptTitle(onEmergencySkip = onEmergencySkip)
            Text(
                text = stringResource(R.string.intervention_prompt_body),
                style = MaterialTheme.typography.bodyLarge
            )
            if (hideLockOutcomes) {
                Text(
                    text = stringResource(R.string.intervention_possible_reasons),
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    hiddenReasonOptions(showMoreHiddenReasons).forEach { option ->
                        Box(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (keepLockedReasons.contains(option)) {
                                        onKeepLockedReasonSelected(option)
                                    } else {
                                        viewModel.onReasonChanged(option)
                                        coroutineScope.launch {
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    }
                                },
                                onLongClick = { longPressedReason = option },
                            )
                        ) {
                            SuggestionChip(onClick = {}, label = { Text(option) })
                        }
                    }
                }
                if (!showMoreHiddenReasons) {
                    TextButton(onClick = { showMoreHiddenReasons = true }) {
                        Text(stringResource(R.string.show_more))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.intervention_use_device),
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visibleContinueReasons(showMoreContinueReasons).forEach { option ->
                        Box(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    viewModel.onReasonChanged(option)
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                },
                                onLongClick = { longPressedReason = option },
                            )
                        ) {
                            SuggestionChip(onClick = {}, label = { Text(option) })
                        }
                    }
                }
                if (!showMoreContinueReasons) {
                    TextButton(onClick = { showMoreContinueReasons = true }) {
                        Text(stringResource(R.string.show_more))
                    }
                }
                Text(
                    text = stringResource(R.string.intervention_keep_device_locked),
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    keepLockedReasons.forEach { option ->
                        Box(
                            modifier = Modifier.combinedClickable(
                                onClick = { onKeepLockedReasonSelected(option) },
                                onLongClick = { longPressedReason = option },
                            )
                        ) {
                            SuggestionChip(onClick = {}, label = { Text(option) })
                        }
                    }
                }
            }
            OutlinedTextField(
                value = reason,
                onValueChange = viewModel::onReasonChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.intervention_reason_label)) },
                placeholder = { Text(stringResource(R.string.intervention_reason_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // TODO: Make Enter-to-submit optional behind a user setting.
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                isError = reason.isNotBlank() && (!isReasonValid || isRepeatedDistractionReason),
                supportingText = {
                    if (reason.isNotBlank() && isRepeatedDistractionReason) {
                        Text(stringResource(R.string.intervention_reason_repeated))
                    } else if (reason.isNotBlank() && !isReasonValid) {
                        Text(stringResource(R.string.intervention_reason_invalid))
                    }
                }
            )
            OutlinedButton(
                onClick = onKeepLocked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.intervention_keep_it_locked))
            }
            Button(
                onClick = onContinue,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        !canContinue -> stringResource(R.string.intervention_continue_in_seconds, secondsLeft.coerceAtLeast(1))
                        reason.trim().isBlank() -> stringResource(R.string.intervention_enter_reason_to_continue)
                        isRepeatedDistractionReason -> stringResource(R.string.intervention_pick_different_reason)
                        !isReasonValid -> stringResource(R.string.intervention_enter_clearer_reason)
                        else -> stringResource(R.string.intervention_use_my_phone)
                    }
                )
            }
            if (hardModeEnabled) {
                Text(
                    text = stringResource(R.string.intervention_hard_mode_note),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    longPressedReason?.let { pressedReason ->
        ReasonUsageDialog(
            reason = pressedReason,
            viewModel = viewModel,
            onDismiss = { longPressedReason = null },
        )
    }
}

@Composable
private fun ReasonUsageDialog(
    reason: String,
    viewModel: InterventionViewModel,
    onDismiss: () -> Unit,
) {
    var counts by remember(reason) { mutableStateOf(0 to 0) }

    LaunchedEffect(reason) {
        counts = viewModel.reasonUsageCounts(reason)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reason_usage_dialog_title, reason)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.reason_usage_today, counts.first))
                Text(stringResource(R.string.reason_usage_last_hour, counts.second))
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}

@Composable
private fun PromptTitle(onEmergencySkip: () -> Unit) {
    val emergencyTag = "emergency_skip"
    val prefix = stringResource(R.string.prompt_title_prefix)
    val now = stringResource(R.string.prompt_title_now)
    val suffix = stringResource(R.string.prompt_title_suffix)
    val promptTitle = buildAnnotatedString {
        append(prefix)
        pushStringAnnotation(tag = emergencyTag, annotation = emergencyTag)
        pushStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold,
            )
        )
        append(now)
        pop()
        pop()
        append(suffix)
    }

    ClickableText(
        text = promptTitle,
        style = MaterialTheme.typography.headlineMedium.copy(
            color = MaterialTheme.colorScheme.onBackground
        ),
        onClick = { offset ->
            promptTitle.getStringAnnotations(
                tag = emergencyTag,
                start = offset,
                end = offset,
            ).firstOrNull()?.let {
                onEmergencySkip()
            }
        }
    )
}

private fun visibleContinueReasons(showMore: Boolean): List<String> =
    if (showMore) continueReasons else continueReasons.take(6)

private fun hiddenReasonOptions(showMore: Boolean): List<String> =
    if (showMore) mergedReasons else mergedReasons.take(8)

private val continueReasons = listOf(
    "Reply to someone",
    "Check messages",
    "Call someone",
    "Look something up",
    "Use the camera",
    "Check directions",
    "Catch up on social media",
    "Look at memes",
    "Open Instagram",
    "Open TikTok",
    "Open YouTube",
    "Check Reddit",
    "Play music or a podcast",
    "Check my calendar",
    "Open a ticket or booking",
    "Check email",
    "Check my bank",
    "Read the news",
    "Read something",
    "Watch a video",
    "Check the weather",
    "Use notes or tasks",
    "Shop for something",
    "Play a game",
    "Use two-factor authentication",
    "Scan a code",
    "Check a delivery",
    "Order food",
    "Pay for something",
    "Read a document",
    "Join a call",
    "Something else",
)

private val keepLockedReasons = listOf(
    "Just checking",
    "I opened it automatically",
    "I am bored",
    "I want to scroll",
    "I want to check Instagram",
    "I want to check TikTok",
    "I just want dopamine",
    "No real reason",
)

private val mergedReasons = listOf(
    "Reply to someone",
    "Check messages",
    "Just checking",
    "Look something up",
    "I opened it automatically",
    "Use the camera",
    "I am bored",
    "Check directions",
    "Catch up on social media",
    "Look at memes",
    "Open Instagram",
    "I want to check Instagram",
    "Open TikTok",
    "I want to check TikTok",
    "Open YouTube",
    "No real reason",
    "I want to scroll",
    "Play music or a podcast",
    "Check my calendar",
    "Open a ticket or booking",
    "Check email",
    "Check my bank",
    "Read the news",
    "Watch a video",
    "Read something",
    "Check the weather",
    "Use notes or tasks",
    "Shop for something",
    "Play a game",
    "Use two-factor authentication",
    "Scan a code",
    "Check a delivery",
    "Order food",
    "Pay for something",
    "Read a document",
    "Join a call",
    "I just want dopamine",
    "Something else",
)
