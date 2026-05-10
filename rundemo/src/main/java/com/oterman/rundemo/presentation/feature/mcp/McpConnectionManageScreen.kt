package com.oterman.rundemo.presentation.feature.mcp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.data.network.dto.response.McpConnectionDto
import com.oterman.rundemo.data.network.dto.response.McpScopeDescDto
import com.oterman.rundemo.data.network.dto.response.McpToolUsageDto
import com.oterman.rundemo.data.network.dto.response.OAuth2UserAuthorizationDto
import com.oterman.rundemo.presentation.components.AppCard
import com.oterman.rundemo.ui.theme.RunTheme
import java.text.SimpleDateFormat
import java.util.Locale

private const val MCP_SERVER_ADDRESS = "https://yayarun.cn/mcp"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpConnectionManageScreen(
    onNavigateBack: () -> Unit,
    viewModel: McpConnectionManageViewModel = viewModel(
        factory = McpConnectionManageViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadScopes(force = true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.showUsageDetail) "用量明细" else "连接与授权",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.showUsageDetail) {
                                viewModel.dismissUsageDetail()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.load(isRefresh = true) },
            state = pullState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null && uiState.connections.isEmpty() && uiState.usages.isEmpty() -> {
                    McpErrorState(
                        message = uiState.errorMessage.orEmpty(),
                        onRetry = { viewModel.load() }
                    )
                }
                uiState.showUsageDetail -> {
                    UsageDetailContent(
                        usages = uiState.usages,
                        onNavigateBack = viewModel::dismissUsageDetail
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            AuthorizationTabSelector(
                                selectedTab = uiState.selectedTab,
                                onTabSelected = viewModel::selectTab
                            )
                        }

                        when (uiState.selectedTab) {
                            AuthorizationManageTab.AI_CONNECTION -> {
                                item {
                                    McpGuideSection(
                                        onCopied = { viewModel.showSnackbar("已复制服务地址") }
                                    )
                                }
                                item {
                                    UsageSummarySection(
                                        todayCount = uiState.todayCount,
                                        todayLimit = uiState.todayLimit,
                                        monthCount = uiState.monthCount,
                                        monthLimit = uiState.monthLimit,
                                        onDetailClick = viewModel::showUsageDetail
                                    )
                                }
                                item {
                                    ConnectionSection(
                                        connections = uiState.connections,
                                        activeCount = uiState.activeConnectionCount,
                                        onManage = viewModel::selectConnection
                                    )
                                }
                            }
                            AuthorizationManageTab.PARTNER_AUTHORIZATION -> {
                                item {
                                    PartnerAuthorizationSection(
                                        items = uiState.partnerAuthorizations,
                                        onRevoke = viewModel::preparePartnerRevoke
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.selectedConnection?.let { connection ->
        ScopeEditSheet(
            connection = connection,
            scopeOptions = uiState.supportedScopes,
            isLoadingScopes = uiState.isLoadingScopes,
            scopeErrorMessage = uiState.scopeErrorMessage,
            onDismiss = viewModel::dismissConnectionSheet,
            onSave = { scopeCodes, connectionName ->
                viewModel.updateScope(connection, scopeCodes, connectionName)
            },
            onRevoke = {
                viewModel.prepareRevoke(connection)
            }
        )
    }

    uiState.pendingRevokeConnection?.let { connection ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRevokeDialog,
            title = { Text("吊销连接") },
            text = { Text("吊销「${connection.displayName}」后，对应 Access Token 将立即失效。") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmRevoke,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("吊销")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRevokeDialog) {
                    Text("取消")
                }
            }
        )
    }

    uiState.pendingPartnerRevokeItem?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPartnerRevokeDialog,
            title = { Text("取消合作方授权") },
            text = { Text("确定要取消「${item.clientName ?: item.clientId ?: "该应用"}」的授权吗？取消后该应用将无法继续访问您的跑鸭数据。") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmPartnerRevoke,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("取消授权")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPartnerRevokeDialog) {
                    Text("不取消")
                }
            }
        )
    }
}

@Composable
private fun AuthorizationTabSelector(
    selectedTab: AuthorizationManageTab,
    onTabSelected: (AuthorizationManageTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(9.dp)
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AuthorizationManageTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) RunTheme.colorScheme.blue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun McpGuideSection(onCopied: () -> Unit) {
    val context = LocalContext.current
    SectionCard {
        SectionTitle(
            title = "MCP 服务",
            subtitle = "添加到支持 MCP 的 AI 客户端",
            icon = Icons.Outlined.SyncAlt
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = MCP_SERVER_ADDRESS,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("MCP 服务地址", MCP_SERVER_ADDRESS))
                    onCopied()
                }
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("复制")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        GuideStep(1, "复制 MCP 服务地址。")
        GuideStep(2, "在支持 MCP 的 AI 客户端中添加远程 MCP Server。")
        GuideStep(3, "使用跑鸭账号登录授权，回到本页刷新查看连接。")
    }
}

