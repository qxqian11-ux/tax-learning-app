package cn.taxlearning.ledger

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert.*
import android.app.Application
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    @Test fun testCrudYearIsolationRestartAndAtomicRestore() = runBlocking {
        val app=instrumentation.targetContext.applicationContext as Application
        val name="verification-${UUID.randomUUID()}.db"
        var repo=Repository(app,name)
        try {
            repo.all() // waits for first-time demo seeding
            val r=Income(year=2019,month=1,category="工资薪金",source="测试来源",payer="模拟",incomeCents=10,declaredCents=1,paidCents=1,date="2019-01-01")
            val other=r.copy(id=UUID.randomUUID().toString(),year=2020,date="2020-01-01",incomeCents=20)
            repo.save(r,false); repo.save(other,false)
            assertEquals(10L,repo.observe(2019).first().totals().income)
            assertEquals(20L,repo.observe(2020).first().totals().income)
            repo.save(r.copy(incomeCents=30),true)
            assertEquals(30L,repo.observe(2019).first().totals().income)
            repo.db.close(); repo=Repository(app,name)
            assertEquals(30L,repo.get(r.id)!!.incomeCents)
            assertEquals(20L,repo.get(other.id)!!.incomeCents)
            assertEquals(0,repo.restore(listOf(other)).first)
            val fresh=r.copy(id=UUID.randomUUID().toString())
            try { repo.restore(listOf(fresh,other.copy(incomeCents=90))); fail("冲突应回滚") } catch(e: IllegalArgumentException) { }
            assertNull(repo.get(fresh.id)); assertEquals(20L,repo.get(other.id)!!.incomeCents)
            repo.delete(r.id); assertTrue(repo.observe(2019).first().isEmpty()); assertNotNull(repo.get(other.id))
            val demos=repo.all().filter { it.year==Rules.currentYear() }
            demos.forEach { repo.delete(it.id) }; repo.db.close(); repo=Repository(app,name)
            assertTrue(repo.all().none { it.year==Rules.currentYear() })
        } finally { repo.db.close(); app.deleteDatabase(name) }
    }
}
