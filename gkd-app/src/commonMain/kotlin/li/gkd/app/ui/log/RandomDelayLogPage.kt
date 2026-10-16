package li.gkd.app.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import li.gkd.app.logging.RandomDelayLog
import li.gkd.app.resources.*
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.*
import li.gkd.app.ui.home.appLabel
import li.gkd.app.ui.image.GkAppIcon
import li.gkd.app.ui.navigation.AppConfigRoute
import li.gkd.app.ui.share.noRippleClickable
import li.gkd.app.ui.style.itemHorizontalPadding
import li.gkd.app.ui.style.scaffoldPadding
import li.gkd.app.util.copyText
import org.jetbrains.compose.resources.stringResource

@Composable
fun RandomDelayLogPage() {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { RandomDelayLogViewModel() }
    val list = vm.pagingDataFlow.collectAsLazyPagingItems()
    var shownLog by remember { mutableStateOf<RandomDelayLog?>(null) }
    val pageScrollState = rememberListScrollState()
    val scrollBehavior = pageScrollState.scrollBehavior
    pageScrollState.ResetOnChange(list.itemCount > 0)

    GkScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GkTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    GkIconButton(imageVector = GkIcons.ArrowBack, onClick = { mainVm.navigator.pop() })
                },
                title = {
                    Text(
                        stringResource(Res.string.random_delay_log_title),
                        Modifier.noRippleClickable(onClick = pageScrollState::resetScroll),
                    )
                },
                actions = {
                    TextButton(onClick = { list.refresh() }) {
                        Text(stringResource(Res.string.random_delay_log_refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.scaffoldPadding(padding)) {
            Text(
                stringResource(Res.string.random_delay_log_description),
                Modifier.padding(horizontal = itemHorizontalPadding, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val error = list.loadState.refresh as? LoadState.Error
            if (error != null) {
                Text(stringResource(Res.string.data_load_failed), Modifier.padding(itemHorizontalPadding))
                Text(error.error.message.orEmpty(), Modifier.padding(horizontal = itemHorizontalPadding))
                TextButton(onClick = { list.retry() }) {
                    Text(stringResource(Res.string.random_delay_log_refresh))
                }
            } else {
                GkLogTimeline(
                    items = list,
                    listState = pageScrollState.listState,
                    key = { it.id },
                    appId = { it.appId },
                    time = { it.time },
                    appLabel = { if (it.isEmpty()) stringResource(Res.string.random_delay_log_unknown_app) else appLabel(it) },
                    appIcon = { if (it.isNotEmpty()) GkAppIcon(it, 24.dp) },
                    appName = { id, modifier ->
                        if (id.isEmpty()) {
                            Text(stringResource(Res.string.random_delay_log_unknown_app), modifier)
                        } else {
                            GkAppNameText(id, modifier = modifier, style = MaterialTheme.typography.titleSmall)
                        }
                    },
                    onOpenApp = { if (it.isNotEmpty()) mainVm.navigator.navigate(AppConfigRoute(it)) },
                ) { log ->
                    Column(
                        Modifier.fillMaxWidth()
                            .gkLogTimelineRail(MaterialTheme.colorScheme.outlineVariant)
                            .clickable { shownLog = log }
                            .padding(start = 56.dp, end = itemHorizontalPadding, top = 6.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(Res.string.random_delay_log_value, log.delayMillis.toString()),
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            GkLogTimeText(log.time)
                        }
                        Text(
                            log.details.lineSequence().firstOrNull { it.startsWith("[1]: ") }?.removePrefix("[1]: ").orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    shownLog?.let { log ->
        GkAlertDialog(
            onDismissRequest = { shownLog = null },
            title = { Text(stringResource(Res.string.random_delay_log_title)) },
            text = { Text(log.details) },
            confirmButton = {
                TextButton(onClick = { copyText(log.details) }) {
                    Text(stringResource(Res.string.random_delay_log_copy))
                }
            },
            dismissButton = {
                TextButton(onClick = { shownLog = null }) { Text(stringResource(Res.string.action_close)) }
            },
        )
    }
}
