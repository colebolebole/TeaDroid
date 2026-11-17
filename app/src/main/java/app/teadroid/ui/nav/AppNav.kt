package app.teadroid.ui.nav

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.background
import app.teadroid.data.Graph
import app.teadroid.ui.issue.IssueScreen
import app.teadroid.ui.issue.NewIssueScreen
import app.teadroid.ui.login.LoginScreen
import app.teadroid.ui.main.MainScreen
import app.teadroid.ui.repo.FileScreen
import app.teadroid.ui.repo.RepoScreen
import app.teadroid.ui.search.SearchScreen
import app.teadroid.ui.settings.SettingsScreen
import app.teadroid.ui.user.UserScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    const val REPO = "repo?owner={owner}&repo={repo}"
    const val FILE = "file?owner={owner}&repo={repo}&ref={ref}&path={path}"
    const val ISSUE = "issue?owner={owner}&repo={repo}&number={number}&pull={pull}"
    const val NEW_ISSUE = "newissue?owner={owner}&repo={repo}"
    const val USER = "user?name={name}"

    private fun e(s: String) = Uri.encode(s)

    fun repo(owner: String, repo: String) = "repo?owner=${e(owner)}&repo=${e(repo)}"
    fun repo(fullName: String): String {
        val (o, r) = fullName.split('/', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        return repo(o, r)
    }

    fun file(owner: String, repo: String, ref: String, path: String) =
        "file?owner=${e(owner)}&repo=${e(repo)}&ref=${e(ref)}&path=${e(path)}"

    fun issue(owner: String, repo: String, number: Int, pull: Boolean) =
        "issue?owner=${e(owner)}&repo=${e(repo)}&number=$number&pull=$pull"

    fun newIssue(owner: String, repo: String) = "newissue?owner=${e(owner)}&repo=${e(repo)}"
    fun user(name: String) = "user?name=${e(name)}"
}

private fun strArg(name: String) = navArgument(name) { type = NavType.StringType; defaultValue = "" }

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val start = if (Graph.session.isLoggedIn) Routes.MAIN else Routes.LOGIN
    val spec = spring<Float>(dampingRatio = 0.9f, stiffness = 500f)

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { fadeIn(spec) + scaleIn(spec, initialScale = 0.94f) }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { fadeOut(spec) }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { fadeIn(spec) }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { fadeOut(spec) + scaleOut(spec, targetScale = 0.94f) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = nav,
            startDestination = start,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit,
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(onLoggedIn = {
                    nav.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
                })
            }
            composable(Routes.MAIN) { MainScreen(nav) }
            composable(Routes.REPO, listOf(strArg("owner"), strArg("repo"))) { RepoScreen(nav) }
            composable(
                Routes.FILE,
                listOf(strArg("owner"), strArg("repo"), strArg("ref"), strArg("path")),
            ) { FileScreen(nav) }
            composable(
                Routes.ISSUE,
                listOf(
                    strArg("owner"), strArg("repo"),
                    navArgument("number") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("pull") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { IssueScreen(nav) }
            composable(Routes.NEW_ISSUE, listOf(strArg("owner"), strArg("repo"))) { NewIssueScreen(nav) }
            composable(Routes.USER, listOf(strArg("name"))) { entry ->
                UserScreen(nav, name = entry.arguments?.getString("name").orEmpty(), isMe = false)
            }
            composable(Routes.SEARCH) { SearchScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(nav) }
        }
    }
}

fun NavController.signOutToLogin() {
    Graph.session.signOut()
    navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
}
