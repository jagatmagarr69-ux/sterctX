package com.example.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.example.model.ShizukuState
import com.example.model.ShizukuStatusInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {
    private const val TAG = "StretchX_Shizuku"
    const val SHIZUKU_REQUEST_CODE = 8842

    private var appContext: Context? = null

    private val _status = MutableStateFlow(
        ShizukuStatusInfo(state = ShizukuState.NOT_RUNNING, detailMessage = "Initializing Shizuku...")
    )
    val status: StateFlow<ShizukuStatusInfo> = _status.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku binder received from IPC")
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Shizuku binder died or was stopped")
        _status.value = ShizukuStatusInfo(
            state = ShizukuState.NOT_RUNNING,
            detailMessage = "Shizuku service disconnected or stopped. Please launch Shizuku and restart the service."
        )
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Shizuku permission result callback: granted=$granted, grantResult=$grantResult")
            if (granted) {
                // Re-verify and transition to CONNECTED
                refreshStatus()
            } else {
                val version = try { Shizuku.getVersion() } catch (_: Exception) { 0 }
                val uid = try { Shizuku.getUid() } catch (_: Exception) { -1 }
                _status.value = ShizukuStatusInfo(
                    state = ShizukuState.PERMISSION_DENIED,
                    version = version,
                    uid = uid,
                    detailMessage = "Authorization was denied by user. Open the Shizuku app and allow StretchX in 'Authorized Applications'."
                )
            }
        }
    }

    @Volatile
    private var isListenersRegistered = false

    @Synchronized
    fun init(context: Context) {
        appContext = context.applicationContext

        if (!isListenersRegistered) {
            try {
                Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
                Shizuku.addRequestPermissionResultListener(permissionListener)
                isListenersRegistered = true
                Log.d(TAG, "Registered Shizuku lifecycle and permission listeners")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding Shizuku listeners", e)
            }
        }

        // Proactively request the binder from the provider/manager in case Shizuku is already running
        try {
            ShizukuProvider.requestBinderForNonProviderProcess(context.applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Failed initial requestBinderForNonProviderProcess", e)
        }

        refreshStatus()
    }

    @Synchronized
    fun cleanUp() {
        if (isListenersRegistered) {
            try {
                Shizuku.removeBinderReceivedListener(binderReceivedListener)
                Shizuku.removeBinderDeadListener(binderDeadListener)
                Shizuku.removeRequestPermissionResultListener(permissionListener)
                isListenersRegistered = false
                Log.d(TAG, "Unregistered Shizuku listeners")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing Shizuku listeners", e)
            }
        }
    }

    fun refreshStatus() {
        try {
            var ping = try { Shizuku.pingBinder() } catch (_: Exception) { false }

            // If not responding yet, attempt to query the provider once
            if (!ping && appContext != null) {
                try {
                    ShizukuProvider.requestBinderForNonProviderProcess(appContext!!)
                    ping = try { Shizuku.pingBinder() } catch (_: Exception) { false }
                } catch (e: Exception) {
                    Log.w(TAG, "Error querying ShizukuProvider in refreshStatus", e)
                }
            }

            if (!ping) {
                val isInstalled = isShizukuInstalled(appContext)
                val msg = if (isInstalled) {
                    "Shizuku service is not running. Please launch the Shizuku app and start the service via Wireless Debugging or ADB."
                } else {
                    "Shizuku is not installed on this device. Install the official Shizuku app from Google Play or GitHub to proceed."
                }
                _status.value = ShizukuStatusInfo(
                    state = ShizukuState.NOT_RUNNING,
                    detailMessage = msg
                )
                return
            }

            // Binder connection is alive
            val version = try { Shizuku.getVersion() } catch (_: Exception) { 0 }
            val isPre11 = try { Shizuku.isPreV11() } catch (_: Exception) { false }
            val isRoot = isPre11 || (try { Shizuku.getUid() == 0 } catch (_: Exception) { false })
            val uid = try { Shizuku.getUid() } catch (_: Exception) { -1 }

            if (isPre11) {
                _status.value = ShizukuStatusInfo(
                    state = ShizukuState.CONNECTION_FAILED,
                    version = version,
                    isRoot = isRoot,
                    uid = uid,
                    detailMessage = "Shizuku v$version is outdated (< v11). Please update Shizuku to a modern version."
                )
                return
            }

            val hasPermission = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                Log.e(TAG, "Failed checkSelfPermission", e)
                false
            }

            if (hasPermission) {
                _status.value = ShizukuStatusInfo(
                    state = ShizukuState.CONNECTED,
                    version = version,
                    isRoot = isRoot,
                    uid = uid,
                    detailMessage = "Connected to Shizuku v$version (${if (isRoot) "Root" else "ADB shell"} UID $uid). Privileged access is active."
                )
            } else {
                val isRationale = try { Shizuku.shouldShowRequestPermissionRationale() } catch (_: Exception) { false }
                if (isRationale) {
                    _status.value = ShizukuStatusInfo(
                        state = ShizukuState.PERMISSION_DENIED,
                        version = version,
                        isRoot = isRoot,
                        uid = uid,
                        detailMessage = "Authorization was denied. Please open the Shizuku app, navigate to 'Authorized Applications', and toggle StretchX on."
                    )
                } else {
                    _status.value = ShizukuStatusInfo(
                        state = ShizukuState.AUTHORIZATION_REQUIRED,
                        version = version,
                        isRoot = isRoot,
                        uid = uid,
                        detailMessage = "Shizuku service is running, but StretchX is not yet authorized. Tap 'Authorize StretchX' to grant permission."
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during refreshStatus", e)
            _status.value = ShizukuStatusInfo(
                state = ShizukuState.CONNECTION_FAILED,
                detailMessage = "Shizuku error: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Triggers the Shizuku authorization request flow with accurate state updates and error display.
     */
    fun requestPermission(context: Context? = null) {
        val targetContext = context ?: appContext
        Log.d(TAG, "requestPermission called. Current state: ${_status.value.state}")

        // Check if binder is responsive
        var ping = try { Shizuku.pingBinder() } catch (_: Exception) { false }

        if (!ping && targetContext != null) {
            try {
                ShizukuProvider.requestBinderForNonProviderProcess(targetContext)
                ping = try { Shizuku.pingBinder() } catch (_: Exception) { false }
            } catch (e: Exception) {
                Log.w(TAG, "Error requesting binder in requestPermission", e)
            }
        }

        if (!ping) {
            val isInstalled = isShizukuInstalled(targetContext)
            _status.value = ShizukuStatusInfo(
                state = ShizukuState.NOT_RUNNING,
                detailMessage = if (isInstalled) {
                    "Cannot request authorization: Shizuku is not running. Please open the Shizuku app and start the service first."
                } else {
                    "Cannot request authorization: Shizuku app is not installed on this device."
                }
            )
            return
        }

        try {
            if (Shizuku.isPreV11()) {
                _status.value = ShizukuStatusInfo(
                    state = ShizukuState.CONNECTION_FAILED,
                    detailMessage = "Shizuku version is older than v11 and is unsupported."
                )
                return
            }

            val currentPermission = Shizuku.checkSelfPermission()
            if (currentPermission == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission already granted, refreshing status")
                refreshStatus()
                return
            }

            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Log.d(TAG, "Permission rationale is true, permission previously denied")
                _status.value = ShizukuStatusInfo(
                    state = ShizukuState.PERMISSION_DENIED,
                    version = try { Shizuku.getVersion() } catch (_: Exception) { 0 },
                    uid = try { Shizuku.getUid() } catch (_: Exception) { -1 },
                    detailMessage = "Authorization was previously denied in Shizuku. Tap 'Open Shizuku App' to enable StretchX under Authorized Applications."
                )
            } else {
                Log.d(TAG, "Requesting permission with requestCode: $SHIZUKU_REQUEST_CODE")
                _status.value = ShizukuStatusInfo(
                    state = ShizukuState.CONNECTING,
                    version = try { Shizuku.getVersion() } catch (_: Exception) { 0 },
                    uid = try { Shizuku.getUid() } catch (_: Exception) { -1 },
                    detailMessage = "Requesting Shizuku authorization dialog..."
                )
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during requestPermission", e)
            _status.value = ShizukuStatusInfo(
                state = ShizukuState.CONNECTION_FAILED,
                detailMessage = "Failed to request authorization: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    fun openShizukuApp(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                ?: pm.getLaunchIntentForPackage("moe.shizuku.manager")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Shizuku app", e)
            false
        }
    }

    private fun isShizukuInstalled(context: Context?): Boolean {
        if (context == null) return false
        val pm = context.packageManager
        return try {
            pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            try {
                pm.getPackageInfo("moe.shizuku.manager", 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Executes shell commands through Shizuku ADB privileged process.
     * Enforces that the connection is alive and permission is granted.
     */
    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!_status.value.state.isConnected) {
            return@withContext Result.failure(
                IllegalStateException("Privileged operation blocked: Shizuku is ${_status.value.state.displayName}. Authorization is required.")
            )
        }

        try {
            val process: Process = try {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.isAccessible = true
                method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            } catch (e: Exception) {
                Log.w(TAG, "Shizuku process reflection fallback to Runtime", e)
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            }

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                errorOutput.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Result.success(output.toString().trim())
            } else {
                val err = errorOutput.toString().trim().ifEmpty { "Command exited with code $exitCode" }
                Result.failure(RuntimeException(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Execution failed for command: $command", e)
            if (!Shizuku.pingBinder()) {
                refreshStatus()
            }
            Result.failure(e)
        }
    }
}
