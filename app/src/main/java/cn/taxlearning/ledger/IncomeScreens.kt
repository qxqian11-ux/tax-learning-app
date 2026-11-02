package cn.taxlearning.ledger

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal fun Screen.sectionHeader(title: String, parent: LinearLayout = body) {
    val line=row().apply { setBackgroundColor(Color.WHITE);setPadding(dp(17),dp(14),dp(16),dp(14)) }
    line.addView(View(this).apply { background=shape(ACCENT,2) },LinearLayout.LayoutParams(dp(4),dp(17)).apply { marginEnd=dp(9) })
    line.addView(text(title,15f,INK,true));parent.addView(line,LinearLayout.LayoutParams(-1,-2));parent.addView(View(this).apply { setBackgroundColor(0xFFEEEEEE.toInt()) },LinearLayout.LayoutParams(-1,dp(1)))
}
internal fun Screen.valueRow(parent: LinearLayout, title: String, value: String, labelColor: Int = 0xFF9A9CA2.toInt(), valueColor: Int = INK) {
    val line=row().apply { setBackgroundColor(Color.WHITE);gravity=Gravity.TOP;setPadding(dp(17),dp(13),dp(17),dp(13)) }
    line.addView(text(title,16f,labelColor),LinearLayout.LayoutParams(0,-2,.42f).apply { marginEnd=dp(12) })
    line.addView(text(value,16f,valueColor).apply { gravity=Gravity.END },LinearLayout.LayoutParams(0,-2,.58f));parent.addView(line)
}
internal fun Screen.flatButton(title: String, parent: LinearLayout, action: () -> Unit) = button(title,parent,action=action).apply { cornerRadius=dp(4);textSize=18f;minHeight=dp(50) }
internal fun Screen.topAction(title: String, action: () -> Unit) {
    val link=text(title,15f,ACCENT).apply { gravity=Gravity.CENTER;minimumHeight=dp(48);setPadding(dp(12),0,dp(17),0);setOnClickListener { action() } }
    titleBar.addView(link,android.widget.FrameLayout.LayoutParams(-2,-1,Gravity.END))
}

class QueryActivity : Screen() {
    private var year=Rules.currentYear()
    private val selected=BooleanArray(Rules.categories.size){it<4}
    private var expanded=true
    private lateinit var yearValue: TextView
    private lateinit var choices: LinearLayout
    override fun onCreate(state: Bundle?) {
        super.onCreate(state);year=state?.getInt("year")?:year;expanded=state?.getBoolean("expanded")?:true
        state?.getBooleanArray("categories")?.let { old -> old.copyInto(selected,endIndex=minOf(old.size,selected.size)) }
        setup("收入纳税明细",edgeToEdge=true)
        space(body,12);sectionHeader("请选择纳税年度")
        val annual=row().apply { setBackgroundColor(Color.WHITE);setPadding(dp(17),dp(13),dp(17),dp(13));id=R.id.query_year }
        annual.addView(text("年度",18f,0xFF66686D.toInt()),LinearLayout.LayoutParams(0,-2,1f))
        yearValue=text(year.toString(),18f);annual.addView(yearValue,LinearLayout.LayoutParams(0,-2,1.7f));annual.addView(text("›",25f,0xFFD0D2D7.toInt()))
        annual.setOnClickListener { selectYear() };body.addView(annual);space(body,12);sectionHeader("请选择所得类型")
        choices=column().apply { setBackgroundColor(Color.WHITE) };body.addView(choices);renderChoices()
        val footer=column().apply { setPadding(dp(20),dp(15),dp(20),dp(18));setBackgroundColor(BG) }
        flatButton("查询",footer) {
            if(selected.none { it }) { toast("请至少选择一个所得项目");return@flatButton }
            startActivity(Intent(this,IncomeListActivity::class.java).putExtra("year",year).putStringArrayListExtra("categories",ArrayList(Rules.categories.filterIndexed { i,_ -> selected[i] })))
        }.id=R.id.query_submit
        root.addView(footer)
    }
    private fun renderChoices() {
        choices.removeAllViews()
        Rules.categories.forEachIndexed { index,label ->
            if(expanded || index<4) {
                val check=CheckBox(this).apply {
                    text=label;textSize=18f;setTextColor(INK);isChecked=selected[index];buttonDrawable=CircleCheckDrawable(resources.displayMetrics.density);buttonTintList=null;compoundDrawablePadding=dp(16);setPadding(dp(17),dp(11),dp(17),dp(11));minimumHeight=dp(54)
                    setOnCheckedChangeListener { _,value -> selected[index]=value }
                }
                choices.addView(check,LinearLayout.LayoutParams(-1,-2));choices.addView(View(this).apply { setBackgroundColor(0xFFEEEEEE.toInt()) },LinearLayout.LayoutParams(-1,dp(1)).apply { marginStart=dp(17) })
            }
        }
        val toggle=text(if(expanded)"收起 ⌃" else "展开 ⌄",16f,ACCENT).apply { gravity=Gravity.CENTER;minimumHeight=dp(52);setOnClickListener { expanded=!expanded;renderChoices() } };choices.addView(toggle)
    }
    private fun selectYear() {
        val years=Rules.years();val sheet=BottomSheetDialog(this);val content=column().apply { setPadding(dp(16),dp(18),dp(16),dp(12)) }
        content.addView(text("选择纳税年度",19f,INK,true).apply { gravity=Gravity.CENTER },LinearLayout.LayoutParams(-1,dp(44)))
        val list=ListView(this).apply { adapter=ArrayAdapter(this@QueryActivity,android.R.layout.simple_list_item_single_choice,years.map { "$it 年" });choiceMode=ListView.CHOICE_MODE_SINGLE;setItemChecked(years.indexOf(year),true);setSelection(years.indexOf(year));setOnItemClickListener { _,_,index,_ -> year=years[index];yearValue.text=year.toString();sheet.dismiss() } }
        content.addView(list,LinearLayout.LayoutParams(-1,dp(390)));sheet.setContentView(content);sheet.show();sheet.behavior.isDraggable=false;sheet.behavior.state=com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
    }
    override fun onSaveInstanceState(out: Bundle) { out.putInt("year",year);out.putBooleanArray("categories",selected);out.putBoolean("expanded",expanded);super.onSaveInstanceState(out) }
}

