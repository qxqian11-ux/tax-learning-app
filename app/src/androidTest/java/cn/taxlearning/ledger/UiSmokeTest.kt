package cn.taxlearning.ledger

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert.*
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.io.File

/** On-device smoke checks and screenshots; no network or real user data. */
@RunWith(AndroidJUnit4::class)
class UiSmokeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun open(type: Class<*>, extras: Intent.() -> Unit = {}): Activity {
        val intent=Intent(instrumentation.targetContext,type).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); extras() }
        return instrumentation.startActivitySync(intent).also { instrumentation.waitForIdleSync() }
    }
    private fun texts(view: View): List<String> {
        val own=if(view is TextView)listOf(view.text.toString())else emptyList()
        return own + if(view is ViewGroup)(0 until view.childCount).flatMap { texts(view.getChildAt(it)) }else emptyList()
    }
    private fun assertVisible(a: Activity,s: String) {
        var values=listOf<String>(); instrumentation.runOnMainSync { values=texts(a.window.decorView) }
        assertTrue("未找到界面文字 $s：$values", values.any { it.contains(s) })
    }
    private fun capture(name: String) {
        val bitmap=instrumentation.uiAutomation.takeScreenshot()?:return
        val folder=File(instrumentation.targetContext.filesDir,"screenshots").apply { mkdirs() }
        File(folder,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
    }
    @Test fun testMainQueryEditorStatisticsAndBackupScreens() {
        val screens=listOf(
            Triple(MainActivity::class.java,"重点服务推荐","01-home"),
            Triple(QueryActivity::class.java,"请选择纳税年度","02-query"),
            Triple(IncomeListActivity::class.java,"收入合计","03-records"),
            Triple(EditActivity::class.java,"保存明细","04-editor"),
            Triple(StatsActivity::class.java,"月度收入趋势","05-statistics"),
            Triple(BackupActivity::class.java,"备份为 JSON","06-backup")
        )
        screens.forEach { (type,expected,name) ->
            val a=open(type) { putExtra("year",Rules.currentYear()) }
            try {
                // Repository loading is asynchronous, wait for the expected view rather than sleeping a fixed delay.
                val deadline=System.currentTimeMillis()+15_000
                while(true) {
                    instrumentation.waitForIdleSync()
                    var found=false; instrumentation.runOnMainSync { found=texts(a.window.decorView).any { it.contains(expected) } }
                    if(found)break
                    if(System.currentTimeMillis()>deadline)fail("界面加载超时：$expected")
                    Thread.sleep(100)
                }
                assertVisible(a,expected); capture(name)
            } finally { instrumentation.runOnMainSync { a.finish() }; instrumentation.waitForIdleSync() }
        }
    }
}

