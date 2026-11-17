package site.colebolebole.ezject.data

import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ApiException(val code: Int, message: String) : Exception(message)

private typealias Query = List<Pair<String, String>>

/**
 * Thin client over the Gitea REST API (`/api/v1`).
 * Auth: `Authorization: token <PAT>` (added by an interceptor for requests to the configured host,
 * which also covers avatar/image loads through Coil).
 */
class GiteaApi(private val session: SessionStore) {

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val req = chain.request()
            val server = session.serverUrl.toHttpUrlOrNull()
            val token = session.token
            if (server != null && token.isNotBlank() && req.header("Authorization") == null && req.url.host == server.host) {
                chain.proceed(req.newBuilder().header("Authorization", "token $token").build())
            } else {
                chain.proceed(req)
            }
        })
        .build()

    // ------------------------------------------------------------------ plumbing

    private fun buildUrl(path: String, query: Query, server: String): HttpUrl {
        val b = (server.trimEnd('/') + "/api/v1/").toHttpUrl().newBuilder()
        if (path.isNotBlank()) b.addPathSegments(path.trim('/'))
        query.forEach { (k, v) -> b.addQueryParameter(k, v) }
        return b.build()
    }

    fun rawUrl(owner: String, repo: String, path: String, ref: String?): String =
        buildUrl("repos/$owner/$repo/raw/$path", if (ref.isNullOrBlank()) emptyList() else listOf("ref" to ref), session.serverUrl).toString()

    private suspend fun <T> request(
        method: String,
        path: String,
        query: Query = emptyList(),
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        server: String = session.serverUrl,
        parse: (Response) -> T,
    ): T = withContext(Dispatchers.IO) {
        val rb = Request.Builder().url(buildUrl(path, query, server)).header("Accept", "application/json")
        headers.forEach { (k, v) -> rb.header(k, v) }
        if (method == "GET") rb.get() else rb.method(method, (body ?: "").toRequestBody(JSON))
        val resp = try {
            http.newCall(rb.build()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw ApiException(0, "Can't reach the server (${e.message ?: "network error"})")
        }
        resp.use { r ->
            if (!r.isSuccessful) throw ApiException(r.code, errorMessage(r.code, r.body.string()))
            parse(r)
        }
    }

    private fun errorMessage(code: Int, text: String): String {
        val msg = runCatching { json.decodeFromString(ErrorBody.serializer(), text).message }.getOrDefault("")
        return when {
            code == 401 -> "Authentication failed. Check your token or password."
            code == 403 -> msg.ifBlank { "You don't have permission to do that." }
            code == 404 -> msg.ifBlank { "Not found." }
            msg.isNotBlank() -> msg
            else -> "Request failed (HTTP $code)"
        }
    }

    private suspend fun <T> getOne(path: String, ser: KSerializer<T>, query: Query = emptyList()): T =
        request("GET", path, query) { json.decodeFromString(ser, it.body.string()) }

    private suspend fun <T> getList(path: String, ser: KSerializer<T>, query: Query = emptyList()): ListResult<T> =
        request("GET", path, query) {
            ListResult(
                json.decodeFromString(ListSerializer(ser), it.body.string()),
                it.header("X-Total-Count")?.toIntOrNull(),
            )
        }

    private suspend fun send(method: String, path: String, body: String? = null, query: Query = emptyList()) {
        request(method, path, query, body) { }
    }

    private fun paging(page: Int, limit: Int): Query = listOf("page" to page.toString(), "limit" to limit.toString())

    // ------------------------------------------------------------------ auth

    suspend fun verify(server: String, token: String): User =
        request("GET", "user", server = server, headers = mapOf("Authorization" to "token $token")) {
            json.decodeFromString(User.serializer(), it.body.string())
        }

    /** Creates a personal access token with basic auth (optionally with a TOTP code). */
    suspend fun createToken(server: String, username: String, password: String, otp: String?): String {
        val name = "EZject Android " + System.currentTimeMillis().toString(36)
        val headers = buildMap {
            put("Authorization", Credentials.basic(username, password))
            if (!otp.isNullOrBlank()) put("X-GITEA-OTP", otp)
        }
        val body = json.encodeToString(CreateAccessTokenBody.serializer(), CreateAccessTokenBody(name, listOf("all")))
        return request("POST", "users/$username/tokens", body = body, headers = headers, server = server) {
            json.decodeFromString(AccessToken.serializer(), it.body.string()).sha1
        }
    }

    suspend fun me(): User = getOne("user", User.serializer())
    suspend fun user(name: String): User = getOne("users/$name", User.serializer())
    suspend fun version(): String = getOne("version", VersionInfo.serializer()).version

    // ------------------------------------------------------------------ repos

    suspend fun myRepos(page: Int, limit: Int) = getList("user/repos", Repo.serializer(), paging(page, limit))
    suspend fun starredRepos(page: Int, limit: Int) = getList("user/starred", Repo.serializer(), paging(page, limit))
    suspend fun userRepos(name: String, page: Int, limit: Int) =
        getList("users/$name/repos", Repo.serializer(), paging(page, limit))

    suspend fun repo(owner: String, name: String): Repo = getOne("repos/$owner/$name", Repo.serializer())

    suspend fun searchRepos(q: String, page: Int, limit: Int): ListResult<Repo> =
        request("GET", "repos/search", listOf("q" to q, "sort" to "updated", "order" to "desc") + paging(page, limit)) {
            ListResult(
                json.decodeFromString(RepoSearchResult.serializer(), it.body.string()).data,
                it.header("X-Total-Count")?.toIntOrNull(),
            )
        }

    suspend fun searchUsers(q: String, page: Int, limit: Int): ListResult<User> =
        request("GET", "users/search", listOf("q" to q) + paging(page, limit)) {
            ListResult(
                json.decodeFromString(UserSearchResult.serializer(), it.body.string()).data,
                it.header("X-Total-Count")?.toIntOrNull(),
            )
        }

    suspend fun createRepo(name: String, description: String, private: Boolean): Repo =
        request(
            "POST", "user/repos",
            body = json.encodeToString(CreateRepoBody.serializer(), CreateRepoBody(name, description, private, true)),
        ) { json.decodeFromString(Repo.serializer(), it.body.string()) }

    suspend fun isStarred(owner: String, name: String): Boolean =
        try {
            request("GET", "user/starred/$owner/$name") { true }
        } catch (e: ApiException) {
            if (e.code == 404) false else throw e
        }

    suspend fun setStarred(owner: String, name: String, starred: Boolean) =
        send(if (starred) "PUT" else "DELETE", "user/starred/$owner/$name")

    suspend fun branches(owner: String, name: String): List<Branch> =
        getList("repos/$owner/$name/branches", Branch.serializer(), paging(1, 100)).items

    suspend fun commits(owner: String, name: String, ref: String?, page: Int, limit: Int): ListResult<Commit> =
        getList(
            "repos/$owner/$name/commits", Commit.serializer(),
            buildList {
                if (!ref.isNullOrBlank()) add("sha" to ref)
                add("stat" to "false"); add("verification" to "false"); add("files" to "false")
                addAll(paging(page, limit))
            },
        )

    suspend fun releases(owner: String, name: String, page: Int, limit: Int) =
        getList("repos/$owner/$name/releases", Release.serializer(), paging(page, limit))

    suspend fun labels(owner: String, name: String): List<Label> =
        getList("repos/$owner/$name/labels", Label.serializer(), paging(1, 100)).items

    // ------------------------------------------------------------------ files

    suspend fun listDir(owner: String, name: String, path: String, ref: String?): List<ContentEntry> {
        val query = if (ref.isNullOrBlank()) emptyList() else listOf("ref" to ref)
        val p = if (path.isBlank()) "repos/$owner/$name/contents" else "repos/$owner/$name/contents/$path"
        return request("GET", p, query) {
            val text = it.body.string()
            if (text.trimStart().startsWith("[")) json.decodeFromString(ListSerializer(ContentEntry.serializer()), text)
            else listOf(json.decodeFromString(ContentEntry.serializer(), text))
        }
    }

    suspend fun fileEntry(owner: String, name: String, path: String, ref: String?): ContentEntry =
        getOne(
            "repos/$owner/$name/contents/$path", ContentEntry.serializer(),
            if (ref.isNullOrBlank()) emptyList() else listOf("ref" to ref),
        )

    suspend fun rawBytes(owner: String, name: String, path: String, ref: String?): ByteArray =
        request(
            "GET", "repos/$owner/$name/raw/$path",
            if (ref.isNullOrBlank()) emptyList() else listOf("ref" to ref),
            headers = mapOf("Accept" to "*/*"),
        ) { it.body.bytes() }

    fun decodeBase64(content: String): ByteArray = Base64.decode(content, Base64.DEFAULT)

    // ------------------------------------------------------------------ issues & pulls

    suspend fun repoIssues(owner: String, name: String, state: String, page: Int, limit: Int) =
        getList(
            "repos/$owner/$name/issues", Issue.serializer(),
            listOf("state" to state, "type" to "issues") + paging(page, limit),
        )

    suspend fun repoPulls(owner: String, name: String, state: String, page: Int, limit: Int) =
        getList(
            "repos/$owner/$name/pulls", PullRequest.serializer(),
            listOf("state" to state) + paging(page, limit),
        )

    /** Cross-repo issue/PR search. [scope] is one of assigned, created, mentioned, review_requested (or null). */
    suspend fun searchIssues(type: String, state: String, scope: String?, page: Int, limit: Int) =
        getList(
            "repos/issues/search", Issue.serializer(),
            buildList {
                add("type" to type); add("state" to state)
                if (scope != null) add(scope to "true")
                addAll(paging(page, limit))
            },
        )

    suspend fun issue(owner: String, name: String, number: Int): Issue =
        getOne("repos/$owner/$name/issues/$number", Issue.serializer())

    suspend fun pull(owner: String, name: String, number: Int): PullRequest =
        getOne("repos/$owner/$name/pulls/$number", PullRequest.serializer())

    suspend fun comments(owner: String, name: String, number: Int): List<Comment> =
        getList("repos/$owner/$name/issues/$number/comments", Comment.serializer()).items

    suspend fun addComment(owner: String, name: String, number: Int, body: String) =
        send(
            "POST", "repos/$owner/$name/issues/$number/comments",
            json.encodeToString(CommentBody.serializer(), CommentBody(body)),
        )

    suspend fun setIssueState(owner: String, name: String, number: Int, state: String) =
        send(
            "PATCH", "repos/$owner/$name/issues/$number",
            json.encodeToString(EditIssueBody.serializer(), EditIssueBody(state)),
        )

    suspend fun createIssue(owner: String, name: String, title: String, body: String, labels: List<Long>): Issue =
        request(
            "POST", "repos/$owner/$name/issues",
            body = json.encodeToString(CreateIssueBody.serializer(), CreateIssueBody(title, body, labels)),
        ) { json.decodeFromString(Issue.serializer(), it.body.string()) }

    suspend fun mergePull(owner: String, name: String, number: Int, style: String, deleteBranch: Boolean) =
        send(
            "POST", "repos/$owner/$name/pulls/$number/merge",
            json.encodeToString(MergeBody.serializer(), MergeBody(style, deleteBranch)),
        )

    /** Issue + (for PRs) pull details + comments, fetched in parallel. */
    suspend fun issueBundle(owner: String, name: String, number: Int, isPull: Boolean): IssueBundle =
        coroutineScope {
            val issue = async { issue(owner, name, number) }
            val pull = if (isPull) async { pull(owner, name, number) } else null
            val comments = async { comments(owner, name, number) }
            IssueBundle(issue.await(), pull?.await(), comments.await())
        }

    // ------------------------------------------------------------------ activity & notifications

    suspend fun feeds(name: String, page: Int, limit: Int) =
        getList("users/$name/activities/feeds", Activity.serializer(), paging(page, limit))

    suspend fun notifications(all: Boolean, page: Int, limit: Int): ListResult<Notification> =
        getList(
            "notifications", Notification.serializer(),
            buildList {
                if (all) add("all" to "true") else { add("status-types" to "unread"); add("status-types" to "pinned") }
                addAll(paging(page, limit))
            },
        )

    suspend fun unreadCount(): Long = getOne("notifications/new", NotificationCount.serializer()).new

    suspend fun markAllRead() = send("PUT", "notifications")
    suspend fun markRead(id: Long) = send("PATCH", "notifications/threads/$id", query = listOf("to-status" to "read"))

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

data class IssueBundle(val issue: Issue, val pull: PullRequest?, val comments: List<Comment>)

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            cont.resume(response) { _, value, _ -> value.close() }
        }
    })
    cont.invokeOnCancellation { runCatching { cancel() } }
}
