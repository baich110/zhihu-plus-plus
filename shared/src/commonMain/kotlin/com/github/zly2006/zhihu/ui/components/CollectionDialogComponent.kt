/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.data.Collection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDialogComponent(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    collections: List<Collection>,
    onLoadCollections: () -> Unit,
    onToggleFavorite: (Collection) -> Unit,
    onCreateCollection: (title: String, description: String, isPublic: Boolean) -> Unit,
) {
    // 新建收藏夹对话框状态
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showDialog) {
        if (showDialog && collections.isEmpty()) {
            onLoadCollections()
        }
    }

    // 对话框内容
    if (showDialog) {
        MyModalBottomSheet(
            onDismissRequest = onDismiss,
            // 无障碍：读屏进入弹层时先念「选择收藏夹」，知道自己在哪。
            paneTitle = "选择收藏夹",
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                // 标题
                Text(
                    text = "选择收藏夹",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                // 收藏夹列表
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 新建收藏夹按钮
                    item {
                        Surface(
                            // 无障碍：整行合并，避免图标和文字各念一遍「新建收藏夹」。
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {}
                                .clickable(
                                    onClickLabel = "双击以新建收藏夹",
                                ) {
                                    showCreateDialog = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    // 无障碍：文字已经说明，图标描述置空避免重复。
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "新建收藏夹",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    items(collections) { collection ->
                        CollectionItem(
                            collection = collection,
                            onToggle = {
                                onToggleFavorite(collection)
                                onLoadCollections()
                            },
                        )
                    }
                }
            }
        }
    }

    // 新建收藏夹对话框
    CreateCollectionDialog(
        showDialog = showCreateDialog,
        onDismiss = { showCreateDialog = false },
        onConfirm = { title, description, isPublic ->
            onCreateCollection(title, description, isPublic)
            showCreateDialog = false
        },
    )
}

@Composable
private fun CollectionItem(
    collection: Collection,
    onToggle: () -> Unit,
) {
    Surface(
        // 无障碍：整行合并成一个焦点，读屏一次念完「标题、简介、篇数、收藏状态」。
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                stateDescription = if (collection.isFavorited) "已收藏" else "未收藏"
            }.clickable(
                onClickLabel = if (collection.isFavorited) "双击以取消收藏" else "双击以收藏",
            ) { onToggle() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (collection.description.isNotEmpty()) {
                    Text(
                        text = collection.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = "${collection.itemCount} 篇内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Icon(
                imageVector = if (collection.isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (collection.isFavorited) "已收藏" else "未收藏",
                tint = if (collection.isFavorited) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
