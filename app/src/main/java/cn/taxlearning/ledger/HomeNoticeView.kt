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

/** Short broadcasts stay still; overflowing broadcasts move as complete icon/text items. */
internal class HomeNoticeView(private val host: MainActivity) : LinearLayout(host), DefaultLifecycleObserver {
    private val ticker = NoticeMarqueeView(host)
    private var collection: Job? = null
    init {
        id = R.id.home_notice
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        background = host.rounded(0xFFFFF5E2.toInt(), 9)
        setPadding(host.u(8), host.u(10), host.u(8), host.u(10))
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
        if (childCount > 0) ticker.setRunning(isAttachedToWindow && windowVisibility == View.VISIBLE && host.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
}

private class NoticeMarqueeView(private val host: MainActivity) : View(host) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9E6A18.toInt()
        textSize = host.resources.displayMetrics.widthPixels * 15f / 400f * host.resources.configuration.fontScale * if (host.care) 1.18f else 1f
    }
    private val player = NoticePlayback(host.resources.displayMetrics.widthPixels * 32f / 400f)
    private val iconSize = host.u(25).toFloat()
    private val textStart = iconSize + host.u(11)
    private val iconBounds = android.graphics.RectF()
    private var messages = emptyList<String>()
    private var running = false
    private var previousFrame = 0L
    private var framePosted = false
    private var error = false
    val currentContent: String get() = messages.getOrNull(player.index) ?: if (error) "广播暂时无法读取，长按管理" else "暂无广播提醒，长按添加"
    private val frame = object : Runnable {
        override fun run() {
            framePosted = false
            if (!running || !isAttachedToWindow || windowVisibility != View.VISIBLE || !player.needsTick) return
            val now = SystemClock.uptimeMillis()
            val before = player.index
            val elapsed = (now - previousFrame).coerceAtLeast(0L)
            player.tick(if (player.overflowing) elapsed.coerceAtMost(80L) else elapsed)
            previousFrame = now
            if (before != player.index) updateDescription()
            invalidate(); scheduleFrame()
        }
    }
    init { id = R.id.notice_marquee; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES; updateDescription() }
    private fun configure() {
        player.configure(messages.map { textStart + paint.measureText(it) }, width.toFloat())
        previousFrame = SystemClock.uptimeMillis()
        removeCallbacks(frame); framePosted = false; invalidate(); scheduleFrame()
    }
    fun setMessages(values: List<String>) {
        val normalized = values.map { it.replace(Regex("\\s+"), " ").trim() }
        if (messages != normalized || error) {
            messages = normalized; error = false; player.reset(); configure(); updateDescription()
        }
    }
    fun showError() { messages = emptyList(); error = true; configure(); updateDescription() }
    fun setRunning(value: Boolean) {
        if (running == value) return
        running = value; previousFrame = SystemClock.uptimeMillis()
        if (!value) { removeCallbacks(frame); framePosted = false }
        else scheduleFrame()
    }
    private fun scheduleFrame() {
        if (running && player.needsTick && isAttachedToWindow && !framePosted) {
            framePosted = true
            if (player.overflowing) postOnAnimation(frame) else postDelayed(frame, player.nextDelayMillis)
        }
    }
    private fun updateDescription() { contentDescription = "广播内容：$currentContent" }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { super.onSizeChanged(w,h,oldw,oldh); configure(); updateDescription() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val x = if (messages.isEmpty()) 0f else player.x
        val baseline = (height - paint.ascent() - paint.descent()) / 2f
        val save = canvas.save(); canvas.clipRect(0, 0, width, height)
        iconBounds.set(x, (height-iconSize)/2f, x+iconSize, (height+iconSize)/2f)
        ReferenceAssets.draw(context, canvas, "crop_notice.png", iconBounds)
        canvas.drawText(currentContent, x+textStart, baseline, paint)
        canvas.restoreToCount(save)
    }
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = android.widget.TextView::class.java.name; info.text = currentContent
        info.extras.putFloat("cn.taxlearning.notice.offsetPx", -player.x)
        info.extras.putBoolean("cn.taxlearning.notice.running", running && player.overflowing)
        info.extras.putInt("cn.taxlearning.notice.index", player.index)
        info.extras.putString("cn.taxlearning.notice.mode", if (player.overflowing) "scroll" else "static")
        info.extras.putFloat("cn.taxlearning.notice.iconX", player.x)
        info.extras.putFloat("cn.taxlearning.notice.textX", player.x+textStart)
        info.extras.putFloat("cn.taxlearning.notice.itemWidth", player.itemWidth)
        info.extras.putFloat("cn.taxlearning.notice.viewportWidth", player.viewport)
        info.extras.putLong("cn.taxlearning.notice.completed", player.completed)
        info.extras.putFloat("cn.taxlearning.notice.lastExitRight", player.lastExitRight)
    }
    override fun onDetachedFromWindow() { setRunning(false); super.onDetachedFromWindow() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); scheduleFrame() }
}
