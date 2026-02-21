package com.androidledger.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidledger.ui.theme.ExpenseRed
import com.androidledger.ui.theme.IncomeGreen
import com.androidledger.ui.theme.TransferBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransaction: (String) -> Unit = {},
    onNavigateToAddTransaction: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val balanceHidden by viewModel.balanceHidden.collectAsStateWithLifecycle()

    var sourcesExpanded by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\u9996\u9875") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Notification icon with unconfirmed badge
                    if (uiState.unconfirmedCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (uiState.unconfirmedCount > 99) "99+"
                                        else uiState.unconfirmedCount.toString(),
                                        fontSize = 10.sp
                                    )
                                }
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "\u672A\u786E\u8BA4\u4EA4\u6613",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "\u901A\u77E5",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "\u5FEB\u901F\u8BB0\u8D26")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // -- Account Switcher Chips --
            item {
                AccountSwitcherRow(
                    profiles = uiState.profiles,
                    selectedProfileId = uiState.selectedProfileId,
                    onSelect = { viewModel.selectProfile(it) }
                )
            }

            // -- Monthly Overview Card --
            item {
                MonthlyOverviewCard(
                    monthLabel = uiState.monthLabel,
                    monthlyIncome = uiState.monthlyIncome,
                    monthlyExpense = uiState.monthlyExpense,
                    monthlyNet = uiState.monthlyNet,
                    currency = uiState.primaryCurrency,
                    balanceHidden = balanceHidden
                )
            }

            // -- Wallet Balance Cards --
            if (uiState.walletItems.isNotEmpty()) {
                item {
                    SectionHeader(title = "\u94B1\u5305\u603B\u89C8")
                    WalletCardsRow(
                        wallets = uiState.walletItems,
                        balanceHidden = balanceHidden,
                        showProfileName = uiState.selectedProfileId == null
                    )
                }
            }

            // -- Source Balances (collapsible) --
            if (uiState.sourceItems.isNotEmpty()) {
                item {
                    SourceSectionHeader(
                        expanded = sourcesExpanded,
                        onToggle = { sourcesExpanded = !sourcesExpanded },
                        onToggleHidden = { viewModel.toggleBalanceHidden() },
                        balanceHidden = balanceHidden
                    )
                }

                item {
                    AnimatedVisibility(
                        visible = sourcesExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            uiState.sourceItems.forEach { source ->
                                SourceRow(
                                    source = source,
                                    balanceHidden = balanceHidden,
                                    showProfileName = uiState.selectedProfileId == null
                                )
                            }
                        }
                    }
                }
            }

            // -- Category Pie Chart --
            item {
                SectionHeader(title = "\u672C\u6708\u652F\u51FA\u5206\u7C7B")
                if (uiState.categoryExpenses.isEmpty()) {
                    EmptyStateCard(message = "\u672C\u6708\u6682\u65E0\u652F\u51FA\u8BB0\u5F55")
                } else {
                    CategoryPieChartSection(
                        categories = uiState.categoryExpenses,
                        currency = uiState.primaryCurrency,
                        balanceHidden = balanceHidden
                    )
                }
            }

            // -- Recent Transactions --
            item {
                SectionHeader(title = "\u6700\u8FD1\u4EA4\u6613")
            }

            if (uiState.transactionGroups.isEmpty()) {
                item {
                    EmptyStateCard(message = "\u6682\u65E0\u4EA4\u6613\u8BB0\u5F55")
                }
            } else {
                uiState.transactionGroups.forEach { group ->
                    item {
                        TransactionDateHeader(
                            dateLabel = group.dateLabel,
                            dailyTotal = group.dailyTotal,
                            currency = group.currency,
                            balanceHidden = balanceHidden
                        )
                    }
                    items(
                        items = group.transactions,
                        key = { it.transactionId }
                    ) { tx ->
                        TransactionRow(
                            transaction = tx,
                            showProfileName = uiState.selectedProfileId == null,
                            balanceHidden = balanceHidden,
                            onClick = { onNavigateToTransaction(tx.transactionId) }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// Account Switcher
// ============================================================

@Composable
private fun AccountSwitcherRow(
    profiles: List<com.androidledger.data.entity.Profile>,
    selectedProfileId: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        FilterChip(
            selected = selectedProfileId == null,
            onClick = { onSelect(null) },
            label = { Text("\u5168\u90E8") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        // Per-profile chips
        profiles.forEach { profile ->
            FilterChip(
                selected = selectedProfileId == profile.id,
                onClick = { onSelect(profile.id) },
                label = { Text("${profile.emoji} ${profile.name}") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// ============================================================
// Monthly Overview Card
// ============================================================

@Composable
private fun MonthlyOverviewCard(
    monthLabel: String,
    monthlyIncome: Long,
    monthlyExpense: Long,
    monthlyNet: Long,
    currency: String,
    balanceHidden: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "\u672C\u6708\u6536\u5165",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (balanceHidden) "****"
                        else DashboardViewModel.formatAmountAbs(monthlyIncome, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
                // Expense
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "\u672C\u6708\u652F\u51FA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (balanceHidden) "****"
                        else DashboardViewModel.formatAmountAbs(monthlyExpense, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
                // Net
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "\u51C0\u6D41\u6C34",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (balanceHidden) "****"
                        else DashboardViewModel.formatAmount(monthlyNet, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (monthlyNet >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }
        }
    }
}

// ============================================================
// Section Header
// ============================================================

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.onSurface
    )
}

// ============================================================
// Wallet Cards (horizontal scroll)
// ============================================================

@Composable
private fun WalletCardsRow(
    wallets: List<WalletDisplayItem>,
    balanceHidden: Boolean,
    showProfileName: Boolean
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(wallets, key = { it.walletId }) { wallet ->
            WalletCard(
                wallet = wallet,
                balanceHidden = balanceHidden,
                showProfileName = showProfileName
            )
        }
    }
}

@Composable
private fun WalletCard(
    wallet: WalletDisplayItem,
    balanceHidden: Boolean,
    showProfileName: Boolean
) {
    Card(
        modifier = Modifier
            .width(180.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (showProfileName && wallet.profileName.isNotEmpty()) {
                Text(
                    text = wallet.profileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = wallet.walletName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (balanceHidden) "****"
                else DashboardViewModel.formatAmountAbs(wallet.totalBalance, wallet.currency),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${wallet.sources.size}\u4E2A\u8D26\u6237",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================
// Source Balances (collapsible)
// ============================================================

@Composable
private fun SourceSectionHeader(
    expanded: Boolean,
    onToggle: () -> Unit,
    onToggleHidden: () -> Unit,
    balanceHidden: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u8D26\u6237\u4F59\u989D",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onToggleHidden, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (balanceHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (balanceHidden) "\u663E\u793A\u4F59\u989D" else "\u9690\u85CF\u4F59\u989D",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "\u6536\u8D77" else "\u5C55\u5F00",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SourceRow(
    source: SourceDisplayItem,
    balanceHidden: Boolean,
    showProfileName: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Source icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = source.icon,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (showProfileName && source.profileName.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    ProfileTag(name = source.profileName)
                }
            }
            Text(
                text = source.walletName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (balanceHidden) "****"
            else DashboardViewModel.formatAmountAbs(source.balance, source.currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================================
// Category Pie Chart
// ============================================================

@Composable
private fun CategoryPieChartSection(
    categories: List<CategoryExpenseItem>,
    currency: String,
    balanceHidden: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pie chart
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                PieChart(
                    categories = categories,
                    modifier = Modifier.size(120.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Legend - top 5
            Column(modifier = Modifier.weight(1f)) {
                categories.take(5).forEach { cat ->
                    CategoryLegendRow(
                        category = cat,
                        currency = currency,
                        balanceHidden = balanceHidden
                    )
                }
                if (categories.size > 5) {
                    Text(
                        text = "\u2026\u53CA\u5176\u4ED6${categories.size - 5}\u4E2A\u5206\u7C7B",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PieChart(
    categories: List<CategoryExpenseItem>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val strokeWidth = canvasSize * 0.22f
        var startAngle = -90f

        if (categories.isEmpty()) {
            // Empty state circle
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = (canvasSize - strokeWidth) / 2f,
                style = Stroke(width = strokeWidth)
            )
            return@Canvas
        }

        categories.forEach { cat ->
            val sweepAngle = cat.percentage / 100f * 360f
            val color = parseColor(cat.color)
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth),
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CategoryLegendRow(
    category: CategoryExpenseItem,
    currency: String,
    balanceHidden: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(parseColor(category.color))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${category.emoji} ${category.name}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (balanceHidden) "****"
            else DashboardViewModel.formatAmountAbs(category.amount, currency),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${String.format("%.1f", category.percentage)}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ============================================================
// Recent Transactions
// ============================================================

@Composable
private fun TransactionDateHeader(
    dateLabel: String,
    dailyTotal: Long,
    currency: String,
    balanceHidden: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (dailyTotal > 0) {
            Text(
                text = if (balanceHidden) "\u652F\u51FA: ****"
                else "\u652F\u51FA: ${DashboardViewModel.formatAmountAbs(dailyTotal, currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = ExpenseRed
            )
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionDisplayItem,
    showProfileName: Boolean,
    balanceHidden: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category emoji
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.categoryEmoji,
                fontSize = 20.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Merchant and metadata
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (showProfileName && transaction.profileName.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    ProfileTag(name = transaction.profileName)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " \u00B7 ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Amount and time
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (balanceHidden) "****" else transaction.amount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = when (transaction.direction) {
                    "IN" -> IncomeGreen
                    "OUT" -> ExpenseRed
                    "TRANSFER" -> TransferBlue
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = transaction.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

// ============================================================
// Shared Components
// ============================================================

@Composable
private fun ProfileTag(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp)
        )
    }
}

// ============================================================
// Utility
// ============================================================

private fun parseColor(hex: String): Color {
    return try {
        val colorString = hex.removePrefix("#")
        val colorInt = colorString.toLong(16)
        when (colorString.length) {
            6 -> Color(
                red = ((colorInt shr 16) and 0xFF) / 255f,
                green = ((colorInt shr 8) and 0xFF) / 255f,
                blue = (colorInt and 0xFF) / 255f
            )
            8 -> Color(
                alpha = ((colorInt shr 24) and 0xFF) / 255f,
                red = ((colorInt shr 16) and 0xFF) / 255f,
                green = ((colorInt shr 8) and 0xFF) / 255f,
                blue = (colorInt and 0xFF) / 255f
            )
            else -> Color.Gray
        }
    } catch (_: Exception) {
        Color.Gray
    }
}
