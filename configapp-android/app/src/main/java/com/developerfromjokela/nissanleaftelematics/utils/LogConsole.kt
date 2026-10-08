package com.developerfromjokela.nissanleaftelematics.utils

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.ConcurrentLinkedQueue

class LogConsole(
    private val activity: Activity,
    private val heightDp: Int = 200,
    private val maxLines: Int = 300,
    private val logcatArgs: List<String> = listOf("-v", "time")
) {
    private val ui = Handler(Looper.getMainLooper())
    private val pending = ConcurrentLinkedQueue<String>()
    private val lines = ArrayDeque<String>()

    private var scroll: ScrollView? = null
    private var text: TextView? = null
    private var process: Process? = null
    private var thread: Thread? = null

    private val flusher = object : Runnable {
        override fun run() {
            if (pending.isNotEmpty()) {
                while (true) {
                    val l = pending.poll() ?: break
                    lines.addLast(l)
                    if (lines.size > maxLines) lines.removeFirst()
                }
                text?.text = lines.joinToString("\n")
                scroll?.post { scroll?.fullScroll(ScrollView.FOCUS_DOWN) }
            }
            ui.postDelayed(this, 200)
        }
    }

    fun show() {
        if (scroll != null) return

        val tv = TextView(activity).apply {
            setTextColor(Color.parseColor("#00FF66"))
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        val sv = ScrollView(activity).apply {
            setBackgroundColor(Color.parseColor("#99000000"))
            elevation = dp(8).toFloat()
            addView(tv, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            // Keep below the status bar
            @Suppress("DEPRECATION")
            setOnApplyWindowInsetsListener { v, insets ->
                (v.layoutParams as? FrameLayout.LayoutParams)?.let {
                    it.topMargin = insets.systemWindowInsetTop
                    v.layoutParams = it
                }
                insets
            }
        }
        text = tv
        scroll = sv

        activity.addContentView(
            sv,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp), Gravity.TOP
            )
        )

        startLogcat()
        ui.post(flusher)
    }

    fun hide() {
        ui.removeCallbacks(flusher)
        stopLogcat()
        scroll?.let { (it.parent as? ViewGroup)?.removeView(it) }
        scroll = null
        text = null
        lines.clear()
        pending.clear()
    }

    private fun startLogcat() {
        thread = Thread {
            try {
                val cmd = listOf("logcat") + logcatArgs + listOf("-T", "200")
                val p = Runtime.getRuntime().exec(cmd.toTypedArray())
                process = p
                p.inputStream.bufferedReader().use { r ->
                    while (!Thread.currentThread().isInterrupted) {
                        pending.add(r.readLine() ?: break)
                    }
                }
            } catch (_: Exception) {}
        }.also { it.isDaemon = true; it.start() }
    }

    private fun stopLogcat() {
        thread?.interrupt()
        process?.destroy()
        thread = null
        process = null
    }

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), activity.resources.displayMetrics
    ).toInt()
}