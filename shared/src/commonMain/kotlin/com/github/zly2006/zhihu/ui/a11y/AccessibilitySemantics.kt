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

package com.github.zly2006.zhihu.ui.a11y

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem

/*
 * 无障碍语义工具集。
 *
 * 集中放置读屏（TalkBack / 争渡 / 天坦等）相关的语义构造逻辑，避免各处重复拼字符串、
 * 也避免出现「Thumbnail」「Avatar」这类硬编码英文描述被读屏念出来。
 *
 * 设计原则：
 * 1. 所有面向读屏的文本必须是中文，且符合中文语序（先说主体，再说补充）。
 * 2. 卡片、列表项一律合并语义，一个条目只占一个焦点。
 * 3. 装饰性元素（纯图标、分隔线）显式置空或不暴露。
 * 4. 状态变化（加载、失败、成功）用 liveRegion 主动播报。
 */

/**
 * 把 [FeedDisplayItem] 转成一句完整的、可朗读的描述。
 *
 * 输出顺序：类型 → 标题 → 作者 → 摘要 → 附加信息。
 * 读屏用户听到的第一句就能判断「这是什么、谁写的、讲什么」。
 *
 * 例：`回答，如何评价 XXX，作者 张三，摘要：这是正文开头……，1234 赞同`
 */
fun FeedDisplayItem.toAccessibilityLabel(): String {
    val builder = StringBuilder()

    // 1. 内容类型
    val typeLabel = feed?.target.toTypeLabel()
    if (typeLabel != null) {
        builder.append(typeLabel).append('，')
    }

    // 2. 标题（过滤掉 HTML 标签和多余空白）
    val cleanTitle = title.plainText()
    if (cleanTitle.isNotEmpty()) {
        builder.append(cleanTitle).append('，')
    }

    // 3. 作者
    val cleanAuthor = authorName?.plainText()
    if (!cleanAuthor.isNullOrEmpty()) {
        builder.append("作者 ").append(cleanAuthor).append('，')
    }

    // 4. 摘要（截断，避免读屏念太久）
    val cleanSummary = summary?.plainText()
    if (!cleanSummary.isNullOrEmpty() && cleanSummary != cleanTitle) {
        builder.append(cleanSummary.take(ACCESSIBILITY_SUMMARY_LIMIT))
        if (cleanSummary.length > ACCESSIBILITY_SUMMARY_LIMIT) {
            builder.append('…')
        }
        builder.append('，')
    }

    // 5. 附加信息（赞同数、评论数等，details 里已经是「1234 赞同 · 56 评论」这种格式）
    val cleanDetails = details.plainText()
    if (cleanDetails.isNotEmpty()) {
        builder.append(cleanDetails)
    }

    return builder.toString().trimEnd('，', ' ', '·')
}

/**
 * 卡片被点击时读屏念的提示语，例如「双击以打开回答」。
 *
 * 返回 null 表示该条目不可点击（例如纯外链或已过滤内容），此时不应添加 onClickLabel。
 */
fun FeedDisplayItem.toClickLabel(): String? {
    if (isFiltered) return null
    val typeLabel = feed?.target.toTypeLabel() ?: return "双击以打开"
    return "双击以打开$typeLabel"
}

/** 内容类型的中文名。 */
private fun Feed.Target?.toTypeLabel(): String? = when (this) {
    is Feed.AnswerTarget -> "回答"
    is Feed.ArticleTarget -> "文章"
    is Feed.QuestionTarget -> "问题"
    is Feed.PinTarget -> "想法"
    is Feed.VideoTarget -> "视频"
    null -> null
}

/**
 * 去掉 HTML 标签、实体和多余空白，得到适合朗读的纯文本。
 *
 * 知乎的 title / summary 常常带 `<em>`、`<br>`、`&nbsp;` 这类标记，直接丢给读屏会念出
 * 「小于号 em 大于号」这种噪音，必须先清洗。
 */
fun String.plainText(): String = this
    .replace(Regex("<[^>]*>"), "")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace(Regex("\\s+"), " ")
    .trim()

/**
 * 标记为标题，让读屏可以按标题快速跳转。
 *
 * 用在页面主标题、章节标题、卡片大标题上。不要给正文段落用。
 */
fun Modifier.a11yHeading(): Modifier = this.semantics { heading() }

/**
 * 标记为「温和播报」区域。
 *
 * 用于加载完成、加载失败、操作成功这类状态提示。读屏会在当前朗读结束后补念，
 * 不会打断用户正在听的内容。
 *
 * 注意：不要给频繁变化的元素（例如进度百分比、输入框）加这个，否则会刷屏。
 */
fun Modifier.a11yLiveRegion(): Modifier = this.semantics {
    liveRegion = LiveRegionMode.Polite
}

/**
 * 标记为遍历组，让读屏把子树当成一个整体来遍历。
 *
 * 用在列表容器、卡片组上。配合子项的 [a11yTraversalIndex] 使用，
 * 可以保证焦点顺序与视觉顺序一致，不会出现「划到下面又跳回上面」。
 */
fun Modifier.a11yTraversalGroup(): Modifier = this.semantics {
    isTraversalGroup = true
}

/**
 * 指定遍历顺序。
 *
 * 数值小的先被聚焦。用于修正视觉顺序与语义树顺序不一致的场景
 * （例如 Duo3 排版把作者移到底部，但语义上应该先念标题）。
 */
fun Modifier.a11yTraversalIndex(index: Float): Modifier = this.semantics {
    traversalIndex = index
}

/**
 * 把整块内容合并成一个焦点，并给出一句完整描述。
 *
 * 这是卡片、列表项的标准做法：用户划一下就能听完一项，而不是划十几下。
 * [label] 为空时只合并不设描述，让子元素文本自然拼合。
 */
fun Modifier.a11yMerge(label: String? = null): Modifier = this.semantics(mergeDescendants = true) {
    if (!label.isNullOrBlank()) {
        contentDescription = label
    }
}

/**
 * 标记为纯装饰元素，从无障碍树中完全移除。
 *
 * 用于：装饰性图标、分隔线、纯背景图、空的可点击容器。
 * 这些元素如果留在树里，读屏会念「按钮」「图片」但用户点不动，属于噪音。
 */
fun Modifier.a11yDecorative(): Modifier = this.clearAndSetSemantics {}

/**
 * 给元素挂上无障碍自定义动作。
 *
 * 读屏用户划到该元素后，通过动作菜单（TalkBack 是「上滑再下滑」）即可触发，
 * 不必为了一个小按钮额外划好几下。
 *
 * 用法：
 * ```
 * Modifier.a11yCustomActions(
 *     "更多选项" to { showMenu = true },
 *     "收藏" to { toggleFavorite() },
 * )
 * ```
 *
 * 注意：动作名称要动词开头、简短明确（「点赞」「收藏」「分享」），
 * 不要写「点击这里进行点赞操作」这种啰嗦说法。
 */
fun Modifier.a11yCustomActions(vararg actions: Pair<String, () -> Unit>): Modifier =
    this.semantics {
        customActions = actions.map { (label, action) ->
            CustomAccessibilityAction(label) {
                action()
                true
            }
        }
    }

/** 摘要朗读长度上限（字符）。超过就截断，避免用户干等。 */
private const val ACCESSIBILITY_SUMMARY_LIMIT = 80
