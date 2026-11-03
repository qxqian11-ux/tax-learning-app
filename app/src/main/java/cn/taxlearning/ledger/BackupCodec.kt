package cn.taxlearning.ledger

import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

object BackupCodec {
    const val LIMIT_BYTES = 64 * 1024 * 1024
    const val LIMIT_RECORDS = Rules.MAX_RECORDS
    private fun json(r: Income) = JSONObject().put("id", r.id).put("year", r.year).put("month", r.month)
        .put("category", r.category).put("source", r.source).put("payer", r.payer)
        .put("incomeCents", r.incomeCents).put("declaredCents", r.declaredCents).put("paidCents", r.paidCents)
        .put("date", r.date).put("note", r.note).apply {if(r.detailJson!="{}")put("details",JSONObject(r.detailJson))}
    fun encode(rows: List<Income>): String = JSONObject().put("format", "tax-learning-backup")
        .put("version", if(rows.any {it.detailJson!="{}"})2 else 1).put("exportedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.ROOT).format(Date()))
        .put("records", JSONArray().apply { rows.forEach { put(json(it)) } }).toString(2)
    private fun string(o: JSONObject, key: String): String {
        require(o.has(key) && o.get(key) is String) { "字段 $key 缺失或不是文字" }
        return o.getString(key)
    }
    private fun integer(o: JSONObject, key: String): Long {
        require(o.has(key) && o.get(key) is Number) { "字段 $key 缺失或不是整数" }
        return o.get(key).toString().toLongOrNull() ?: throw IllegalArgumentException("字段 $key 必须是整数")
    }
    fun decode(text: String, maxYear: Int = Rules.currentYear()): List<Income> {
        require(text.toByteArray(Charsets.UTF_8).size <= LIMIT_BYTES) { "备份文件不能大于 64 MB" }
        try {
            val o = JSONObject(text.removePrefix("\uFEFF"))
            require(string(o,"format") == "tax-learning-backup" && integer(o,"version") in 1L..2L) { "不是受支持的税知账备份文件" }
            val a = o.getJSONArray("records")
            require(a.length() <= LIMIT_RECORDS) { "单个备份最多 10,000 条明细" }
            val seen = mutableSetOf<String>()
            return (0 until a.length()).map { index ->
                try {
                    val j = a.getJSONObject(index)
                    val y = integer(j,"year"); val m = integer(j,"month")
                    require(y in 2019L..maxYear.toLong() && m in 1L..12L) { "年度或月份超出范围" }
                    Income(string(j,"id"), y.toInt(), m.toInt(), string(j,"category"), string(j,"source"), string(j,"payer"),
                        integer(j,"incomeCents"), integer(j,"declaredCents"), integer(j,"paidCents"), string(j,"date"), string(j,"note"), if(j.has("details"))IncomeDetails.canonical(j.getJSONObject("details")) else "{}")
                        .also { Rules.validate(it, maxYear); require(seen.add(it.id)) { "备份内存在重复记录编号" } }
                } catch(e: Exception) { throw IllegalArgumentException("第 ${index + 1} 条记录：${e.message}") }
            }
        } catch(e: IllegalArgumentException) { throw e }
        catch(e: Exception) { throw IllegalArgumentException("JSON 格式或必要字段有误：${e.message}") }
    }
    private fun csvCell(s: String): String {
        val safe = if (s.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'$s" else s
        return "\"${safe.replace("\"", "\"\"")}\""
    }
    fun csv(rows: List<Income>): String = "\uFEFF" +
        listOf("记录编号,纳税年度,纳税月份,所得项目,收入来源,扣缴义务人,收入金额(元),已申报税额(元),已缴税额(元),收入日期,备注,详情字段JSON")
            .plus(rows.map { r -> listOf(r.id, r.year.toString(), r.month.toString(), r.category, r.source, r.payer,
                Rules.decimal(r.incomeCents), Rules.decimal(r.declaredCents), Rules.decimal(r.paidCents), r.date, r.note, r.detailJson).joinToString(",") { csvCell(it) } })
            .joinToString("\r\n")
}
