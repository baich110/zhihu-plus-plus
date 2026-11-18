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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ColorPickerDialog(
    title: String = "选择颜色",
    initialColor: Color,
    presetColors: List<Color> = listOf(
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFF44336),
        Color(0xFFFF9800),
        Color(0xFF9C27B0),
        Color(0xFF607D8B),
    ),
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }

    val currentColor = Color(red, green, blue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                )

                Spacer(modifier = Modifier.height(16.dp))

                ColorSlider(
                    label = "红色",
                    value = red,
                    color = Color.Red,
                    onValueChange = { red = it },
                )

                ColorSlider(
                    label = "绿色",
                    value = green,
                    color = Color.Green,
                    onValueChange = { green = it },
                )

                ColorSlider(
                    label = "蓝色",
                    value = blue,
                    color = Color.Blue,
                    onValueChange = { blue = it },
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "RGB: (${(red * 255).roundToInt()}, ${(green * 255).roundToInt()}, ${(blue * 255).roundToInt()}) #${currentColor.toArgb().toUInt().toString(16).padStart(3, '0').takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("预设颜色", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    presetColors.forEach { presetColor ->
                        PresetColor(presetColor) {
                            red = it.red
                            green = it.green
                            blue = it.blue
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            modifier = Modifier.width(50.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${(value * 255).roundToInt()}",
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PresetColor(
    color: Color,
    onSelect: (Color) -> Unit,
) {
    // 无障碍：色块是纯视觉元素，读屏看不见颜色。把 RGB 转成可朗读的中文描述，
    // 否则用户只能听到一串「按钮」，完全不知道选的是什么颜色。
    val colorLabel = describeColor(color)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .semantics { contentDescription = colorLabel }
            .clickable(
                onClickLabel = "双击以选择$colorLabel",
            ) { onSelect(color) },
    )
}

/**
 * 把颜色转成适合朗读的中文描述。
 *
 * 先判断是否为常见纯色（红绿蓝黑白灰），否则按色相归类并给出深浅。
 * 例：`#F57C00` → 「橙色」，`#2196F3` → 「蓝色」，`#222222` → 「深灰」。
 */
private fun describeColor(color: Color): String {
    val r = (color.red * 255).roundToInt()
    val g = (color.green * 255).roundToInt()
    val b = (color.blue * 255).roundToInt()
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    // 低饱和度：黑白灰
    if (delta < 24) {
        return when {
            max < 50 -> "黑色"
            max < 110 -> "深灰"
            max < 180 -> "灰色"
            max < 235 -> "浅灰"
            else -> "白色"
        }
    }

    // 按色相归类
    val hue = when {
        max == r -> (60.0 * (g - b) / delta + 360) % 360
        max == g -> 60.0 * (b - r) / delta + 120
        else -> 60.0 * (r - g) / delta + 240
    }
    val base = when {
        hue < 15 || hue >= 345 -> "红色"
        hue < 45 -> "橙色"
        hue < 70 -> "黄色"
        hue < 165 -> "绿色"
        hue < 195 -> "青色"
        hue < 255 -> "蓝色"
        hue < 285 -> "紫色"
        hue < 345 -> "粉色"
        else -> "红色"
    }
    val tone = when {
        max < 90 -> "深"
        max > 220 && delta < 90 -> "浅"
        else -> ""
    }
    return tone + base
}
