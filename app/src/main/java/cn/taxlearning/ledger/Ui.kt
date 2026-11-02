package cn.taxlearning.ledger

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

const val ACCENT = 0xFF4088F5.toInt()
const val INK = 0xFF252936.toInt()
const val MUTED = 0xFF747884.toInt()
const val BG = 0xFFF5F6FA.toInt()
const val PALE = 0xFFEDF5FF.toInt()

abstract class Screen : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        if(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("secure",false)) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }
    val repo get() = (application as TaxApp).repository
    lateinit var root: LinearLayout
    lateinit var body: LinearLayout
    fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()
    fun text(s: String, size: Float = 14f, color: Int = INK, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color)
        if(bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    fun shape(color: Int = Color.WHITE, radius: Int = 16) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    fun pad(v: View, n: Int = 18) { v.setPadding(dp(n),dp(n),dp(n),dp(n)) }
    fun space(parent: LinearLayout, n: Int = 12) { parent.addView(View(this), LinearLayout.LayoutParams(1,dp(n))) }
    fun card(parent: LinearLayout = body, color: Int = Color.WHITE, block: LinearLayout.() -> Unit): LinearLayout {
        return column().apply {
            background = shape(color); pad(this)
            parent.addView(this, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(12) })
            block()
        }
    }
    fun label(s: String, parent: LinearLayout = body) { parent.addView(text(s,16f,INK,true)); space(parent) }
    fun button(s: String, parent: LinearLayout = body, outline: Boolean = false, action: () -> Unit): MaterialButton {
        return MaterialButton(this, null, if(outline) com.google.android.material.R.attr.materialButtonOutlinedStyle else com.google.android.material.R.attr.materialButtonStyle).apply {
            text=s; isAllCaps=false; textSize=15f; cornerRadius=dp(12); minHeight=dp(52)
            insetTop=0; insetBottom=0
            if(outline) { setTextColor(ACCENT); strokeColor=ColorStateList.valueOf(0xFFD4E4FC.toInt()); strokeWidth=dp(1) }
            else { backgroundTintList=ColorStateList.valueOf(ACCENT); setTextColor(Color.WHITE) }
            parent.addView(this, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(8) })
            setOnClickListener { action() }
        }
    }
    fun banner(s: String = "学习演示 · 所有记录均为模拟数据", parent: LinearLayout = body) {
        parent.addView(text(s,12f,ACCENT).apply { background=shape(PALE,10); pad(this,12) }, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(16) })
    }
    fun item(title: String, subtitle: String? = null, icon: Int = R.drawable.ic_book, parent: LinearLayout = body, action: () -> Unit) {
        val r = row().apply { minimumHeight=dp(64); setPadding(dp(2),dp(10),dp(2),dp(10)); isClickable=true; isFocusable=true }
        val img = ImageView(this).apply { setImageResource(icon); importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        r.addView(img,LinearLayout.LayoutParams(dp(24),dp(24)).apply { marginEnd=dp(14) })
        val c=column(); c.addView(text(title,15f)); if(subtitle!=null)c.addView(text(subtitle,12f,MUTED))
        r.addView(c,LinearLayout.LayoutParams(0,-2,1f)); r.addView(text("›",24f,MUTED))
        val a=obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)); r.foreground=a.getDrawable(0); a.recycle()
        r.setOnClickListener { action() }; parent.addView(r,LinearLayout.LayoutParams(-1,-2))
    }
    fun setup(title: String, back: Boolean = true) {
        root=column().apply { setBackgroundColor(BG) }; setContentView(root)
        val bar=row().apply { setBackgroundColor(Color.WHITE); setPadding(dp(8),dp(6),dp(16),dp(6)); minimumHeight=dp(58) }
        if(back) bar.addView(ImageButton(this).apply {
            setImageResource(R.drawable.ic_back); contentDescription="返回"; setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }, LinearLayout.LayoutParams(dp(48),dp(48)))
        else bar.setPadding(dp(20),dp(12),dp(20),dp(12))
        bar.addView(text(title,18f,INK,true),LinearLayout.LayoutParams(0,-2,1f))
        root.addView(bar,LinearLayout.LayoutParams(-1,-2))
        val scroll=ScrollView(this).apply { isFillViewport=true; clipToPadding=false }
        body=column().apply { setPadding(dp(16),dp(20),dp(16),dp(24)) }
        scroll.addView(body,ViewGroup.LayoutParams(-1,-2)); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    }
    fun go(cls: Class<*>, year: Int? = null, id: String? = null, title: String? = null) {
        startActivity(Intent(this,cls).apply { if(year!=null)putExtra("year",year); if(id!=null)putExtra("id",id); if(title!=null)putExtra("title",title) })
    }
    fun info(title: String) = go(InfoActivity::class.java,title=title)
    fun error(e: Throwable) {
        MaterialAlertDialogBuilder(this).setTitle("操作未完成").setMessage(e.message ?: "暂时无法操作，请重试")
            .setPositiveButton("知道了",null).show()
    }
    fun work(block: suspend () -> Unit) { lifecycleScope.launch {
        try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) { error(e) }
    } }
    fun toast(s: String) { Toast.makeText(this,s,Toast.LENGTH_LONG).show() }
    fun fieldValue(parent: LinearLayout, name: String, value: String) {
        parent.addView(text(name,12f,MUTED)); space(parent,3); parent.addView(text(value,15f)); space(parent,16)
    }
    fun totals(parent: LinearLayout, year: Int, rows: List<Income>) {
        val t=rows.totals()
        card(parent,ACCENT) {
            addView(text("$year 年度收入合计（元）",13f,0xFFE0EDFF.toInt()))
            space(this,6); addView(text(Rules.money(t.income),30f,Color.WHITE,true)); space(this,16)
            addView(text("已申报税额  ¥ ${Rules.money(t.declared)}",14f,Color.WHITE))
            addView(text("已缴税额      ¥ ${Rules.money(t.paid)}",14f,Color.WHITE))
            space(this,8); addView(text("全年 ${rows.size} 笔 · 不受下方筛选影响",11f,0xFFE0EDFF.toInt()))
        }
    }
    fun spinner(parent: LinearLayout, title: String, choices: List<String>, selected: Int = 0, callback: (Int) -> Unit): Spinner {
        parent.addView(text(title,13f,MUTED)); space(parent,4)
        return Spinner(this).apply {
            background=shape(Color.WHITE,10); minimumHeight=dp(52)
            adapter=ArrayAdapter(this@Screen,android.R.layout.simple_spinner_dropdown_item,choices)
            setSelection(selected.coerceAtLeast(0)); parent.addView(this,LinearLayout.LayoutParams(-1,dp(52)))
            onItemSelectedListener=object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?,v: View?,i: Int,id: Long) { callback(i) }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
    }
}

