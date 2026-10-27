package com.example.service

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.shizuku.DisplayCommandRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class StretchXTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = "StretchX Reset"
        tile.contentDescription = "Tap to emergency reset display resolution and density"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        Log.d("StretchX_Tile", "Quick Settings Tile clicked for reset")

        // Haptic feedback
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}

        Toast.makeText(this, "StretchX: Resetting Display...", Toast.LENGTH_SHORT).show()

        scope.launch {
            val result = DisplayCommandRunner.resetDisplay()
            if (result.isSuccess) {
                Log.i("StretchX_Tile", "Display reset completed via QS Tile")
            } else {
                Log.e("StretchX_Tile", "Display reset failed via QS Tile: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
