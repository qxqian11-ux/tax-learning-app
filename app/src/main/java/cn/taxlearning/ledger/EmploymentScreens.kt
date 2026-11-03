package cn.taxlearning.ledger

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class EmploymentListActivity : Screen() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setup("任职受雇", edgeToEdge = true)
        body.setPadding(ru(17), ru(12), ru(17), ru(20))
        (titleBar.getChildAt(0) as TextView).apply {
            id = R.id.employment_title
            contentDescription = "任职受雇，点击新增"
            setOnClickListener { go(EmploymentEditActivity::class.java) }
        }
        topAction("新增") { go(EmploymentEditActivity::class.java) }
        titleBar.getChildAt(titleBar.childCount - 1).id = R.id.employment_add
        work { employmentRepo.observe().collect { render(it) } }
    }
    private fun render(records: List<Employment>) {
        body.removeAllViews()
        if (records.isEmpty()) {
            body.addView(referenceText("暂无任职受雇记录",17f,MUTED).apply {
                gravity = Gravity.CENTER; setPadding(0,ru(70),0,ru(12))
            })
            body.addView(referenceText("点击右上角“新增”添加单位",14f,MUTED).apply { gravity = Gravity.CENTER })
        }
        records.forEach { record ->
            val card = column().apply {
                background = shape(Color.WHITE,4)
                setPadding(ru(22),ru(22),ru(22),ru(18))
                isClickable = true; isFocusable = true
                contentDescription = "任职记录：${record.companyName}"
            }
            card.addView(referenceText(record.companyName,18.5f,Color.BLACK).apply { minimumHeight=ru(25) })
            referenceGap(card,7)
            listOf(
                "统一社会信用代码" to record.creditCode,
                "职务" to record.position,
                "任职受雇日期" to record.startDate,
                "离职日期" to record.endDate
            ).forEach { (label,value) ->
                card.addView(referenceText("$label：${value.ifEmpty { "-" }}",16f,0xFF9FA1A5.toInt()).apply {
                    minimumHeight=ru(28); gravity=Gravity.CENTER_VERTICAL
                })
            }
            val attrs=obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            card.foreground=attrs.getDrawable(0); attrs.recycle()
            card.setOnClickListener { go(EmploymentEditActivity::class.java,id=record.id) }
            card.setOnLongClickListener {
                MaterialAlertDialogBuilder(this).setTitle("删除这条任职记录？")
                    .setMessage(record.companyName+"\n删除后无法撤销。")
                    .setNegativeButton("取消",null)
                    .setPositiveButton("确认删除") { _,_ -> work { employmentRepo.delete(record.id);toast("任职记录已删除") } }.show()
                true
            }
            body.addView(card,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=ru(12) })
        }
    }
}

