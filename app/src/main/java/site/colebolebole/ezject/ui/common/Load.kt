package site.colebolebole.ezject.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import site.colebolebole.ezject.data.ListResult

/** A single loadable value with loading / error state. */
class Single<T>(
    private val scope: CoroutineScope,
    private val fetch: suspend () -> T,
) {
    var value by mutableStateOf<T?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var gen = 0

    fun load() {
        val g = ++gen
        scope.launch {
            loading = true
            error = null
            try {
                val v = fetch()
                if (g == gen) value = v
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (g == gen) error = e.message ?: "Something went wrong"
            } finally {
                if (g == gen) loading = false
            }
        }
    }

    fun ensureLoaded() {
        if (value == null && !loading && error == null) load()
    }

    /** Replaces the cached value locally (e.g. after an optimistic update). */
    fun set(v: T?) {
        value = v
    }
}

/** A paginated list: first page, load-more, refresh. */
class Paged<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int = 30,
    private val fetch: suspend (page: Int, limit: Int) -> ListResult<T>,
) {
    var items by mutableStateOf<List<T>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var endReached by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var total by mutableStateOf<Int?>(null)
        private set

    private var page = 1
    private var gen = 0

    fun refresh() {
        val g = ++gen
        scope.launch {
            loading = true
            loadingMore = false
            error = null
            try {
                val r = fetch(1, pageSize)
                if (g == gen) {
                    items = r.items
                    total = r.total
                    page = 1
                    endReached = r.items.size < pageSize
                    loaded = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (g == gen) error = e.message ?: "Something went wrong"
            } finally {
                if (g == gen) loading = false
            }
        }
    }

    fun loadMore() {
        if (!loaded || loading || loadingMore || endReached) return
        val g = gen
        scope.launch {
            loadingMore = true
            error = null
            try {
                val r = fetch(page + 1, pageSize)
                if (g == gen) {
                    items = items + r.items
                    page += 1
                    endReached = r.items.size < pageSize
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (g == gen) error = e.message ?: "Something went wrong"
            } finally {
                if (g == gen) loadingMore = false
            }
        }
    }

    fun ensureLoaded() {
        if (!loaded && !loading && error == null) refresh()
    }
}

/** ViewModel factory that exposes the nav-entry arguments through a [SavedStateHandle]. */
@Composable
inline fun <reified VM : ViewModel> navViewModel(
    key: String? = null,
    crossinline create: (SavedStateHandle) -> VM,
): VM = viewModel(
    key = key,
    factory = viewModelFactory { initializer { create(createSavedStateHandle()) } },
)
