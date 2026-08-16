package com.example.data.local

import com.example.data.local.entity.RoomPlayer
import com.example.data.local.entity.TournamentRoom
import com.example.model.PRESET_AVATARS
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

object TournamentHub {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var repository: BasketballRepository? = null

    private val _activeRoom = MutableStateFlow<TournamentRoom?>(null)
    val activeRoom: StateFlow<TournamentRoom?> = _activeRoom.asStateFlow()

    private val _roomPlayers = MutableStateFlow<List<RoomPlayer>>(emptyList())
    val roomPlayers: StateFlow<List<RoomPlayer>> = _roomPlayers.asStateFlow()

    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()

    private var matchTimerJob: Job? = null
    private var botSimulationJob: Job? = null

    fun initialize(repo: BasketballRepository) {
        repository = repo
    }

    fun createRoom(
        roomCode: String,
        roomName: String,
        adminPin: String,
        durationSeconds: Int,
        targetScore: Int,
        gameMode: String,
        hoopSpeed: String
    ) {
        val newRoom = TournamentRoom(
            roomCode = roomCode.uppercase(),
            roomName = roomName.ifBlank { "Turnamen Basket #$roomCode" },
            adminPin = adminPin.ifBlank { "admin123" },
            durationSeconds = durationSeconds,
            targetScore = targetScore,
            gameMode = gameMode,
            hoopSpeed = hoopSpeed,
            isStarted = false,
            isPaused = false,
            isFinished = false,
            remainingSeconds = durationSeconds
        )
        _activeRoom.value = newRoom
        _roomPlayers.value = emptyList()

        scope.launch {
            repository?.saveRoom(newRoom)
        }
    }

    fun joinRoom(
        roomCode: String,
        playerId: String,
        playerName: String,
        avatarId: String,
        jerseyNumber: Int
    ): Boolean {
        val code = roomCode.uppercase()
        val current = _activeRoom.value
        if (current == null || current.roomCode != code) {
            // Check in repository
            val room = runBlocking { repository?.getRoom(code) } ?: return false
            _activeRoom.value = room
        }

        val existing = _roomPlayers.value.find { it.id == playerId }
        val player = existing?.copy(
            playerName = playerName,
            avatarId = avatarId,
            jerseyNumber = jerseyNumber,
            isReady = true
        ) ?: RoomPlayer(
            id = playerId,
            roomCode = code,
            playerName = playerName,
            avatarId = avatarId,
            jerseyNumber = jerseyNumber,
            isReady = true
        )

        val updated = _roomPlayers.value.filterNot { it.id == playerId } + player
        _roomPlayers.value = updated

        scope.launch {
            repository?.saveRoomPlayer(player)
        }
        return true
    }

    fun addDemoCompetitor(roomCode: String) {
        val index = (_roomPlayers.value.size % PRESET_AVATARS.size)
        val preset = PRESET_AVATARS[index]
        val botId = "bot_${System.currentTimeMillis() % 10000}"
        val botName = "Rival ${preset.name.split(" ").first()}"
        
        val player = RoomPlayer(
            id = botId,
            roomCode = roomCode,
            playerName = botName,
            avatarId = preset.id,
            jerseyNumber = preset.defaultNumber,
            score = 0,
            isReady = true
        )
        _roomPlayers.value = _roomPlayers.value + player
        scope.launch {
            repository?.saveRoomPlayer(player)
        }
    }

    fun removePlayer(playerId: String) {
        val roomCode = _activeRoom.value?.roomCode ?: return
        _roomPlayers.value = _roomPlayers.value.filterNot { it.id == playerId }
        scope.launch {
            repository?.removePlayer(roomCode, playerId)
        }
    }

    fun startMatch() {
        val room = _activeRoom.value ?: return
        if (room.isStarted && !room.isPaused) return

        scope.launch {
            // 3-second countdown
            for (i in 3 downTo 1) {
                _countdown.value = i
                delay(1000)
            }
            _countdown.value = 0
            delay(500)
            _countdown.value = null

            val startedRoom = room.copy(
                isStarted = true,
                isPaused = false,
                isFinished = false,
                remainingSeconds = room.durationSeconds
            )
            _activeRoom.value = startedRoom
            repository?.saveRoom(startedRoom)

            startTimerLoop()
            startBotSimulation()
        }
    }

