package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GameScanner
import com.example.model.DisplayConfig
import com.example.model.GameProfile
import com.example.model.InstalledGame
import com.example.model.ProjectionEngine
import com.example.model.ScalingMode
import com.example.model.SessionState
import com.example.model.SessionStatus
import com.example.model.ShizukuState
import com.example.model.ShizukuStatusInfo
import com.example.service.FloatingBubbleService
import com.example.shizuku.DisplayCommandRunner
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StretchXViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val profileDao = database.gameProfileDao()
    private val gameScanner = GameScanner(application)

    val shizukuStatus: StateFlow<ShizukuStatusInfo> = ShizukuManager.status

    private val _nativeConfig = MutableStateFlow(DisplayCommandRunner.getHardwareBaseline(application))
    val nativeConfig: StateFlow<DisplayConfig> = _nativeConfig.asStateFlow()

    private val _currentConfig = MutableStateFlow(_nativeConfig.value)
    val currentConfig: StateFlow<DisplayConfig> = _currentConfig.asStateFlow()

    private val _installedGames = MutableStateFlow<List<InstalledGame>>(emptyList())
    val installedGames: StateFlow<List<InstalledGame>> = _installedGames.asStateFlow()

    val gameProfiles: StateFlow<List<GameProfile>> = profileDao.getAllProfilesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteProfiles: StateFlow<List<GameProfile>> = profileDao.getFavoritesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sessionState = MutableStateFlow(
        SessionState(
            currentConfig = _currentConfig.value,
            nativeConfig = _nativeConfig.value,
            sessionStatus = SessionStatus.IDLE
        )
    )
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    // Filter & Search states
    val searchQuery = MutableStateFlow("")
    val onlyConfiguredFilter = MutableStateFlow(false)

    // Filtered installed games
    val filteredGames: StateFlow<List<InstalledGame>> = combine(
        _installedGames,
        searchQuery,
        onlyConfiguredFilter
    ) { games, query, onlyConfigured ->
        games.filter { game ->
            val matchesQuery = query.isBlank() ||
                    game.appName.contains(query, ignoreCase = true) ||
                    game.packageName.contains(query, ignoreCase = true)
            val matchesConfigured = !onlyConfigured || game.hasCustomProfile
            matchesQuery && matchesConfigured
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Safety Countdown Job
    private var countdownJob: Job? = null

    // Selected game for editing
    private val _editingGame = MutableStateFlow<InstalledGame?>(null)
    val editingGame: StateFlow<InstalledGame?> = _editingGame.asStateFlow()

    init {
        refreshHardwareDisplayMetrics()
        refreshInstalledGames()

        // Observe profile changes to keep games list updated
        viewModelScope.launch {
            gameProfiles.collect { profiles ->
                updateInstalledGamesWithProfiles(profiles)
            }
        }
    }

    fun refreshAll() {
        ShizukuManager.refreshStatus()
        refreshHardwareDisplayMetrics()
        refreshInstalledGames()
    }

    fun requestShizukuAuth() {
        ShizukuManager.requestPermission(getApplication())
    }

    fun openShizukuApp() {
        ShizukuManager.openShizukuApp(getApplication())
    }

    fun refreshHardwareDisplayMetrics() {
        viewModelScope.launch {
            val baseline = DisplayCommandRunner.getHardwareBaseline(getApplication())
            var finalNative = baseline
            var currentActive = baseline

            if (shizukuStatus.value.state.isConnected) {
                val (physicalSize, overrideSize) = DisplayCommandRunner.queryDisplaySize()
                val (physicalDensity, overrideDensity) = DisplayCommandRunner.queryDisplayDensity()

                val nativeW = physicalSize?.width ?: baseline.width
                val nativeH = physicalSize?.height ?: baseline.height
                val nativeDpi = physicalDensity ?: baseline.densityDpi
                finalNative = DisplayConfig(nativeW, nativeH, nativeDpi)

                val activeW = overrideSize?.width ?: nativeW
                val activeH = overrideSize?.height ?: nativeH
                val activeDpi = overrideDensity ?: nativeDpi
                currentActive = DisplayConfig(activeW, activeH, activeDpi)
            }

            _nativeConfig.value = finalNative
            _currentConfig.value = currentActive
            _sessionState.value = _sessionState.value.copy(
                nativeConfig = finalNative,
                currentConfig = currentActive,
                isAltered = currentActive.width != finalNative.width ||
                        currentActive.height != finalNative.height ||
                        currentActive.densityDpi != finalNative.densityDpi
            )
        }
    }

    fun refreshInstalledGames() {
        viewModelScope.launch {
            val games = gameScanner.getInstalledGames(gameProfiles.value)
            _installedGames.value = games
        }
    }

    private fun updateInstalledGamesWithProfiles(profiles: List<GameProfile>) {
        val map = profiles.associateBy { it.packageName }
        _installedGames.value = _installedGames.value.map { game ->
            val profile = map[game.packageName]
            game.copy(
                hasCustomProfile = profile != null,
                configuredProfile = profile
            )
        }
    }

    fun openProfileEditor(game: InstalledGame) {
        _editingGame.value = game
    }

    fun closeProfileEditor() {
        _editingGame.value = null
    }

    fun saveProfile(profile: GameProfile) {
        viewModelScope.launch {
            profileDao.insertOrUpdate(profile)
            _editingGame.value = null
            refreshInstalledGames()
        }
    }

    fun deleteProfile(packageName: String) {
        viewModelScope.launch {
            profileDao.deleteByPackage(packageName)
            refreshInstalledGames()
        }
    }

    fun toggleFavorite(profile: GameProfile) {
        viewModelScope.launch {
            profileDao.insertOrUpdate(profile.copy(isFavorite = !profile.isFavorite))
        }
    }

    /**
     * Activates a per-game projection session using the Cat Mouse Pro projection multiplier model.
     * Starts the Game Session Watcher which guarantees automatic restoration whenever the launcher or
     * other applications enter the foreground.
     */
    fun activateProjection(
        config: DisplayConfig,
        multiplier: Float,
        mode: ScalingMode,
        targetPackage: String?
    ) {
        countdownJob?.cancel()
        val game = installedGames.value.firstOrNull { it.packageName == targetPackage }
        val gameName = game?.appName ?: "Active Game"

        viewModelScope.launch {
            val result = DisplayCommandRunner.applyConfiguration(config)
            if (result.isSuccess) {
                _currentConfig.value = config
                _sessionState.value = _sessionState.value.copy(
                    activePackage = targetPackage,
                    activeGameName = gameName,
                    sessionStatus = SessionStatus.GAME_ACTIVE,
                    currentConfig = config,
                    projectionMultiplier = multiplier,
                    scalingMode = mode,
                    isAltered = true,
                    lastAppliedMessage = "Projection active: ${config.longEdge}×${config.shortEdge} (${String.format("%.2f", multiplier)}×)"
                )

                // Start Game Session Lifecycle Coordinator
                val context = getApplication<Application>()
                FloatingBubbleService.startSession(
                    context = context,
                    packageName = targetPackage ?: context.packageName,
                    gameName = gameName,
                    multiplier = multiplier,
                    mode = mode,
                    targetConfig = config
                )

                if (targetPackage != null) {
                    gameScanner.launchGame(targetPackage)
                }
            } else {
                _sessionState.value = _sessionState.value.copy(
                    lastAppliedMessage = "Activation failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun confirmAndKeepResolution() {
        countdownJob?.cancel()
        _sessionState.value = _sessionState.value.copy(
            isCountdownActive = false,
            countdownSecondsRemaining = 0,
            lastAppliedMessage = "Projection confirmed"
        )
    }

    /**
     * Restores device physical display resolution and density, terminating the active game session.
     */
    fun revertResolutionNow(reason: String = "Display reset to native") {
        countdownJob?.cancel()
        FloatingBubbleService.stopSession(getApplication())

        viewModelScope.launch {
            val result = DisplayCommandRunner.resetDisplay()
            if (result.isSuccess) {
                _currentConfig.value = _nativeConfig.value
                _sessionState.value = _sessionState.value.copy(
                    activePackage = null,
                    activeGameName = null,
                    sessionStatus = SessionStatus.IDLE,
                    currentConfig = _nativeConfig.value,
                    isAltered = false,
                    isCountdownActive = false,
                    countdownSecondsRemaining = 0,
                    lastAppliedMessage = reason
                )
            } else {
                _sessionState.value = _sessionState.value.copy(
                    lastAppliedMessage = "Reset failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Launches a game with its configured projection profile, starting the session manager.
     */
    fun launchGameWithProfile(game: InstalledGame) {
        viewModelScope.launch {
            val profile = game.configuredProfile ?: GameProfile(
                packageName = game.packageName,
                appName = game.appName,
                projectionMultiplier = 1.11f,
                scalingModeName = ScalingMode.STRETCH.name
            )

            val target = ProjectionEngine.calculateProjection(
                native = nativeConfig.value,
                multiplier = profile.projectionMultiplier,
                mode = profile.scalingMode
            )

            if (profile.autoApplyOnLaunch) {
                DisplayCommandRunner.applyConfiguration(target)
                _currentConfig.value = target
                _sessionState.value = _sessionState.value.copy(
                    activePackage = game.packageName,
                    activeGameName = game.appName,
                    sessionStatus = SessionStatus.GAME_ACTIVE,
                    currentConfig = target,
                    projectionMultiplier = profile.projectionMultiplier,
                    scalingMode = profile.scalingMode,
                    isAltered = true
                )
            }

            profileDao.updateLastUsed(game.packageName, System.currentTimeMillis())

            val context = getApplication<Application>()
            FloatingBubbleService.startSession(
                context = context,
                packageName = game.packageName,
                gameName = game.appName,
                multiplier = profile.projectionMultiplier,
                mode = profile.scalingMode,
                targetConfig = target
            )

            gameScanner.launchGame(game.packageName)
        }
    }

    fun stopFloatingOverlay() {
        FloatingBubbleService.stopSession(getApplication())
    }

    fun requestOverlayPermission(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