@Composable
private fun UsageSummarySection(
    todayCount: Int,
    todayLimit: Int,
    monthCount: Int,
    monthLimit: Int,
    onDetailClick: () -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(
                title = "工具用量",
                subtitle = "查看调用次数和周期额度",
                icon = Icons.Outlined.BarChart,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDetailClick) {
                Text("明细")
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UsageMetric(
                title = "今日",
                count = todayCount,
                limit = todayLimit,
                modifier = Modifier.weight(1f)
            )
            UsageMetric(
                title = "本月",
                count = monthCount,
                limit = monthLimit,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConnectionSection(
    connections: List<McpConnectionDto>,
    activeCount: Int,
    onManage: (McpConnectionDto) -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(
                title = "已授权 AI 连接",
                subtitle = "管理连接名称、权限和吊销",
                icon = Icons.Outlined.Link,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$activeCount 个连接",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (connections.isEmpty()) {
            InlineEmptyState("暂无已授权连接", "添加 MCP 服务并授权后会显示在这里。", Icons.Outlined.Link)
        } else {
            connections.forEachIndexed { index, connection ->
                ConnectionRow(connection = connection, onManage = { onManage(connection) })
                if (index < connections.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PartnerAuthorizationSection(
    items: List<OAuth2UserAuthorizationDto>,
    onRevoke: (OAuth2UserAuthorizationDto) -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(
                title = "合作方数据授权",
                subtitle = "管理已授权访问跑鸭数据的应用",
                icon = Icons.Outlined.Key,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${items.size} 个应用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            InlineEmptyState("暂无合作方数据授权", "授权合作方应用后会显示在这里。", Icons.Outlined.Key)
        } else {
            items.forEachIndexed { index, item ->
                PartnerAuthorizationRow(item = item, onRevoke = { onRevoke(item) })
                if (index < items.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun UsageDetailContent(
    usages: List<McpToolUsageDto>,
    onNavigateBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "工具调用次数和周期额度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (usages.isEmpty()) {
            item {
                SectionCard {
                    Text("暂无工具用量数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(usages, key = { it.id }) { usage ->
                SectionCard {
                    Text(usage.usageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    UsageSummaryRow("今日", usage.todayCount ?: 0, usage.todayLimit ?: 0)
                    Spacer(modifier = Modifier.height(12.dp))
                    UsageSummaryRow("本月", usage.monthCount ?: 0, usage.monthLimit ?: 0)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScopeEditSheet(
    connection: McpConnectionDto,
    scopeOptions: List<McpScopeDescDto>,
    isLoadingScopes: Boolean,
    scopeErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (Set<String>, String) -> Unit,
    onRevoke: () -> Unit
) {
    var connectionName by remember(connection.id) { mutableStateOf(connection.connectionName.orEmpty()) }
    var selectedScopeCodes by remember(connection.id) { mutableStateOf(connection.activeScopeCodes) }
    val scrollState = rememberScrollState()
    val validScopeOptions = scopeOptions.filter { it.scopeCode.isNotEmpty() }
    val sheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.78f
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Text("管理连接", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = connectionName,
                    onValueChange = { connectionName = it.take(50) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("连接名称") },
                    singleLine = true
                )

                connection.clientDisplayName?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("客户端：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("授权权限", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                if (isLoadingScopes) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "正在从服务端加载权限列表...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (validScopeOptions.isEmpty()) {
                    Text(
                        text = scopeErrorMessage ?: "权限列表未从服务端加载，请返回刷新后重试。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    validScopeOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedScopeCodes = if (selectedScopeCodes.contains(option.scopeCode)) {
                                        selectedScopeCodes - option.scopeCode
                                    } else {
                                        selectedScopeCodes + option.scopeCode
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedScopeCodes.contains(option.scopeCode),
                                onCheckedChange = { checked ->
                                    selectedScopeCodes = if (checked) {
                                        selectedScopeCodes + option.scopeCode
                                    } else {
                                        selectedScopeCodes - option.scopeCode
                                    }
                                }
                            )
                            Text(option.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "修改后会立即影响该 MCP 连接的工具访问权限。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRevoke,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("吊销连接")
                }
                Button(
                    onClick = { onSave(selectedScopeCodes, connectionName) },
                    modifier = Modifier.weight(1f),
                    enabled = validScopeOptions.isNotEmpty() && !isLoadingScopes
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UsageMetric(
    title: String,
    count: Int,
    limit: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Text(usageDisplayText(count, limit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        UsageProgressBar(progress = usageProgress(count, limit))
    }
}

@Composable
private fun UsageSummaryRow(title: String, count: Int, limit: Int) {
    Column {
        Row {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.weight(1f))
            Text(usageDisplayText(count, limit), fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        UsageProgressBar(progress = usageProgress(count, limit))
    }
}

@Composable
private fun UsageProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(5.dp)
                .background(RunTheme.colorScheme.blue)
        )
    }
}

@Composable
private fun ConnectionRow(connection: McpConnectionDto, onManage: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = connection.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            connection.clientDisplayName?.let {
                Text(
                    text = "客户端：$it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                connection.permissionSummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(
            onClick = onManage,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("管理")
        }
    }
}

@Composable
private fun PartnerAuthorizationRow(item: OAuth2UserAuthorizationDto, onRevoke: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.clientName ?: item.clientId ?: "未知应用", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            item.scope?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item.authorizeTime?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "授权时间：${formatDisplayDate(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(
            onClick = onRevoke,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("取消")
        }
    }
}

@Composable
private fun GuideStep(index: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(RunTheme.colorScheme.blue.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(index.toString(), style = MaterialTheme.typography.labelSmall, color = RunTheme.colorScheme.blue, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InlineEmptyState(title: String, message: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun McpErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

private fun usageDisplayText(count: Int, limit: Int): String {
    return if (limit == 0) "$count / 不限" else "$count / $limit"
}

private fun usageProgress(count: Int, limit: Int): Float {
    return if (limit > 0) {
        (count.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    } else {
        (count.toFloat() / 100f).coerceIn(0f, 1f)
    }
}

private fun formatDisplayDate(raw: String): String {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd HH:mm:ss"
    )
    for (format in formats) {
        runCatching {
            val parser = SimpleDateFormat(format, Locale.US)
            val date = parser.parse(raw)
            if (date != null) {
                return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(date)
            }
        }
    }
    return raw.take(16)
}
