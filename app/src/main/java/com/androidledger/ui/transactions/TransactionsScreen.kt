package com.androidledger.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidledger.ui.theme.ExpenseRed
import com.androidledger.ui.theme.IncomeGreen
import com.androidledger.ui.theme.TransferBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToTransaction: (String) -> Unit = {},
    onNavigateToCsvImport: () -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val dateGroups by viewModel.dateGroups.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // Date range picker dialog state
    var showDateRangeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar - clean, minimal, no elevation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u8D26\u5355",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onNavigateToCsvImport,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FileUpload,
                    contentDescription = "\u5BFC\u5165",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Search bar - Wise style: rounded, subtle grey fill, no heavy outline
        WiseSearchBar(
            query = filterState.searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips row
        FilterChipsRow(
            filterState = filterState,
            profiles = profiles,
            wallets = wallets,
            sources = sources,
            categories = categories,
            onTimeRangeChanged = { viewModel.setTimeRange(it) },
            onShowDateRangePicker = { showDateRangeDialog = true },
            onProfileChanged = { viewModel.setProfileFilter(it) },
            onWalletChanged = { viewModel.setWalletFilter(it) },
            onSourceChanged = { viewModel.setSourceFilter(it) },
            onCategoryChanged = { viewModel.setCategoryFilter(it) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Main transaction list
        Box(modifier = Modifier.weight(1f)) {
            if (dateGroups.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    dateGroups.forEach { group ->
                        // Date header
                        item(key = "header_${group.dateLabel}") {
                            DateGroupHeader(group)
                        }
                        // Transaction items
                        items(
                            items = group.items,
                            key = { it.id }
                        ) { item ->
                            TransactionRow(
                                item = item,
                                onClick = { onNavigateToTransaction(item.id) }
                            )
                        }
                    }
                }
            }
        }

        // Bottom summary bar
        SummaryBar(summary = summary)
    }

    // Date range picker dialog
    if (showDateRangeDialog) {
        DateRangePickerDialog(
            initialStartMillis = filterState.customStartDate,
            initialEndMillis = filterState.customEndDate,
            onDismiss = { showDateRangeDialog = false },
            onConfirm = { start, end ->
                viewModel.setCustomDateRange(start, end)
                showDateRangeDialog = false
            }
        )
    }
}

// ---------- Wise-Style Search Bar ----------

@Composable
private fun WiseSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "\u641C\u7D22",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "\u641C\u7D22\u5546\u6237/\u5907\u6CE8/\u91D1\u989D",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "\u6E05\u9664",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------- Filter Chips Row ----------

@Composable
private fun FilterChipsRow(
    filterState: TransactionsFilterState,
    profiles: List<com.androidledger.data.entity.Profile>,
    wallets: List<com.androidledger.data.entity.Wallet>,
    sources: List<com.androidledger.data.entity.Source>,
    categories: Map<String, com.androidledger.data.entity.Category>,
    onTimeRangeChanged: (TimeRange) -> Unit,
    onShowDateRangePicker: () -> Unit,
    onProfileChanged: (String?) -> Unit,
    onWalletChanged: (String?) -> Unit,
    onSourceChanged: (String?) -> Unit,
    onCategoryChanged: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Time range chip - always "active" since time is always selected
        TimeRangeChip(
            selectedTimeRange = filterState.selectedTimeRange,
            onTimeRangeChanged = onTimeRangeChanged,
            onShowDateRangePicker = onShowDateRangePicker
        )

        // Profile chip
        WiseDropdownChip(
            label = when {
                filterState.selectedProfileId != null ->
                    profiles.find { it.id == filterState.selectedProfileId }?.name ?: "\u5168\u90E8"
                else -> "\u8D26\u6237"
            },
            isActive = filterState.selectedProfileId != null,
            options = buildList {
                add(null to "\u5168\u90E8")
                profiles.forEach { add(it.id to "${it.emoji} ${it.name}") }
            },
            onSelected = onProfileChanged
        )

        // Wallet chip
        WiseDropdownChip(
            label = when {
                filterState.selectedWalletId != null ->
                    wallets.find { it.id == filterState.selectedWalletId }?.let { "${it.currency} ${it.name}" }
                        ?: "\u5168\u90E8"
                else -> "\u94B1\u5305"
            },
            isActive = filterState.selectedWalletId != null,
            options = buildList {
                add(null to "\u5168\u90E8")
                wallets.forEach { add(it.id to "${it.currency} ${it.name}") }
            },
            onSelected = onWalletChanged
        )

        // Source chip
        WiseDropdownChip(
            label = when {
                filterState.selectedSourceId != null ->
                    sources.find { it.id == filterState.selectedSourceId }?.name ?: "\u5168\u90E8"
                else -> "\u6765\u6E90"
            },
            isActive = filterState.selectedSourceId != null,
            options = buildList {
                add(null to "\u5168\u90E8")
                sources.forEach { add(it.id to "${it.icon ?: ""} ${it.name}".trim()) }
            },
            onSelected = onSourceChanged
        )

        // Category chip
        WiseDropdownChip(
            label = when {
                filterState.selectedCategoryId != null ->
                    categories[filterState.selectedCategoryId]?.let { "${it.icon} ${it.name}" }
                        ?: "\u5168\u90E8"
                else -> "\u5206\u7C7B"
            },
            isActive = filterState.selectedCategoryId != null,
            options = buildList {
                add(null to "\u5168\u90E8")
                categories.values.forEach { add(it.id to "${it.icon} ${it.name}") }
            },
            onSelected = onCategoryChanged
        )
    }
}

// ---------- Wise-Style Pill Chip ----------

