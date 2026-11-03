package cn.taxlearning.ledger

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Entity(tableName = "notices")
data class Notice(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notice_metadata")
data class NoticeMetadata(@PrimaryKey val key: String, val value: String)

object NoticeRules {
    const val INITIAL_CONTENT = "个人所得税综合所得年度汇算学习提醒"
    fun validate(row: Notice) {
        require(runCatching { UUID.fromString(row.id).toString() == row.id }.getOrDefault(false)) { "广播记录编号无效" }
        require(row.content.isNotBlank() && row.content.length <= 500) { "请填写广播内容，最多 500 字" }
    }
}

@Dao
interface NoticeDao {
    @Query("SELECT * FROM notices ORDER BY createdAt, id") fun observe(): kotlinx.coroutines.flow.Flow<List<Notice>>
    @Query("SELECT * FROM notices ORDER BY createdAt, id") fun all(): List<Notice>
    @Query("SELECT * FROM notices WHERE id = :id") fun get(id: String): Notice?
    @Query("SELECT COUNT(*) FROM notices") fun count(): Int
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insert(row: Notice)
    @Update fun update(row: Notice): Int
    @Query("DELETE FROM notices WHERE id = :id") fun delete(id: String): Int
    @Query("SELECT * FROM notice_metadata WHERE `key` = :key") fun metadata(key: String): NoticeMetadata?
    @Insert(onConflict = OnConflictStrategy.ABORT) fun mark(row: NoticeMetadata)
}

@Database(entities = [Notice::class, NoticeMetadata::class], version = 1, exportSchema = true)
abstract class NoticeDatabase : RoomDatabase() { abstract fun notices(): NoticeDao }

/** Independent local data; the one-time reminder marker remains after deleting the final notice. */
class NoticeRepository(context: Context, databaseName: String = "learning-notices.db") {
    val db = Room.databaseBuilder(context.applicationContext, NoticeDatabase::class.java, databaseName).build()
    private val dao = db.notices()
    private val ready = CompletableDeferred<Unit>()
    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                db.runInTransaction {
                    if (dao.metadata("initial-reminder-v1") == null) {
                        dao.insert(Notice(content = NoticeRules.INITIAL_CONTENT, createdAt = 0))
                        dao.mark(NoticeMetadata("initial-reminder-v1", "done"))
                    }
                }
                ready.complete(Unit)
            } catch (e: Exception) { ready.completeExceptionally(e) }
        }
    }
    fun observe() = flow { ready.await(); emitAll(dao.observe()) }
    suspend fun all() = withContext(Dispatchers.IO) { ready.await(); dao.all() }
    suspend fun get(id: String) = withContext(Dispatchers.IO) { ready.await(); dao.get(id) }
    suspend fun save(row: Notice, editing: Boolean) = withContext(Dispatchers.IO) {
        ready.await(); NoticeRules.validate(row)
        db.runInTransaction {
            if (editing) check(dao.update(row) == 1) { "该广播已删除，请返回列表" }
            else {
                check(dao.count() < 1_000) { "广播已达 1,000 条，请先整理数据" }
                dao.insert(row)
            }
        }
    }
    suspend fun delete(id: String) = withContext(Dispatchers.IO) { ready.await(); dao.delete(id) }
    companion object {
        @Volatile private var instance: NoticeRepository? = null
        fun get(context: Context): NoticeRepository = instance ?: synchronized(this) {
            instance ?: NoticeRepository(context).also { instance = it }
        }
    }
}

internal val Screen.noticeRepo get() = NoticeRepository.get(applicationContext)
