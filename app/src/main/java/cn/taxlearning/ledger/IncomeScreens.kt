package cn.taxlearning.ledger

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import android.app.Dialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal fun Screen.sectionHeader(title: String, parent: LinearLayout = body) {
    val line=row().apply { setBackgroundColor(Color.WHITE);minimumHeight=ru(48);setPadding(ru(17),ru(12),ru(16),ru(12)) }
    line.addView(View(this).apply { background=shape(ACCENT,2) },LinearLayout.LayoutParams(ru(4),ru(17)).apply { marginEnd=ru(9) })
    line.addView(referenceText(title,15.5f,INK,true));parent.addView(line,LinearLayout.LayoutParams(-1,-2))
    parent.addView(View(this).apply { setBackgroundColor(0xFFECECEC.toInt()) },LinearLayout.LayoutParams(-1,1))
}
internal fun Screen.valueRow(parent: LinearLayout,title: String,value: String,labelColor: Int=0xFF9C9DA2.toInt(),valueColor: Int=INK,minHeight: Int=36) {
    val line=row().apply { setBackgroundColor(Color.WHITE);gravity=Gravity.TOP;minimumHeight=ru(minHeight);setPadding(ru(17),ru(8),ru(17),ru(8)) }
    line.addView(referenceText(title,16f,labelColor),LinearLayout.LayoutParams(0,-2,.34f).apply { marginEnd=ru(12) })
    line.addView(referenceText(value,16f,valueColor).apply { gravity=Gravity.END },LinearLayout.LayoutParams(0,-2,.66f));parent.addView(line)
}
internal fun Screen.flatButton(title: String,parent: LinearLayout,action: ()->Unit)=button(title,parent,action=action).apply {
    cornerRadius=ru(4);textSize=18f*resources.displayMetrics.widthPixels/resources.displayMetrics.density/400f;minHeight=ru(50)
}
internal fun Screen.topAction(title: String,action: ()->Unit){
    val link=referenceText(title,16f,ACCENT).apply {gravity=Gravity.CENTER;minimumHeight=ru(48);setPadding(ru(10),0,ru(28),0);setOnClickListener {action()}}
    titleBar.addView(link,android.widget.FrameLayout.LayoutParams(-2,-1,Gravity.END))
}

