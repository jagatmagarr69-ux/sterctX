package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.DisplayConfig
import com.example.model.ProjectionEngine
import com.example.model.ScalingMode
import com.example.shizuku.DisplayCommandRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FloatingBubbleService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var overlayRootView: FrameLayout? = null
    private var isExpanded = false

    private lateinit var layoutParams: WindowManager.LayoutParams
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private var activeGamePackage: String? = null
    private var activeGameName: String = "Game"
    private var currentMultiplier: Float = 1.11f
    private var currentMode: ScalingMode = ScalingMode.STRETCH
    private var targetConfig: DisplayConfig? = null
    private var nativeConfig: DisplayConfig? = null

    // Lifecycle monitoring state
    private var watcherJob: Job? = null
    private var isCurrentlyProjected = false

    companion object {
        private const val NOTIFICATION_ID = 9021
        private const val CHANNEL_ID = "stretchx_session_channel"

        const val EXTRA_GAME_PACKAGE = "extra_game_package"
        const val EXTRA_GAME_NAME = "extra_game_name"
        const val EXTRA_MULTIPLIER = "extra_multiplier"
        const val EXTRA_SCALING_MODE = "extra_scaling_mode"
        const val EXTRA_TARGET_WIDTH = "extra_target_width"
        const val EXTRA_TARGET_HEIGHT = "extra_target_height"
        const val EXTRA_TARGET_DPI = "extra_target_dpi"

        const val ACTION_STOP_SESSION = "com.example.stretchx.ACTION_STOP_SESSION"
        const val ACTION_RESTORE_DISPLAY = "com.example.stretchx.ACTION_RESTORE_DISPLAY"

        fun startSession(
            context: Context,
            packageName: String,
            gameName: String,
            multiplier: Float,
            mode: ScalingMode,
            targetConfig: DisplayConfig
        ) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                putExtra(EXTRA_GAME_PACKAGE, packageName)
                putExtra(EXTRA_GAME_NAME, gameName)
                putExtra(EXTRA_MULTIPLIER, multiplier)
                putExtra(EXTRA_SCALING_MODE, mode.name)
                putExtra(EXTRA_TARGET_WIDTH, targetConfig.width)
                putExtra(EXTRA_TARGET_HEIGHT, targetConfig.height)
                putExtra(EXTRA_TARGET_DPI, targetConfig.densityDpi)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopSession(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_STOP_SESSION
            }
            context.startService(intent)
        }

        fun start(context: Context, gameName: String = "Standby") {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                putExtra(EXTRA_GAME_NAME, gameName)
                putExtra(EXTRA_MULTIPLIER, 1.11f)
                putExtra(EXTRA_SCALING_MODE, ScalingMode.STRETCH.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            stopSession(context)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        nativeConfig = DisplayCommandRunner.getHardwareBaseline(this)
        startForeground(NOTIFICATION_ID, buildNotification("StretchX Session Initializing..."))

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Settings.canDrawOverlays(this)) {
            initOverlay()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SESSION || intent?.action == ACTION_RESTORE_DISPLAY) {
            restoreAndTerminate()
            return START_NOT_STICKY
        }

        intent?.getStringExtra(EXTRA_GAME_PACKAGE)?.let { activeGamePackage = it }
        intent?.getStringExtra(EXTRA_GAME_NAME)?.let { activeGameName = it }
        currentMultiplier = intent?.getFloatExtra(EXTRA_MULTIPLIER, 1.11f) ?: 1.11f
        intent?.getStringExtra(EXTRA_SCALING_MODE)?.let {
            currentMode = try { ScalingMode.valueOf(it) } catch (_: Exception) { ScalingMode.STRETCH }
        }

        val tw = intent?.getIntExtra(EXTRA_TARGET_WIDTH, 0) ?: 0
        val th = intent?.getIntExtra(EXTRA_TARGET_HEIGHT, 0) ?: 0
        val tdpi = intent?.getIntExtra(EXTRA_TARGET_DPI, 0) ?: 0

        targetConfig = if (tw > 0 && th > 0) {
            DisplayConfig(tw, th, tdpi)
        } else {
            val native = nativeConfig ?: DisplayConfig(1080, 2400, 420)
            ProjectionEngine.calculateProjection(native, currentMultiplier, currentMode)
        }

        updateNotification("Active in $activeGameName (${String.format("%.2f", currentMultiplier)}×)")
        updateOverlayContent()

        startLifecycleWatcher()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        watcherJob?.cancel()
        scope.cancel()
        removeOverlay()
    }

    /**
     * Actively watches the foreground app.
     * When the target game is in foreground: applies target projection resolution.
     * When the user returns to the launcher or switches apps: IMMEDIATELY restores native resolution!
     */
    private fun startLifecycleWatcher() {
        watcherJob?.cancel()
        watcherJob = scope.launch(Dispatchers.IO) {
            val pkg = activeGamePackage ?: return@launch
            Log.d("StretchX_Session", "Starting foreground activity lifecycle watcher for $pkg")

            while (isActive) {
                delay(700)
                try {
                    val topPackage = DisplayCommandRunner.queryTopPackage()
                    if (topPackage != null) {
                        val isGameForeground = topPackage == pkg

                        if (isGameForeground && !isCurrentlyProjected) {
                            // User returned to game: apply projection resolution!
                            targetConfig?.let { cfg ->
                                DisplayCommandRunner.applyConfiguration(cfg)
                                isCurrentlyProjected = true
                                Log.i("StretchX_Session", "Game $pkg resumed -> applied projection ${cfg.displayLabel}")
                                launch(Dispatchers.Main) {
                                    updateNotification("Game Active: $activeGameName (${String.format("%.2f", currentMultiplier)}×)")
                                    updateOverlayContent()
                                }
                            }
                        } else if (!isGameForeground && isCurrentlyProjected && topPackage != packageName) {
                            // User left game (home screen, settings, etc.): RESTORE NATIVE IMMEDIATELY!
                            DisplayCommandRunner.resetDisplay()
                            isCurrentlyProjected = false
                            Log.i("StretchX_Session", "User left game (now in $topPackage) -> restored native display")
                            launch(Dispatchers.Main) {
                                updateNotification("Launcher Clean — Native Display Restored")
                                updateOverlayContent()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("StretchX_Session", "Error during lifecycle check", e)
                }
            }
        }
    }

    private fun restoreAndTerminate() {
        watcherJob?.cancel()
        scope.launch(Dispatchers.IO) {
            DisplayCommandRunner.resetDisplay()
            isCurrentlyProjected = false
            launch(Dispatchers.Main) {
                removeOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "StretchX Game Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live per-game session status and auto-restores display on exit"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val restoreIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, EmergencyResetReceiver::class.java).apply {
                action = EmergencyResetReceiver.ACTION_EMERGENCY_RESET
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FloatingBubbleService::class.java).apply {
                action = ACTION_STOP_SESSION
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StretchX: $activeGameName")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_crop)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_revert, "RESTORE DISPLAY", restoreIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "END SESSION", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverlay() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        overlayRootView = FrameLayout(this)
        buildViews()

        overlayRootView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        windowManager?.updateViewLayout(overlayRootView, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        toggleExpanded()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(overlayRootView, layoutParams)
        } catch (e: Exception) {
            Log.e("StretchX_Overlay", "Failed to add window overlay view", e)
        }
    }

    private fun buildViews() {
        val root = overlayRootView ?: return
        root.removeAllViews()

        if (!isExpanded) {
            // Collapsed Circular Badge
            val bubble = FrameLayout(this).apply {
                val sizePx = dpToPx(52)
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#0B0F17"))
                    setStroke(dpToPx(2), Color.parseColor(if (isCurrentlyProjected) "#00F5D4" else "#94A3B8"))
                }
                background = bg
                elevation = dpToPx(8).toFloat()

                val text = TextView(this@FloatingBubbleService).apply {
                    this.text = if (isCurrentlyProjected) "${String.format("%.2f", currentMultiplier)}x" else "SX"
                    setTextColor(Color.parseColor(if (isCurrentlyProjected) "#00F5D4" else "#94A3B8"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                }
                addView(text, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
            }
            root.addView(bubble)
        } else {
            // Expanded Controller Dashboard Panel
            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val pad = dpToPx(14)
                setPadding(pad, pad, pad, pad)

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(16).toFloat()
                    setColor(Color.parseColor("#131926"))
                    setStroke(dpToPx(1), Color.parseColor("#00F5D4"))
                }
                background = bg
                elevation = dpToPx(12).toFloat()

                // Header
                val header = TextView(this@FloatingBubbleService).apply {
                    text = "StretchX Projection Controller"
                    setTextColor(Color.parseColor("#00F5D4"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                addView(header)

                // Subtitle with Session State
                val sub = TextView(this@FloatingBubbleService).apply {
                    text = "$activeGameName • ${if (isCurrentlyProjected) "Projected" else "Native Restored"}"
                    setTextColor(Color.parseColor(if (isCurrentlyProjected) "#10B981" else "#F59E0B"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(0, 0, 0, dpToPx(8))
                }
                addView(sub)

                // Multiplier Info
                val native = nativeConfig ?: DisplayConfig(1080, 2400, 420)
                val targetLong = (native.longEdge * currentMultiplier).toInt()
                val info = TextView(this@FloatingBubbleService).apply {
                    text = "Projection: ${targetLong} × ${native.shortEdge} (${String.format("%.2f", currentMultiplier)}×)"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(0, 0, 0, dpToPx(6))
                }
                addView(info)

                // Multipliers Row
                val btnRow = LinearLayout(this@FloatingBubbleService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dpToPx(4), 0, dpToPx(4))
                }

                fun createActionButton(title: String, colorHex: String, onClick: () -> Unit): Button {
                    return Button(this@FloatingBubbleService).apply {
                        text = title
                        setTextColor(Color.WHITE)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                        setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
                        minHeight = dpToPx(34)
                        minWidth = dpToPx(44)
                        val btnBg = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dpToPx(8).toFloat()
                            setColor(Color.parseColor(colorHex))
                        }
                        background = btnBg
                        setOnClickListener { onClick() }
                    }
                }

                val multipliers = listOf(1.00f, 1.05f, 1.11f, 1.15f, 1.20f, 1.30f)
                multipliers.forEach { m ->
                    val isSelected = Math.abs(currentMultiplier - m) < 0.02f
                    btnRow.addView(createActionButton(
                        "${String.format("%.2f", m)}x",
                        if (isSelected) "#00F5D4" else "#1E293B"
                    ) {
                        applyNewMultiplier(m)
                    })
                    btnRow.addView(View(this@FloatingBubbleService).apply {
                        layoutParams = LinearLayout.LayoutParams(dpToPx(4), 1)
                    })
                }
                addView(btnRow)

                // Action Row: Restore & End Session / Collapse
                val bottomRow = LinearLayout(this@FloatingBubbleService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dpToPx(8), 0, 0)
                }

                bottomRow.addView(createActionButton("RESTORE DISPLAY", "#FF3366") {
                    restoreAndTerminate()
                })
                bottomRow.addView(View(this@FloatingBubbleService).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(8), 1)
                })
                bottomRow.addView(createActionButton("COLLAPSE", "#1E293B") {
                    toggleExpanded()
                })
                addView(bottomRow)
            }
            root.addView(panel)
        }
    }

    private fun applyNewMultiplier(multiplier: Float) {
        currentMultiplier = multiplier
        val native = nativeConfig ?: DisplayConfig(1080, 2400, 420)
        val newTarget = ProjectionEngine.calculateProjection(native, multiplier, currentMode)
        targetConfig = newTarget

        scope.launch(Dispatchers.IO) {
            DisplayCommandRunner.applyConfiguration(newTarget)
            isCurrentlyProjected = true
            launch(Dispatchers.Main) {
                updateNotification("Active in $activeGameName (${String.format("%.2f", multiplier)}×)")
                buildViews()
            }
        }
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        buildViews()
    }

    private fun updateOverlayContent() {
        if (overlayRootView != null) {
            buildViews()
        }
    }

    private fun removeOverlay() {
        if (overlayRootView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayRootView)
            } catch (e: Exception) {
                Log.e("StretchX_Overlay", "Failed to remove window overlay view", e)
            }
            overlayRootView = null
        }
    }

    private fun dpToPx(dp: Int): Int {
        val metrics: DisplayMetrics = resources.displayMetrics
        return (dp * metrics.density).toInt()
    }
}
