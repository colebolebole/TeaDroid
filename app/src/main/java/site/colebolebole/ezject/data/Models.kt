package site.colebolebole.ezject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long = 0,
    val login: String = "",
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val description: String = "",
    val location: String = "",
    val website: String = "",
    val created: String = "",
    @SerialName("followers_count") val followers: Int = 0,
    @SerialName("following_count") val following: Int = 0,
    @SerialName("starred_repos_count") val starredRepos: Int = 0,
    @SerialName("is_admin") val isAdmin: Boolean = false,
) {
    val displayName: String get() = fullName.ifBlank { login }
}

@Serializable
data class Permissions(
    val admin: Boolean = false,
    val push: Boolean = false,
    val pull: Boolean = false,
)

@Serializable
data class Repo(
    val id: Long = 0,
    val name: String = "",
    @SerialName("full_name") val fullName: String = "",
    val description: String = "",
    val owner: User = User(),
    val private: Boolean = false,
    val fork: Boolean = false,
    val archived: Boolean = false,
    val empty: Boolean = false,
    val mirror: Boolean = false,
    val template: Boolean = false,
    @SerialName("stars_count") val stars: Int = 0,
    @SerialName("forks_count") val forks: Int = 0,
    @SerialName("watchers_count") val watchers: Int = 0,
    @SerialName("open_issues_count") val openIssues: Int = 0,
    @SerialName("open_pr_counter") val openPulls: Int = 0,
    @SerialName("release_counter") val releaseCount: Int = 0,
    @SerialName("default_branch") val defaultBranch: String = "",
    val language: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("clone_url") val cloneUrl: String = "",
    @SerialName("ssh_url") val sshUrl: String = "",
    val size: Long = 0,
    val website: String = "",
    val topics: List<String> = emptyList(),
    @SerialName("has_issues") val hasIssues: Boolean = true,
    @SerialName("has_pull_requests") val hasPullRequests: Boolean = true,
    @SerialName("has_releases") val hasReleases: Boolean = true,
    val parent: Repo? = null,
    val permissions: Permissions? = null,
)

@Serializable
data class Label(
    val id: Long = 0,
    val name: String = "",
    val color: String = "",
    val description: String = "",
)

@Serializable
data class Milestone(
    val id: Long = 0,
    val title: String = "",
    val state: String = "",
)

@Serializable
data class PullRequestMeta(
    val merged: Boolean = false,
    val draft: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
)

@Serializable
data class RepoMeta(
    val id: Long = 0,
    val name: String = "",
    val owner: String = "",
    @SerialName("full_name") val fullName: String = "",
)

@Serializable
data class Issue(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val body: String = "",
    val state: String = "open",
    val user: User = User(),
    val labels: List<Label> = emptyList(),
    val assignees: List<User> = emptyList(),
    val milestone: Milestone? = null,
    val comments: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("pull_request") val pullRequest: PullRequestMeta? = null,
    val repository: RepoMeta? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("is_locked") val isLocked: Boolean = false,
) {
    val isPull: Boolean get() = pullRequest != null
}

@Serializable
data class BranchRef(
    val label: String = "",
    val ref: String = "",
    val sha: String = "",
)

@Serializable
data class PullRequest(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val body: String = "",
    val state: String = "open",
    val user: User = User(),
    val labels: List<Label> = emptyList(),
    val assignees: List<User> = emptyList(),
    val merged: Boolean = false,
    val mergeable: Boolean = false,
    val draft: Boolean = false,
    val head: BranchRef = BranchRef(),
    val base: BranchRef = BranchRef(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("merged_at") val mergedAt: String? = null,
    @SerialName("merged_by") val mergedBy: User? = null,
    val comments: Int = 0,
    val additions: Int = 0,
    val deletions: Int = 0,
    @SerialName("changed_files") val changedFiles: Int = 0,
    @SerialName("html_url") val htmlUrl: String = "",
)

@Serializable
data class Comment(
    val id: Long = 0,
    val body: String = "",
    val user: User = User(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
)

@Serializable
data class CommitUser(
    val name: String = "",
    val email: String = "",
    val date: String = "",
)

@Serializable
data class RepoCommit(
    val message: String = "",
    val author: CommitUser = CommitUser(),
)

@Serializable
data class Commit(
    val sha: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val commit: RepoCommit = RepoCommit(),
    val author: User? = null,
    val created: String = "",
)

@Serializable
data class ContentEntry(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val type: String = "file",
    val size: Long = 0,
    val encoding: String? = null,
    val content: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
) {
    val isDir: Boolean get() = type == "dir"
}

@Serializable
data class Branch(
    val name: String = "",
    val protected: Boolean = false,
)

@Serializable
data class Attachment(
    val id: Long = 0,
    val name: String = "",
    val size: Long = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("browser_download_url") val url: String = "",
)

@Serializable
data class Release(
    val id: Long = 0,
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    val author: User = User(),
    val assets: List<Attachment> = emptyList(),
    @SerialName("html_url") val htmlUrl: String = "",
)

@Serializable
data class NotificationSubject(
    val title: String = "",
    val url: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val state: String = "",
    val type: String = "",
)

@Serializable
data class Notification(
    val id: Long = 0,
    val unread: Boolean = false,
    val pinned: Boolean = false,
    @SerialName("updated_at") val updatedAt: String = "",
    val subject: NotificationSubject = NotificationSubject(),
    val repository: Repo? = null,
)

@Serializable
data class Activity(
    val id: Long = 0,
    @SerialName("op_type") val opType: String = "",
    @SerialName("act_user") val actUser: User = User(),
    val repo: Repo? = null,
    val content: String = "",
    val created: String = "",
    @SerialName("ref_name") val refName: String = "",
    @SerialName("is_private") val isPrivate: Boolean = false,
)

// ----- Wrappers & request bodies -----

@Serializable
data class RepoSearchResult(val ok: Boolean = true, val data: List<Repo> = emptyList())

@Serializable
data class UserSearchResult(val ok: Boolean = true, val data: List<User> = emptyList())

@Serializable
data class VersionInfo(val version: String = "")

@Serializable
data class NotificationCount(val new: Long = 0)

@Serializable
data class AccessToken(val id: Long = 0, val name: String = "", val sha1: String = "")

@Serializable
data class CreateAccessTokenBody(val name: String, val scopes: List<String>)

@Serializable
data class CreateIssueBody(val title: String, val body: String, val labels: List<Long>)

@Serializable
data class EditIssueBody(val state: String)

@Serializable
data class CommentBody(val body: String)

@Serializable
data class MergeBody(
    @SerialName("do") val style: String,
    @SerialName("delete_branch_after_merge") val deleteBranch: Boolean,
)

@Serializable
data class CreateRepoBody(
    val name: String,
    val description: String,
    val private: Boolean,
    @SerialName("auto_init") val autoInit: Boolean,
)

@Serializable
data class ErrorBody(val message: String = "")

class ListResult<T>(val items: List<T>, val total: Int?)
