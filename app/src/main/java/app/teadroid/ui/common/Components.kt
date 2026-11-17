package app.teadroid.ui.common

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import app.teadroid.data.Label
import java.time.OffsetDateTime

// ------------------------------------------------------------------ time

fun timeAgo(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        val millis = OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        DateUtils.getRelativeTimeSpanString(
            millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()
    }.getOrDefault("")
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}

// ------------------------------------------------------------------ shapes

/** Expressive "segmented list" shape: large outer corners, tight inner corners. */
fun segmentShape(index: Int, count: Int, outer: Dp = 24.dp, inner: Dp = 6.dp): Shape {
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
fun SegmentCard(
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

// ------------------------------------------------------------------ avatars & chips

@Composable
fun Avatar(
    url: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = CircleShape,
) {
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** Cookie-shaped avatar, the signature expressive treatment for hero avatars. */
@Composable
fun CookieAvatar(url: String?, name: String, size: Dp = 88.dp, modifier: Modifier = Modifier) {
    Avatar(url, name, modifier, size, MaterialShapes.Cookie9Sided.toShape())
}

@Composable
fun LabelChip(label: Label) {
    val bg = remember(label.color) {
        runCatching { Color(("FF" + label.color.trimStart('#')).toLong(16)) }.getOrDefault(Color(0xFF888888))
    }
    val fg = if (bg.luminance() > 0.5f) Color(0xFF1B1B1B) else Color.White
    Surface(shape = RoundedCornerShape(50), color = bg, contentColor = fg) {
        Text(
            label.name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
fun StateBadge(state: String, merged: Boolean = false, draft: Boolean = false, isPull: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val (text, icon, bg, fg) = when {
        merged -> BadgeStyle("Merged", Icons.Outlined.MergeType, cs.tertiaryContainer, cs.onTertiaryContainer)
        draft -> BadgeStyle("Draft", Icons.Outlined.ErrorOutline, cs.surfaceContainerHighest, cs.onSurfaceVariant)
        state == "closed" -> BadgeStyle("Closed", Icons.Outlined.CheckCircle, cs.errorContainer, cs.onErrorContainer)
        else -> BadgeStyle(
            "Open",
            if (isPull) Icons.Outlined.MergeType else Icons.Outlined.ErrorOutline,
            cs.primaryContainer, cs.onPrimaryContainer,
        )
    }
    Surface(shape = RoundedCornerShape(50), color = bg, contentColor = fg) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class BadgeStyle(val text: String, val icon: ImageVector, val bg: Color, val fg: Color)

// ------------------------------------------------------------------ states

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(88.dp).clip(MaterialShapes.Cookie6Sided.toShape())
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(88.dp).clip(MaterialShapes.Burst.toShape())
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
        }
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            FilledTonalButton(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

// ------------------------------------------------------------------ refresh & paging

@Composable
fun RefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
        content = content,
    )
}

@Composable
fun LoadMoreEffect(state: LazyListState, paged: Paged<*>) {
    val nearEnd by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, paged.items.size) {
        if (nearEnd) paged.loadMore()
    }
}

/** Emits loading / error / empty / items / footer rows for a [Paged] list inside a LazyColumn. */
fun <T> LazyListScope.pagedItems(
    paged: Paged<T>,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptySubtitle: String? = null,
    row: @Composable LazyItemScope.(index: Int, item: T, shape: Shape) -> Unit,
) {
    val items = paged.items
    when {
        !paged.loaded && paged.error != null -> item { ErrorState(paged.error ?: "", paged::refresh) }
        !paged.loaded -> item { LoadingBox() }
        items.isEmpty() -> item { EmptyState(emptyIcon, emptyTitle, subtitle = emptySubtitle) }
        else -> {
            itemsIndexed(items) { i, it -> row(i, it, segmentShape(i, items.size)) }
            if (paged.loadingMore) {
                item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(40.dp)) } }
            } else if (paged.error != null) {
                item { ErrorState(paged.error ?: "", paged::loadMore) }
            }
        }
    }
}

/** A full-screen paged list with pull-to-refresh. */
@Composable
fun <T> PagedLazyColumn(
    paged: Paged<T>,
    emptyIcon: ImageVector,
    emptyTitle: String,
    modifier: Modifier = Modifier,
    emptySubtitle: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    header: (LazyListScope.() -> Unit)? = null,
    row: @Composable LazyItemScope.(index: Int, item: T, shape: Shape) -> Unit,
) {
    val state = rememberLazyListState()
    LoadMoreEffect(state, paged)
    RefreshBox(paged.loading && paged.loaded, paged::refresh, modifier) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            header?.invoke(this)
            pagedItems(paged, emptyIcon, emptyTitle, emptySubtitle, row)
        }
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun MetaText(text: String, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun HSpace(width: Dp) = Spacer(Modifier.width(width))
