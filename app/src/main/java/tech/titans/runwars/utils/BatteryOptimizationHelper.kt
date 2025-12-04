package tech.titans.runwars.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi

object BatteryOptimizationHelper {

    /**
     * Check if battery optimizations are disabled for the app
     */
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName
            return powerManager.isIgnoringBatteryOptimizations(packageName)
        }
        return true // Not applicable for older versions
    }

    /**
     * Open battery optimization settings for the app
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestDisableBatteryOptimization(context: Context) {
        if (!isBatteryOptimizationDisabled(context)) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    /**
     * Open general battery optimization settings
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }
        context.startActivity(intent)
    }
}

