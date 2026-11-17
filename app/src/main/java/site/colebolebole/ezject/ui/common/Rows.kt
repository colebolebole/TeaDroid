package site.colebolebole.ezject.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import site.colebolebole.ezject.data.Issue
import site.colebolebole.ezject.data.PullRequest
import site.colebolebole.ezject.data.Repo
import site.colebolebole.ezject.data.User

@Composable
fun RepoRow(repo: Repo, shape: Shape, onClick: () -> Unit) {
    SegmentCard(shape = shape, onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Avatar(repo.owner.avatarUrl, repo.owner.login, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        repo.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (repo.private) Icon(Icons.Outlined.Lock, "Private", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (repo.description.isNotBlank()) {
                    Text(
                        repo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (repo.language.isNotBlank()) MetaText(repo.language)
                    IconMeta(Icons.Outlined.StarBorder, repo.stars.toString())
                    IconMeta(Icons.Outlined.CallSplit, repo.forks.toString())
                    if (repo.archived) MetaText("Archived")
                    if (repo.fork) MetaText("Fork")
                    if (repo.mirror) MetaText("Mirror")
                    MetaText("Updated ${timeAgo(repo.updatedAt)}")
                }
            }
        }
    }
}

@Composable
fun IconMeta(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        MetaText(text)
    }
}

@Composable
fun IssueRow(issue: Issue, shape: Shape, showRepo: Boolean, onClick: () -> Unit) {
    SegmentCard(shape = shape, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showRepo && issue.repository != null) {
                MetaText(issue.repository.fullName)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text(
                    issue.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StateBadge(
                    issue.state,
                    merged = issue.pullRequest?.merged == true,
                    draft = issue.pullRequest?.draft == true,
                    isPull = issue.isPull,
                )
            }
            if (issue.labels.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    issue.labels.forEach { LabelChip(it) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MetaText("#${issue.number} by ${issue.user.login} · ${timeAgo(issue.createdAt)}", Modifier.weight(1f, fill = false))
                if (issue.comments > 0) IconMeta(Icons.Outlined.ChatBubbleOutline, issue.comments.toString())
            }
        }
    }
}

@Composable
fun PullRow(pr: PullRequest, shape: Shape, onClick: () -> Unit) {
    SegmentCard(shape = shape, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text(
                    pr.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StateBadge(pr.state, merged = pr.merged, draft = pr.draft, isPull = true)
            }
            if (pr.labels.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    pr.labels.forEach { LabelChip(it) }
                }
            }
            MetaText("#${pr.number} by ${pr.user.login} · ${pr.head.ref} → ${pr.base.ref}")
            MetaText(timeAgo(pr.updatedAt.ifBlank { pr.createdAt }))
        }
    }
}

@Composable
fun UserRow(user: User, shape: Shape, onClick: () -> Unit) {
    SegmentCard(shape = shape, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(user.avatarUrl, user.login, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (user.fullName.isNotBlank()) MetaText("@${user.login}")
                if (user.description.isNotBlank()) MetaText(user.description, maxLines = 2)
            }
        }
    }
}
