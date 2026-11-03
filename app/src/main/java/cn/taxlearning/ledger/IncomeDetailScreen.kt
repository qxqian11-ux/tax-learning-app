package cn.taxlearning.ledger

import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DetailActivity:Screen() {
    private var expanded=false
    private var busy=false
    override fun onCreate(state:Bundle?){super.onCreate(state);expanded=state?.getBoolean("expanded")?:false;setup("收入纳税明细详情",edgeToEdge=true);topAction("申诉"){info("异议申诉")}}
    override fun onResume(){super.onResume();reload()}
    private fun reload(){work {val record=repo.get(intent.getStringExtra("id")?:"");if(record==null){toast("这条明细已不存在");finish()}else render(record)}}
    private fun calculation(record:Income){MaterialAlertDialogBuilder(this).setTitle("模拟税款记录").setMessage("收入 ${Rules.decimal(record.incomeCents)} 元\n已申报税额 ${Rules.decimal(record.declaredCents)} 元\n已缴税额 ${Rules.decimal(record.paidCents)} 元\n本期专项扣除 ${IncomeDetails.amount(record,"special")}\n\n本页均为手工录入的本机数据，不自动计算真实应纳税额。").setPositiveButton("知道了",null).show()}
    private fun edit(record:Income,key:String,title:String,current:String){
        if(busy)return
        val numeric=key in IncomeDetails.moneyKeys || key in setOf("income","declared")
        val input=EditText(this).apply {id=R.id.detail_edit_input;inputType=if(numeric)InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT;setText(current.takeUnless {it=="—"}?.removeSuffix("元")?:"");setSelectAllOnFocus(true);setPadding(ru(20),ru(16),ru(20),ru(16));maxLines=if(numeric)1 else 3}
        val dialog=MaterialAlertDialogBuilder(this).setTitle("修改$title").setView(input).setNegativeButton("取消",null).setPositiveButton("保存",null).create()
        dialog.setOnShowListener {dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if(!busy){busy=true;work {try {val latest=repo.get(record.id)?:throw IllegalArgumentException("明细已不存在");val updated=IncomeDetails.update(latest,key,input.text.toString());repo.save(updated,true);dialog.dismiss();render(updated)}catch(e:Exception){if(e is kotlinx.coroutines.CancellationException)throw e;input.error=e.message}finally{busy=false}}}
        }};dialog.show()
    }
    private fun row(record:Income,key:String,title:String,value:String,parent:LinearLayout=body,height:Int=36,color:Int=Color.WHITE){
        val line=row().apply {setBackgroundColor(color);gravity=Gravity.TOP;minimumHeight=ru(height);setPadding(ru(17),ru(8),ru(17),ru(8))}
        val labelWeight=if(key=="donation").5f else .34f
        line.addView(referenceText("$title：",if(key in IncomeDetails.components)14.5f else 16f,0xFF9C9DA2.toInt()),LinearLayout.LayoutParams(0,-2,labelWeight).apply {marginEnd=ru(12)})
        line.addView(referenceText(value,16f).apply {gravity=Gravity.END;contentDescription="编辑：$title";isClickable=true;isFocusable=true;minimumHeight=ru(22);setOnClickListener {edit(record,key,title,value)}},LinearLayout.LayoutParams(0,-2,1f-labelWeight));parent.addView(line)
    }
    private fun render(record:Income){
        val scroll=findViewById<ScrollView>(R.id.page_scroll);val position=scroll.scrollY
        body.removeAllViews();referenceGap(body,12);sectionHeader("纳税明细信息")
        row(record,"income","收入",Rules.decimal(record.incomeCents)+"元");row(record,"declared","已申报税额",Rules.decimal(record.declaredCents)+"元")
        val calc=row().apply {gravity=Gravity.END or Gravity.CENTER_VERTICAL;minimumHeight=ru(50);setPadding(ru(17),ru(8),ru(17),ru(8));setBackgroundColor(Color.WHITE);setOnClickListener {calculation(record)}}
        calc.addView(referenceText("查看税款计算",16f,ACCENT));calc.addView(ChevronView(this,ACCENT),LinearLayout.LayoutParams(ru(12),ru(20)));body.addView(calc)
        referenceGap(body,12);sectionHeader("纳税明细-基础情况")
        row(record,"subcategory","所得项目小类",IncomeDetails.value(record,"subcategory",if(record.category=="工资薪金")"正常工资薪金" else record.category))
        row(record,"payer","扣缴义务人名称",record.payer,height=58)
        row(record,"payerTaxId","扣缴义务人纳税人识别号",IncomeDetails.value(record,"payerTaxId"),height=58)
        row(record,"agency","主管税务机构",IncomeDetails.value(record,"agency"),height=58)
        row(record,"channel","申报渠道",IncomeDetails.value(record,"channel"))
        row(record,"declarationDate","申报日期",IncomeDetails.value(record,"declarationDate"))
        row(record,"period","税款所属期","${record.year}-${record.month.toString().padStart(2,'0')}",height=46)
        referenceGap(body,12)
        body.addView(referenceText("温馨提示：本页收入与扣除以本机录入的模拟记录为准。专项附加扣除和个人养老金等未维护的项目不生成金额，您可以点击右侧数据修改，并通过“查看税款计算”核对。",14f).apply {minimumHeight=ru(80);setBackgroundColor(0xFFEAF4FF.toInt());setPadding(ru(17),ru(12),ru(17),ru(12))
            val link="查看税款计算";val offset=text.toString().lastIndexOf(link);val styled=android.text.SpannableString(text)
            styled.setSpan(object:android.text.style.ClickableSpan(){override fun onClick(view:android.view.View){calculation(record)};override fun updateDrawState(paint:android.text.TextPaint){paint.color=ACCENT;paint.isUnderlineText=false}},offset,offset+link.length,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text=styled;movementMethod=android.text.method.LinkMovementMethod.getInstance()
        })
        referenceGap(body,12);sectionHeader("本期收入与扣除详情")
        row(record,"income","本期收入",Rules.decimal(record.incomeCents)+"元")
        row(record,"taxFree","本期免税收入",IncomeDetails.amount(record,"taxFree"))
        row(record,"expense","本期减除费用",IncomeDetails.amount(record,"expense"))
        val special=row().apply {setBackgroundColor(Color.WHITE);minimumHeight=ru(46);setPadding(ru(17),ru(8),ru(17),ru(8))}
        special.addView(referenceText("本期专项扣除：",16f,0xFF9C9DA2.toInt()),LinearLayout.LayoutParams(0,-2,1f))
        special.addView(referenceText(IncomeDetails.amount(record,"special"),16f,0xFF606370.toInt()).apply {contentDescription="编辑：本期专项扣除";setOnClickListener {edit(record,"special","本期专项扣除",IncomeDetails.amount(record,"special"))}})
        special.addView(referenceText(if(expanded)"▲" else "▼",14f,0xFF303133.toInt()).apply {id=R.id.detail_special_toggle;contentDescription=if(expanded)"收起专项扣除" else "展开专项扣除";gravity=Gravity.CENTER;setOnClickListener {expanded=!expanded;render(record)}},LinearLayout.LayoutParams(ru(20),ru(28)).apply {marginStart=ru(4)});body.addView(special)
        if(expanded){val group=column().apply {id=R.id.detail_special_components;setBackgroundColor(0xFFF8F9FE.toInt());setPadding(ru(15),0,ru(20),0)};IncomeDetails.components.zip(listOf("基本养老保险","基本医疗保险","失业保险","住房公积金")).forEach {(key,label)->row(record,key,label,IncomeDetails.amount(record,key),group,49,0xFFF8F9FE.toInt())};body.addView(group)}
        row(record,"other","本期其他扣除",IncomeDetails.amount(record,"other"))
        row(record,"donation","本期准予扣除的捐赠项目",IncomeDetails.amount(record,"donation"),height=58)
        referenceGap(body,24);scroll.post {scroll.scrollTo(0,position)}
    }
    override fun onSaveInstanceState(out:Bundle){out.putBoolean("expanded",expanded);super.onSaveInstanceState(out)}
}
