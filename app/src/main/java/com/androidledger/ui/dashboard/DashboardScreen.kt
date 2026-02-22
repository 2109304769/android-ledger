package com.androidledger.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidledger.ui.theme.ContentSecondary
import com.androidledger.ui.theme.ContentTertiary
import com.androidledger.ui.theme.ExpenseRed
import com.androidledger.ui.theme.IncomeGreen
import com.androidledger.ui.theme.TransferBlue
import com.androidledger.ui.theme.WiseForestGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransaction: (String) -> Unit = {},
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToQuickEntry: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val balanceHidden by viewModel.balanceHidden.collectAsStateWithLifecycle()

    var sourcesExpanded by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "\u6211\u7684\u8D26\u672C",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    if (uiState.unconfirmedCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = ExpenseRed,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (uiState.unconfirmedCount > 99) "99+"
                                        else uiState.unconfirmedCount.toString(),
                                        fontSize = 10.sp
                                    )
                                }
                            },
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "\u672A\u786E\u8BA4\u4EA4\u6613",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "\u901A\u77E5",
                                modifier = Modifier.size(22.dp),
                                tint = ContentTertiary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = WiseForestGreen,
                contentColor = Color.White,
                shape = CircleShape
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
                    SectionLabel(title = "\u94B1\u5305\u603B\u89C8")
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
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            uiState.sourceItems.forEachIndexed { index, source ->
                                SourceRow(
                                    source = source,
                                    balanceHidden = balanceHidden,
                                    showProfileName = uiState.selectedProfileId == null
                                )
                                if (index < uiState.sourceItems.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -- Category Pie Chart --
            item {
                SectionLabel(title = "\u672C\u6708\u652F\u51FA\u5206\u7C7B")
                if (uiState.categoryExpenses.isEmpty()) {
                    EmptyState(message = "\u672C\u6708\u6682\u65E0\u652F\u51FA\u8BB0\u5F55")
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
                SectionLabel(title = "\u6700\u8FD1\u4EA4\u6613")
            }

            if (uiState.transactionGroups.isEmpty()) {
                item {
                    EmptyState(message = "\u6682\u65E0\u4EA4\u6613\u8BB0\u5F55")
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
// Account Switcher - Wise pill-style chips
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // "All" chip
        WisePillChip(
            text = "\u5168\u90E8",
            selected = selectedProfileId == null,
            onClick = { onSelect(null) }
        )
        // Per-profile chips
        profiles.forEach { profile ->
            WisePillChip(
                text = "${profile.emoji} ${profile.name}",
                selected = selectedProfileId == profile.id,
                onClick = { onSelect(profile.id) }
            )
        }
    }
}

@Composable
private fun WisePillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) WiseForestGreen else Color.Transparent
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (!selected) Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(50)
                ) else Modifier
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = contentColor
        )
    }
}

// ============================================================
// Monthly Overview Card - White with subtle border
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
    ) {
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.bodySmall,
            color = ContentTertiary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Income column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\u6536\u5165",
                    style = MaterialTheme.typography.bodySmall,
                    color = ContentTertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (balanceHidden) "****"
                    else DashboardViewModel.formatAmountAbs(monthlyIncome, currency),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = IncomeGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Thin vertical divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )

            // Expense column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\u652F\u51FA",
                    style = MaterialTheme.typography.bodySmall,
                    color = ContentTertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (balanceHidden) "****"
                    else DashboardViewModel.formatAmountAbs(monthlyExpense, currency),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = ExpenseRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Thin vertical divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )

            // Net column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\u7ED3\u4F59",
                    style = MaterialTheme.typography.bodySmall,
                    color = ContentTertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (balanceHidden) "****"
                    else DashboardViewModel.formatAmount(monthlyNet, currency),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (monthlyNet >= 0) IncomeGreen else ExpenseRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================
// Section Label - Calm, minimal
// ============================================================

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
        color = ContentTertiary
    )
}

// ============================================================
// Wallet Cards - White with thin border
// ============================================================