class IncomeListActivity : Screen() {
    private var year=0;private var month=0;private var category="全部所得项目";private var page=0
    private var rows: List<Income> = emptyList()
    private var allowed: List<String> = Rules.categories
    private lateinit var summary: LinearLayout;private lateinit var records: LinearLayout;private lateinit var filters: LinearLayout
    override fun onCreate(state: Bundle?) {
        super.onCreate(state);year=intent.getIntExtra("year",Rules.currentYear());month=state?.getInt("month")?:0;category=state?.getString("category")?:category;page=state?.getInt("page")?:0
        allowed=intent.getStringArrayListExtra("categories")?:Rules.categories
        setup("收入纳税明细",edgeToEdge=true);topAction("批量申诉") { info("批量申诉") };space(body,12)
        summary=column();body.addView(summary)
        filters=column().apply { setPadding(dp(17),dp(12),dp(17),dp(14));setBackgroundColor(Color.WHITE) };body.addView(filters)
        filters.addView(text("$year 年 · 筛选条件",14f,INK,true))
        spinner(filters,"纳税月份",listOf("全部月份")+(1..12).map { "${it}月" },month) { month=it;page=0;renderRows() }
        space(filters,8);val cats=listOf("全部所得项目")+allowed
        spinner(filters,"所得项目",cats,cats.indexOf(category)) { category=cats[it];page=0;renderRows() }
        this@IncomeListActivity.filters.visibility=if(state?.getBoolean("filters")==true)View.VISIBLE else View.GONE
        records=column();body.addView(records)
        val footer=row().apply { setBackgroundColor(Color.WHITE);setPadding(dp(6),dp(5),dp(6),dp(5));elevation=dp(2).toFloat() }
        listOf("筛选","年度统计","＋ 新增").forEachIndexed { index,label -> val action=text(label,15f,ACCENT).apply { gravity=Gravity.CENTER;minimumHeight=dp(46);id=listOf(R.id.income_filter,R.id.income_statistics,R.id.income_add)[index];setOnClickListener { when(index){0 -> {this@IncomeListActivity.filters.visibility=if(this@IncomeListActivity.filters.visibility==View.VISIBLE)View.GONE else View.VISIBLE};1 -> go(StatsActivity::class.java,year);else -> go(EditActivity::class.java,year)} } };footer.addView(action,LinearLayout.LayoutParams(0,-2,1f)) };root.addView(footer)
        work { repo.observe(year).collect { rows=it;renderSummary();renderRows() } }
    }
    private fun renderSummary() {
        summary.removeAllViews();val totals=rows.totals()
        valueRow(summary,"收入合计 ⓘ：","${Rules.decimal(totals.income)}元",INK)
        summary.addView(View(this).apply { setBackgroundColor(0xFFEEEEEE.toInt()) },LinearLayout.LayoutParams(-1,dp(1)).apply { setMargins(dp(17),0,dp(17),0) })
        valueRow(summary,"已申报税额合计：","${Rules.decimal(totals.declared)}元",INK)
        valueRow(summary,"已缴税额合计：","${Rules.decimal(totals.paid)}元",0xFF8D919A.toInt())
        summary.setOnClickListener { MaterialAlertDialogBuilder(this).setTitle("$year 年度汇总").setMessage("收入、已申报税额与已缴税额合计来自该年度全部 ${rows.size} 条本机记录；下方筛选仅改变明细列表。所有金额均为模拟录入数据。").setPositiveButton("知道了",null).show() }
    }
    private fun renderRows() {
        if(!::records.isInitialized)return
        records.removeAllViews()
        val filtered=rows.filter { (month==0||it.month==month)&&it.category in allowed&&(category=="全部所得项目"||it.category==category) }
        if(month!=0 || category!="全部所得项目")records.addView(text("筛选结果 ${filtered.size} 笔 · 收入 ¥ ${Rules.money(filtered.totals().income)}",13f,MUTED).apply { setPadding(dp(17),dp(12),dp(17),dp(8)) })
        if(filtered.isEmpty()) {
            val empty=column().apply { setBackgroundColor(Color.WHITE);setPadding(dp(20),dp(45),dp(20),dp(45));gravity=Gravity.CENTER }
            empty.addView(text("暂时没有收入明细",18f,INK,true));space(empty,10);empty.addView(text("可以调整筛选条件，或新增 $year 年的收入记录。",13f,MUTED));records.addView(empty,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(12) })
        }
        val pages=((filtered.size+49)/50).coerceAtLeast(1);page=page.coerceIn(0,pages-1)
        filtered.drop(page*50).take(50).forEach { record ->
            val line=column().apply { setBackgroundColor(Color.WHITE);setPadding(dp(17),dp(17),dp(17),dp(17)) }
            val title=row();title.addView(text(record.category,19f),LinearLayout.LayoutParams(0,-2,1f));title.addView(text("${record.year}-${record.month.toString().padStart(2,'0')}",18f).apply { gravity=Gravity.END });line.addView(title);space(line,9)
            val details=row();val copy=column();copy.addView(text("所得项目小类：${if(record.category=="工资薪金")"正常工资薪金" else record.category}",15f,0xFFA0A2A7.toInt()))
            copy.addView(text("扣缴义务人：${record.payer}",15f,0xFFA0A2A7.toInt()));copy.addView(text("收入：${Rules.decimal(record.incomeCents)}元",15f,0xFFA0A2A7.toInt()));copy.addView(text("已申报税额：${Rules.decimal(record.declaredCents)}元",15f,0xFFA0A2A7.toInt()));copy.addView(text("已缴税额：${Rules.decimal(record.paidCents)}元",13f,0xFFA0A2A7.toInt()));copy.addView(text(record.source,12f,MUTED))
            details.addView(copy,LinearLayout.LayoutParams(0,-2,1f));details.addView(text("›",30f,0xFFD0D2D7.toInt()),LinearLayout.LayoutParams(dp(20),-2));line.addView(details)
            line.setOnClickListener { go(DetailActivity::class.java,id=record.id) }
            line.setOnLongClickListener { MaterialAlertDialogBuilder(this).setTitle("操作明细").setItems(arrayOf("查看详情","修改明细","删除明细")) { _,index -> when(index){0 -> go(DetailActivity::class.java,id=record.id);1 -> go(EditActivity::class.java,id=record.id);else -> confirmDelete(record){} } }.show();true }
            records.addView(line,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(12) })
        }
        if(pages>1){records.addView(text("第 ${page+1} / $pages 页 · 每页 50 笔",13f,MUTED));if(page>0)button("上一页",records,true){page--;renderRows()};if(page<pages-1)button("下一页",records,true){page++;renderRows()}}
    }
    override fun onSaveInstanceState(out: Bundle) { out.putInt("month",month);out.putString("category",category);out.putInt("page",page);out.putBoolean("filters",this@IncomeListActivity.filters.visibility==View.VISIBLE);super.onSaveInstanceState(out) }
}

