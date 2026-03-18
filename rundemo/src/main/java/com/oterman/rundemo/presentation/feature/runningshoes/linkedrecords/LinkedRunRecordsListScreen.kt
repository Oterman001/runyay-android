package com.oterman.rundemo.presentation.feature.runningshoes.linkedrecords

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.repository.RunningShoeRepository
import com.oterman.rundemo.presentation.feature.home.components.RunRecordItem
import com.oterman.rundemo.presentation.feature.runningshoes.batchlink.BatchLinkRunRecordsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedRunRecordsListScreen(
    shoeId: String,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { RunningShoeRepository(context) }
    var records by remember { mutableStateOf<List<RunRecordEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showBatchLinkSheet by remember { mutableStateOf(false) }

    LaunchedEffect(shoeId, refreshTrigger) {
        isLoading = true
        records = repository.getLinkedRecords(shoeId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关联记录 (${records.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showBatchLinkSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "关联记录")
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (records.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无关联记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(records, key = { it.workoutId }) { record ->
                    RunRecordItem(
                        record = record,
                        trackPoints = null,
                        onClick = {}
                    )
                }
            }
        }
    }

    if (showBatchLinkSheet) {
        BatchLinkRunRecordsSheet(
            shoeId = shoeId,
            onDismiss = { showBatchLinkSheet = false },
            onLinkSuccess = {
                showBatchLinkSheet = false
                refreshTrigger++
            }
        )
    }
}