@Composable
private fun WalletCardsRow(
    wallets: List<WalletDisplayItem>,
    balanceHidden: Boolean,
    showProfileName: Boolean
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
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
    Column(
        modifier = Modifier
            .width(180.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        if (showProfileName && wallet.profileName.isNotEmpty()) {
            Text(
                text = wallet.profileName,
                style = MaterialTheme.typography.labelSmall,
                color = ContentTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Text(
            text = wallet.walletName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (balanceHidden) "****"
            else DashboardViewModel.formatAmountAbs(wallet.totalBalance, wallet.currency),
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${wallet.sources.size}\u4E2A\u8D26\u6237",
            style = MaterialTheme.typography.bodySmall,
            color = ContentTertiary
        )
    }
}

// ============================================================
// Source Balances - Clean list, no card wrapper
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
            .padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u8D26\u6237\u4F59\u989D",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            modifier = Modifier.weight(1f),
            color = ContentTertiary
        )
        IconButton(onClick = onToggleHidden, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (balanceHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (balanceHidden) "\u663E\u793A\u4F59\u989D" else "\u9690\u85CF\u4F59\u989D",
                modifier = Modifier.size(16.dp),
                tint = ContentTertiary
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "\u6536\u8D77" else "\u5C55\u5F00",
            modifier = Modifier.size(20.dp),
            tint = ContentTertiary
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
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Source icon - 40dp circle
        Box(
            modifier = Modifier
                .size(40.dp)
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
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (showProfileName && source.profileName.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    ProfileTag(name = source.profileName)
                }
            }
            Text(
                text = source.walletName,
                style = MaterialTheme.typography.bodySmall,
                color = ContentTertiary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (balanceHidden) "****"
            else DashboardViewModel.formatAmountAbs(source.balance, source.currency),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ============================================================
// Category Pie Chart - Clean donut
// ============================================================

@Composable
private fun CategoryPieChartSection(
    categories: List<CategoryExpenseItem>,
    currency: String,
    balanceHidden: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        // Donut chart centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                categories = categories,
                modifier = Modifier.size(140.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Legend items
        categories.take(6).forEach { cat ->
            CategoryLegendRow(
                category = cat,
                currency = currency,
                balanceHidden = balanceHidden
            )
        }
        if (categories.size > 6) {
            Text(
                text = "\u2026\u53CA\u5176\u4ED6${categories.size - 6}\u4E2A\u5206\u7C7B",
                style = MaterialTheme.typography.bodySmall,
                color = ContentTertiary,
                modifier = Modifier.padding(top = 4.dp, start = 16.dp)
            )
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
        val strokeWidth = canvasSize * 0.28f
        var startAngle = -90f

        if (categories.isEmpty()) {
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.2f),
                radius = (canvasSize - strokeWidth) / 2f,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            return@Canvas
        }

        val gapAngle = 2f

        categories.forEach { cat ->
            val rawSweep = cat.percentage / 100f * 360f
            val sweepAngle = (rawSweep - gapAngle).coerceAtLeast(0.5f)
            val color = parseColor(cat.color)
            drawArc(
                color = color,
                startAngle = startAngle + gapAngle / 2f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += rawSweep
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
            .padding(vertical = 5.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small color dot - 8dp
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(parseColor(category.color))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "${category.emoji} ${category.name}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (balanceHidden) "****"
            else DashboardViewModel.formatAmountAbs(category.amount, currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${String.format("%.1f", category.percentage)}%",
            style = MaterialTheme.typography.bodySmall,
            color = ContentTertiary,
            maxLines = 1
        )
    }
}

// ============================================================
// Recent Transactions - Clean list, no card wrappers
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = ContentTertiary,
            modifier = Modifier.weight(1f)
        )
        if (dailyTotal > 0) {
            Text(
                text = if (balanceHidden) "\u652F\u51FA: ****"
                else "\u652F\u51FA: ${DashboardViewModel.formatAmountAbs(dailyTotal, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = ContentTertiary
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category emoji in soft grey circle - 44dp
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.categoryEmoji,
                fontSize = 20.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Merchant and source info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (showProfileName && transaction.profileName.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    ProfileTag(name = transaction.profileName)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = transaction.sourceName,
                style = MaterialTheme.typography.bodySmall,
                color = ContentTertiary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Amount and time, right-aligned
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (balanceHidden) "****" else formatTransactionAmount(transaction),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = when (transaction.direction) {
                    "IN" -> IncomeGreen
                    "OUT" -> ExpenseRed
                    "TRANSFER" -> TransferBlue
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = transaction.time,
                style = MaterialTheme.typography.bodySmall,
                color = ContentTertiary
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp, end = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
}

private fun formatTransactionAmount(transaction: TransactionDisplayItem): String {
    val prefix = when (transaction.direction) {
        "IN" -> "+"
        "OUT" -> "-"
        else -> ""
    }
    // The amount string from ViewModel already has sign handling,
    // so we use the raw amount but add a subtle prefix
    return if (transaction.amount.startsWith("-") || transaction.amount.startsWith("+")) {
        transaction.amount
    } else {
        "$prefix${transaction.amount}"
    }
}

// ============================================================
// Shared Components
// ============================================================

@Composable
private fun ProfileTag(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = ContentSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = ContentTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp)
    )
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