class EmploymentEditActivity : Screen() {
    private var recordId=UUID.randomUUID().toString()
    private var createdAt=System.currentTimeMillis()
    private var editing=false
    private var loaded=false
    private var saving=false
    private var initial=""
    private val fields=linkedMapOf<String,TextInputEditText>()
    private lateinit var form: LinearLayout

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        editing=intent.hasExtra("id")
        recordId=intent.getStringExtra("id")?:state?.getString("recordId")?:recordId
        setup(if(editing)"编辑任职受雇" else "新增任职受雇",edgeToEdge=true)
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(saving) { toast("正在保存，请稍候");return }
                if(loaded && snapshot()!=initial) MaterialAlertDialogBuilder(this@EmploymentEditActivity)
                    .setTitle("放弃尚未保存的修改？").setNegativeButton("继续编辑",null)
                    .setPositiveButton("放弃修改") { _,_ -> finish() }.show()
                else finish()
            }
        })
        work {
            val row=if(editing)employmentRepo.get(recordId) else null
            if(editing && row==null) { toast("任职记录已不存在");finish();return@work }
            createdAt=state?.getLong("createdAt")?:row?.createdAt?:createdAt
            referenceGap(body,12);sectionHeader("任职受雇信息")
            form=column().apply { setBackgroundColor(Color.WHITE);setPadding(ru(17),ru(15),ru(17),ru(16)) }
            body.addView(form)
            field("companyName","单位名称 *",state?.getString("companyName")?:row?.companyName?:"",100,R.id.employment_company)
            field("creditCode","统一社会信用代码",state?.getString("creditCode")?:row?.creditCode?:"",18,R.id.employment_credit)
            field("position","职务",state?.getString("position")?:row?.position?:"",50,R.id.employment_position)
            field("startDate","任职受雇日期",state?.getString("startDate")?:row?.startDate?:"",10,R.id.employment_start,true)
            field("endDate","离职日期",state?.getString("endDate")?:row?.endDate?:"",10,R.id.employment_end,true)
            form.addView(referenceText("除单位名称外均可留空，列表中显示为“-”。",12f,MUTED))
            val footer=column().apply { setPadding(ru(17),ru(8),ru(17),ru(12));setBackgroundColor(Color.WHITE) }
            flatButton("保存",footer) { save() }.id=R.id.employment_save
            root.addView(footer)
            initial=state?.getString("initial")?:snapshot();loaded=true
        }
    }
    private fun field(key:String,label:String,value:String,max:Int,viewId:Int,date:Boolean=false) {
        val layout=TextInputLayout(this).apply { hint=label;boxBackgroundMode=TextInputLayout.BOX_BACKGROUND_NONE }
        val input=TextInputEditText(layout.context).apply {
            id=viewId;setText(value);setTextColor(INK);textSize=16f;minHeight=dp(52)
            inputType=InputType.TYPE_CLASS_TEXT;isSingleLine=true;isSaveEnabled=false
            filters=if(key=="creditCode")arrayOf(InputFilter.AllCaps(),InputFilter.LengthFilter(max)) else arrayOf(InputFilter.LengthFilter(max))
        }
        layout.addView(input,LinearLayout.LayoutParams(-1,-2))
        if(date) {
            layout.helperText="YYYY-MM-DD，可点击日历选择"
            layout.endIconMode=TextInputLayout.END_ICON_CUSTOM
            layout.setEndIconDrawable(android.R.drawable.ic_menu_my_calendar)
            layout.endIconContentDescription="选择$label"
            layout.setEndIconOnClickListener {
                val now=Calendar.getInstance()
                val parts=if(Rules.dateValid(input.text.toString()))input.text.toString().split("-").map { it.toInt() }
                    else listOf(now.get(Calendar.YEAR),now.get(Calendar.MONTH)+1,now.get(Calendar.DAY_OF_MONTH))
                DatePickerDialog(this,{_,y,m,d -> input.setText(String.format(Locale.ROOT,"%04d-%02d-%02d",y,m+1,d))},parts[0],parts[1]-1,parts[2]).apply {
                    setButton(android.content.DialogInterface.BUTTON_NEUTRAL,"清空") {_,_ -> input.setText("")}
                }.show()
            }
        }
        form.addView(layout,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=ru(10)})
        fields[key]=input
    }
    private fun value(key:String)=fields.getValue(key).text.toString().trim()
    private fun snapshot()=fields.map { it.value.text.toString() }.toString()
    private fun save() {
        if(!loaded || saving)return
        try {
            val row=Employment(recordId,value("companyName"),value("creditCode").uppercase(Locale.ROOT),value("position"),value("startDate"),value("endDate"),createdAt)
            EmploymentRules.validate(row);saving=true
            work { try { employmentRepo.save(row,editing);toast("保存成功");finish() } finally { saving=false } }
        } catch(e:Exception) { error(e) }
    }
    override fun onSaveInstanceState(out:Bundle) {
        out.putString("recordId",recordId);out.putLong("createdAt",createdAt);out.putString("initial",initial)
        fields.forEach { (key,value) -> out.putString(key,value.text.toString()) }
        super.onSaveInstanceState(out)
    }
}