class QueryActivity : Screen() {
    private var year=Rules.currentYear()
    private val selected=BooleanArray(Rules.categories.size){it<4}
    private var expanded=false
    private lateinit var yearValue: TextView
    private lateinit var choices: LinearLayout
    private lateinit var queryFooter: LinearLayout
    private var yearSheet: Dialog? = null
    override fun onCreate(state: Bundle?) {
        super.onCreate(state);year=state?.getInt("year")?:year;expanded=state?.getBoolean("expanded")?:false
        state?.getBooleanArray("categories")?.let { old -> old.copyInto(selected,endIndex=minOf(old.size,selected.size)) }
        setup("收入纳税明细",edgeToEdge=true)
        topAction("⋯") { MaterialAlertDialogBuilder(this).setTitle("所得类型（含历史分类）").setMultiChoiceItems(Rules.categories.toTypedArray(),selected) {_,index,checked -> selected[index]=checked;renderChoices()}.setPositiveButton("完成",null).show() }
        referenceGap(body,12);sectionHeader("请选择纳税年度")
        val annual=row().apply { setBackgroundColor(Color.WHITE);minimumHeight=ru(50);setPadding(ru(17),ru(11),ru(17),ru(11));id=R.id.query_year }
        annual.addView(referenceText("年度",17.5f,0xFF66686D.toInt()),LinearLayout.LayoutParams(0,-2,1f))
        yearValue=referenceText(year.toString(),17.5f);annual.addView(yearValue,LinearLayout.LayoutParams(0,-2,1.78f));annual.addView(ChevronView(this,0xFFD6D6D6.toInt()),LinearLayout.LayoutParams(ru(8),ru(14)))
        annual.setOnClickListener { selectYear() };body.addView(annual);referenceGap(body,12);sectionHeader("请选择所得类型")
        choices=column().apply { setBackgroundColor(Color.WHITE) };body.addView(choices);renderChoices()
        val footer=column().apply { setPadding(ru(20),ru(26),ru(20),0);setBackgroundColor(BG) };queryFooter=footer
        flatButton("查询",footer) {
            if(selected.none { it }) { toast("请至少选择一个所得项目");return@flatButton }
            startActivity(Intent(this,IncomeListActivity::class.java).putExtra("year",year).putStringArrayListExtra("categories",ArrayList(Rules.categories.filterIndexed { i,_ -> selected[i] })))
        }.id=R.id.query_submit
        attachQueryFooter()
    }
    private fun attachQueryFooter() {
        if(!::queryFooter.isInitialized)return
        (queryFooter.parent as? android.view.ViewGroup)?.removeView(queryFooter)
        queryFooter.setPadding(ru(20),ru(if(expanded)26 else 34),ru(20),0)
        if(expanded)root.addView(queryFooter) else body.addView(queryFooter)
    }
    private fun renderChoices() {
        choices.removeAllViews()
        Rules.categories.forEachIndexed { index,label ->
            if(index<9 && (expanded || index<4)) {
                val check=CheckBox(this).apply {
                    text=label;textSize=17.5f*resources.displayMetrics.widthPixels/resources.displayMetrics.density/400f;includeFontPadding=false;setTextColor(INK);isChecked=selected[index];buttonDrawable=CircleCheckDrawable(resources.displayMetrics.widthPixels/400f);buttonTintList=null;compoundDrawablePadding=ru(11);setPadding(ru(17),ru(11),ru(17),ru(11));minimumHeight=ru(54)
                    setOnCheckedChangeListener { _,value -> selected[index]=value }
                }
                choices.addView(check,LinearLayout.LayoutParams(-1,-2));choices.addView(View(this).apply { setBackgroundColor(0xFFEEEEEE.toInt()) },LinearLayout.LayoutParams(-1,1).apply { marginStart=ru(17) })
            }
        }
        val toggle=row().apply {id=R.id.query_category_toggle;gravity=Gravity.CENTER;minimumHeight=ru(52);setOnClickListener {expanded=!expanded;renderChoices()}}
        toggle.addView(referenceText(if(expanded)"收起" else "其他类型",16f,ACCENT))
        toggle.addView(DoubleChevronView(this,expanded),LinearLayout.LayoutParams(ru(18),ru(18)).apply {marginStart=ru(5)})
        choices.addView(toggle);attachQueryFooter()
    }
    private fun selectYear() {
        yearSheet?.dismiss()
        val sheet=Dialog(this).apply {requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)};yearSheet=sheet
        sheet.setOnDismissListener {if(yearSheet===sheet)yearSheet=null}
        val content=column().apply {
            setBackgroundColor(Color.WHITE);clipToOutline=true
            outlineProvider=object:android.view.ViewOutlineProvider() {
                override fun getOutline(view:View,outline:android.graphics.Outline) {val radius=ru(12);outline.setRoundRect(0,0,view.width,view.height+radius,radius.toFloat())}
            }
        }
        val actions=row()
        val cancel=referenceText("取消",17f,0xFF606266.toInt()).apply {id=R.id.query_year_cancel;gravity=Gravity.CENTER_VERTICAL;setPadding(ru(17),0,0,0);setOnClickListener {sheet.dismiss()}}
        actions.addView(cancel,LinearLayout.LayoutParams(0,-1,1f))
        val wheel=ReferenceYearWheel(this,year)
        val confirm=referenceText("确定",17f,0xFF5094CA.toInt()).apply {id=R.id.query_year_confirm;gravity=Gravity.END or Gravity.CENTER_VERTICAL;setPadding(0,0,ru(17),0);setOnClickListener {year=wheel.selectedYear;yearValue.text=year.toString();sheet.dismiss()}}
        actions.addView(confirm,LinearLayout.LayoutParams(0,-1,1f));content.addView(actions,LinearLayout.LayoutParams(-1,ru(46)))
        content.addView(View(this).apply {setBackgroundColor(0xFFE0E0E0.toInt())},LinearLayout.LayoutParams(-1,1))
        content.addView(wheel,LinearLayout.LayoutParams(-1,ru(301)));content.setPadding(0,0,0,ru(8))
        sheet.setContentView(content);sheet.setCanceledOnTouchOutside(true)
        sheet.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);setDimAmount(.5f)
            setGravity(Gravity.BOTTOM)
        }
        sheet.show()
        sheet.window?.apply {decorView.setPadding(0,0,0,0);setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT)}
    }
    override fun onDestroy() {yearSheet?.dismiss();yearSheet=null;super.onDestroy()}
    override fun onSaveInstanceState(out: Bundle) { out.putInt("year",year);out.putBooleanArray("categories",selected);out.putBoolean("expanded",expanded);super.onSaveInstanceState(out) }
}