class QueryActivity : Screen() {
    private var year=Rules.currentYear()
    private val selected=BooleanArray(Rules.categories.size) { true }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); year=state?.getInt("year")?:year
        state?.getBooleanArray("categories")?.copyInto(selected)
        setup("收入纳税明细查询"); banner()
        body.addView(text("选择查询条件",24f,INK,true)); space(body,20)
        val years=Rules.years()
        spinner(body,"纳税年度",years.map { "$it 年" },years.indexOf(year)) { year=years[it] }
        space(body,20); label("所得项目")
        card {
            Rules.categories.forEachIndexed { i,s ->
                addView(com.google.android.material.checkbox.MaterialCheckBox(this@QueryActivity).apply {
                    text=s; textSize=15f; isChecked=selected[i]; minHeight=dp(48)
                    setOnCheckedChangeListener { _,checked -> selected[i]=checked }
                })
            }
        }
        button("查询") {
            if(selected.none { it }) { toast("请至少选择一个所得项目"); return@button }
            startActivity(Intent(this,IncomeListActivity::class.java).putExtra("year",year)
                .putStringArrayListExtra("categories",ArrayList(Rules.categories.filterIndexed { i,_ -> selected[i] })))
        }
        space(body,12); body.addView(text("可查询 2019 年至当前年份。查询结果来自本机数据库，仅用于课程演示。",12f,MUTED))
    }
    override fun onSaveInstanceState(out: Bundle) { out.putInt("year",year); out.putBooleanArray("categories",selected); super.onSaveInstanceState(out) }
}

