// Added in fork: Google App warm-up (Shizuku / virtual display). GPL-3.0, like the rest of MiCTS.
package com.parallelc.micts.warmup

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.*
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object GsaWarmup {
    private const val GSA = "com.google.android.googlequicksearchbox"
    private const val HANDOFF = "com.google.android.apps.gsa.assistant.handoff.AssistantHandoffActivity"
    @Volatile var lastWarmMs = 0L
    private var vd: VirtualDisplay? = null

    private fun intent() = Intent().apply {
        setClassName(GSA, HANDOFF)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                 Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_NO_HISTORY or
                 Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }

    /** Variant A: own private virtual display, started by the app itself (no Shizuku needed). */
    fun warmOnVirtualDisplay(ctx: Context): Boolean = try {
        val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val d = vd ?: dm.createVirtualDisplay("micts-warm", 64, 64, 160, null,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY).also { vd = it }
        val opts = ActivityOptions.makeBasic().setLaunchDisplayId(d.display.displayId)
        ctx.startActivity(intent(), opts.toBundle())
        lastWarmMs = SystemClock.elapsedRealtime(); true
    } catch (_: Throwable) { false }

    /** Variant B: 1x1 freeform window via Shizuku (shell uid). */
    fun warmFreeform(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            HiddenApiBypass.addHiddenApiExemptions("Landroid/app/")
        val atm = Class.forName("android.app.IActivityTaskManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity_task")))
        val opts = ActivityOptions.makeBasic()
        ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType).invoke(opts, 5)
        opts.launchBounds = Rect(0, 0, 1, 1)
        val i = intent()
        val m = atm.javaClass.methods.first { it.name == "startActivity" && it.parameterTypes.size == 11 }
        m.invoke(atm, null, "com.android.shell", null, i, i.type, null, null, 0, 0, null, opts.toBundle())
        lastWarmMs = SystemClock.elapsedRealtime(); true
    } catch (_: Throwable) { false }

    fun warm(ctx: Context) {
        if (SystemClock.elapsedRealtime() - lastWarmMs < 60_000) return
        Thread { if (!warmOnVirtualDisplay(ctx)) warmFreeform() }.start()
    }
}
