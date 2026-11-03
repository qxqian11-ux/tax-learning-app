package cn.taxlearning.ledger

/** One complete icon/text item at a time. Widths include the icon and its text gap. */
internal class NoticePlayback(private val pixelsPerSecond: Float, private val shortHoldMillis: Float = 3000f) {
    private var widths = emptyList<Float>()
    var viewport = 0f; private set
    var index = 0; private set
    var x = 0f; private set
    var completed = 0L; private set
    var lastExitRight = Float.NaN; private set
    private var heldMillis = 0f
    val itemWidth get() = widths.getOrElse(index) { 0f }
    val overflowing get() = viewport > 0f && itemWidth > viewport
    val needsTick get() = widths.isNotEmpty() && viewport > 0f && (overflowing || widths.size > 1)
    val nextDelayMillis get() = if (overflowing) 0L else (shortHoldMillis - heldMillis).toLong().coerceAtLeast(1L)
    fun configure(itemWidths: List<Float>, availableWidth: Float) {
        val available = availableWidth.coerceAtLeast(0f)
        if (widths == itemWidths && viewport == available) return
        widths = itemWidths; viewport = available; index = 0; completed = 0
        lastExitRight = Float.NaN; startItem()
    }
    fun reset() { index = 0; completed = 0; lastExitRight = Float.NaN; startItem() }
    private fun startItem() { heldMillis = 0f; x = if (overflowing) viewport else 0f }
    fun tick(elapsedMillis: Long) {
        if (!needsTick || elapsedMillis <= 0) return
        if (overflowing) {
            x -= pixelsPerSecond * elapsedMillis / 1000f
            if (x + itemWidth > 0f) return
            lastExitRight = x + itemWidth
        } else {
            heldMillis += elapsedMillis
            if (heldMillis < shortHoldMillis) return
            lastExitRight = Float.NaN
        }
        // Discard excess frame time: the next item starts only after this one has finished.
        index = (index + 1) % widths.size; completed++; startItem()
    }
}
