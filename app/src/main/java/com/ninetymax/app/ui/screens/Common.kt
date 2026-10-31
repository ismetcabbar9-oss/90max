package com.ninetymax.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ninetymax.app.ui.Page
import java.time.LocalDate

@Composable
fun TabbedPage(
    title: String,
    tabs: List<String>,
    back: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable (Int) -> Unit = {},
    content: @Composable (Int) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Page(title, back = back, actions = actions, fab = { fab(tab) }) {
        Column(Modifier.fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp, containerColor = MaterialTheme.colorScheme.background) {
                tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
            Box(Modifier.weight(1f)) { content(tab) }
        }
    }
}

fun today(): LocalDate = LocalDate.now()
