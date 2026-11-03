package cn.taxlearning.ledger

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EmploymentPersistenceTest {
    @Test fun countsIncludeAllRowsAndChangesSurviveReopen()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="employment-test-${UUID.randomUUID()}.db"
        var store=EmploymentRepository(context,name)
        try {
            assertEquals(0,store.observeCount().first())
            val a=Employment(companyName="模拟单位甲",startDate="2020-09-09",createdAt=1)
            val b=Employment(companyName="模拟单位乙",endDate="2025-01-01",createdAt=2)
            store.save(a,false);store.save(b,false)
            assertEquals(2,store.observeCount().first())
            val edited=a.copy(companyName="模拟单位甲（编辑）",creditCode="123456789012345678",position="工程师",endDate="2026-10-31")
            store.save(edited,true);assertEquals(listOf(edited,b),store.all())
            store.db.close();store=EmploymentRepository(context,name)
            assertEquals(2,store.observeCount().first());assertEquals(edited,store.get(a.id))
            store.delete(b.id);assertEquals(1,store.observeCount().first())
            store.db.close();store=EmploymentRepository(context,name)
            assertEquals(listOf(edited),store.all())
            store.delete(a.id);store.db.close();store=EmploymentRepository(context,name)
            assertEquals(0,store.observeCount().first());assertTrue(store.all().isEmpty())
        } finally { store.db.close();context.deleteDatabase(name) }
    }
}
