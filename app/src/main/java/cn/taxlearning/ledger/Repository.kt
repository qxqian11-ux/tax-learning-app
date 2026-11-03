package cn.taxlearning.ledger

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.util.Locale

class TaxApp : Application() {
    val repository by lazy { Repository(this) }
    override fun onCreate() { super.onCreate(); repository }
}
class Repository(app: Application, databaseName: String = "learning-ledger.db") {
    val db = Room.databaseBuilder(app, LedgerDatabase::class.java, databaseName).addMigrations(LedgerMigrations.V1_TO_V2).build()
    private val dao = db.incomes()
    private val ready = CompletableDeferred<Unit>()
    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                db.runInTransaction {
                    if (dao.metadata("seed-v1") == null) {
                        val year = Rules.currentYear()
                        (1..3).forEach { m ->
                            dao.insert(Income(year=year, month=m, category="工资薪金", source="课程演示 · 月度工资", payer="星河科技（模拟）",
                                incomeCents=1_280_000L + m * 30_000L, declaredCents=36_000L + m * 1000, paidCents=36_000L + m * 1000,
                                date=String.format(Locale.ROOT,"%04d-%02d-15",year,m), note="首次安装示例，可自由修改或删除"))
                        }
                        dao.insert(Income(year=year,month=3,category="劳务报酬",source="课程演示 · 设计实践",payer="青禾工作室（模拟）",
                            incomeCents=320_000,declaredCents=20_000,paidCents=20_000,date="$year-03-20"))
                        dao.mark(Metadata("seed-v1", "done"))
                    }
                }
                ready.complete(Unit)
            } catch(e: Exception) { ready.completeExceptionally(e) }
        }
    }
    fun observe(year: Int) = flow { ready.await(); emitAll(dao.observeYear(year)) }
    suspend fun get(id: String): Income? = withContext(Dispatchers.IO) { ready.await(); dao.get(id) }
    suspend fun all(year: Int? = null): List<Income> = withContext(Dispatchers.IO) {
        ready.await(); dao.all().filter { year == null || it.year == year }
    }
    suspend fun save(row: Income, editing: Boolean) = withContext(Dispatchers.IO) {
        ready.await(); Rules.validate(row)
        db.runInTransaction {
            if (editing) check(dao.update(row) == 1) { "此明细已被删除，请返回列表" }
            else { check(dao.all().size < Rules.MAX_RECORDS) { "记录已达 10,000 条上限，请先备份并整理数据" }; dao.insert(row) }
        }
    }
    suspend fun delete(id: String) = withContext(Dispatchers.IO) { ready.await(); dao.delete(id) }
    suspend fun restore(rows: List<Income>): Pair<Int,Int> = withContext(Dispatchers.IO) {
        ready.await(); rows.forEach { Rules.validate(it) }
        var added = 0; var skipped = 0
        db.runInTransaction {
            check(dao.all().size + rows.count { dao.get(it.id) == null } <= Rules.MAX_RECORDS) { "导入后记录将超出 10,000 条上限" }
            rows.forEach { r ->
                val existing = dao.get(r.id)
                when {
                    existing == null -> { dao.insert(r); added++ }
                    existing == r -> skipped++
                    else -> throw IllegalArgumentException("编号 ${r.id} 与本机内容冲突；为保护数据，本次未导入任何记录。请先导出备份并核对。")
                }
            }
        }
        added to skipped
    }
}
