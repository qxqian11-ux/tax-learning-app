package cn.taxlearning.ledger

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NoticePersistenceTest {
    @Test fun reminderSeedsOnceAndEditsAndEmptyStoreSurviveReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "notice-test-${UUID.randomUUID()}.db"
        var store = NoticeRepository(context, name)
        try {
            val initial = store.all()
            assertEquals(1, initial.size); assertEquals(NoticeRules.INITIAL_CONTENT, initial.single().content)
            val first = Notice(content = "本地广播测试甲", createdAt = 1)
            val second = Notice(content = "本地广播测试乙", createdAt = 2)
            store.save(first, false); store.save(second, false)
            val edited = first.copy(content = "本地广播测试甲（修改后）")
            store.save(edited, true)
            assertEquals(listOf(initial.single(), edited, second), store.observe().first())
            store.db.close(); store = NoticeRepository(context, name)
            assertEquals(listOf(initial.single(), edited, second), store.all())
            store.delete(second.id); assertNull(store.get(second.id)); assertEquals(edited, store.get(first.id))
            store.all().forEach { store.delete(it.id) }
            store.db.close(); store = NoticeRepository(context, name)
            assertTrue(store.all().isEmpty()); assertTrue(store.observe().first().isEmpty())
        } finally { store.db.close(); context.deleteDatabase(name) }
    }
}
