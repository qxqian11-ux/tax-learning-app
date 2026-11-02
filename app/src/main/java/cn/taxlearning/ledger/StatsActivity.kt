package cn.taxlearning.ledger

import android.os.Bundle
import android.widget.LinearLayout
import android.view.View
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class StatsActivity : Screen() {
    private var year=Rules.currentYear()
    private var job: Job?=null
    private lateinit var charts: LinearLayout
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); year=state?.getInt("year")?:intent.getIntExtra("year",year)
        setup("年度收入统计"); banner("统计仅用于学习演示，不代表真实税务计算")
        val years=Rules.years()
        spinner(body,"纳税年度",years.map { "$it 年" },years.indexOf(year)) { year=years[it]; observe() }
        space(body); charts=column(); body.addView(charts); observe()
    }
    override fun onSaveInstanceState(out: Bundle) { out.putInt("year",year); super.onSaveInstanceState(out) }
    private fun observe() {
        if(!::charts.isInitialized)return
        job?.cancel()
        // The flow stays attached to this screen and recalculates on every database change.
        job=lifecycleScope.launch {
            try { repo.observe(year).collect { rows ->
                charts.removeAllViews(); totals(charts,year,rows)
                label("月度收入趋势",charts)
                card(charts) {
                    val max=(1..12).maxOf { month -> rows.filter { it.month==month }.sumOf { it.incomeCents } }.coerceAtLeast(1)
                    (1..12).forEach { month -> bar(this,"${month}月",rows.filter { it.month==month }.sumOf { it.incomeCents },max) }
                }
                label("所得项目分布",charts)
                card(charts) {
                    val max=Rules.categories.maxOf { c -> rows.filter { it.category==c }.sumOf { it.incomeCents } }.coerceAtLeast(1)
                    Rules.categories.forEach { c -> bar(this,c,rows.filter { it.category==c }.sumOf { it.incomeCents },max) }
                }
                if(rows.isEmpty()) charts.addView(text("该年度暂无数据。新增明细后统计会自动更新。",13f,MUTED))
            } } catch(e: kotlinx.coroutines.CancellationException) { throw e } catch(e: Exception) { error(e) }
        }
    }
    private fun bar(parent: LinearLayout,title: String,amount: Long,max: Long) {
        parent.addView(text("$title   ¥ ${Rules.money(amount)}",13f)); space(parent,5)
        val track=row().apply { background=shape(PALE,4); contentDescription="$title 收入 ${Rules.money(amount)} 元" }
        val fraction=(amount.toDouble()/max).toFloat()
        if(fraction>0)track.addView(View(this).apply { background=shape(RED,4) },LinearLayout.LayoutParams(0,dp(7),fraction.coerceAtLeast(.005f)))
        if(fraction<1)track.addView(View(this),LinearLayout.LayoutParams(0,dp(7),1-fraction))
        parent.addView(track,LinearLayout.LayoutParams(-1,dp(7))); space(parent,14)
    }
}
