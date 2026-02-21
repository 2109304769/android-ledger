package com.androidledger.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidledger.data.entity.Profile
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Wallet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val allWallets by viewModel.allWallets.collectAsStateWithLifecycle()
    val allSources by viewModel.allSources.collectAsStateWithLifecycle()
    val exchangeRate by viewModel.exchangeRate.collectAsStateWithLifecycle()

    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var showCreateWalletDialog by remember { mutableStateOf<String?>(null) }
    var showCreateSourceDialog by remember { mutableStateOf<String?>(null) }
    var showEditRateDialog by remember { mutableStateOf(false) }
    var expandedProfileId by remember { mutableStateOf<String?>(null) }
    var expandedWalletId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ========== Section: Account Management ==========
            item {
                SectionHeader(title = "账户管理")
            }

            if (profiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = "暂无账户，请点击下方按钮新建",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(profiles, key = { it.id }) { profile ->
                val isExpanded = expandedProfileId == profile.id
                val walletsForProfile = allWallets.filter { it.profileId == profile.id }

                ProfileCard(
                    profile = profile,
                    isExpanded = isExpanded,
                    wallets = walletsForProfile,
                    allSources = allSources,
                    expandedWalletId = expandedWalletId,
                    onToggleExpand = {
                        expandedProfileId = if (isExpanded) null else profile.id
                    },
                    onToggleWalletExpand = { walletId ->
                        expandedWalletId = if (expandedWalletId == walletId) null else walletId
                    },
                    onDeleteProfile = { viewModel.deleteProfile(profile) },
                    onAddWallet = { showCreateWalletDialog = profile.id },
                    onDeleteWallet = { wallet -> viewModel.deleteWallet(wallet) },
                    onAddSource = { walletId -> showCreateSourceDialog = walletId },
                    onDeleteSource = { source -> viewModel.deleteSource(source) }
                )
            }

            item {
                OutlinedButton(
                    onClick = { showCreateProfileDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("新建账户")
                }
            }

            // ========== Section: Exchange Rate ==========
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "汇率设置")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditRateDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "EUR / CNY",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "点击修改汇率",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = exchangeRate?.rate?.toString() ?: "7.85",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ========== Section: About ==========
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "关于")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "应用版本",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Text(
                            text = "1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ========== Dialogs ==========

    if (showCreateProfileDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateProfileDialog = false },
            onCreate = { name, type, emoji ->
                viewModel.createProfile(name, type, emoji)
                showCreateProfileDialog = false
            }
        )
    }

    if (showCreateWalletDialog != null) {
        CreateWalletDialog(
            onDismiss = { showCreateWalletDialog = null },
            onCreate = { name, currency ->
                viewModel.createWallet(showCreateWalletDialog!!, name, currency)
                showCreateWalletDialog = null
            }
        )
    }

    if (showCreateSourceDialog != null) {
        CreateSourceDialog(
            onDismiss = { showCreateSourceDialog = null },
            onCreate = { name, type, icon ->
                viewModel.createSource(showCreateSourceDialog!!, name, type, icon)
                showCreateSourceDialog = null
            }
        )
    }

    if (showEditRateDialog) {
        EditExchangeRateDialog(
            currentRate = exchangeRate?.rate ?: 7.85,
            onDismiss = { showEditRateDialog = false },
            onConfirm = { newRate ->
                viewModel.updateExchangeRate(newRate)
                showEditRateDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ProfileCard(
    profile: Profile,
    isExpanded: Boolean,
    wallets: List<Wallet>,
    allSources: List<Source>,
    expandedWalletId: String?,
    onToggleExpand: () -> Unit,
    onToggleWalletExpand: (String) -> Unit,
    onDeleteProfile: () -> Unit,
    onAddWallet: () -> Unit,
    onDeleteWallet: (Wallet) -> Unit,
    onAddSource: (String) -> Unit,
    onDeleteSource: (Source) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Profile header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.emoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (profile.type == "PERSONAL") "个人" else "商业",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "删除账户",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开"
                    )
                }
            }

            // Expanded content: wallets and sources
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (wallets.isEmpty()) {
                        Text(
                            text = "暂无钱包",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }

                    wallets.forEach { wallet ->
                        val isWalletExpanded = expandedWalletId == wallet.id
                        val sourcesForWallet = allSources.filter { it.walletId == wallet.id }

                        WalletItem(
                            wallet = wallet,
                            isExpanded = isWalletExpanded,
                            sources = sourcesForWallet,
                            onToggleExpand = { onToggleWalletExpand(wallet.id) },
                            onDeleteWallet = { onDeleteWallet(wallet) },
                            onAddSource = { onAddSource(wallet.id) },
                            onDeleteSource = onDeleteSource
                        )
                    }

                    TextButton(
                        onClick = onAddWallet,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加钱包")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除账户 \"${profile.name}\" 吗？该操作将同时删除其下所有钱包和来源。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProfile()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun WalletItem(
    wallet: Wallet,
    isExpanded: Boolean,
    sources: List<Source>,
    onToggleExpand: () -> Unit,
    onDeleteWallet: () -> Unit,
    onAddSource: () -> Unit,
    onDeleteSource: (Source) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Wallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = wallet.currency,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除钱包",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                if (sources.isEmpty()) {
                    Text(
                        text = "暂无来源",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                sources.forEach { source ->
                    SourceItem(
                        source = source,
                        onDelete = { onDeleteSource(source) }
                    )
                }

                TextButton(
                    onClick = onAddSource,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加来源", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除钱包 \"${wallet.name}\" 吗？该操作将同时删除其下所有来源。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteWallet()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SourceItem(
    source: Source,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = sourceTypeIcon(source.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = sourceTypeLabel(source.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "删除来源",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除来源 \"${source.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun sourceTypeIcon(type: String): ImageVector {
    return when (type) {
        "BANK" -> Icons.Filled.AccountBalance
        "EWALLET" -> Icons.Filled.CreditCard
        "CASH" -> Icons.Filled.Money
        else -> Icons.Filled.MoreHoriz
    }
}

private fun sourceTypeLabel(type: String): String {
    return when (type) {
        "BANK" -> "银行"
        "EWALLET" -> "电子钱包"
        "CASH" -> "现金"
        else -> "其他"
    }
}

// ========== Dialogs ==========

@Composable
private fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, type: String, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("PERSONAL") }
    var emoji by remember { mutableStateOf("\uD83D\uDC64") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建账户") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账户名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type dropdown
                Column {
                    Text(
                        text = "账户类型",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { typeDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedType == "PERSONAL") "个人" else "商业")
                    }
                    DropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("个人") },
                            onClick = {
                                selectedType = "PERSONAL"
                                typeDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("商业") },
                            onClick = {
                                selectedType = "BUSINESS"
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = emoji,
                    onValueChange = { if (it.length <= 2) emoji = it },
                    label = { Text("图标 (Emoji)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), selectedType, emoji.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CreateWalletDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, currency: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("EUR") }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加钱包") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("钱包名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "货币类型",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { currencyDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedCurrency)
                    }
                    DropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("EUR") },
                            onClick = {
                                selectedCurrency = "EUR"
                                currencyDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("CNY") },
                            onClick = {
                                selectedCurrency = "CNY"
                                currencyDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), selectedCurrency) },
                enabled = name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CreateSourceDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, type: String, icon: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("BANK") }
    var icon by remember { mutableStateOf("") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val sourceTypes = listOf(
        "BANK" to "银行",
        "EWALLET" to "电子钱包",
        "CASH" to "现金",
        "OTHER" to "其他"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加来源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("来源名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "来源类型",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { typeDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(sourceTypes.first { it.first == selectedType }.second)
                    }
                    DropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        sourceTypes.forEach { (typeKey, typeLabel) ->
                            DropdownMenuItem(
                                text = { Text(typeLabel) },
                                onClick = {
                                    selectedType = typeKey
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = icon,
                    onValueChange = { if (it.length <= 2) icon = it },
                    label = { Text("图标 (可选，Emoji)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        name.trim(),
                        selectedType,
                        icon.trim().ifBlank { null }
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EditExchangeRateDialog(
    currentRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var rateText by remember { mutableStateOf(currentRate.toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改汇率") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EUR -> CNY",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = {
                        rateText = it
                        isError = it.toDoubleOrNull() == null || (it.toDoubleOrNull() ?: 0.0) <= 0.0
                    },
                    label = { Text("汇率") },
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("请输入有效的汇率") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rate = rateText.toDoubleOrNull()
                    if (rate != null && rate > 0) {
                        onConfirm(rate)
                    }
                },
                enabled = !isError && rateText.isNotBlank()
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
