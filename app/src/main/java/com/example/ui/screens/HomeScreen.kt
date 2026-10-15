package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.QuickAccessItem
import com.example.data.repository.NetworkRepository
import com.example.ui.MainViewModel
import com.example.ui.components.getIconForKey
import com.example.ui.theme.DarkBlueContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StatusEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val networkInfo by viewModel.networkInfo.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingNetInfo.collectAsStateWithLifecycle()
    val quickAccessItems by viewModel.quickAccessItems.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("All") }
    var itemToEdit by remember { mutableStateOf<QuickAccessItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showRestoreDefaultsDialog by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }

    // Drag-and-drop state
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragAccumulatedOffset by remember { mutableStateOf(Offset.Zero) }

    val categories = listOf("All", "Diagnostics", "Discovery", "Calculators", "Management", "External")

    val filteredItems = remember(quickAccessItems, selectedCategory) {
        if (selectedCategory == "All") {
            quickAccessItems.filter { it.isVisible }
        } else {
            quickAccessItems.filter {
                it.isVisible && (it.categoryTag.equals(selectedCategory, ignoreCase = true) ||
                        (selectedCategory == "External" && it.actionType != "TOOL"))
            }
        }
    }

    val activeTargets = remember(quickAccessItems) {
        quickAccessItems.map { it.target }.toSet()
    }
    val missingDefaultCount = remember(activeTargets) {
        NetworkRepository.DEFAULT_QUICK_ACCESS_ITEMS.count { it.target !in activeTargets }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Top Connectivity Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Slate900,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, Slate800)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Connection",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (networkInfo?.isConnected == true) StatusEmerald else Slate600)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = networkInfo?.wifiSsid?.takeIf { it != "N/A" } ?: networkInfo?.connectionType ?: "Offline",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate100
                            )
                        }
                        Text(
                            text = "IP: ${networkInfo?.localIpv4 ?: "127.0.0.1"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.refreshNetworkInfo() },
                        modifier = Modifier.testTag("home_refresh_net_info_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Network Info",
                            tint = if (isRefreshing) ElectricCyan else Slate400
                        )
                    }
                    IconButton(
                        onClick = { isReorderMode = !isReorderMode },
                        modifier = Modifier.testTag("home_toggle_reorder_btn")
                    ) {
                        Icon(
                            imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                            contentDescription = "Reorder Grid",
                            tint = if (isReorderMode) StatusEmerald else Slate400
                        )
                    }
                    IconButton(
                        onClick = { showRestoreDefaultsDialog = true },
                        modifier = Modifier.testTag("home_restore_defaults_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restore Default Tools",
                            tint = if (missingDefaultCount > 0) ElectricCyan else Slate400
                        )
                    }
                    IconButton(
                        onClick = {
                            itemToEdit = null
                            showEditDialog = true
                        },
                        modifier = Modifier.testTag("home_add_shortcut_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Quick Access Shortcut",
                            tint = ElectricCyan
                        )
                    }
                }
            }
        }

        // Missing Default Tools Re-add Banner
        AnimatedVisibility(visible = missingDefaultCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clickable { showRestoreDefaultsDialog = true }
                    .testTag("home_missing_defaults_banner"),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$missingDefaultCount default tool${if (missingDefaultCount > 1) "s" else ""} removed from dashboard",
                            fontSize = 12.sp,
                            color = Slate100,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Re-add Tools \u2192",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }
            }
        }

        // Reorder Mode Banner
        AnimatedVisibility(visible = isReorderMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBlueContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Drag & Drop Mode Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = "Hold and drag any card to reorder, or use arrow buttons",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }
                    Button(
                        onClick = { isReorderMode = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate950),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("home_reorder_done_btn")
                    ) {
                        Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Category Filter Chips Carousel
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Slate900,
                        labelColor = Slate400,
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = Slate950
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Slate800,
                        selectedBorderColor = ElectricCyan
                    )
                )
            }
        }

        // Dense Grid of Tool & External App Cards with Drag & Drop Reordering
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(filteredItems, key = { _, item -> item.id }) { index, item ->
                val isBeingDragged = draggingId == item.id

                val cardModifier = if (isBeingDragged) {
                    Modifier
                        .zIndex(10f)
                        .offset {
                            IntOffset(
                                dragAccumulatedOffset.x.roundToInt(),
                                dragAccumulatedOffset.y.roundToInt()
                            )
                        }
                        .scale(1.05f)
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                } else {
                    Modifier.zIndex(1f)
                }

                QuickAccessCard(
                    item = item,
                    isReorderMode = isReorderMode,
                    isDragging = isBeingDragged,
                    canMoveLeft = index > 0,
                    canMoveRight = index < filteredItems.size - 1,
                    modifier = cardModifier.pointerInput(filteredItems, index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingId = item.id
                                dragAccumulatedOffset = Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulatedOffset += dragAmount

                                val currentId = draggingId ?: return@detectDragGesturesAfterLongPress
                                val currentIdx = filteredItems.indexOfFirst { it.id == currentId }
                                if (currentIdx == -1) return@detectDragGesturesAfterLongPress

                                // In a 2-column grid, horizontal threshold ~120px, vertical threshold ~80px
                                val thresholdX = 120f
                                val thresholdY = 80f

                                var targetIdx = currentIdx
                                if (dragAccumulatedOffset.x > thresholdX && (currentIdx % 2 == 0) && currentIdx + 1 < filteredItems.size) {
                                    targetIdx = currentIdx + 1
                                    dragAccumulatedOffset = Offset(dragAccumulatedOffset.x - thresholdX, dragAccumulatedOffset.y)
                                } else if (dragAccumulatedOffset.x < -thresholdX && (currentIdx % 2 == 1) && currentIdx - 1 >= 0) {
                                    targetIdx = currentIdx - 1
                                    dragAccumulatedOffset = Offset(dragAccumulatedOffset.x + thresholdX, dragAccumulatedOffset.y)
                                } else if (dragAccumulatedOffset.y > thresholdY && currentIdx + 2 < filteredItems.size) {
                                    targetIdx = currentIdx + 2
                                    dragAccumulatedOffset = Offset(dragAccumulatedOffset.x, dragAccumulatedOffset.y - thresholdY)
                                } else if (dragAccumulatedOffset.y < -thresholdY && currentIdx - 2 >= 0) {
                                    targetIdx = currentIdx - 2
                                    dragAccumulatedOffset = Offset(dragAccumulatedOffset.x, dragAccumulatedOffset.y + thresholdY)
                                }

                                if (targetIdx != currentIdx) {
                                    val mutable = quickAccessItems.toMutableList()
                                    val fromItem = filteredItems[currentIdx]
                                    val toItem = filteredItems[targetIdx]
                                    val fromGlobalIdx = mutable.indexOfFirst { it.id == fromItem.id }
                                    val toGlobalIdx = mutable.indexOfFirst { it.id == toItem.id }
                                    if (fromGlobalIdx != -1 && toGlobalIdx != -1) {
                                        val moved = mutable.removeAt(fromGlobalIdx)
                                        mutable.add(toGlobalIdx, moved)
                                        viewModel.reorderQuickAccessItems(mutable)
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingId = null
                                dragAccumulatedOffset = Offset.Zero
                            },
                            onDragCancel = {
                                draggingId = null
                                dragAccumulatedOffset = Offset.Zero
                            }
                        )
                    },
                    onMoveLeft = {
                        if (index > 0) {
                            val mutable = quickAccessItems.toMutableList()
                            val fromIdx = mutable.indexOfFirst { it.id == item.id }
                            val targetItem = filteredItems[index - 1]
                            val toIdx = mutable.indexOfFirst { it.id == targetItem.id }
                            if (fromIdx != -1 && toIdx != -1) {
                                val temp = mutable[fromIdx]
                                mutable[fromIdx] = mutable[toIdx]
                                mutable[toIdx] = temp
                                viewModel.reorderQuickAccessItems(mutable)
                            }
                        }
                    },
                    onMoveRight = {
                        if (index < filteredItems.size - 1) {
                            val mutable = quickAccessItems.toMutableList()
                            val fromIdx = mutable.indexOfFirst { it.id == item.id }
                            val targetItem = filteredItems[index + 1]
                            val toIdx = mutable.indexOfFirst { it.id == targetItem.id }
                            if (fromIdx != -1 && toIdx != -1) {
                                val temp = mutable[fromIdx]
                                mutable[fromIdx] = mutable[toIdx]
                                mutable[toIdx] = temp
                                viewModel.reorderQuickAccessItems(mutable)
                            }
                        }
                    },
                    onClick = {
                        if (!isReorderMode && draggingId == null) {
                            viewModel.launchQuickAccessItem(item)
                        }
                    },
                    onEdit = {
                        itemToEdit = item
                        showEditDialog = true
                    }
                )
            }
        }
    }

    if (showEditDialog) {
        QuickAccessEditDialog(
            itemToEdit = itemToEdit,
            onDismiss = { showEditDialog = false },
            onSave = { updatedItem ->
                if (updatedItem.id == 0L) {
                    viewModel.saveQuickAccessItem(updatedItem)
                } else {
                    viewModel.updateQuickAccessItem(updatedItem)
                }
            },
            onDelete = { toDel ->
                viewModel.deleteQuickAccessItem(toDel)
            },
            onOpenRestoreCatalog = {
                showEditDialog = false
                showRestoreDefaultsDialog = true
            }
        )
    }

    if (showRestoreDefaultsDialog) {
        RestoreDefaultToolsDialog(
            currentItems = quickAccessItems,
            onDismiss = { showRestoreDefaultsDialog = false },
            onReaddTool = { toolTarget ->
                viewModel.readdDefaultItem(toolTarget)
            },
            onRestoreAll = {
                viewModel.restoreAllDefaultItems()
                showRestoreDefaultsDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickAccessCard(
    item: QuickAccessItem,
    isReorderMode: Boolean,
    isDragging: Boolean = false,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onEdit
            )
            .testTag("tool_card_${item.target.replace('.', '_')}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(
            if (isDragging) 2.dp else 1.dp,
            if (isDragging) ElectricCyan else if (isReorderMode) ElectricCyan.copy(alpha = 0.6f) else Slate800
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (item.actionType == "TOOL") DarkBlueContainer else Slate800),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForKey(item.iconKey),
                        contentDescription = item.title,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isReorderMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag Handle",
                            tint = ElectricCyan,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp)
                        )
                        IconButton(
                            onClick = onMoveLeft,
                            enabled = canMoveLeft,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Move earlier",
                                tint = if (canMoveLeft) ElectricCyan else Slate700,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveRight,
                            enabled = canMoveRight,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Move later",
                                tint = if (canMoveRight) ElectricCyan else Slate700,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else if (item.actionType != "TOOL") {
                    Box(
                        modifier = Modifier
                            .background(Slate800, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "APP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Slate600,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = Slate100,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
