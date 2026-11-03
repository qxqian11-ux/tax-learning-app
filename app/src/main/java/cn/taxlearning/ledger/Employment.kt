package cn.taxlearning.ledger

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

@Entity(tableName = "employments")
data class Employment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyName: String,
    val creditCode: String = "",
    val position: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

object EmploymentRules {
    fun validate(row: Employment) {
        require(runCatching { UUID.fromString(row.id).toString() == row.id }.getOrDefault(false)) { "记录编号无效" }
        require(row.companyName.isNotBlank() && row.companyName.length <= 100) { "请填写单位名称，最多 100 字" }
        require(row.creditCode.isEmpty() || row.creditCode.matches(Regex("[A-Z0-9]{18}"))) { "统一社会信用代码应为 18 位字母或数字，也可以留空" }
        require(row.position.length <= 50) { "职务最多 50 字" }
        require(row.startDate.isEmpty() || Rules.dateValid(row.startDate)) { "任职受雇日期请填写有效的 YYYY-MM-DD 日期，也可以留空" }
        require(row.endDate.isEmpty() || Rules.dateValid(row.endDate)) { "离职日期请填写有效的 YYYY-MM-DD 日期，也可以留空" }
        require(row.startDate.isEmpty() || row.endDate.isEmpty() || row.endDate >= row.startDate) { "离职日期不能早于任职受雇日期" }
    }
}

@Dao
interface EmploymentDao {
    @Query("SELECT * FROM employments ORDER BY createdAt, id") fun observe(): Flow<List<Employment>>
    @Query("SELECT COUNT(*) FROM employments") fun observeCount(): Flow<Int>
    @Query("SELECT * FROM employments ORDER BY createdAt, id") fun all(): List<Employment>
    @Query("SELECT * FROM employments WHERE id = :id") fun get(id: String): Employment?
    @Query("SELECT COUNT(*) FROM employments") fun count(): Int
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insert(row: Employment)
    @Update fun update(row: Employment): Int
    @Query("DELETE FROM employments WHERE id = :id") fun delete(id: String): Int
}

@Database(entities = [Employment::class], version = 1, exportSchema = true)
abstract class EmploymentDatabase : RoomDatabase() { abstract fun employments(): EmploymentDao }

/** Separate local store: adding employment records does not migrate or reseed income data. */
class EmploymentRepository(context: Context, databaseName: String = "learning-employment.db") {
    val db = Room.databaseBuilder(context.applicationContext, EmploymentDatabase::class.java, databaseName).build()
    private val dao = db.employments()
    fun observe() = dao.observe()
    fun observeCount() = dao.observeCount()
    suspend fun all() = withContext(Dispatchers.IO) { dao.all() }
    suspend fun get(id: String) = withContext(Dispatchers.IO) { dao.get(id) }
    suspend fun save(row: Employment, editing: Boolean) = withContext(Dispatchers.IO) {
        EmploymentRules.validate(row)
        db.runInTransaction {
            if (editing) check(dao.update(row) == 1) { "该任职记录已删除，请返回列表" }
            else {
                check(dao.count() < 10_000) { "任职记录已达 10,000 条，请先整理数据" }
                dao.insert(row)
            }
        }
    }
    suspend fun delete(id: String) = withContext(Dispatchers.IO) { dao.delete(id) }
    companion object {
        @Volatile private var instance: EmploymentRepository? = null
        fun get(context: Context): EmploymentRepository = instance ?: synchronized(this) {
            instance ?: EmploymentRepository(context).also { instance = it }
        }
    }
}

internal val Screen.employmentRepo get() = EmploymentRepository.get(applicationContext)
