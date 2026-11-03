package cn.taxlearning.ledger

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.LinearLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.activity.OnBackPressedCallback
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class EditActivity : Screen() {
    private var rowId=UUID.randomUUID().toString()
    private var editing=false
    private var detailJson="{}"
    private var year=Rules.currentYear(); private var month=1; private var category=Rules.categories.first()
    private var date=""
    private val fields=linkedMapOf<String,TextInputEditText>()
    private val layouts=linkedMapOf<String,TextInputLayout>()
    private var initial=""
    private var loaded=false
    private var saving=false
    private lateinit var formGroup: LinearLayout
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); editing=intent.hasExtra("id"); rowId=intent.getStringExtra("id")?:state?.getString("rowId")?:rowId
        setup(if(editing) "修改收入明细" else "新增收入明细",edgeToEdge=true)
        onBackPressedDispatcher.addCallback(this,object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(saving) { toast("正在保存，请稍候"); return }
                if(loaded && snapshot()!=initial) MaterialAlertDialogBuilder(this@EditActivity)
                    .setTitle("放弃尚未保存的修改？").setNegativeButton("继续编辑",null)
                    .setPositiveButton("放弃修改") { _,_ -> finish() }.show()
                else finish()
            }
        })
        work {
            val r=if(editing) repo.get(rowId) else null
            if(editing && r==null) { toast("明细已不存在"); finish(); return@work };detailJson=r?.detailJson?:"{}"
            year=state?.getInt("year")?:r?.year?:intent.getIntExtra("year",Rules.currentYear())
            month=state?.getInt("month")?:r?.month?:(Calendar.getInstance().get(Calendar.MONTH)+1)
            category=state?.getString("category")?:r?.category?:category
            date=state?.getString("date")?:r?.date?:String.format(Locale.ROOT,"%04d-%02d-01",year,month)
            banner("模拟记录 · 金额单位：元")
            group("纳税信息")
            val years=Rules.years()
            spinner(formGroup,"纳税年度 *",years.map { "$it 年" },years.indexOf(year)) { year=years[it] }
            space(formGroup)
            spinner(formGroup,"纳税月份 *",(1..12).map { "${it}月" },month-1) { month=it+1 }
            space(formGroup)
            spinner(formGroup,"所得项目 *",Rules.categories,Rules.categories.indexOf(category)) { category=Rules.categories[it] }
            space(formGroup)
            group("收入与扣缴信息")
            field("source","收入来源 *",state?.getString("source")?:r?.source?:"",100)
            field("payer","扣缴义务人 *",state?.getString("payer")?:r?.payer?:"",100)
            group("金额信息")
            field("income","收入金额 *",state?.getString("income")?:r?.let { Rules.decimal(it.incomeCents) }?:"",13,true)
            field("declared","已申报税额 *",state?.getString("declared")?:r?.let { Rules.decimal(it.declaredCents) }?:"",13,true)
            field("paid","已缴税额 *",state?.getString("paid")?:r?.let { Rules.decimal(it.paidCents) }?:"",13,true)
            group("日期与备注")
            val dateButton=button("收入日期 *  $date",formGroup,outline=true) {}
            dateButton.setOnClickListener {
                val p=date.split("-").map { it.toInt() }
                DatePickerDialog(this@EditActivity,{ _,y,m,d ->
                    date=String.format(Locale.ROOT,"%04d-%02d-%02d",y,m+1,d); dateButton.text=getString(R.string.income_date,date)
                },p[0],p[1]-1,p[2]).apply {
                    datePicker.minDate=Calendar.getInstance().apply { clear(); set(2019,0,1) }.timeInMillis
                    datePicker.maxDate=Calendar.getInstance().apply { clear(); set(Rules.currentYear(),11,31) }.timeInMillis
                }.show()
            }
            space(formGroup)
            field("note","备注（选填）",state?.getString("note")?:r?.note?:"",500)
            formGroup.addView(text("无扣缴方可填写“自行申报（模拟）”。收入日期的年份应与纳税年度一致。",12f,MUTED))
            val saveBar=column().apply { setPadding(dp(17),dp(8),dp(17),dp(12));setBackgroundColor(android.graphics.Color.WHITE) }
            flatButton("保存明细",saveBar) { save() };root.addView(saveBar)
            initial=state?.getString("initial")?:snapshot(); loaded=true
        }
    }
    private fun group(title: String) {
        space(body,12);sectionHeader(title)
        formGroup=column().apply { setPadding(dp(17),dp(12),dp(17),dp(8));setBackgroundColor(android.graphics.Color.WHITE) }
        body.addView(formGroup)
    }
    private fun field(key: String, title: String, value: String, max: Int, money: Boolean=false) {
        val l=TextInputLayout(this).apply { hint=title; boxBackgroundMode=TextInputLayout.BOX_BACKGROUND_NONE; setBoxCornerRadii(dp(10).toFloat(),dp(10).toFloat(),dp(10).toFloat(),dp(10).toFloat()); isHintEnabled=true }
        val input=TextInputEditText(l.context).apply {
            setText(value); setTextColor(INK); textSize=15f; minHeight=dp(56)
            inputType=if(money) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            if(key=="note") { inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; minLines=3; maxLines=5 }
            filters=arrayOf(InputFilter.LengthFilter(max)); isSaveEnabled=false
        }
        l.addView(input,LinearLayout.LayoutParams(-1,-2)); formGroup.addView(l,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(14) })
        fields[key]=input; layouts[key]=l
    }
    private fun value(k: String)=fields.getValue(k).text.toString().trim()
    private fun snapshot()=listOf(year,month,category,date,fields.map { it.value.text.toString() }).toString()
    private fun save() {
        if(saving || !loaded)return
        layouts.values.forEach { it.error=null }
        fun amount(k: String,label: String): Long = try { Rules.cents(value(k),label) } catch(e: Exception) { layouts[k]?.error=e.message; fields[k]?.requestFocus(); throw e }
        try {
            listOf("source" to "请填写收入来源","payer" to "请填写扣缴义务人").forEach { (k,hint) ->
                if(value(k).isBlank()) { layouts[k]?.error=hint; fields[k]?.requestFocus(); throw IllegalArgumentException(hint) }
            }
            val r=Income(rowId,year,month,category,value("source"),value("payer"),amount("income","收入金额"),amount("declared","已申报税额"),amount("paid","已缴税额"),date,value("note"),detailJson)
            Rules.validate(r); saving=true
            work {
                try { repo.save(r,editing); toast("保存成功"); finish() } finally { saving=false }
            }
        } catch(e: Exception) { error(e) }
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString("rowId",rowId); out.putInt("year",year); out.putInt("month",month); out.putString("category",category); out.putString("date",date)
        out.putString("initial",initial); fields.forEach { (k,v) -> out.putString(k,v.text.toString()) }; super.onSaveInstanceState(out)
    }
}
