package com.androidledger.ui.addtransaction

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidledger.data.entity.Category
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Tag
import com.androidledger.data.entity.Wallet
import com.androidledger.ui.theme.ExpenseRed
import com.androidledger.ui.theme.ExpenseRedLight
import com.androidledger.ui.theme.IncomeGreen
import com.androidledger.ui.theme.IncomeGreenLight
import com.androidledger.ui.theme.TransferBlue
import com.androidledger.ui.theme.TransferBlueLight
import com.androidledger.ui.theme.WiseForestGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allSources by viewModel.allSources.collectAsState()
    val allWallets by viewModel.allWallets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current

    // Listen for save success
    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { success ->
            if (success) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                snackbarHostState.showSnackbar(
                    message = "\u2713 \u5DF2\u8BB0\u5F55",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val tabColor = when (uiState.currentTab) {
        "EXPENSE" -> ExpenseRed
        "INCOME" -> IncomeGreen
        "TRANSFER" -> TransferBlue
        else -> MaterialTheme.colorScheme.primary
    }

    val tabColorLight = when (uiState.currentTab) {
        "EXPENSE" -> ExpenseRedLight
        "INCOME" -> IncomeGreenLight
        "TRANSFER" -> TransferBlueLight
        else -> MaterialTheme.colorScheme.primary
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Scrollable content area (everything above keypad)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Tab bar - Pill-style chips
                TransactionTabBar(
                    currentTab = uiState.currentTab,
                    tabColor = tabColor,
                    onTabChange = viewModel::onTabChange
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Amount display
                AmountDisplay(
                    amountText = uiState.amountText,
                    calculatedAmount = uiState.calculatedAmount,
                    currency = uiState.selectedCurrency,
                    tabColor = tabColor,
                    onCurrencyToggle = viewModel::onCurrencyToggle
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtle divider
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Source quick cards
                SourceQuickCards(
                    label = if (uiState.currentTab == "TRANSFER") "\u8F6C\u51FA\u6765\u6E90" else "\u8D26\u6237",
                    sources = allSources.filter { it.isArchived == 0 },
                    wallets = allWallets,
                    selectedSource = uiState.selectedSource,
                    onSourceSelect = viewModel::onSourceSelect
                )

                // 4. Transfer target (only in transfer mode)
                if (uiState.currentTab == "TRANSFER") {
                    Spacer(modifier = Modifier.height(8.dp))
                    SourceQuickCards(
                        label = "\u8F6C\u5165\u6765\u6E90",
                        sources = allSources.filter {
                            it.isArchived == 0 && it.id != uiState.selectedSource?.id
                        },
                        wallets = allWallets,
                        selectedSource = uiState.selectedTransferTarget,
                        onSourceSelect = viewModel::onTransferTargetSelect
                    )

                    // Cross-currency exchange rate
                    val fromWallet = allWallets.find { it.id == uiState.selectedSource?.walletId }
                    val toWallet = allWallets.find { it.id == uiState.selectedTransferTarget?.walletId }
                    if (fromWallet != null && toWallet != null && fromWallet.currency != toWallet.currency) {
                        CrossCurrencyRateInput(
                            fromCurrency = fromWallet.currency,
                            toCurrency = toWallet.currency,
                            rate = uiState.transferRate,
                            onRateChange = viewModel::onTransferRateChange
                        )
                    }
                }

                // 5. Category grid (not for transfer)
                if (uiState.currentTab != "TRANSFER") {
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryGrid(
                        categories = categories,
                        selectedCategory = uiState.selectedCategory,
                        tabColor = tabColor,
                        onCategorySelect = viewModel::onCategorySelect
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 6. Optional fields
                OptionalFieldsSection(
                    showOptionalFields = uiState.showOptionalFields,
                    onToggle = viewModel::onToggleOptionalFields,
                    merchant = uiState.merchant,
                    onMerchantChange = viewModel::onMerchantChange,
                    description = uiState.description,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    occurredAt = uiState.occurredAt,
                    onOccurredAtChange = viewModel::onOccurredAtChange,
                    tags = tags,
                    selectedTags = uiState.selectedTags,
                    onTagToggle = viewModel::onTagToggle
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 7. Custom number keypad (fixed at bottom)
            CustomKeypad(
                tabColor = tabColor,
                tabColorLight = tabColorLight,
                onDigitPress = viewModel::onDigitPress,
                onOperatorPress = viewModel::onOperatorPress,
                onBackspace = viewModel::onBackspace,
                onClear = viewModel::onClear,
                onComplete = viewModel::onSave
            )
        }
    }
}

// ============================================================================
// 1. Tab Bar - Wise-style Pill Chips
// ============================================================================

@Composable
private fun TransactionTabBar(
    currentTab: String,
    tabColor: Color,
    onTabChange: (String) -> Unit
) {
    val tabs = listOf(
        "EXPENSE" to "\u652F\u51FA",
        "INCOME" to "\u6536\u5165",
        "TRANSFER" to "\u8F6C\u8D26"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tabs.forEach { (key, label) ->
            val isSelected = currentTab == key
            val chipColor = when (key) {
                "EXPENSE" -> ExpenseRed
                "INCOME" -> IncomeGreen
                "TRANSFER" -> TransferBlue
                else -> MaterialTheme.colorScheme.primary
            }
            val chipColorLight = when (key) {
                "EXPENSE" -> ExpenseRed.copy(alpha = 0.1f)
                "INCOME" -> IncomeGreen.copy(alpha = 0.1f)
                "TRANSFER" -> TransferBlue.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            }

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) chipColorLight else Color.Transparent,
                animationSpec = tween(200),
                label = "tab_bg_$key"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) chipColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                animationSpec = tween(200),
                label = "tab_border_$key"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "tab_text_$key"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onTabChange(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

// ============================================================================
// 2. Amount Display - Wise-style Clean & Prominent
// ============================================================================

@Composable
private fun AmountDisplay(
    amountText: String,
    calculatedAmount: Long,
    currency: String,
    tabColor: Color,
    onCurrencyToggle: () -> Unit
) {
    val currencySymbol = if (currency == "EUR") "\u20AC" else "\u00A5"
    val hasExpression = amountText.any { it in "+-*/" }
    val displayText = amountText.ifEmpty { "0" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Currency toggle - subtle tappable pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clickable { onCurrencyToggle() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currency,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Amount text - large, light weight, Wise style
        Text(
            text = displayText,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            letterSpacing = (-1).sp
        )

        // Show calculated result when there is an expression
        if (hasExpression && calculatedAmount > 0L) {
            val resultStr = formatAmount(calculatedAmount)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "= $currencySymbol$resultStr",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

private fun formatAmount(amountMinor: Long): String {
    val whole = amountMinor / 100
    val fraction = amountMinor % 100
    return if (fraction == 0L) {
        whole.toString()
    } else {
        String.format("%d.%02d", whole, fraction)
    }
}

// ============================================================================
// 3. Source Quick Cards - Wise-style Clean Cards
// ============================================================================

@Composable
private fun SourceQuickCards(
    label: String,
    sources: List<Source>,
    wallets: List<Wallet>,
    selectedSource: Source?,
    onSourceSelect: (Source) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            letterSpacing = 0.5.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sources.forEach { source ->
                val isSelected = selectedSource?.id == source.id
                val wallet = wallets.find { it.id == source.walletId }
                val currencySymbol = when (wallet?.currency) {
                    "EUR" -> "\u20AC"
                    "CNY" -> "\u00A5"
                    else -> ""
                }
                val balanceStr = formatAmount(source.balanceSnapshot)

                SourceCard(
                    icon = source.icon ?: getSourceTypeIcon(source.type),
                    name = source.name,
                    balance = "$currencySymbol$balanceStr",
                    isSelected = isSelected,
                    onClick = { onSourceSelect(source) }
                )
            }
        }
    }
}

@Composable
private fun SourceCard(
    icon: String,
    name: String,
    balance: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) WiseForestGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        animationSpec = tween(200),
        label = "source_border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(200),
        label = "source_border_width"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) WiseForestGreen.copy(alpha = 0.04f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "source_bg"
    )

    Surface(
        modifier = Modifier
            .width(100.dp)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon in a soft circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = balance,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp
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
// 4. Cross-Currency Rate Input (for transfers)
// ============================================================================

@Composable
private fun CrossCurrencyRateInput(
    fromCurrency: String,
    toCurrency: String,
    rate: String,
    onRateChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u6C47\u7387: 1 $fromCurrency = ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = rate,
            onValueChange = onRateChange,
            modifier = Modifier.width(100.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = " $toCurrency",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// 5. Category Grid - Wise-style Clean Grid
// ============================================================================

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedCategory: Category?,
    tabColor: Color,
    onCategorySelect: (Category) -> Unit
) {
    if (categories.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "\u5206\u7C7B",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            letterSpacing = 0.5.sp
        )
        // Use a fixed-height grid to show categories in 4 columns
        val rows = (categories.size + 3) / 4
        val gridHeight = (rows * 84).dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            userScrollEnabled = false
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryItem(
                    category = category,
                    isSelected = selectedCategory?.id == category.id,
                    tabColor = tabColor,
                    onClick = { onCategorySelect(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    tabColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else Color.Transparent,
        animationSpec = tween(200),
        label = "cat_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "cat_border"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emoji in a soft grey circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.icon,
                fontSize = 22.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// 6. Optional Fields - Wise-style Clean Expandable
// ============================================================================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun OptionalFieldsSection(
    showOptionalFields: Boolean,
    onToggle: () -> Unit,
    merchant: String,
    onMerchantChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    occurredAt: Long,
    onOccurredAtChange: (Long) -> Unit,
    tags: List<Tag>,
    selectedTags: List<Tag>,
    onTagToggle: (Tag) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Toggle row - clean text button style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showOptionalFields) "\u6536\u8D77\u8BE6\u60C5"
                else "\u6DFB\u52A0\u5907\u6CE8 / \u5546\u6237 / \u6807\u7B7E",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Normal
            )
            Icon(
                imageVector = if (showOptionalFields) Icons.Default.ExpandLess
                else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(
            visible = showOptionalFields,
            enter = expandVertically(animationSpec = tween(250)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Merchant
                OutlinedTextField(
                    value = merchant,
                    onValueChange = onMerchantChange,
                    label = { Text("\u5546\u6237") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("\u5907\u6CE8") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // DateTime picker
                DateTimeSelector(
                    occurredAt = occurredAt,
                    onOccurredAtChange = onOccurredAtChange
                )

                // Tags
                if (tags.isNotEmpty()) {
                    Text(
                        text = "\u6807\u7B7E",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            val isSelected = selectedTags.any { it.id == tag.id }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTagToggle(tag) },
                                label = {
                                    Text(
                                        tag.name,
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeSelector(
    occurredAt: Long,
    onOccurredAtChange: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(occurredAt))
    val timeStr = timeFormat.format(Date(occurredAt))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Date button - clean text button style
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            onClick = { showDatePicker = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        // Time button
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            onClick = { showTimePicker = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = occurredAt
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        val existingCal = Calendar.getInstance().apply { timeInMillis = occurredAt }
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, existingCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, existingCal.get(Calendar.MINUTE))
                            set(Calendar.SECOND, existingCal.get(Calendar.SECOND))
                        }
                        onOccurredAtChange(newCal.timeInMillis)
                    }
                    showDatePicker = false
                }) {
                    Text("\u786E\u5B9A")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("\u53D6\u6D88")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = occurredAt }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\u9009\u62E9\u65F6\u95F4",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("\u53D6\u6D88")
                        }
                        TextButton(onClick = {
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = occurredAt
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                            }
                            onOccurredAtChange(newCal.timeInMillis)
                            showTimePicker = false
                        }) {
                            Text("\u786E\u5B9A")
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. Custom Number Keypad - Wise-style Clean Flat Buttons
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomKeypad(
    tabColor: Color,
    tabColorLight: Color,
    onDigitPress: (String) -> Unit,
    onOperatorPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onComplete: () -> Unit
) {
    val view = LocalView.current

    // Keypad layout: 4 rows x 4 columns
    // [7][8][9][+]
    // [4][5][6][-]
    // [1][2][3][x]
    // [.][0][BS][Done]
    val keys = listOf(
        listOf("7", "8", "9", "+"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "\u00D7"),
        listOf(".", "0", "\u232B", "\u5B8C\u6210")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Subtle top divider
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(2.dp))

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEach { key ->
                    val isOperator = key in listOf("+", "-", "\u00D7")
                    val isBackspace = key == "\u232B"
                    val isComplete = key == "\u5B8C\u6210"
                    val isDigit = key.length == 1 && (key[0].isDigit() || key == ".")

                    val bgColor = when {
                        isComplete -> WiseForestGreen
                        isOperator -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    }
                    val textColor = when {
                        isComplete -> Color.White
                        isOperator -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.8f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .then(
                                if (isBackspace) {
                                    Modifier.combinedClickable(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            onBackspace()
                                        },
                                        onLongClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            onClear()
                                        }
                                    )
                                } else {
                                    Modifier.clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        when {
                                            isComplete -> onComplete()
                                            isOperator -> {
                                                val op = when (key) {
                                                    "\u00D7" -> "*"
                                                    else -> key
                                                }
                                                onOperatorPress(op)
                                            }
                                            isDigit -> onDigitPress(key)
                                        }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBackspace) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = if (isComplete) "\u5B8C\u6210" else key,
                                fontSize = when {
                                    isComplete -> 16.sp
                                    isOperator -> 20.sp
                                    else -> 20.sp
                                },
                                fontWeight = when {
                                    isComplete -> FontWeight.SemiBold
                                    isOperator -> FontWeight.Medium
                                    else -> FontWeight.Normal
                                },
                                color = textColor,
                                letterSpacing = if (isComplete) 2.sp else 0.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }
}