class IncomeListActivity : Screen() {
    private var year=0;private var page=0
    private var rows: List<Income> = emptyList()
    private var allowed: List<String> = Rules.categories
    private lateinit var summary: LinearLayout;private lateinit var records: LinearLayout
    override fun onCreate(state: Bundle?) {
        super.onCreate(state);year=intent.getIntExtra("year",Rules.currentYear());page=state?.getInt("page")?:0
        allowed=intent.getStringArrayListExtra("categories")?:Rules.categories
        setup("收入纳税明细",edgeToEdge=true);topAction("批量申诉") { info("批量申诉") };referenceGap(body,12)
        summary=column();body.addView(summary)
        (titleBar.getChildAt(0) as TextView).apply {id=R.id.income_title_add;contentDescription="收入纳税明细，点击新增";setOnClickListener {go(EditActivity::class.java,year)}}
        records=column();body.addView(records)
        work { repo.observe(year).collect { rows=it;renderSummary();renderRows() } }
    }
    private fun renderSummary() {
        summary.removeAllViews();val totals=rows.totals()
        fun sumRow(label: String,value: Long){
            val line=row().apply {setBackgroundColor(Color.WHITE);minimumHeight=ru(45);setPadding(ru(17),ru(10),ru(17),ru(10))}
            line.addView(referenceText(label,16f),LinearLayout.LayoutParams(0,-2,1f))
            line.addView(referenceText("${Rules.decimal(value)}元",16f));summary.addView(line)
        }
        val incomeLine=row().apply {setBackgroundColor(Color.WHITE);minimumHeight=ru(45);setPadding(ru(17),ru(10),ru(17),ru(10))}
        incomeLine.addView(referenceText("收入合计",16f))
        incomeLine.addView(SummaryHelpView(this).apply {id=R.id.income_summary_help;contentDescription="收入合计说明"},LinearLayout.LayoutParams(ru(16),ru(16)).apply {marginStart=ru(5);marginEnd=ru(1)})
        incomeLine.addView(referenceText("：",16f),LinearLayout.LayoutParams(0,-2,1f))
        incomeLine.addView(referenceText("${Rules.decimal(totals.income)}元",16f));summary.addView(incomeLine)
        summary.addView(View(this).apply {setBackgroundColor(0xFFEEEEEE.toInt())},LinearLayout.LayoutParams(-1,1).apply {setMargins(ru(17),0,ru(17),0)})
        sumRow("已申报税额合计：",totals.declared)
        summary.setOnClickListener { MaterialAlertDialogBuilder(this).setTitle("$year 年度汇总").setMessage("收入、已申报税额与已缴税额合计来自该年度全部 ${rows.size} 条本机记录。\n收入 ${Rules.decimal(totals.income)} 元，已申报 ${Rules.decimal(totals.declared)} 元，已缴 ${Rules.decimal(totals.paid)} 元。所有金额均为模拟录入数据。").setPositiveButton("知道了",null).show() }
    }
    private fun renderRows() {
        if(!::records.isInitialized)return
        records.removeAllViews()
        val filtered=rows.filter { it.category in allowed }
        if(filtered.isEmpty()) {
            val empty=column().apply { setBackgroundColor(Color.WHITE);setPadding(ru(20),ru(45),ru(20),ru(45));gravity=Gravity.CENTER }
            empty.addView(text("暂时没有收入明细",18f,INK,true));space(empty,10);empty.addView(text("点击顶部“收入纳税明细”新增 $year 年的记录。",13f,MUTED));records.addView(empty,LinearLayout.LayoutParams(-1,-2).apply { topMargin=ru(12) })
        }
        val pages=((filtered.size+49)/50).coerceAtLeast(1);page=page.coerceIn(0,pages-1)
        filtered.drop(page*50).take(50).forEach { record ->
            val line=column().apply { setBackgroundColor(Color.WHITE);setPadding(ru(17),ru(17),ru(17),ru(17));contentDescription="${record.source}，${record.year}年${record.month}月，${record.category}，收入${Rules.decimal(record.incomeCents)}元" }
            val title=row();title.addView(referenceText(record.category,18.5f),LinearLayout.LayoutParams(0,ru(24),1f))
            title.addView(referenceText("${record.year}-${record.month.toString().padStart(2,'0')}",18f).apply {gravity=Gravity.END},LinearLayout.LayoutParams(-2,ru(24)).apply {marginEnd=ru(34)})
            line.addView(title);referenceGap(line,6)
            val details=row();val copy=column()
            listOf("所得项目小类：${if(record.category=="工资薪金")"正常工资薪金" else record.category}","扣缴义务人：${record.payer}","收入：${Rules.decimal(record.incomeCents)}元","已申报税额：${Rules.decimal(record.declaredCents)}元").forEachIndexed { index,label ->
                copy.addView(referenceText(label,16f,0xFFA3A3A3.toInt()).apply { minimumHeight=ru(22);isSingleLine=true;ellipsize=android.text.TextUtils.TruncateAt.END },LinearLayout.LayoutParams(-1,-2).apply {if(index>0)topMargin=ru(6)})
            }
            details.addView(copy,LinearLayout.LayoutParams(0,-2,1f));details.addView(ChevronView(this,0xFFD1D1D1.toInt()),LinearLayout.LayoutParams(ru(12),ru(20)))
            line.addView(details)
            line.setOnClickListener { go(DetailActivity::class.java,id=record.id) }
            line.setOnLongClickListener { MaterialAlertDialogBuilder(this).setTitle("操作明细").setItems(arrayOf("查看详情","修改明细","删除明细")) { _,index -> when(index){0 -> go(DetailActivity::class.java,id=record.id);1 -> go(EditActivity::class.java,id=record.id);else -> confirmDelete(record){} } }.show();true }
            records.addView(line,LinearLayout.LayoutParams(-1,-2).apply { topMargin=ru(12) })
        }
        if(pages>1){records.addView(text("第 ${page+1} / $pages 页 · 每页 50 笔",13f,MUTED));if(page>0)button("上一页",records,true){page--;renderRows()};if(page<pages-1)button("下一页",records,true){page++;renderRows()}}
    }
    override fun onSaveInstanceState(out: Bundle) { out.putInt("page",page);super.onSaveInstanceState(out) }
}

