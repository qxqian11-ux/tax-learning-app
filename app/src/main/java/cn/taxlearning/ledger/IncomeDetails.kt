package cn.taxlearning.ledger

import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

/** Optional manually entered detail fields. Stored and backed up with the same income record. */
object IncomeDetails {
    val moneyKeys=setOf("taxFree","expense","special","pension","medical","unemployment","housing","other","donation")
    private val textKeys=setOf("subcategory","payerTaxId","agency","channel","declarationDate")
    val components=listOf("pension","medical","unemployment","housing")
    fun parse(value:String)=JSONObject(value)
    fun canonical(value:JSONObject):String=JSONObject().apply {value.keys().asSequence().toList().sorted().forEach {put(it,value.get(it))}}.toString()
    fun validate(value:String) {
        require(value.length<=8000){"详情字段过长"};val json=parse(value)
        json.keys().forEach { key ->
            val field=json.get(key)
            when(key) {
                in moneyKeys -> require(field is Number && field.toString().toLongOrNull() in 0L..Rules.MAX_CENTS){"详情金额无效"}
                in textKeys -> {require(field is String && field.length<=200){"详情文字无效"};if(key=="declarationDate" && field.isNotEmpty())require(Rules.dateValid(field)){"申报日期无效"}}
                else -> throw IllegalArgumentException("不支持的详情字段：$key")
            }
        }
    }
    fun amount(record:Income,key:String):String {val j=parse(record.detailJson);return if(j.has(key))Rules.decimal(j.getLong(key))+"元" else "—"}
    fun value(record:Income,key:String,fallback:String="—")=parse(record.detailJson).optString(key,fallback).ifBlank {fallback}
    fun update(record:Income,key:String,input:String):Income {
        val value=input.trim();val j=parse(record.detailJson)
        val updated=when(key) {
            "income" -> record.copy(incomeCents=Rules.cents(value,"收入"))
            "declared" -> record.copy(declaredCents=Rules.cents(value,"已申报税额"))
            "payer" -> record.copy(payer=value)
            "period" -> {
                require(value.matches(Regex("[0-9]{4}-[0-9]{2}"))){"税款所属期格式应为 YYYY-MM"}
                val y=value.substring(0,4).toInt();val m=value.substring(5).toInt()
                require(y in 2019..Rules.currentYear() && m in 1..12){"税款所属期超出范围"}
                val calendar=Calendar.getInstance().apply {clear();set(y,m-1,1)}
                val day=record.date.takeLast(2).toInt().coerceAtMost(calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                record.copy(year=y,month=m,date=String.format(Locale.ROOT,"%04d-%02d-%02d",y,m,day))
            }
            in moneyKeys -> {
                if(value.isEmpty())j.remove(key) else j.put(key,Rules.cents(value,"金额"))
                if(key in components){val total=components.sumOf {j.optLong(it,0L)};require(total<=Rules.MAX_CENTS){"专项扣除合计过大"};j.put("special",total)}
                record.copy(detailJson=canonical(j))
            }
            in textKeys -> {if(value.isEmpty())j.remove(key) else j.put(key,value);record.copy(detailJson=canonical(j))}
            else -> throw IllegalArgumentException("不支持的编辑项目")
        }
        Rules.validate(updated);return updated
    }
}
