package com.oterman.rundemo.presentation.feature.share.components

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.feature.share.ShareTemplateSpec
import com.oterman.rundemo.ui.theme.RunBlue

/**
 * 模板分享占位库。首版只承接模板入口和扩展能力声明，不生成最终图片。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareTemplateGallery(
    templates: List<ShareTemplateSpec>,
    selectedTemplateId: String,
    onTemplateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "选择分享模板",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "先占位 4 套模板，具体样式和编辑能力后续逐步开放。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2
        ) {
            templates.forEachIndexed { index, template ->
                ShareTemplateCard(
                    template = template,
                    selected = template.id == selectedTemplateId,
                    accentColors = templateAccentColors(index),
                    onClick = { onTemplateSelected(template.id) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (templates.size % 2 != 0) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        val selectedTemplate = templates.firstOrNull { it.id == selectedTemplateId }
        if (selectedTemplate != null) {
            SelectedTemplatePlaceholder(template = selectedTemplate)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareTemplateCard(
    template: ShareTemplateSpec,
    selected: Boolean,
    accentColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) RunBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 2.dp else 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(accentColors))
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.84f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.46f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.56f))
                    )
                }

                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(RunBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Text(
                text = template.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                template.tags.forEach { tag ->
                    TemplateTag(text = tag)
                }
            }
        }
    }
}

@Composable
private fun SelectedTemplatePlaceholder(
    template: ShareTemplateSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RunBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = template.name.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RunBlue
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${template.name}样式设计中",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "当前仅支持选择占位，保存与分享将在模板视觉完成后开放。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TemplateTag(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = RunBlue,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(RunBlue.copy(alpha = 0.09f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

private fun templateAccentColors(index: Int): List<Color> {
    return when (index % 4) {
        0 -> listOf(Color(0xFF101820), Color(0xFFFFC857))
        1 -> listOf(Color(0xFF0B5D7A), Color(0xFF78D5D7))
        2 -> listOf(Color(0xFF2E3A59), Color(0xFF8BD17C))
        else -> listOf(Color(0xFF8B2F4B), Color(0xFFFFA62B))
    }
}
