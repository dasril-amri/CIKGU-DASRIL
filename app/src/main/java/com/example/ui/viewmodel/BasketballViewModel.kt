package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffects
import com.example.data.local.AppDatabase
import com.example.data.local.BasketballRepository
import com.example.data.local.TournamentHub
import com.example.data.local.entity.MatchRecord
import com.example.data.local.entity.PlayerProfile
import com.example.data.local.entity.RoomPlayer
import com.example.data.local.entity.TournamentRoom
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class BasketballViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BasketballRepository(db.basketballDao())

    init {
        TournamentHub.initialize(repository)
        viewModelScope.launch {
            repository.getProfile()
        }
    }

    // Player Profile State
    val playerProfile: StateFlow<PlayerProfile> = repository.playerProfile
        .map { it ?: PlayerProfile() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerProfile())

    // Match Records History
    val matchRecords: StateFlow<List<MatchRecord>> = repository.matchRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Tournament Room State
    val activeTournamentRoom: StateFlow<TournamentRoom?> = TournamentHub.activeRoom
    val tournamentPlayers: StateFlow<List<RoomPlayer>> = TournamentHub.roomPlayers
    val tournamentCountdown: StateFlow<Int?> = TournamentHub.countdown

    // Solo Game State
    private val _soloGameMode = MutableStateFlow(GameMode.SOLO_FREE)
    val soloGameMode: StateFlow<GameMode> = _soloGameMode.asStateFlow()

    private val _currentShotZone = MutableStateFlow(ShotZone.TWO_POINT_MID)
    val currentShotZone: StateFlow<ShotZone> = _currentShotZone.asStateFlow()

    private val _soloScore = MutableStateFlow(0)
    val soloScore: StateFlow<Int> = _soloScore.asStateFlow()

    private val _soloStreak = MutableStateFlow(0)
    val soloStreak: StateFlow<Int> = _soloStreak.asStateFlow()

    private val _highestStreakInMatch = MutableStateFlow(0)
    val highestStreakInMatch: StateFlow<Int> = _highestStreakInMatch.asStateFlow()

    private val _totalShotsTaken = MutableStateFlow(0)
    val totalShotsTaken: StateFlow<Int> = _totalShotsTaken.asStateFlow()

    private val _totalShotsScored = MutableStateFlow(0)
    val totalShotsScored: StateFlow<Int> = _totalShotsScored.asStateFlow()

    private val _twoPointsCount = MutableStateFlow(0)
    val twoPointsCount: StateFlow<Int> = _twoPointsCount.asStateFlow()

    private val _threePointsCount = MutableStateFlow(0)
    val threePointsCount: StateFlow<Int> = _threePointsCount.asStateFlow()

    private val _swishCount = MutableStateFlow(0)
    val swishCount: StateFlow<Int> = _swishCount.asStateFlow()

    private val _soloTimerSeconds = MutableStateFlow<Int?>(null)
    val soloTimerSeconds: StateFlow<Int?> = _soloTimerSeconds.asStateFlow()

    private val _isGamePaused = MutableStateFlow(false)
    val isGamePaused: StateFlow<Boolean> = _isGamePaused.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _hoopSpeed = MutableStateFlow(HoopSpeed.MEDIUM)
    val hoopSpeed: StateFlow<HoopSpeed> = _hoopSpeed.asStateFlow()

    // Admin State
    private val _adminPassword = MutableStateFlow("admin123")
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    // Audio Mute State
    private val _isSfxMuted = MutableStateFlow(SoundEffects.isSfxMuted())
    val isSfxMuted: StateFlow<Boolean> = _isSfxMuted.asStateFlow()

    private val _isMusicMuted = MutableStateFlow(SoundEffects.isMusicMuted())
    val isMusicMuted: StateFlow<Boolean> = _isMusicMuted.asStateFlow()

    private val _isMuted = MutableStateFlow(SoundEffects.isAudioMuted())
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var soloTimerJob: Job? = null

    // -------------------------------------------------------------
    // PROFILE ACTIONS
    // -------------------------------------------------------------
    fun saveFullProfile(name: String, jerseyNumber: Int, avatarId: String, ballSkinId: String) {
        viewModelScope.launch {
            val current = repository.getProfile()
            val updated = current.copy(
                name = name.trim().ifBlank { "Shooter Ace" },
                jerseyNumber = jerseyNumber.coerceIn(0, 99),
                avatarId = avatarId,
                ballSkinId = ballSkinId
            )
            repository.saveProfile(updated)
        }
    }

    fun updatePlayerName(name: String) {
        viewModelScope.launch {
            val current = repository.getProfile()
            repository.saveProfile(current.copy(name = name.trim().ifBlank { "Shooter Ace" }))
        }
    }

    fun updateAvatar(avatar: Avatar) {
        viewModelScope.launch {
            val current = repository.getProfile()
            repository.saveProfile(
                current.copy(
                    avatarId = avatar.id,
                    jerseyNumber = avatar.defaultNumber
                )
            )
        }
    }

    fun updateJerseyNumber(number: Int) {
        viewModelScope.launch {
            val current = repository.getProfile()
            repository.saveProfile(current.copy(jerseyNumber = number.coerceIn(0, 99)))
        }
    }

    fun updateBallSkin(skin: BallSkin) {
        viewModelScope.launch {
            val current = repository.getProfile()
            repository.saveProfile(current.copy(ballSkinId = skin.id))
        }
    }

    // -------------------------------------------------------------
    // SOLO GAMEPLAY ACTIONS
    // -------------------------------------------------------------
    fun startSoloGame(mode: GameMode, hoopSpeedPreset: HoopSpeed = HoopSpeed.MEDIUM) {
        _soloGameMode.value = mode
        _hoopSpeed.value = hoopSpeedPreset
        _soloScore.value = 0
        _soloStreak.value = 0
        _highestStreakInMatch.value = 0
        _totalShotsTaken.value = 0
        _totalShotsScored.value = 0
        _twoPointsCount.value = 0
        _threePointsCount.value = 0
        _swishCount.value = 0
        _isGamePaused.value = false
        _isGameOver.value = false

        // Default initial zone based on mode
        _currentShotZone.value = when (mode) {
            GameMode.SOLO_3PT_CONTEST -> ShotZone.THREE_POINT
            GameMode.SOLO_DISTANCE_LADDER -> ShotZone.TWO_POINT_CLOSE
            else -> ShotZone.TWO_POINT_MID
        }

        soloTimerJob?.cancel()
        if (mode == GameMode.SOLO_TIME_ATTACK) {
            _soloTimerSeconds.value = 60
            startSoloTimer(60)
        } else {
            _soloTimerSeconds.value = null
        }

        // Start background music loop
        SoundEffects.startMusic()
    }

    private fun startSoloTimer(seconds: Int) {
        soloTimerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                if (!_isGamePaused.value) {
                    remaining--
                    _soloTimerSeconds.value = remaining
                    if (remaining == 0) {
                        finishSoloGame()
                    }
                }
            }
        }
    }

    fun setShotZone(zone: ShotZone) {
        _currentShotZone.value = zone
    }

    fun togglePause() {
        _isGamePaused.value = !_isGamePaused.value
    }

    fun onShotTaken() {
        _totalShotsTaken.value += 1
    }

    fun onScoreMade(points: Int, isSwish: Boolean, isBankShot: Boolean) {
        _totalShotsScored.value += 1
        val newStreak = _soloStreak.value + 1
        _soloStreak.value = newStreak
        if (newStreak > _highestStreakInMatch.value) {
            _highestStreakInMatch.value = newStreak
        }

        if (points >= 3) {
            _threePointsCount.value += 1
        } else {
            _twoPointsCount.value += 1
        }

        if (isSwish) {
            _swishCount.value += 1
        }

        // Streak multiplier for arcade mode
        val streakBonus = if (newStreak >= 5) 3 else if (newStreak >= 3) 1 else 0
        _soloScore.value += (points + streakBonus)

        if (newStreak == 3 || newStreak == 5) {
            SoundEffects.playStreak()
        }

        // Distance Ladder mode progression: step further back
        if (_soloGameMode.value == GameMode.SOLO_DISTANCE_LADDER) {
            val count = _totalShotsScored.value
            _currentShotZone.value = when {
                count >= 9 -> ShotZone.DEEP_THREE
                count >= 6 -> ShotZone.THREE_POINT
                count >= 3 -> ShotZone.TWO_POINT_MID
                else -> ShotZone.TWO_POINT_CLOSE
            }
        }
    }

    fun onShotMissed() {
        _soloStreak.value = 0
    }

    fun finishSoloGame() {
        soloTimerJob?.cancel()
        _isGameOver.value = true
        SoundEffects.playBuzzer()

        // Save match record to Room
        viewModelScope.launch {
            val totalTaken = _totalShotsTaken.value
            val totalScored = _totalShotsScored.value
            val acc = if (totalTaken > 0) (totalScored.toFloat() / totalTaken) * 100f else 0f

            val record = MatchRecord(
                gameMode = _soloGameMode.value.displayName,
                score = _soloScore.value,
                totalShots = totalTaken,
                totalGoals = totalScored,
                twoPointsScored = _twoPointsCount.value,
                threePointsScored = _threePointsCount.value,
                swishCount = _swishCount.value,
                highestStreak = _highestStreakInMatch.value,
                accuracyPercent = acc
            )
            repository.saveMatchRecord(record)

            // Update user profile aggregate
            val currentProf = repository.getProfile()
            val newHighestSolo = maxOf(currentProf.highestScoreSolo, _soloScore.value)
            val newHighestStreak = maxOf(currentProf.highestStreak, _highestStreakInMatch.value)

            repository.saveProfile(
                currentProf.copy(
                    totalScore = currentProf.totalScore + _soloScore.value,
                    totalShots = currentProf.totalShots + totalTaken,
                    totalGoals = currentProf.totalGoals + totalScored,
                    highestScoreSolo = newHighestSolo,
                    highestStreak = newHighestStreak,
                    matchesPlayed = currentProf.matchesPlayed + 1
                )
            )
        }
    }

    // -------------------------------------------------------------
    // GROUP TOURNAMENT & ADMIN CONTROLS
    // -------------------------------------------------------------
    fun verifyAdminPassword(pin: String): Boolean {
        val valid = (pin.trim() == _adminPassword.value.trim())
        _isAdminAuthenticated.value = valid
        return valid
    }

    fun updateAdminPassword(newPin: String) {
        if (newPin.isNotBlank()) {
            _adminPassword.value = newPin.trim()
        }
    }

    fun createTournamentRoom(
        roomCode: String,
        roomName: String,
        durationSeconds: Int,
        targetScore: Int,
        gameMode: String,
        hoopSpeed: String
    ) {
        TournamentHub.createRoom(
            roomCode = roomCode,
            roomName = roomName,
            adminPin = _adminPassword.value,
            durationSeconds = durationSeconds,
            targetScore = targetScore,
            gameMode = gameMode,
            hoopSpeed = hoopSpeed
        )
    }

    fun joinTournamentRoom(roomCode: String): Boolean {
        val prof = playerProfile.value
        return TournamentHub.joinRoom(
            roomCode = roomCode,
            playerId = prof.id,
            playerName = prof.name,
            avatarId = prof.avatarId,
            jerseyNumber = prof.jerseyNumber
        )
    }

    fun addDemoCompetitor(roomCode: String) {
        TournamentHub.addDemoCompetitor(roomCode)
    }

    fun removePlayerFromRoom(playerId: String) {
        TournamentHub.removePlayer(playerId)
    }

    fun startTournamentMatch() {
        SoundEffects.playWhistle()
        TournamentHub.startMatch()
    }

    fun pauseTournamentMatch() {
        TournamentHub.pauseMatch()
    }

    fun finishTournamentMatch() {
        SoundEffects.playBuzzer()
        TournamentHub.finishMatch()
    }

    fun resetTournament() {
        TournamentHub.resetTournament()
    }

    fun submitTournamentShot(points: Int, isGoal: Boolean, streak: Int) {
        val prof = playerProfile.value
        TournamentHub.recordPlayerShot(
            playerId = prof.id,
            points = points,
            isGoal = isGoal,
            streak = streak
        )
    }

    // -------------------------------------------------------------
    // SOUND & AUDIO CONTROLS
    // -------------------------------------------------------------
    fun playSoundEffect(type: String) {
        when (type) {
            "swish" -> SoundEffects.playSwish()
            "score" -> SoundEffects.playScore()
            "rim" -> SoundEffects.playRimHit()
            "backboard" -> SoundEffects.playBackboard()
            "bounce" -> SoundEffects.playBounce()
            "buzzer" -> SoundEffects.playBuzzer()
            "whistle" -> SoundEffects.playWhistle()
            "whoosh" -> SoundEffects.playWhoosh()
            "streak" -> SoundEffects.playStreak()
        }
    }

    fun toggleSfx() {
        val muted = SoundEffects.toggleSfx()
        _isSfxMuted.value = muted
    }

    fun toggleMusic() {
        val muted = SoundEffects.toggleMusic()
        _isMusicMuted.value = muted
    }

    fun toggleMute() {
        val muted = SoundEffects.toggleMute()
        _isMuted.value = muted
        _isSfxMuted.value = muted
        _isMusicMuted.value = muted
    }

    fun stopMusic() {
        SoundEffects.stopMusic()
    }
}