class IncomeListActivity : Screen() {
    private var year=0; private var month=0; private var category="全部所得项目"
    private var rows: List<Income> = emptyList()
    private var page=0
    private lateinit var summary: LinearLayout
    private lateinit var records: LinearLayout
    private var allowed: List<String> = Rules.categories
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); year=intent.getIntExtra("year",Rules.currentYear())
        month=state?.getInt("month")?:0; category=state?.getString("category")?:category
        allowed=intent.getStringArrayListExtra("categories")?:Rules.categories
        setup("收入纳税明细 · $year"); banner()
        summary=column(); body.addView(summary)
        button("＋ 新增收入明细") { go(EditActivity::class.java,year) }
        button("查看年度统计",outline=true) { go(StatsActivity::class.java,year) }
        space(body,18)
        spinner(body,"纳税月份",listOf("全部月份")+(1..12).map { "${it}月" },month) { month=it; page=0; renderRows() }
        space(body,12)
        val choices=listOf("全部所得项目")+allowed
        spinner(body,"所得项目",choices,choices.indexOf(category)) { category=choices[it]; page=0; renderRows() }
        space(body,16); records=column(); body.addView(records)
        work { repo.observe(year).collect { rows=it; summary.removeAllViews(); totals(summary,year,rows); renderRows() } }
    }
    override fun onSaveInstanceState(out: Bundle) { out.putInt("month",month); out.putString("category",category); super.onSaveInstanceState(out) }
    private fun renderRows() {
        if(!::records.isInitialized)return
        records.removeAllViews()
        val filtered=rows.filter { (month==0||it.month==month) && it.category in allowed && (category=="全部所得项目"||it.category==category) }
        records.addView(text("筛选结果 ${filtered.size} 笔 · 收入 ¥ ${Rules.money(filtered.totals().income)}",13f,MUTED)); space(records)
        if(filtered.isEmpty()) card(records) {
            addView(text("暂时没有收入明细",18f,INK,true)); space(this,8)
            addView(text("可以调整筛选条件，或为 $year 年新增第一笔收入。",14f,MUTED))
        }
        val pages=((filtered.size+49)/50).coerceAtLeast(1)
        page=page.coerceIn(0,pages-1)
        filtered.drop(page*50).take(50).forEach { r -> card(records) {
            addView(text("${r.month}月  ·  ${r.category}",16f,INK,true)); space(this,8)
            addView(text(r.source,14f)); addView(text("扣缴义务人：${r.payer}",12f,MUTED)); space(this,12)
            addView(text("收入金额（元）",12f,MUTED)); addView(text(Rules.money(r.incomeCents),24f,ACCENT,true)); space(this,6)
            addView(text("已申报 ¥ ${Rules.money(r.declaredCents)}\n已缴税 ¥ ${Rules.money(r.paidCents)}",13f,MUTED))
            addView(text("所属年度 $year · 收入日期 ${r.date}",11f,MUTED)); space(this,6)
            button("查看详情",this,true) { go(DetailActivity::class.java,id=r.id) }
            val actions=row()
            val modify=TextView(this@IncomeListActivity).apply { text="修改"; setTextColor(ACCENT); gravity=Gravity.CENTER; minHeight=dp(48); setOnClickListener { go(EditActivity::class.java,id=r.id) } }
            val delete=TextView(this@IncomeListActivity).apply { text="删除"; setTextColor(MUTED); gravity=Gravity.CENTER; minHeight=dp(48); setOnClickListener { confirmDelete(r) { } } }
            actions.addView(modify,LinearLayout.LayoutParams(0,-2,1f)); actions.addView(delete,LinearLayout.LayoutParams(0,-2,1f)); addView(actions)
            setOnClickListener { go(DetailActivity::class.java,id=r.id) }
        } }
        if(pages>1) {
            records.addView(text("第 ${page+1} / $pages 页 · 每页 50 笔",13f,MUTED))
            if(page>0)button("上一页",records,true) { page--; renderRows() }
            if(page<pages-1)button("下一页",records,true) { page++; renderRows() }
        }
    }
}

fun Screen.confirmDelete(r: Income, after: () -> Unit) {
    MaterialAlertDialogBuilder(this).setTitle("删除这笔收入？")
        .setMessage("${r.year} 年 ${r.month} 月 · ${r.category}\n收入 ¥ ${Rules.money(r.incomeCents)}\n删除后不可撤销，可先在“我的”中导出备份。")
        .setNegativeButton("取消",null).setPositiveButton("确认删除") { _,_ -> work { repo.delete(r.id); toast("明细已删除"); after() } }.show()
}

class DetailActivity : Screen() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); setup("收入纳税明细详情") }
    override fun onResume() { super.onResume(); work {
        val r=repo.get(intent.getStringExtra("id")?:"")
        if(r==null) { toast("这条明细已不存在"); finish(); return@work }
        body.removeAllViews(); banner(); card {
            addView(text(r.category,22f,INK,true)); space(this,16)
            listOf("收入来源" to r.source,"扣缴义务人" to r.payer,"纳税年度" to "${r.year} 年","纳税月份" to "${r.month} 月",
                "收入金额" to "¥ ${Rules.money(r.incomeCents)}","已申报税额" to "¥ ${Rules.money(r.declaredCents)}",
                "已缴税额" to "¥ ${Rules.money(r.paidCents)}","收入日期" to r.date,"备注" to r.note.ifBlank { "无" }).forEach { fieldValue(this,it.first,it.second) }
        }
        button("修改明细") { go(EditActivity::class.java,id=r.id) }
        button("删除明细",outline=true) { confirmDelete(r) { finish() } }
    } }
}
