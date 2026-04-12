package com.example.paradacerta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.paradacerta.navigation.AppNavGraph
import com.example.paradacerta.navigation.Screen
import com.example.paradacerta.ui.theme.ParadaCertaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParadaCertaTheme {
                ParadaCertaApp()
            }
        }
    }
}

/**
 * Composable principal do aplicativo com Bottom Navigation
 */
@Composable
fun ParadaCertaApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define se deve mostrar o Bottom Navigation
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Map.route,
        Screen.Scanner.route,
        Screen.Profile.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop até o início do grafo para evitar stack gigante
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Evitar múltiplas cópias da mesma destination
                            launchSingleTop = true
                            // Restaurar estado ao voltar para a tela
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Bottom Navigation Bar com os 4 itens principais
 */
@Composable
private fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Item do Bottom Navigation
 */
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val title: String
)

/**
 * Lista de itens do Bottom Navigation
 */
private val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        icon = Icons.Default.Home,
        title = "Início"
    ),
    BottomNavItem(
        route = Screen.Map.route,
        icon = Icons.Default.LocationOn,
        title = "Mapa"
    ),
    BottomNavItem(
        route = Screen.Scanner.route,
        icon = Icons.Default.QrCodeScanner,
        title = "Scanner"
    ),
    BottomNavItem(
        route = Screen.Profile.route,
        icon = Icons.Default.Person,
        title = "Perfil"
    )
)