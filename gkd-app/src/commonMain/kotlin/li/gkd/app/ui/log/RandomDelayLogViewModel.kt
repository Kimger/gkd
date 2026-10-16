package li.gkd.app.ui.log

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.gkd.app.logging.RandomDelayLog
import li.gkd.app.logging.readRandomDelayLogs
import li.gkd.app.storage.appStorage
import li.gkd.app.ui.state.BaseViewModel
import li.gkd.app.util.LogUtils

class RandomDelayLogViewModel : BaseViewModel() {
    val pagingDataFlow = Pager(PagingConfig(pageSize = 100, enablePlaceholders = false)) {
        RandomDelayLogPagingSource()
    }.flow.cachedIn(scope)
}

private class RandomDelayLogPagingSource : PagingSource<Int, RandomDelayLog>() {
    private var snapshot: List<RandomDelayLog>? = null

    override fun getRefreshKey(state: PagingState<Int, RandomDelayLog>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RandomDelayLog> =
        withContext(Dispatchers.IO) {
            try {
                val records = snapshot ?: run {
                    LogUtils.flush()
                    readRandomDelayLogs(appStorage().log).also { snapshot = it }
                }
                val start = params.key ?: 0
                val end = minOf(start + params.loadSize, records.size)
                LoadResult.Page(
                    data = records.subList(start, end),
                    prevKey = null,
                    nextKey = end.takeIf { it < records.size },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
}
