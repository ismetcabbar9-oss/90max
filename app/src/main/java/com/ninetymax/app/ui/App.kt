package com.ninetymax.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ninetymax.app.data.AppData
import com.ninetymax.app.data.Repository
import com.ninetymax.app.notify.Reminders
import com.ninetymax.app.ui.components.LocalWide
import com.ninetymax.app.ui.screens.*
import com.ninetymax.app.ui.theme.MaxTheme

val LocalRepo = staticCompositionLocalOf<Repository> { error("Repository yok") }
val LocalNav = staticCompositionLocalOf<NavHostController> { error("Nav yok") }
val LocalSnackbar = compositionLocalOf { SnackbarHostState() }

@Composable
fun appData(): AppData {
    val state by LocalRepo.current.state.collectAsStateWithLifecycle()
    return state
}

data class Dest(val route: String, val label: String, val icon: ImageVector)

object Routes {
    const val TODAY = "today"; const val WORKOUT = "workout"; const val NUTRITION = "nutrition"
    const val PRODUCTS = "products"; const val MORE = "more"; const val CARE = "care"; const val BODY = "body"
    const val STYLE = "style"; const val PROGRESS = "progress"; const val SETTINGS = "settings"
    const val SUPPLEMENTS = "supplements"; const val ROUTINES = "routines"
    fun product(id: String) = "product/$id"
    fun productEdit(id: String) = "productEdit/$id"
    fun exercise(id: String) = "exercise/$id"
    fun session(templateId: String) = "session/$templateId"
}

private val phoneDests = listOf(
    Dest(Routes.TODAY, "Bugün", Icons.Rounded.Today),
    Dest(Routes.WORKOUT, "Antrenman", Icons.Rounded.FitnessCenter),
    Dest(Routes.NUTRITION, "Beslenme", Icons.Rounded.Restaurant),
    Dest(Routes.PRODUCTS, "Ürünler", Icons.Rounded.ShoppingBag),
    Dest(Routes.MORE, "Daha", Icons.Rounded.GridView),
)

val moreDests = listOf(
    Dest(Routes.CARE, "Bakım", Icons.Rounded.Spa),
    Dest(Routes.BODY, "Duruş & Uyku", Icons.Rounded.SelfImprovement),
    Dest(Routes.SUPPLEMENTS, "Takviyeler", Icons.Rounded.Medication),
    Dest(Routes.STYLE, "Stil", Icons.Rounded.Checkroom),
    Dest(Routes.PROGRESS, "Gelişim", Icons.Rounded.ShowChart),
    Dest(Routes.SETTINGS, "Ayarlar", Icons.Rounded.Settings),
)

private val railDests = phoneDests.dropLast(1) + moreDests

@Composable
fun MaxRoot(repo: Repository) {
    val data by repo.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(data.reminders) { Reminders.scheduleAll(context, data.reminders) }
    MaxTheme(data.settings.theme) {
        CompositionLocalProvider(LocalRepo provides repo) {
            if (!data.onboarded) OnboardingScreen() else MainShell()
        }
    }
}

@Composable
private fun MainShell() {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    fun go(route: String) {
        // Sekme zaten yığındaysa köküne dön (ör. alt ekrandayken "Daha"ya dokunmak menüyü açar).
        if (nav.popBackStack(route, inclusive = false)) return
        nav.navigate(route) {
            popUpTo(Routes.TODAY) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val wide = maxWidth >= 840.dp
        CompositionLocalProvider(LocalNav provides nav, LocalWide provides wide, LocalSnackbar provides snackbar) {
            Row(Modifier.fillMaxSize()) {
                if (useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Spacer(Modifier.height(8.dp))
                            railDests.forEach { d ->
                                NavigationRailItem(
                                    selected = current == d.route, onClick = { go(d.route) },
                                    icon = { Icon(d.icon, null) }, label = { Text(d.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                )
                            }
                        }
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        if (!useRail) NavigationBar {
                            phoneDests.forEach { d ->
                                val selected = current == d.route || (d.route == Routes.MORE && moreDests.any { it.route == current })
                                NavigationBarItem(selected = selected, onClick = { go(d.route) }, icon = { Icon(d.icon, null) }, label = { Text(d.label, maxLines = 1) })
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.padding(padding).consumeWindowInsets(padding)) { AppNavHost(nav) }
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController) {
    NavHost(nav, startDestination = Routes.TODAY) {
        composable(Routes.TODAY) { TodayScreen() }
        composable(Routes.WORKOUT) { WorkoutScreen() }
        composable(Routes.NUTRITION) { NutritionScreen() }
        composable(Routes.PRODUCTS) { ProductsScreen() }
        composable(Routes.MORE) { MoreScreen() }
        composable(Routes.CARE) { CareScreen() }
        composable(Routes.BODY) { BodyScreen() }
        composable(Routes.SUPPLEMENTS) { SupplementsScreen() }
        composable(Routes.STYLE) { StyleScreen() }
        composable(Routes.PROGRESS) { ProgressScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
        composable(Routes.ROUTINES) { RoutinesScreen() }
        composable("product/{id}") { ProductDetailScreen(it.arguments?.getString("id").orEmpty()) }
        composable("productEdit/{id}") { ProductEditScreen(it.arguments?.getString("id").orEmpty()) }
        composable("exercise/{id}") { ExerciseDetailScreen(it.arguments?.getString("id").orEmpty()) }
        composable("session/{id}") { SessionScreen(it.arguments?.getString("id").orEmpty()) }
    }
}

/** Her ekranın üst çubuğu ve isteğe bağlı FAB'ı. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page(
    title: String,
    back: Boolean = false,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val nav = LocalNav.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { if (back) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") } },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = fab,
        containerColor = MaterialTheme.colorScheme.background,
    ) { p -> Box(Modifier.padding(p).consumeWindowInsets(p)) { content() } }
}
