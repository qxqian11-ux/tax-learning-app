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
const val INK = 0xFF303133.toInt()
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
    lateinit var titleBar: android.widget.FrameLayout
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
    fun setup(title: String, back: Boolean = true, edgeToEdge: Boolean = false) {
        root=column().apply { setBackgroundColor(BG) };setContentView(root)
        titleBar=android.widget.FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        titleBar.addView(referenceText(title,18f,0xFF303133.toInt()).apply { gravity=Gravity.CENTER;setPadding(ru(67),0,ru(67),0);isSingleLine=true },android.widget.FrameLayout.LayoutParams(-1,-1))
        if(back){
            val backButton=row().apply { gravity=Gravity.CENTER_VERTICAL;setPadding(ru(16),0,ru(8),0);contentDescription="返回";setOnClickListener { onBackPressedDispatcher.onBackPressed() } }
            backButton.addView(ChevronView(this,ACCENT,true),LinearLayout.LayoutParams(ru(10),ru(22)))
            backButton.addView(referenceText("返回",17f,ACCENT),LinearLayout.LayoutParams(-2,-2).apply { marginStart=ru(4) })
            titleBar.addView(backButton,android.widget.FrameLayout.LayoutParams(-2,-1,Gravity.START))
        }
        root.addView(titleBar,LinearLayout.LayoutParams(-1,ru(48)))
        root.addView(View(this).apply { setBackgroundColor(0xFFE6E8EB.toInt()) },LinearLayout.LayoutParams(-1,1))
        val scroll=ScrollView(this).apply { id=R.id.page_scroll;isFillViewport=true;clipToPadding=false }
        body=column().apply { if(!edgeToEdge)setPadding(dp(16),dp(20),dp(16),dp(24)) }
        scroll.addView(body,ViewGroup.LayoutParams(-1,-2));root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    }
    fun go(cls: Class<*>, year: Int? = null, id: String? = null, title: String? = null) {
        startActivity(Intent(this,cls).apply { if(year!=null)putExtra("year",year); if(id!=null)putExtra("id",id); if(title!=null)putExtra("title",title) })
    }
    fun info(title: String) {
        if(title=="任职受雇信息" || title=="任职受雇")go(EmploymentListActivity::class.java)
        else go(InfoActivity::class.java,title=title)
    }
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

fun Screen.confirmDelete(r: Income, after: () -> Unit) {
    MaterialAlertDialogBuilder(this).setTitle("删除这笔收入？")
        .setMessage("${r.year} 年 ${r.month} 月 · ${r.category}\n收入 ¥ ${Rules.money(r.incomeCents)}\n删除后不可撤销，可先在“我的”中导出备份。")
        .setNegativeButton("取消",null).setPositiveButton("确认删除") { _,_ -> work { repo.delete(r.id); toast("明细已删除"); after() } }.show()
}

