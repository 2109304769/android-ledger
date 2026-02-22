package com.androidledger.ui.quickentry

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Wallet
import com.androidledger.ui.theme.WiseForestGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEntryScreen(
    onNavigateToStandard: () -> Unit = {},
    viewModel: QuickEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allWallets by viewModel.allWallets.collectAsStateWithLifecycle()

    val doneButtonColor = WiseForestGreen

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.FlashOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("\u6781\u901F\u8BB0\u8D26")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    TextButton(onClick = onNavigateToStandard) {
                        Text(
                            text = "\u5B8C\u6574\u6A21\u5F0F",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "\u5B8C\u6574\u6A21\u5F0F",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Amount display
                AmountDisplay(
                    amountText = uiState.amountText,
                    calculatedAmount = uiState.calculatedAmount,
                    currency = uiState.selectedCurrency,
                    onCurrencyToggle = viewModel::onCurrencyToggle
                )

                // Source quick cards
                SourceQuickCards(
                    sources = uiState.topSources,
                    wallets = allWallets,
                    selectedSource = uiState.selectedSource,
                    onSourceSelect = viewModel::onSourceSelect
                )

                Spacer(modifier = Modifier.weight(1f))

                // Custom number keypad with integrated Done button
                QuickEntryKeypad(
                    doneButtonColor = doneButtonColor,
                    onDigitPress = viewModel::onDigitPress,
                    onOperatorPress = viewModel::onOperatorPress,
                    onBackspace = viewModel::onBackspace,
                    onClear = viewModel::onClear,
                    onComplete = viewModel::saveTransaction
                )
            }

            // Success overlay message
            AnimatedVisibility(
                visible = uiState.resultMessage != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                SuccessOverlay(
                    message = uiState.resultMessage ?: "",
                    canUndo = uiState.canUndo,
                    onUndo = viewModel::undoLastTransaction
                )
            }
        }
    }
}

// ============================================================================
// Amount Display
// ============================================================================

@Composable
private fun AmountDisplay(
    amountText: String,
    calculatedAmount: Long,
    currency: String,
    onCurrencyToggle: () -> Unit
) {
    val currencySymbol = if (currency == "EUR") "\u20AC" else "\u00A5"
    val hasExpression = amountText.any { it in "+-*/" }
    val displayText = amountText.ifEmpty { "0" }
    val displayColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Currency toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = displayColor.copy(alpha = 0.1f),
                modifier = Modifier.clickable { onCurrencyToggle() }
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = displayColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Amount text
            Text(
                text = displayText,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
                textAlign = TextAlign.Center
            )
        }

        // Show calculated result when there is an expression
        if (hasExpression && calculatedAmount > 0L) {
            val resultStr = formatAmountDisplay(calculatedAmount)
            Text(
                text = "= $currencySymbol$resultStr",
                fontSize = 20.sp,
                color = displayColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatAmountDisplay(amountMinor: Long): String {
    val whole = amountMinor / 100
    val fraction = amountMinor % 100
    return if (fraction == 0L) {
        whole.toString()
    } else {
        String.format("%d.%02d", whole, fraction)
    }
}

// ============================================================================
// Source Quick Cards
// ============================================================================

@Composable
private fun SourceQuickCards(
    sources: List<Source>,
    wallets: List<Wallet>,
    selectedSource: Source?,
    onSourceSelect: (Source) -> Unit
) {
    if (sources.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sources.forEach { source ->
            val isSelected = selectedSource?.id == source.id
            val wallet = wallets.find { it.id == source.walletId }
            val currencySymbol = when (wallet?.currency) {
                "EUR" -> "\u20AC"
                "CNY" -> "\u00A5"
                else -> ""
            }
            val balanceStr = formatAmountDisplay(source.balanceSnapshot)

            QuickSourceCard(
                icon = source.icon ?: getSourceTypeIcon(source.type),
                name = source.name,
                balance = "$currencySymbol$balanceStr",
                isSelected = isSelected,
                onClick = { onSourceSelect(source) }
            )
        }
    }
}

@Composable
private fun QuickSourceCard(
    icon: String,
    name: String,
    balance: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "quick_source_border"
    )

    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = balance,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getSourceTypeIcon(type: String): String {
    return when (type.lowercase()) {
        "bank" -> "\uD83C\uDFE6"
        "ewallet" -> "\uD83D\uDCF1"
        "cash" -> "\uD83D\uDCB5"
        "credit_card" -> "\uD83D\uDCB3"
        else -> "\uD83D\uDCBC"
    }
}

// ============================================================================
// Custom Number Keypad with integrated Done button
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickEntryKeypad(
    doneButtonColor: Color,
    onDigitPress: (String) -> Unit,
    onOperatorPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onComplete: () -> Unit
) {
    val view = LocalView.current

    // Layout:
    // [7][8][9][ + ]
    // [4][5][6][ - ]
    // [1][2][3][   ]
    // [.][0][BS][Done]
    // The Done button spans the right column for rows 3 and 4

    Surface(
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: 7 8 9 +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("7", "8", "9").forEach { key ->
                    KeypadButton(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDigitPress(key)
                        }
                    )
                }
                KeypadButton(
                    key = "+",
                    modifier = Modifier.weight(1f),
                    isOperator = true,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onOperatorPress("+")
                    }
                )
            }

            // Row 2: 4 5 6 -
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("4", "5", "6").forEach { key ->
                    KeypadButton(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDigitPress(key)
                        }
                    )
                }
                KeypadButton(
                    key = "-",
                    modifier = Modifier.weight(1f),
                    isOperator = true,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onOperatorPress("-")
                    }
                )
            }

            // Row 3 and 4: Number keys on left 3 columns, Done button spanning right column
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Left 3 columns: rows 3 and 4
                Column(
                    modifier = Modifier.weight(3f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Row 3: 1 2 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("1", "2", "3").forEach { key ->
                            KeypadButton(
                                key = key,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onDigitPress(key)
                                }
                            )
                        }
                    }
                    // Row 4: . 0 BS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        KeypadButton(
                            key = ".",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onDigitPress(".")
                            }
                        )
                        KeypadButton(
                            key = "0",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onDigitPress("0")
                            }
                        )
                        // Backspace with long-press to clear
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.8f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .combinedClickable(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        onBackspace()
                                    },
                                    onLongClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        onClear()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "\u9000\u683C",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Right column: large Done button spanning 2 rows
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.45f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(doneButtonColor)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onComplete()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "\u5B8C\u6210",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "\u2713",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    key: String,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isOperator)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surface

    val textColor = if (isOperator)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .aspectRatio(1.8f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            fontSize = 22.sp,
            fontWeight = if (isOperator) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

// ============================================================================
// Success Overlay
// ============================================================================

@Composable
private fun SuccessOverlay(
    message: String,
    canUndo: Boolean,
    onUndo: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WiseForestGreen
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (canUndo) {
                TextButton(
                    onClick = onUndo
                ) {
                    Text(
                        text = "\u64A4\u9500",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