    fun pauseMatch() {
        val room = _activeRoom.value ?: return
        val paused = room.copy(isPaused = !room.isPaused)
        _activeRoom.value = paused
        scope.launch { repository?.saveRoom(paused) }
    }

    fun finishMatch() {
        matchTimerJob?.cancel()
        botSimulationJob?.cancel()
        val room = _activeRoom.value ?: return
        val finished = room.copy(
            isStarted = true,
            isPaused = false,
            isFinished = true,
            remainingSeconds = 0
        )
        _activeRoom.value = finished
        scope.launch { repository?.saveRoom(finished) }
    }

    fun recordPlayerShot(
        playerId: String,
        points: Int,
        isGoal: Boolean,
        streak: Int
    ) {
        val current = _roomPlayers.value.toMutableList()
        val index = current.indexOfFirst { it.id == playerId }
        if (index >= 0) {
            val p = current[index]
            val newTwo = p.twoPointsScored + if (isGoal && points == 2) 1 else 0
            val newThree = p.threePointsScored + if (isGoal && points == 3) 1 else 0
            val newGoals = p.totalGoals + if (isGoal) 1 else 0
            val newScore = p.score + if (isGoal) points else 0
            val newStreak = maxOf(p.highestStreak, streak)

            val updatedPlayer = p.copy(
                score = newScore,
                totalShots = p.totalShots + 1,
                totalGoals = newGoals,
                twoPointsScored = newTwo,
                threePointsScored = newThree,
                highestStreak = newStreak,
                updatedAt = System.currentTimeMillis()
            )
            current[index] = updatedPlayer
            
            // Recalculate ranks
            val sorted = current.sortedByDescending { it.score }
            val ranked = sorted.mapIndexed { rankIdx, player ->
                player.copy(rank = rankIdx + 1)
            }
            _roomPlayers.value = ranked

            scope.launch {
                repository?.saveRoomPlayer(updatedPlayer)
            }

            // Check if target score reached
            val target = _activeRoom.value?.targetScore ?: 0
            if (target > 0 && newScore >= target) {
                finishMatch()
            }
        }
    }

    private fun startTimerLoop() {
        matchTimerJob?.cancel()
        matchTimerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val current = _activeRoom.value ?: break
                if (current.isPaused || current.isFinished) continue

                val nextSeconds = current.remainingSeconds - 1
                if (nextSeconds <= 0) {
                    val finished = current.copy(remainingSeconds = 0, isFinished = true)
                    _activeRoom.value = finished
                    repository?.saveRoom(finished)
                    botSimulationJob?.cancel()
                    break
                } else {
                    val updated = current.copy(remainingSeconds = nextSeconds)
                    _activeRoom.value = updated
                }
            }
        }
    }

    private fun startBotSimulation() {
        botSimulationJob?.cancel()
        botSimulationJob = scope.launch {
            while (isActive) {
                delay(Random.nextLong(2200, 4800))
                val currentRoom = _activeRoom.value ?: break
                if (currentRoom.isPaused || currentRoom.isFinished) continue

                val bots = _roomPlayers.value.filter { it.id.startsWith("bot_") }
                if (bots.isEmpty()) continue

                val randomBot = bots.random()
                val isThreePoint = Random.nextBoolean()
                val isGoal = Random.nextFloat() > 0.38f
                val points = if (isThreePoint) 3 else 2

                recordPlayerShot(
                    playerId = randomBot.id,
                    points = points,
                    isGoal = isGoal,
                    streak = if (isGoal) 1 else 0
                )
            }
        }
    }

    fun resetTournament() {
        matchTimerJob?.cancel()
        botSimulationJob?.cancel()
        val room = _activeRoom.value ?: return
        val reset = room.copy(
            isStarted = false,
            isPaused = false,
            isFinished = false,
            remainingSeconds = room.durationSeconds
        )
        _activeRoom.value = reset
        _roomPlayers.value = _roomPlayers.value.map {
            it.copy(
                score = 0,
                twoPointsScored = 0,
                threePointsScored = 0,
                totalShots = 0,
                totalGoals = 0,
                highestStreak = 0,
                rank = 1
            )
        }
        scope.launch {
            repository?.saveRoom(reset)
            _roomPlayers.value.forEach { repository?.saveRoomPlayer(it) }
        }
    }
}
