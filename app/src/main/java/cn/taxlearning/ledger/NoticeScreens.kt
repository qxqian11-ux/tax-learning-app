package cn.taxlearning.ledger

import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.UUID

class NoticeListActivity : Screen() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setup("广播提醒", edgeToEdge = true)
        body.setPadding(ru(17), ru(12), ru(17), ru(20))
        (titleBar.getChildAt(0) as TextView).apply {
            id = R.id.notice_title
            contentDescription = "广播提醒，点击新增"
            setOnClickListener { go(NoticeEditActivity::class.java) }
        }
        topAction("新增") { go(NoticeEditActivity::class.java) }
        titleBar.getChildAt(titleBar.childCount - 1).id = R.id.notice_add
        work { noticeRepo.observe().collect { render(it) } }
    }
    private fun render(records: List<Notice>) {
        body.removeAllViews()
        if (records.isEmpty()) {
            body.addView(referenceText("暂无广播提醒", 17f, MUTED).apply {
                gravity = Gravity.CENTER; setPadding(0, ru(70), 0, ru(12))
            })
            body.addView(referenceText("点击右上角“新增”添加广播", 14f, MUTED).apply { gravity = Gravity.CENTER })
        }
        records.forEach { record ->
            val card = column().apply {
                background = shape(Color.WHITE, 4)
                setPadding(ru(20), ru(19), ru(20), ru(19))
                isClickable = true; isFocusable = true
                contentDescription = "广播提醒：${record.content}"
            }
            card.addView(referenceText(record.content, 17f, INK))
            val attrs = obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            card.foreground = attrs.getDrawable(0); attrs.recycle()
            card.setOnClickListener { go(NoticeEditActivity::class.java, id = record.id) }
            card.setOnLongClickListener {
                MaterialAlertDialogBuilder(this).setTitle("删除这条广播？")
                    .setMessage(record.content + "\n删除后无法撤销。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确认删除") { _, _ -> work { noticeRepo.delete(record.id); toast("广播已删除") } }.show()
                true
            }
            body.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = ru(12) })
        }
    }
}

class NoticeEditActivity : Screen() {
    private var recordId = UUID.randomUUID().toString()
    private var createdAt = System.currentTimeMillis()
    private var loaded = false
    private var saving = false
    private var initial = ""
    private lateinit var input: TextInputEditText
    private val editing get() = intent.hasExtra("id")
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        recordId = intent.getStringExtra("id") ?: state?.getString("recordId") ?: recordId
        setup(if (editing) "编辑广播提醒" else "新增广播提醒", edgeToEdge = true)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (saving) { toast("正在保存，请稍候"); return }
                if (loaded && input.text.toString() != initial) MaterialAlertDialogBuilder(this@NoticeEditActivity)
                    .setTitle("放弃尚未保存的修改？").setNegativeButton("继续编辑", null)
                    .setPositiveButton("放弃修改") { _, _ -> finish() }.show()
                else finish()
            }
        })
        work {
            val row = if (editing) noticeRepo.get(recordId) else null
            if (editing && row == null) { toast("广播已不存在"); finish(); return@work }
            createdAt = state?.getLong("createdAt") ?: row?.createdAt ?: createdAt
            referenceGap(body, 12); sectionHeader("广播内容")
            val form = column().apply { setBackgroundColor(Color.WHITE); setPadding(ru(17), ru(15), ru(17), ru(20)) }
            val layout = TextInputLayout(this@NoticeEditActivity).apply {
                hint = "广播内容 *"; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_NONE
                helperText = "最多 500 字，保存后在首页滚动展示"
            }
            input = TextInputEditText(layout.context).apply {
                id = R.id.notice_content; setTextColor(INK); textSize = 16f
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 4; maxLines = 10; gravity = Gravity.TOP
                filters = arrayOf(InputFilter.LengthFilter(500)); isSaveEnabled = false
                setText(state?.getString("content") ?: row?.content ?: "")
            }
            layout.addView(input, LinearLayout.LayoutParams(-1, -2)); form.addView(layout)
            body.addView(form)
            val footer = column().apply { setPadding(ru(17), ru(8), ru(17), ru(12)); setBackgroundColor(Color.WHITE) }
            flatButton("保存", footer) { save() }.id = R.id.notice_save
            root.addView(footer)
            initial = state?.getString("initial") ?: input.text.toString(); loaded = true
        }
    }
    private fun save() {
        if (!loaded || saving || isFinishing) return
        try {
            val row = Notice(recordId, input.text.toString().trim(), createdAt)
            NoticeRules.validate(row); saving = true
            work {
                try { noticeRepo.save(row, editing); toast("保存成功"); finish() }
                finally { if (!isFinishing) saving = false }
            }
        } catch (e: Exception) { error(e) }
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString("recordId", recordId); out.putLong("createdAt", createdAt); out.putString("initial", initial)
        if (::input.isInitialized) out.putString("content", input.text.toString())
        super.onSaveInstanceState(out)
    }
}
