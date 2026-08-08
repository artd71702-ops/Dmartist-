package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.ConversationHistoryDrawerContent
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.OmniAiTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val useDynamicColor by viewModel.useDynamicColor.collectAsState()
            val themePreset by viewModel.themePreset.collectAsState()

            OmniAiTheme(
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
                themePreset = themePreset
            ) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val conversations by viewModel.conversations.collectAsState()
                val currentConversationId by viewModel.currentConversationId.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ConversationHistoryDrawerContent(
                            conversations = conversations,
                            currentConversationId = currentConversationId,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { query -> viewModel.onSearchQueryChanged(query) },
                            onSelectConversation = { id ->
                                viewModel.selectConversation(id)
                                scope.launch { drawerState.close() }
                            },
                            onNewConversation = {
                                viewModel.createNewConversation()
                                scope.launch { drawerState.close() }
                            },
                            onDeleteConversation = { id ->
                                viewModel.deleteConversation(id)
                            },
                            onOpenDashboard = {
                                scope.launch { drawerState.close() }
                                navController.navigate("dashboard")
                            },
                            onOpenSettings = {
                                scope.launch { drawerState.close() }
                                navController.navigate("settings")
                            }
                        )
                    }
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "chat",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("chat") {
                            ChatScreen(
                                viewModel = viewModel,
                                onOpenDrawer = {
                                    scope.launch { drawerState.open() }
                                },
                                onOpenSettings = {
                                    navController.navigate("settings")
                                },
                                onOpenDashboard = {
                                    navController.navigate("dashboard")
                                }
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
