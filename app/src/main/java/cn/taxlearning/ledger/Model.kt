package cn.taxlearning.ledger

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

object Rules {
    val categories = listOf("工资薪金", "劳务报酬", "稿酬", "特许权使用费", "经营所得", "利息、股息、红利", "财产租赁", "财产转让", "偶然所得", "其他所得")
    fun currentYear() = Calendar.getInstance().get(Calendar.YEAR)
    fun years() = (2019..currentYear()).reversed().toList()
    const val MAX_CENTS = 999_999_999_999L
    const val MAX_RECORDS = 10_000
    fun cents(text: String, label: String): Long {
        require(text.matches(Regex("[0-9]+(\\.[0-9]{1,2})?"))) { "$label 请输入非负金额，最多两位小数" }
        return try {
            BigDecimal(text).movePointRight(2).longValueExact().also {
                require(it <= MAX_CENTS) { "$label 不能超过 9,999,999,999.99 元" }
            }
        } catch (e: ArithmeticException) { throw IllegalArgumentException("$label 金额过大") }
    }
    fun decimal(cents: Long) = BigDecimal.valueOf(cents, 2).toPlainString()
    fun money(cents: Long): String = String.format(Locale.CHINA, "%,.2f", BigDecimal.valueOf(cents, 2))
    fun dateValid(date: String): Boolean {
        if (!date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) return false
        return runCatching {
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
            f.format(f.parse(date)!!) == date
        }.getOrDefault(false)
    }
    fun validate(r: Income, maxYear: Int = currentYear()) {
        require(runCatching { UUID.fromString(r.id).toString() == r.id }.getOrDefault(false)) { "记录编号不是有效的 UUID" }
        require(r.year in 2019..maxYear) { "纳税年度须在 2019 至 $maxYear 年之间" }
        require(r.month in 1..12) { "请选择 1 至 12 月" }
        require(r.category in categories) { "请选择有效的所得项目" }
        require(r.source.isNotBlank() && r.source.length <= 100) { "收入来源为必填项，最多 100 字" }
        require(r.payer.isNotBlank() && r.payer.length <= 100) { "扣缴义务人为必填项，最多 100 字；无扣缴方可填“自行申报（模拟）”" }
        require(listOf(r.incomeCents, r.declaredCents, r.paidCents).all { it in 0..MAX_CENTS }) { "金额超出范围" }
        require(dateValid(r.date)) { "请选择有效的收入日期" }
        require(r.date.substring(0,4).toInt() == r.year) { "收入日期的年份须与纳税年度一致" }
        require(r.note.length <= 500) { "备注最多 500 字" }
    }
}

@Entity(tableName = "incomes", indices = [Index(value = ["year", "month"])])
data class Income(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val year: Int, val month: Int, val category: String, val source: String, val payer: String,
    val incomeCents: Long, val declaredCents: Long, val paidCents: Long, val date: String, val note: String = ""
)
@Entity(tableName = "metadata")
data class Metadata(@PrimaryKey val key: String, val value: String)
@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes WHERE year = :year ORDER BY month DESC, date DESC, id")
    fun observeYear(year: Int): Flow<List<Income>>
    @Query("SELECT * FROM incomes ORDER BY year DESC, month DESC, date DESC, id") fun all(): List<Income>
    @Query("SELECT * FROM incomes WHERE id = :id") fun get(id: String): Income?
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insert(r: Income)
    @Update fun update(r: Income): Int
    @Query("DELETE FROM incomes WHERE id = :id") fun delete(id: String): Int
    @Query("SELECT * FROM metadata WHERE `key` = :key") fun metadata(key: String): Metadata?
    @Insert fun mark(m: Metadata)
}
@Database(entities = [Income::class, Metadata::class], version = 1, exportSchema = true)
abstract class LedgerDatabase : RoomDatabase() { abstract fun incomes(): IncomeDao }

data class Totals(val income: Long, val declared: Long, val paid: Long)
fun List<Income>.totals() = Totals(sumOf { it.incomeCents }, sumOf { it.declaredCents }, sumOf { it.paidCents })
