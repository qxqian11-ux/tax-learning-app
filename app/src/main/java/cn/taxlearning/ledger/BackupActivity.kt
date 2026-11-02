package cn.taxlearning.ledger

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupState : ViewModel() {
    var exportText: String?=null
    var importRows: List<Income>?=null
}
class BackupActivity : Screen() {
    private val model: BackupState by viewModels()
    private val jsonExport=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if(uri!=null) write(uri) else { model.exportText=null; toast("已取消导出") }
    }
    private val csvExport=registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if(uri!=null) write(uri) else { model.exportText=null; toast("已取消导出") }
    }
    private val importFile=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) work {
            val rows=withContext(Dispatchers.IO) {
                val content=contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
                    while(true) { val n=stream.read(buffer); if(n<0)break
                        require(bytes.size()+n<=BackupCodec.LIMIT_BYTES) { "备份文件不能大于 64 MB" }; bytes.write(buffer,0,n) }
                    bytes.toString("UTF-8")
                }?:throw IllegalArgumentException("无法读取此文件，请选择本地 JSON 备份")
                BackupCodec.decode(content)
            }
            model.importRows=rows; confirmImport()
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); setup("数据备份与恢复"); banner("备份仅包含本应用的模拟收入记录")
        card {
            addView(text("给学习记录留一份副本",22f,INK,true)); space(this,10)
            addView(text("卸载应用会清除本机数据库。请将备份保存在系统“文件”中，换机时再导入。",14f,MUTED))
        }
        label("导出数据")
        button("导出 CSV 明细") { selectScope(false) }
        button("备份为 JSON",outline=true) { selectScope(true) }
        space(body,24); label("恢复数据")
        button("选择 JSON 备份文件",outline=true) { importFile.launch(arrayOf("application/json","text/plain","application/octet-stream")) }
        space(body,16)
        body.addView(text("恢复规则\n\n• 支持本应用导出的 JSON，单文件不超过 64 MB / 10,000 条。\n• 相同编号且内容一致的记录自动跳过。\n• 同编号内容冲突时，整次导入取消，不覆盖原数据。\n• 新增、删除、修改会自动更新列表及统计。\n• CSV 用于查看，恢复请使用 JSON。",13f,MUTED))
        if(model.importRows!=null)confirmImport()
    }
    private fun selectScope(json: Boolean) {
        val years=Rules.years(); val choices=listOf("全部年度")+years.map { "$it 年" }
        MaterialAlertDialogBuilder(this).setTitle("选择导出范围").setItems(choices.toTypedArray()) { _,i -> work {
            val rows=repo.all(if(i==0)null else years[i-1])
            model.exportText=withContext(Dispatchers.Default) { if(json)BackupCodec.encode(rows) else BackupCodec.csv(rows) }
            val name="税知账_${if(i==0)"全部年度" else years[i-1]}_${System.currentTimeMillis()}"
            if(json)jsonExport.launch("$name.json") else csvExport.launch("$name.csv")
        } }.show()
    }
    private fun write(uri: android.net.Uri) { work {
        val content=model.exportText?:throw IllegalArgumentException("导出任务已失效，请重新选择导出范围")
        withContext(Dispatchers.IO) {
            contentResolver.openOutputStream(uri,"wt")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                ?:throw IllegalArgumentException("无法写入所选文件，请选择其他位置")
        }
        model.exportText=null; toast("导出成功，文件已保存到所选位置")
    } }
    private fun confirmImport() {
        val rows=model.importRows?:return
        MaterialAlertDialogBuilder(this).setTitle("恢复 ${rows.size} 条模拟记录？")
            .setMessage("备份已通过格式与字段校验。导入将合并新记录，自动跳过完全相同的编号；若内容冲突则整次取消。建议先备份本机数据。")
            .setNegativeButton("取消") { _,_ -> model.importRows=null }
            .setOnCancelListener { model.importRows=null }
            .setPositiveButton("确认恢复") { _,_ -> model.importRows=null; work {
                val (added,skipped)=repo.restore(rows); toast("恢复完成：新增 $added 条，跳过 $skipped 条")
            } }.show()
    }
}