@Composable
private fun WisePillChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.background
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (isActive) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (!isActive) Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(20.dp)
                ) else Modifier
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- Time Range Chip ----------

@Composable
private fun TimeRangeChip(
    selectedTimeRange: TimeRange,
    onTimeRangeChanged: (TimeRange) -> Unit,
    onShowDateRangePicker: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        WisePillChip(
            label = when (selectedTimeRange) {
                TimeRange.THIS_MONTH -> "\u672C\u6708"
                TimeRange.LAST_MONTH -> "\u4E0A\u6708"
                TimeRange.CUSTOM -> "\u81EA\u5B9A\u4E49"
            },
            isActive = true,
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("\u672C\u6708") },
                onClick = {
                    onTimeRangeChanged(TimeRange.THIS_MONTH)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("\u4E0A\u6708") },
                onClick = {
                    onTimeRangeChanged(TimeRange.LAST_MONTH)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("\u81EA\u5B9A\u4E49...") },
                onClick = {
                    expanded = false
                    onShowDateRangePicker()
                }
            )
        }
    }
}

// ---------- Wise Dropdown Chip ----------

@Composable
private fun WiseDropdownChip(
    label: String,
    isActive: Boolean,
    options: List<Pair<String?, String>>,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        WisePillChip(
            label = label,
            isActive = isActive,
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ---------- Date Group Header ----------

@Composable
private fun DateGroupHeader(group: DateGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (group.dailyExpenseTotal > 0) {
            Text(
                text = "\u652F\u51FA ${TransactionsViewModel.formatAmount(group.dailyExpenseTotal, group.dailyExpenseCurrency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- Transaction Row ----------

@Composable
private fun TransactionRow(
    item: TransactionListItem,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Category emoji in soft grey circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.categoryEmoji.isNotEmpty()) {
                    Text(
                        text = item.categoryEmoji,
                        fontSize = 20.sp
                    )
                } else {
                    Text(
                        text = item.merchant.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle: merchant + source
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.merchant.ifEmpty { item.categoryName.ifEmpty { "\u672A\u547D\u540D" } },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!item.isConfirmed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9800)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                if (item.sourceName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.sourceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Right: amount + time
            Column(horizontalAlignment = Alignment.End) {
                val amountColor = when (item.direction) {
                    "IN" -> IncomeGreen
                    "OUT" -> ExpenseRed
                    "TRANSFER" -> TransferBlue
                    else -> MaterialTheme.colorScheme.onSurface
                }
                val prefix = when (item.direction) {
                    "IN" -> "+"
                    "OUT" -> "-"
                    else -> ""
                }
                Text(
                    text = "$prefix${item.amount}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Very thin divider, starts after the emoji circle area
        HorizontalDivider(
            modifier = Modifier.padding(start = 74.dp, end = 20.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

// ---------- Empty State ----------

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Receipt,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\u6682\u65E0\u8D26\u5355\u8BB0\u5F55",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "\u8C03\u6574\u7B5B\u9009\u6761\u4EF6\u6216\u6DFB\u52A0\u65B0\u8D26\u5355",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// ---------- Summary Bar ----------

@Composable
private fun SummaryBar(summary: TransactionsSummary) {
    Column {
        // Thin top border
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryItem(
                label = "\u6536\u5165",
                amount = TransactionsViewModel.formatAmount(summary.totalIncome, "CNY"),
                color = IncomeGreen
            )

            SummaryItem(
                label = "\u652F\u51FA",
                amount = TransactionsViewModel.formatAmount(summary.totalExpense, "CNY"),
                color = ExpenseRed
            )

            val netColor = when {
                summary.netFlow > 0 -> IncomeGreen
                summary.netFlow < 0 -> ExpenseRed
                else -> MaterialTheme.colorScheme.onSurface
            }
            val netPrefix = when {
                summary.netFlow > 0 -> "+"
                summary.netFlow < 0 -> "-"
                else -> ""
            }
            SummaryItem(
                label = "\u51C0\u6D41\u6C34",
                amount = "$netPrefix${TransactionsViewModel.formatAmount(kotlin.math.abs(summary.netFlow), "CNY")}",
                color = netColor
            )
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    amount: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

// ---------- Date Range Picker Dialog ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    initialStartMillis: Long?,
    initialEndMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    var pickingStart by remember { mutableStateOf(true) }
    var selectedStart by remember { mutableStateOf(initialStartMillis) }
    var selectedEnd by remember { mutableStateOf(initialEndMillis) }

    if (pickingStart) {
        val startState = rememberDatePickerState(
            initialSelectedDateMillis = selectedStart
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedStart = startState.selectedDateMillis
                        pickingStart = false
                    },
                    enabled = startState.selectedDateMillis != null
                ) {
                    Text("\u4E0B\u4E00\u6B65")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("\u53D6\u6D88")
                }
            }
        ) {
            Column {
                Text(
                    text = "\u9009\u62E9\u5F00\u59CB\u65E5\u671F",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
                DatePicker(state = startState)
            }
        }
    } else {
        val endState = rememberDatePickerState(
            initialSelectedDateMillis = selectedEnd
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = selectedStart ?: return@TextButton
                        val end = endState.selectedDateMillis ?: return@TextButton
                        // Ensure end is at the end of the day
                        val endOfDay = end + 24 * 60 * 60 * 1000 - 1
                        onConfirm(start, endOfDay)
                    },
                    enabled = endState.selectedDateMillis != null
                ) {
                    Text("\u786E\u5B9A")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = true }) {
                    Text("\u4E0A\u4E00\u6B65")
                }
            }
        ) {
            Column {
                Text(
                    text = "\u9009\u62E9\u7ED3\u675F\u65E5\u671F",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
                DatePicker(state = endState)
            }
        }
    }
}