class DetailActivity : Screen() {
    override fun onCreate(state: Bundle?) {super.onCreate(state);setup("收入纳税明细详情",edgeToEdge=true);topAction("申诉") { info("异议申诉") }}
    override fun onResume() {super.onResume();work {
        val record=repo.get(intent.getStringExtra("id")?:"")
        if(record==null){toast("这条明细已不存在");finish();return@work}
        body.removeAllViews();referenceGap(body,12);sectionHeader("纳税明细信息")
        valueRow(body,"收入：","${Rules.decimal(record.incomeCents)}元");valueRow(body,"已申报税额：","${Rules.decimal(record.declaredCents)}元")
        val calculation=referenceText("查看税款计算 ›",16f,ACCENT).apply { gravity=Gravity.END;setBackgroundColor(Color.WHITE);minimumHeight=ru(50);gravity=Gravity.END or Gravity.CENTER_VERTICAL;setPadding(ru(17),ru(8),ru(17),ru(8));setOnClickListener { MaterialAlertDialogBuilder(this@DetailActivity).setTitle("模拟税款记录").setMessage("本条收入 ${Rules.decimal(record.incomeCents)} 元，已申报税额 ${Rules.decimal(record.declaredCents)} 元，已缴税额 ${Rules.decimal(record.paidCents)} 元。\n\n以上金额由你手动录入，本应用不计算真实应纳税额。未维护的扣除项目不生成虚构金额。").setPositiveButton("知道了",null).show() } };body.addView(calculation)
        referenceGap(body,12);sectionHeader("纳税明细-基础情况")
        listOf("所得项目小类：" to if(record.category=="工资薪金")"正常工资薪金" else record.category,"扣缴义务人名称：" to record.payer,"扣缴义务人纳税人识别号：" to "未维护（模拟）","主管税务机构：" to "未维护（模拟）","申报渠道：" to "手工录入（学习版）","收入日期：" to record.date,"税款所属期：" to "${record.year}-${record.month.toString().padStart(2,'0')}").forEach { valueRow(body,it.first,it.second,minHeight=when(it.first){"扣缴义务人名称：","扣缴义务人纳税人识别号：","主管税务机构：" -> 58;"税款所属期：" -> 46;else -> 36}) }
        referenceGap(body,12);body.addView(referenceText("温馨提示：本页面展示您在本机录入的模拟收入和税额。尚未维护的扣除项目以“—”显示，原有数据可在页面下方修改。",14f,INK).apply { minimumHeight=ru(80);setBackgroundColor(0xFFEAF4FF.toInt());setPadding(ru(17),ru(12),ru(17),ru(12)) })
        referenceGap(body,12);sectionHeader("本期收入与扣除详情")
        valueRow(body,"本期收入：","${Rules.decimal(record.incomeCents)}元")
        listOf("本期免税收入：","本期减除费用：","本期专项扣除：","本期其他扣除：","本期准予扣除的捐赠项目：").forEach { valueRow(body,it,"—") }
        referenceGap(body,12);sectionHeader("本机记录补充");valueRow(body,"收入来源：",record.source);valueRow(body,"已缴税额：","${Rules.decimal(record.paidCents)}元");valueRow(body,"备注：",record.note.ifBlank { "无" })
        val actions=column().apply { setPadding(ru(17),ru(5),ru(17),ru(20)) };button("修改明细",actions){go(EditActivity::class.java,id=record.id)};button("删除明细",actions,true){confirmDelete(record){finish()}};body.addView(actions)
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

