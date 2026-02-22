package com.androidledger.ui.csvimport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidledger.data.entity.Source

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: CsvImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val sources by viewModel.sources.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.processFile(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top app bar
        TopAppBar(
            title = { Text("导入CSV") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.state) {
                ImportState.IDLE -> {
                    IdleContent(
                        onPickFile = {
                            filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                        }
                    )
                }

                ImportState.DETECTING -> {
                    DetectingContent()
                }

                ImportState.PREVIEW -> {
                    PreviewContent(
                        uiState = uiState,
                        sources = sources,
                        wallets = wallets,
                        onSourceSelected = { viewModel.selectSource(it) },
                        onConfirmImport = { viewModel.confirmImport() },
                        onCancel = { viewModel.reset() }
                    )
                }

                ImportState.IMPORTING -> {
                    ImportingContent()
                }

                ImportState.SUCCESS -> {
                    SuccessContent(
                        importedCount = uiState.importResult?.transactions?.size ?: 0,
                        formatName = uiState.importResult?.formatName ?: "",
                        onDone = onNavigateBack,
                        onImportMore = { viewModel.reset() }
                    )
                }

                ImportState.ERROR -> {
                    ErrorContent(
                        errorMessage = uiState.errorMessage ?: "未知错误",
                        onRetry = { viewModel.reset() },
                        onBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(onPickFile: () -> Unit) {
    Spacer(modifier = Modifier.height(64.dp))

    Icon(
        imageVector = Icons.Filled.UploadFile,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "导入CSV账单",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "支持 Revolut、Wise、Poste Italiane 格式",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onPickFile,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Icon(
            imageVector = Icons.Filled.FileOpen,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("选择CSV文件")
    }
}

@Composable
private fun DetectingContent() {
    Spacer(modifier = Modifier.height(100.dp))

    CircularProgressIndicator(modifier = Modifier.size(48.dp))

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "正在识别格式...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewContent(
    uiState: CsvImportUiState,
    sources: List<Source>,
    wallets: List<com.androidledger.data.entity.Wallet>,
    onSourceSelected: (String) -> Unit,
    onConfirmImport: () -> Unit,
    onCancel: () -> Unit
) {
    val result = uiState.importResult ?: return

    // Format detection result
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "识别格式",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = uiState.detectedFormat?.displayName ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Preview card showing counts
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "预览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            PreviewRow(label = "可导入记录", value = "${result.transactions.size} 条")

            if (result.skippedCount > 0) {
                PreviewRow(
                    label = "跳过记录",
                    value = "${result.skippedCount} 条",
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (result.errorCount > 0) {
                PreviewRow(
                    label = "解析错误",
                    value = "${result.errorCount} 条",
                    valueColor = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Show amount range
            if (result.transactions.isNotEmpty()) {
                val currencies = result.transactions.map { it.currency }.distinct()
                val inCount = result.transactions.count { it.direction == "IN" }
                val outCount = result.transactions.count { it.direction == "OUT" }

                PreviewRow(label = "收入记录", value = "$inCount 条")
                PreviewRow(label = "支出记录", value = "$outCount 条")
                PreviewRow(label = "货币", value = currencies.joinToString(", "))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Source selector
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "选择账户来源",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "导入的交易将关联到所选来源",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Source dropdown
            SourceDropdown(
                sources = sources,
                wallets = wallets,
                selectedSourceId = uiState.selectedSourceId,
                onSourceSelected = onSourceSelected
            )

            // Error message for missing source
            if (uiState.errorMessage != null && uiState.state == ImportState.PREVIEW) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        ) {
            Text("取消")
        }

        Button(
            onClick = onConfirmImport,
            modifier = Modifier.weight(1f),
            enabled = uiState.selectedSourceId != null
        ) {
            Text("确认导入")
        }
    }
}

@Composable
private fun PreviewRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
            color = valueColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceDropdown(
    sources: List<Source>,
    wallets: List<com.androidledger.data.entity.Wallet>,
    selectedSourceId: String?,
    onSourceSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val walletMap = wallets.associateBy { it.id }

    val selectedSource = sources.find { it.id == selectedSourceId }
    val displayText = if (selectedSource != null) {
        val wallet = walletMap[selectedSource.walletId]
        val walletLabel = wallet?.let { "${it.currency} ${it.name}" } ?: ""
        "${selectedSource.name} ($walletLabel)"
    } else {
        "请选择来源"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sources.forEach { source ->
                val wallet = walletMap[source.walletId]
                val walletLabel = wallet?.let { "${it.currency} ${it.name}" } ?: ""
                val label = "${source.name} ($walletLabel)"

                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSourceSelected(source.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ImportingContent() {
    Spacer(modifier = Modifier.height(100.dp))

    CircularProgressIndicator(modifier = Modifier.size(48.dp))

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "正在导入...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SuccessContent(
    importedCount: Int,
    formatName: String,
    onDone: () -> Unit,
    onImportMore: () -> Unit
) {
    Spacer(modifier = Modifier.height(64.dp))

    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = Color(0xFF4CAF50)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "导入成功",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF4CAF50)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "已从 $formatName 导入 $importedCount 条交易记录",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "重复记录已自动跳过",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onDone,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Text("完成")
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onImportMore,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Text("继续导入")
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Spacer(modifier = Modifier.height(64.dp))

    Icon(
        imageVector = Icons.Filled.Error,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "导入失败",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Text("重试")
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Text("返回")
    }
}
