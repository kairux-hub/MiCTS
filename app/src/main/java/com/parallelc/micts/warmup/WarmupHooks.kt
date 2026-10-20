// Added in fork: wiring for GsaWarmup. GPL-3.0, like the rest of MiCTS.
package com.parallelc.micts.warmup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import rikka.shizuku.Shizuku

object WarmupHooks {
    private const val STALE_MS = 60_000L
    private const val PRE_TRIGGER_MS = 300L

    /** SCREEN_ON can only be received by a runtime-registered receiver. */
    fun registerScreenOn(app: Context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_ON) GsaWarmup.warm(context.applicationContext)
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            app.registerReceiver(receiver, filter)
        }
    }

    /** Ask Shizuku for permission once its binder is available (needed for the freeform variant). */
    fun requestShizukuPermission() {
        Shizuku.addBinderReceivedListenerSticky {
            runCatching {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED &&
                    !Shizuku.shouldShowRequestPermissionRationale()) {
                    Shizuku.requestPermission(0)
                }
            }
        }
    }

    /**
     * Called before the existing MiCTS delay. If the last warm-up is stale, start one now and make sure
     * the trigger fires at least ~300 ms later. Nothing is launched at trigger time itself.
     */
    fun delayWithWarmup(ctx: Context, delayMs: Long): Long {
        if (SystemClock.elapsedRealtime() - GsaWarmup.lastWarmMs < STALE_MS) return delayMs
        GsaWarmup.warm(ctx.applicationContext)
        return maxOf(delayMs, PRE_TRIGGER_MS)
    }
}
