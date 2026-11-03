package cn.taxlearning.ledger

import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The home broadcast is a real horizontal ticker, including messages shorter than its viewport. */
internal class HomeNoticeView(private val host: MainActivity) : LinearLayout(host), DefaultLifecycleObserver {
    private val ticker = NoticeMarqueeView(host)
    private var collection: Job? = null
    init {
        id = R.id.home_notice
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        background = host.rounded(0xFFFFF5E2.toInt(), 9)
        setPadding(host.u(8), host.u(10), host.u(8), host.u(10))
        addView(ReferenceIcon(host, "notice", 0xFFFF9226.toInt()), LayoutParams(host.u(25), host.u(25)).apply { marginEnd = host.u(11) })
        addView(ticker, LayoutParams(0, -1, 1f))
        host.clickable(this, "首页广播，长按管理") {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(host).setTitle("广播提醒")
                .setMessage(ticker.currentContent).setPositiveButton("知道了", null).show()
        }
        setOnLongClickListener { host.go(NoticeListActivity::class.java); true }
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        host.lifecycle.addObserver(this)
        collection = host.lifecycleScope.launch {
            host.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    NoticeRepository.get(context).observe().collect { ticker.setMessages(it.map { row -> row.content }) }
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { ticker.showError(); host.error(e) }
            }
        }
        updateAnimation()
    }
    override fun onDetachedFromWindow() {
        collection?.cancel(); collection = null
        ticker.setRunning(false); host.lifecycle.removeObserver(this)
        super.onDetachedFromWindow()
    }
    override fun onStart(owner: LifecycleOwner) { updateAnimation() }
    override fun onStop(owner: LifecycleOwner) { ticker.setRunning(false) }
    override fun onWindowVisibilityChanged(visibility: Int) { super.onWindowVisibilityChanged(visibility); updateAnimation() }
    private fun updateAnimation() {
        // View construction may invoke this callback before the field initializer has run.
        if (childCount > 1) ticker.setRunning(isAttachedToWindow && windowVisibility == View.VISIBLE && host.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
}

private class NoticeMarqueeView(private val host: MainActivity) : View(host) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9E6A18.toInt()
        textSize = host.resources.displayMetrics.widthPixels * 15f / 400f * host.resources.configuration.fontScale * if (host.care) 1.18f else 1f
    }
    private var messages = emptyList<String>()
    private var index = 0
    private var offset = 0f
    private var running = false
    private var previousFrame = 0L
    private var framePosted = false
    val currentContent: String get() = messages.getOrNull(index) ?: if (error) "广播暂时无法读取，长按管理" else "暂无广播提醒，长按添加"
    private var error = false
    private val frame = object : Runnable {
        override fun run() {
            framePosted = false
            if (!running || !isAttachedToWindow || windowVisibility != View.VISIBLE || messages.isEmpty()) return
            val now = SystemClock.uptimeMillis()
            if (previousFrame != 0L) {
                offset += (now - previousFrame).coerceAtMost(80L) * host.resources.displayMetrics.widthPixels * 22f / 400_000f
                val distance = cycleWidth()
                if (offset >= distance) { offset -= distance; index = (index + 1) % messages.size; updateDescription() }
            }
            previousFrame = now; invalidate(); scheduleFrame()
        }
    }
    init {
        id = R.id.notice_marquee
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        updateDescription()
    }
    fun setMessages(values: List<String>) {
        val normalized = values.map { it.replace(Regex("\\s+"), " ").trim() }
        if (messages != normalized || error) {
            messages = normalized; index = 0; offset = 0f; previousFrame = 0L; error = false
            updateDescription(); invalidate()
        }
        scheduleFrame()
    }
    fun showError() { messages = emptyList(); index = 0; offset = 0f; error = true; updateDescription(); invalidate() }
    fun setRunning(value: Boolean) {
        running = value
        if (!value) { removeCallbacks(frame); framePosted = false; previousFrame = 0L }
        else scheduleFrame()
    }
    private fun scheduleFrame() {
        if (running && messages.isNotEmpty() && isAttachedToWindow && !framePosted) { framePosted = true; postOnAnimation(frame) }
    }
    private fun cycleWidth() = maxOf(paint.measureText(currentContent), width.toFloat()) + host.u(28)
    private fun updateDescription() { contentDescription = "广播内容：$currentContent" }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val baseline = (height - paint.ascent() - paint.descent()) / 2f
        canvas.save(); canvas.clipRect(0, 0, width, height)
        canvas.drawText(currentContent, -offset, baseline, paint)
        if (messages.isNotEmpty()) canvas.drawText(messages[(index + 1) % messages.size], cycleWidth() - offset, baseline, paint)
        canvas.restore()
    }
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = android.widget.TextView::class.java.name
        info.text = currentContent
        // Machine-readable verification metadata; it does not become spoken or visible interface text.
        info.extras.putFloat("cn.taxlearning.notice.offsetPx", offset)
        info.extras.putBoolean("cn.taxlearning.notice.running", running && messages.isNotEmpty())
    }
    override fun onDetachedFromWindow() { setRunning(false); super.onDetachedFromWindow() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); scheduleFrame() }
}
