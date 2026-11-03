package cn.taxlearning.ledger

import org.junit.Assert.*
import org.junit.Test

class NoticePlaybackTest {
    @Test fun singleFittingItemStaysStillIncludingExactFit() {
        for (width in listOf(50f, 100f)) {
            val player = NoticePlayback(10f); player.configure(listOf(width), 100f)
            player.tick(100000); assertFalse(player.needsTick); assertFalse(player.overflowing)
            assertEquals(0f, player.x, 0f); assertEquals(0L, player.completed)
        }
    }
    @Test fun fittingItemsHoldWithoutScrollingThenAdvanceInOrder() {
        val player = NoticePlayback(10f); player.configure(listOf(50f, 70f), 100f)
        player.tick(2999); assertEquals(0, player.index); assertEquals(0f, player.x, 0f)
        player.tick(1); assertEquals(1, player.index); assertEquals(0f, player.x, 0f)
        player.tick(3000); assertEquals(0, player.index); assertEquals(2L, player.completed)
    }
    @Test fun overflowEntersFromRightAndNextWaitsUntilTailHasLeft() {
        val player = NoticePlayback(10f); player.configure(listOf(120f, 60f), 100f)
        assertEquals(100f, player.x, 0f); assertTrue(player.overflowing)
        player.tick(21999); assertEquals(0, player.index); assertTrue(player.x + player.itemWidth > 0)
        player.tick(1); assertEquals(1, player.index); assertTrue(player.lastExitRight <= 0)
        assertEquals(0f, player.x, 0f); assertFalse(player.overflowing)
    }
    @Test fun singleOverflowRepeatsOnlyAfterCompleteExit() {
        val player = NoticePlayback(10f); player.configure(listOf(120f), 100f)
        player.tick(22000); assertEquals(1L, player.completed); assertEquals(100f, player.x, 0f)
    }
    @Test fun resizingAndReplacingContentRecomputesOverflow() {
        val player = NoticePlayback(10f); player.configure(listOf(120f), 100f); player.tick(1000)
        player.configure(listOf(120f), 200f); assertFalse(player.needsTick); assertEquals(0f, player.x, 0f)
        player.configure(listOf(250f), 200f); assertTrue(player.overflowing); assertEquals(200f, player.x, 0f)
        player.configure(emptyList(), 200f); assertFalse(player.needsTick)
    }
}
