package com.androidledger.ui.transactiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidledger.data.entity.Category
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Tag
import com.androidledger.data.entity.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // Collect navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionDetailEvent.NavigateBack -> onNavigateBack()
                is TransactionDetailEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("交易详情") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditing) {
                            viewModel.cancelEdit()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (uiState.isEditing) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (uiState.isEditing) "取消" else "返回"
                        )
                    }
                },
                actions = {
                    if (!uiState.isEditing && uiState.transaction != null) {
                        IconButton(onClick = { viewModel.toggleEdit() }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑"
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.transaction == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "交易不存在",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val transaction = uiState.transaction!!
            if (uiState.isEditing) {
                EditModeContent(
                    transaction = transaction,
                    uiState = uiState,
                    onSave = { amountMinor, direction, categoryId, clearCategory, sourceId,
                               merchant, description, occurredAt, tagIds, clearTags ->
                        viewModel.updateTransaction(
                            amountMinor = amountMinor,
                            direction = direction,
                            categoryId = categoryId,
                            clearCategory = clearCategory,
                            sourceId = sourceId,
                            merchant = merchant,
                            description = description,
                            occurredAt = occurredAt,
                            tagIds = tagIds,
                            clearTags = clearTags
                        )
                    },
                    onCancel = { viewModel.cancelEdit() },
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                DisplayModeContent(
                    transaction = transaction,
                    uiState = uiState,
                    onConfirm = { viewModel.confirmTransaction() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条交易记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTransaction()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// ──────────────────────────────────────────────────────────
// Display Mode
// ──────────────────────────────────────────────────────────

@Composable
private fun DisplayModeContent(
    transaction: Transaction,
    uiState: TransactionDetailUiState,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Unconfirmed banner
        if (transaction.isConfirmed == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "待确认",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("确认")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Large amount display
        val amountColor = when (transaction.direction) {
            "OUT" -> Color(0xFFD32F2F)
            "IN" -> Color(0xFF388E3C)
            "TRANSFER" -> Color(0xFF1976D2)
            else -> MaterialTheme.colorScheme.onSurface
        }
        val directionPrefix = when (transaction.direction) {
            "OUT" -> "-"
            "IN" -> "+"
            "TRANSFER" -> ""
            else -> ""
        }
        val directionLabel = when (transaction.direction) {
            "OUT" -> "支出"
            "IN" -> "收入"
            "TRANSFER" -> "转账"
            else -> transaction.direction
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = directionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$directionPrefix${formatAmount(transaction.amountMinor)} ${transaction.currency}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = amountColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Info cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(label = "账户", value = uiState.profileName)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(label = "钱包", value = uiState.walletName)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(label = "来源", value = uiState.sourceName)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(
                    label = "分类",
                    value = if (uiState.categoryIcon.isNotEmpty() && uiState.categoryName.isNotEmpty())
                        "${uiState.categoryIcon} ${uiState.categoryName}"
                    else if (uiState.categoryName.isNotEmpty())
                        uiState.categoryName
                    else "未分类"
                )
                if (!transaction.merchant.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "商户", value = transaction.merchant)
                }
                if (!transaction.description.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "备注", value = transaction.description)
                }
                if (!transaction.tagIds.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    val tagNames = resolveTagNames(transaction.tagIds, uiState.allTags)
                    InfoRow(label = "标签", value = tagNames)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(label = "时间", value = formatDateTime(transaction.occurredAt))
                if (transaction.source.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "录入来源", value = transaction.source)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ──────────────────────────────────────────────────────────
// Edit Mode
// ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditModeContent(
    transaction: Transaction,
    uiState: TransactionDetailUiState,
    onSave: (
        amountMinor: Long?,
        direction: String?,
        categoryId: String?,
        clearCategory: Boolean,
        sourceId: String?,
        merchant: String?,
        description: String?,
        occurredAt: Long?,
        tagIds: String?,
        clearTags: Boolean
    ) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Editable state
    var amountText by rememberSaveable { mutableStateOf(formatAmount(transaction.amountMinor)) }
    var selectedDirection by rememberSaveable { mutableStateOf(transaction.direction) }
    var selectedCategoryId by rememberSaveable { mutableStateOf(transaction.categoryId) }
    var selectedSourceId by rememberSaveable { mutableStateOf(transaction.sourceId) }
    var merchantText by rememberSaveable { mutableStateOf(transaction.merchant ?: "") }
    var descriptionText by rememberSaveable { mutableStateOf(transaction.description ?: "") }
    var selectedTime by rememberSaveable { mutableLongStateOf(transaction.occurredAt) }
    // Store tag IDs as comma-separated string for Saver compatibility
    var selectedTagIdsString by rememberSaveable {
        mutableStateOf(transaction.tagIds ?: "")
    }
    val selectedTagIdSet: Set<String> = remember(selectedTagIdsString) {
        selectedTagIdsString.split(",").filter { it.isNotBlank() }.toSet()
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Direction selector
        Text(
            text = "类型",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("OUT" to "支出", "IN" to "收入", "TRANSFER" to "转账").forEach { (dir, label) ->
                val selected = selectedDirection == dir
                val chipColor = when (dir) {
                    "OUT" -> Color(0xFFD32F2F)
                    "IN" -> Color(0xFF388E3C)
                    else -> Color(0xFF1976D2)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) chipColor.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (selected) chipColor else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedDirection = dir }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) chipColor else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount
        Text(
            text = "金额",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            suffix = { Text(transaction.currency) },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Category grid
        Text(
            text = "分类",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CategoryGrid(
            categories = uiState.allCategories,
            selectedCategoryId = selectedCategoryId,
            onSelect = { id ->
                selectedCategoryId = if (selectedCategoryId == id) null else id
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Source selector
        Text(
            text = "来源",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SourceSelector(
            sources = uiState.allSources,
            selectedSourceId = selectedSourceId,
            onSelect = { selectedSourceId = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Merchant
        Text(
            text = "商户",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = merchantText,
            onValueChange = { merchantText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("商户名称（可选）") },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = "备注",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = descriptionText,
            onValueChange = { descriptionText = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            placeholder = { Text("备注信息（可选）") },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date/Time picker
        Text(
            text = "时间",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(formatDate(selectedTime))
            }
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(formatTime(selectedTime))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tags
        if (uiState.allTags.isNotEmpty()) {
            Text(
                text = "标签",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.allTags.forEach { tag ->
                    val selected = selectedTagIdSet.contains(tag.id)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val updatedSet = if (selected) {
                                selectedTagIdSet - tag.id
                            } else {
                                selectedTagIdSet + tag.id
                            }
                            selectedTagIdsString = updatedSet.joinToString(",")
                        },
                        label = { Text(tag.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save and Cancel buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("取消")
            }
            Button(
                onClick = {
                    val parsedAmount = parseAmount(amountText)
                    val tagIdsStr = if (selectedTagIdSet.isEmpty()) null
                    else selectedTagIdSet.joinToString(",")

                    onSave(
                        parsedAmount,
                        selectedDirection,
                        selectedCategoryId,
                        selectedCategoryId == null,
                        selectedSourceId,
                        merchantText.ifBlank { null },
                        descriptionText.ifBlank { null },
                        selectedTime,
                        tagIdsStr,
                        tagIdsStr == null
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTime
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                            val timeCal = Calendar.getInstance().apply { timeInMillis = selectedTime }
                            dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                            dateCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                            dateCal.set(Calendar.SECOND, 0)
                            dateCal.set(Calendar.MILLISECOND, 0)
                            selectedTime = dateCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedTime }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTime }
                        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(Calendar.MINUTE, timePickerState.minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        selectedTime = cal.timeInMillis
                        showTimePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// ──────────────────────────────────────────────────────────
// Category Grid
// ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category.id == selectedCategoryId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(category.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${category.icon} ${category.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Source Selector
// ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceSelector(
    sources: List<Source>,
    selectedSourceId: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sources.forEach { source ->
            val isSelected = source.id == selectedSourceId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(source.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Utility functions
// ──────────────────────────────────────────────────────────

private fun formatAmount(amountMinor: Long): String {
    val whole = amountMinor / 100
    val fraction = amountMinor % 100
    return if (fraction == 0L) {
        "$whole"
    } else {
        String.format(Locale.getDefault(), "%d.%02d", whole, kotlin.math.abs(fraction))
    }
}

private fun parseAmount(text: String): Long {
    return try {
        val cleaned = text.trim()
        if (cleaned.contains(".")) {
            val parts = cleaned.split(".")
            val whole = parts[0].toLongOrNull() ?: 0L
            val fractionStr = parts.getOrElse(1) { "0" }.take(2).padEnd(2, '0')
            val fraction = fractionStr.toLongOrNull() ?: 0L
            whole * 100 + fraction
        } else {
            (cleaned.toLongOrNull() ?: 0L) * 100
        }
    } catch (_: Exception) {
        0L
    }
}

private fun formatDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy\u5E74M\u6708d\u65E5 HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy\u5E74M\u6708d\u65E5", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun resolveTagNames(tagIdsStr: String?, allTags: List<Tag>): String {
    if (tagIdsStr.isNullOrBlank()) return ""
    val ids = tagIdsStr.split(",").filter { it.isNotBlank() }
    val tagMap = allTags.associateBy { it.id }
    return ids.mapNotNull { tagMap[it]?.name }.joinToString(", ")
}
