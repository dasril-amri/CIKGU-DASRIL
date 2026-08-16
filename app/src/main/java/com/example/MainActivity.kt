package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.model.GameMode
import com.example.model.HoopSpeed
import com.example.ui.screens.*
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BasketballViewModel

enum class Screen {
    HOME,
    SOLO_GAME,
    GROUP_LOBBY,
    GROUP_GAME,
    ADMIN_PORTAL,
    MATCH_RESULT,
    STATS_HISTORY
}

class MainActivity : ComponentActivity() {
    private val viewModel: BasketballViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    var currentScreen by remember { mutableStateOf(Screen.HOME) }
                    var isGroupTournamentResult by remember { mutableStateOf(false) }

                    // Hardware back button navigation
                    BackHandler(enabled = currentScreen != Screen.HOME) {
                        when (currentScreen) {
                            Screen.SOLO_GAME -> {
                                viewModel.finishSoloGame()
                                currentScreen = Screen.HOME
                            }
                            Screen.GROUP_GAME -> {
                                currentScreen = Screen.GROUP_LOBBY
                            }
                            Screen.MATCH_RESULT, Screen.ADMIN_PORTAL, Screen.GROUP_LOBBY, Screen.STATS_HISTORY -> {
                                currentScreen = Screen.HOME
                            }
                            else -> {
                                currentScreen = Screen.HOME
                            }
                        }
                    }

                    Crossfade(targetState = currentScreen, label = "screenTransition") { screen ->
                        when (screen) {
                            Screen.HOME -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onStartSoloMode = { mode, speed ->
                                        viewModel.startSoloGame(mode, speed)
                                        currentScreen = Screen.SOLO_GAME
                                    },
                                    onNavigateToGroupLobby = {
                                        currentScreen = Screen.GROUP_LOBBY
                                    },
                                    onNavigateToAdmin = {
                                        currentScreen = Screen.ADMIN_PORTAL
                                    },
                                    onNavigateToStats = {
                                        currentScreen = Screen.STATS_HISTORY
                                    }
                                )
                            }

                            Screen.SOLO_GAME -> {
                                SoloGameScreen(
                                    viewModel = viewModel,
                                    onFinishGame = {
                                        isGroupTournamentResult = false
                                        currentScreen = Screen.MATCH_RESULT
                                    },
                                    onExitToHome = {
                                        currentScreen = Screen.HOME
                                    }
                                )
                            }

                            Screen.GROUP_LOBBY -> {
                                GroupLobbyScreen(
                                    viewModel = viewModel,
                                    onMatchStarted = {
                                        currentScreen = Screen.GROUP_GAME
                                    },
                                    onBackToHome = {
                                        currentScreen = Screen.HOME
                                    }
                                )
                            }

                            Screen.GROUP_GAME -> {
                                GroupPlayerGameScreen(
                                    viewModel = viewModel,
                                    onMatchFinished = {
                                        isGroupTournamentResult = true
                                        currentScreen = Screen.MATCH_RESULT
                                    }
                                )
                            }

                            Screen.ADMIN_PORTAL -> {
                                AdminPortalScreen(
                                    viewModel = viewModel,
                                    onBackToHome = {
                                        currentScreen = Screen.HOME
                                    }
                                )
                            }

                            Screen.MATCH_RESULT -> {
                                MatchResultScreen(
                                    viewModel = viewModel,
                                    isGroupTournament = isGroupTournamentResult,
                                    onPlayAgain = {
                                        if (isGroupTournamentResult) {
                                            currentScreen = Screen.GROUP_LOBBY
                                        } else {
                                            viewModel.startSoloGame(viewModel.soloGameMode.value)
                                            currentScreen = Screen.SOLO_GAME
                                        }
                                    },
                                    onBackToHome = {
                                        currentScreen = Screen.HOME
                                    }
                                )
                            }

                            Screen.STATS_HISTORY -> {
                                StatsHistoryScreen(
                                    viewModel = viewModel,
                                    onBackToHome = {
                                        currentScreen = Screen.HOME
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