class DetailActivity : Screen() {
    override fun onCreate(state: Bundle?) {super.onCreate(state);setup("收入纳税明细详情",edgeToEdge=true);topAction("申诉") { info("异议申诉") }}
    override fun onResume() {super.onResume();work {
        val record=repo.get(intent.getStringExtra("id")?:"")
        if(record==null){toast("这条明细已不存在");finish();return@work}
        body.removeAllViews();space(body,12);sectionHeader("纳税明细信息")
        valueRow(body,"收入：","${Rules.decimal(record.incomeCents)}元");valueRow(body,"已申报税额：","${Rules.decimal(record.declaredCents)}元");valueRow(body,"已缴税额：","${Rules.decimal(record.paidCents)}元")
        val calculation=text("查看税款计算 ›",16f,ACCENT).apply { gravity=Gravity.END;setBackgroundColor(Color.WHITE);setPadding(dp(17),dp(10),dp(17),dp(17));setOnClickListener { MaterialAlertDialogBuilder(this@DetailActivity).setTitle("模拟税款记录").setMessage("本条收入 ${Rules.decimal(record.incomeCents)} 元，已申报税额 ${Rules.decimal(record.declaredCents)} 元，已缴税额 ${Rules.decimal(record.paidCents)} 元。\n\n以上金额由你手动录入，本应用不计算真实应纳税额。未维护的扣除项目不生成虚构金额。").setPositiveButton("知道了",null).show() } };body.addView(calculation)
        space(body,12);sectionHeader("纳税明细-基础情况")
        listOf("所得项目小类：" to if(record.category=="工资薪金")"正常工资薪金" else record.category,"收入来源：" to record.source,"扣缴义务人名称：" to record.payer,"扣缴义务人纳税人识别号：" to "未维护（模拟）","主管税务机构：" to "未维护（模拟）","申报渠道：" to "手工录入（学习版）","收入日期：" to record.date,"税款所属期：" to "${record.year}-${record.month.toString().padStart(2,'0')}").forEach { valueRow(body,it.first,it.second) }
        space(body,12);body.addView(text("温馨提示：这里展示本机保存的模拟记录。未维护的扣除信息以“—”表示，所有金额仅供课程学习。",13f,INK).apply { setBackgroundColor(PALE);setPadding(dp(17),dp(12),dp(17),dp(12)) })
        space(body,12);sectionHeader("本期收入与扣除详情")
        valueRow(body,"本期收入：","${Rules.decimal(record.incomeCents)}元")
        listOf("本期免税收入：","本期减除费用：","本期专项扣除：","本期其他扣除：","本期准予扣除的捐赠项目：").forEach { valueRow(body,it,"—") }
        space(body,12);sectionHeader("备注");valueRow(body,"备注：",record.note.ifBlank { "无" })
        val actions=column().apply { setPadding(dp(17),dp(5),dp(17),dp(20)) };button("修改明细",actions){go(EditActivity::class.java,id=record.id)};button("删除明细",actions,true){confirmDelete(record){finish()}};body.addView(actions)
    }}
}

