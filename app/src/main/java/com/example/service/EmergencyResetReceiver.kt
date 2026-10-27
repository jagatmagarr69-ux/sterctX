package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.shizuku.DisplayCommandRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmergencyResetReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_EMERGENCY_RESET = "com.example.stretchx.ACTION_EMERGENCY_RESET"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_EMERGENCY_RESET) {
            Log.d("StretchX_Emergency", "Emergency reset triggered via BroadcastReceiver")
            Toast.makeText(context, "StretchX: Emergency Display Resetting...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                val result = DisplayCommandRunner.resetDisplay()
                if (result.isSuccess) {
                    Log.i("StretchX_Emergency", "Display successfully reset to hardware defaults")
                } else {
                    Log.e("StretchX_Emergency", "Failed to reset display: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
}