private class CircleCheckDrawable(private val density: Float) : Drawable() {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun isStateful()=true
    override fun onStateChange(stateSet: IntArray): Boolean {invalidateSelf();return true}
    override fun getIntrinsicWidth()=(47*density).toInt()
    override fun getIntrinsicHeight()=(30*density).toInt()
    override fun draw(c: Canvas) {
        val checked=state.contains(android.R.attr.state_checked);val x=bounds.left+29*density;val y=bounds.exactCenterY();val r=11.5f*density
        p.color=if(checked)ACCENT else 0xFFD3D5D8.toInt();p.style=if(checked)Paint.Style.FILL else Paint.Style.STROKE;p.strokeWidth=1.2f*density;c.drawCircle(x,y,r,p)
        if(checked){p.color=Color.WHITE;p.style=Paint.Style.STROKE;p.strokeWidth=1.6f*density;p.strokeCap=Paint.Cap.ROUND;p.strokeJoin=Paint.Join.ROUND;c.drawPath(Path().apply {moveTo(x-5*density,y);lineTo(x-1*density,y+4*density);lineTo(x+6*density,y-4*density)},p)}
    }
    override fun setAlpha(alpha: Int){p.alpha=alpha}
    override fun setColorFilter(filter: ColorFilter?){p.colorFilter=filter}
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

