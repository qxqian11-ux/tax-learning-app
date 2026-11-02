package cn.taxlearning.ledger

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.core.view.doOnLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder

private data class Service(val title: String, val subtitle: String, val color: Int, val action: String=title)
private val blue=0xFF078FFF.toInt()
private val purple=0xFF9957F8.toInt()
private val green=0xFF00CCB7.toInt()
private val amber=0xFFFFAF10.toInt()

internal fun MainActivity.buildReferenceHome() {
    val section=column().apply { setPadding(u(13),0,u(13),0) };body.addView(section)
    val notice=row().apply { background=rounded(0xFFFFF5DF.toInt(),10);setPadding(u(10),u(11),u(10),u(11)) }
    notice.addView(ReferenceIcon(this,"notice",amber),LinearLayout.LayoutParams(u(23),u(23)).apply { marginEnd=u(9) })
    notice.addView(type("个人所得税综合所得年度汇算学习提醒",14f,0xFF9E6A18.toInt()).apply { isSingleLine=true;ellipsize=android.text.TextUtils.TruncateAt.END },LinearLayout.LayoutParams(0,-2,1f))
    clickable(notice,null) { info("年度汇算学习提醒") };section.addView(notice,LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(14);bottomMargin=u(11) })
    val hero=FrameLayout(this).apply { background=gradient(0xFFECFAFF.toInt(),0xFFCEE5FF.toInt(),10);clipToOutline=true }
    hero.addView(ServiceArtwork(this,"hero"),FrameLayout.LayoutParams(-1,-1))
    val topic=type("进入专题页 ›",14f,Color.WHITE).apply { background=gradient(0xFFA1CAFF.toInt(),0xFF78B0F6.toInt(),10);setPadding(u(20),u(12),u(8),u(10)) }
    hero.addView(topic,FrameLayout.LayoutParams(-2,-2,Gravity.TOP or Gravity.END));clickable(topic,null) { info("综合所得年度汇算") }
    val heroText=column().apply { setPadding(u(17),u(57),u(12),0) }
    heroText.addView(type("${Rules.currentYear()-1}综合所得年度汇算",19f,INK,true))
    heroText.addView(type("模拟身份 · 查看年度收入记录",14f),LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(13) })
    val record=type("查看记录",17f,Color.WHITE,true).apply { gravity=Gravity.CENTER;background=gradient(0xFF197EEB.toInt(),0xFF4B97FF.toInt(),28) }
    heroText.addView(record,LinearLayout.LayoutParams(u(169),u(48)).apply { topMargin=u(23) });clickable(record,null) { go(IncomeListActivity::class.java,Rules.currentYear()-1) }
    hero.addView(heroText,FrameLayout.LayoutParams(-1,-1));section.addView(hero,LinearLayout.LayoutParams(-1,u(if(largeText)280 else 214)))
    section.addView(type("重点服务推荐",19f,INK,true),LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(43);bottomMargin=u(17) })
    val deduction=column().apply { background=gradient(Color.WHITE,0xFFE8F4FF.toInt(),13);setPadding(u(13),u(11),u(13),u(15)) }
    val title=row();title.addView(ReferenceIcon(this,"notice",0xFFFF534F.toInt()),LinearLayout.LayoutParams(u(21),u(21)).apply { marginEnd=u(7) });title.addView(type("专项附加扣除",16f,INK,true),LinearLayout.LayoutParams(0,-2,1f))
    val policy=type("相关政策 ›",14f,Color.WHITE).apply { background=gradient(0xFFB6DAFF.toInt(),0xFF72B3F6.toInt(),10);setPadding(u(13),u(10),u(8),u(10)) };title.addView(policy);clickable(policy,null) { info("专项附加扣除") };deduction.addView(title)
    val filled=type("模拟填报 3 项专项附加扣除",17f,INK,true).apply { gravity=Gravity.CENTER;text=android.text.SpannableString(text).apply { setSpan(android.text.style.RelativeSizeSpan(1.6f),5,6,0);setSpan(android.text.style.ForegroundColorSpan(0xFFFFB745.toInt()),5,6,0) } };deduction.addView(filled,LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(24);bottomMargin=u(17) })
    val actions=row().apply { setPadding(u(30),0,u(30),0) }
    listOf("填报","我要查询").forEachIndexed { i,s -> val action=type(s,14f,Color.WHITE,true).apply { gravity=Gravity.CENTER;background=if(i==0)gradient(0xFF26A5FC.toInt(),0xFF008FF4.toInt(),28) else gradient(0xFFFFD249.toInt(),0xFFFFAE00.toInt(),28) };actions.addView(action,LinearLayout.LayoutParams(0,u(46),1f).apply { if(i==0)marginEnd=u(4) });clickable(action,null) { info(if(i==0)"专项附加扣除填报" else "专项附加扣除查询") } };deduction.addView(actions);section.addView(deduction)
    val horizontal=HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false;clipToPadding=false }
    val tiles=row().apply { setPadding(0,u(19),0,0) }
    val entries=listOf(Service("综合所得年度汇算","申报与查询综合所得年度汇算",blue),Service("收入纳税明细","查看个人所得税纳税明细",purple),Service("纳税记录开具","生成或查看纳税记录",green),Service("专项附加扣除","填报与查询7项专项附加扣除信息",amber),Service("更多功能","设置在首页展示的常用功能入口",blue))
    entries.forEachIndexed { index,item ->
        val frame=FrameLayout(this)
        val card=column().apply { background=rounded();setPadding(u(9),u(16),u(8),u(12)) }
        card.addView(type(item.title,14f).apply { minLines=2 });card.addView(type(item.subtitle,11f,0xFF999DA3.toInt()))
        frame.addView(card,FrameLayout.LayoutParams(-1,-1).apply { topMargin=u(28) })
        frame.addView(ReferenceIcon(this,serviceIcon(item.title),item.color),FrameLayout.LayoutParams(u(34),u(34)).apply { leftMargin=u(12) })
        val tag=type(listOf("去申报","去查询","去开具","去填报","去使用")[index],13f,item.color,true).apply { background=rounded(light(item.color),12);setPadding(u(10),u(4),u(7),u(4)) }
        frame.addView(tag,FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM or Gravity.END))
        clickable(frame,null) { openService(item.action) };tiles.addView(frame,LinearLayout.LayoutParams(u(if(largeText)145 else 118),u(if(largeText)210 else 155)).apply { if(index<entries.lastIndex)marginEnd=u(8) })
    }
    horizontal.addView(tiles);section.addView(horizontal,LinearLayout.LayoutParams(-1,-2));horizontal.post { horizontal.scrollTo(homeCardScroll,0) };horizontal.setOnScrollChangeListener { _,x,_,_,_ -> homeCardScroll=x }
    val promo=FrameLayout(this).apply { background=gradient(0xFF51CDED.toInt(),0xFF08BDE9.toInt(),10);clipToOutline=true }
    promo.addView(ServiceArtwork(this,"promo"),FrameLayout.LayoutParams(-1,-1))
    val promoText=column().apply { setPadding(u(18),u(21),u(8),u(12)) };promoText.addView(type("经营所得申报功能介绍",20f,0xFF055C9B.toInt(),true));promoText.addView(type("业务详情  快速了解",14f,0xFF237DA8.toInt(),true),LinearLayout.LayoutParams(-2,-2).apply { topMargin=u(12) });promo.addView(promoText)
    clickable(promo,null) { info("经营所得申报") };section.addView(promo,LinearLayout.LayoutParams(-1,u(98)).apply { topMargin=u(27);bottomMargin=u(21) })
    val feedTabs=row();val articles=column()
    fun fillArticles() {
        feedTabs.removeAllViews();articles.removeAllViews()
        listOf("警示案例","通知公告","热点问题","政策解读").forEachIndexed { index,s -> val t=type(s,if(largeText)14f else 16f,if(index==homeFeedTab)0xFF008CE2.toInt() else INK,index==homeFeedTab).apply { gravity=Gravity.CENTER };feedTabs.addView(t,LinearLayout.LayoutParams(0,u(if(largeText)50 else 35),1f));clickable(t,null) { homeFeedTab=index;fillArticles() } }
        val titles=when(homeFeedTab) {
            0 -> listOf("办理汇算想便捷 APP密码要记牢","获取退税想及时 准确卡号必须有","未依法办理个人所得税综合所得汇算清缴案件","未在法定期限内办理综合所得汇算清缴、虚假填报子女教育专项附加扣除案件")
            1 -> listOf("学习版使用说明","模拟记录备份与恢复提醒")
            2 -> listOf("如何查询收入纳税明细？","如何备份本机收入数据？")
            else -> listOf("综合所得年度汇算学习","专项附加扣除学习介绍")
        }
        titles.forEachIndexed { index,titleText ->
            val line=row().apply { background=rounded(Color.WHITE,19);setPadding(u(17),u(14),u(10),u(12));gravity=Gravity.TOP }
            val copy=column();copy.addView(type(titleText,14f));val date=if(index<2)"◷ 2025-02-28" else "◷ 2024-03-15";copy.addView(type(date,12f,0xFFADB1B5.toInt()),LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(24) })
            val art=ServiceArtwork(this,"article$index")
            if(index>=2)line.addView(art,LinearLayout.LayoutParams(u(144),u(96)).apply { marginEnd=u(10) })
            line.addView(copy,LinearLayout.LayoutParams(0,-2,1f))
            if(index<2)line.addView(art,LinearLayout.LayoutParams(u(144),u(96)).apply { marginStart=u(9) })
            clickable(line,null) { info(titleText) };articles.addView(line,LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(13) })
        }
    }
    section.addView(feedTabs);section.addView(articles);fillArticles()
    val more=type("查看更多 ›",15f,0xFFA3A7AD.toInt()).apply { gravity=Gravity.CENTER;setPadding(0,u(19),0,u(26)) };clickable(more,null) { info(listOf("警示案例","通知公告","热点问题","政策解读")[homeFeedTab]) };section.addView(more)
}

internal fun MainActivity.buildReferenceServices() {
    val page=column().apply { setPadding(u(13),0,u(13),u(12));background=rounded(BG,25) };body.addView(page)
    fun heading(title: String) { page.addView(type(title,19f,INK,true),LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(21);bottomMargin=u(16) }) }
    fun group(items: List<Service>, parent: LinearLayout=page) {
        val outer=column().apply { background=rounded();setPadding(u(12),u(12),u(12),u(12)) }
        items.chunked(3).forEachIndexed { lineIndex,chunk ->
            val line=row().apply { gravity=Gravity.TOP }
            chunk.forEachIndexed { index,item ->
                val tile=column().apply { background=rounded(0xFFF8F9FB.toInt(),10);setPadding(u(10),u(14),u(8),u(10)) }
                tile.minimumHeight=u(139)
                tile.addView(ReferenceIcon(this,serviceIcon(item.title),item.color),LinearLayout.LayoutParams(u(29),u(29)))
                tile.addView(type(item.title,14f,INK,true),LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(14);bottomMargin=u(8) });tile.addView(type(item.subtitle,11f,0xFF999DA3.toInt()))
                clickable(tile,null) { openService(item.action) };line.addView(tile,LinearLayout.LayoutParams(0,-2,1f).apply { if(index<2)marginEnd=u(7) })
            }
            repeat(3-chunk.size){line.addView(View(this),LinearLayout.LayoutParams(0,1,1f).apply { if(it<2-chunk.size)marginEnd=u(7) })}
            outer.addView(line,LinearLayout.LayoutParams(-1,-2).apply { if(lineIndex>0)topMargin=u(8) })
            line.doOnLayout { measured -> for(index in 0 until line.childCount) { val child=line.getChildAt(index);if(child.measuredHeight<measured.height)child.layoutParams=child.layoutParams.apply { height=measured.height } } }
        }
        parent.addView(outer)
    }
    heading("办税")
    val tax=column().apply { background=rounded();setPadding(u(13),u(13),u(13),u(13)) }
    listOf(Service("综合所得年度汇算","申报与查询综合所得年度汇算",blue),Service("专项附加扣除","填报与查询7项专项附加扣除信息",amber)).forEachIndexed { index,item ->
        val feature=row().apply { background=rounded(light(item.color),5);setPadding(u(16),u(16),u(10),u(14)) };val copy=column();copy.addView(type(item.title,18f,if(index==0)0xFF1C5D91.toInt() else 0xFF996116.toInt(),true));copy.addView(type(item.subtitle,13f,if(index==0)0xFF668FB4.toInt() else 0xFFBC965B.toInt()),LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(9) });feature.addView(copy,LinearLayout.LayoutParams(0,-2,1f));feature.addView(ReferenceIcon(this,"document",item.color),LinearLayout.LayoutParams(u(44),u(44)));clickable(feature,null) { openService(item.action) };tax.addView(feature,LinearLayout.LayoutParams(-1,-2).apply { if(index==0)bottomMargin=u(13) })
    }
    group(listOf(Service("个人养老金扣除管理","录入或查看个人养老金扣除信息",blue),Service("经营所得申报","取得经营所得后办理申报",green),Service("纳税记录开具","生成或查看纳税记录",green,"纳税记录"),Service("公益慈善捐赠扣除填报","录入或查看准予扣除的捐赠记录",purple),Service("委托代理关系管理","委托代理机构或个人代办年度汇算",amber),Service("涉税服务人员信息管理","填报涉税服务人员信息与申领信用码",blue)),tax)
    val warning=row().apply { setPadding(u(6),u(4),0,0) };warning.addView(ServiceArtwork(this,"warning"),LinearLayout.LayoutParams(u(107),u(70)));warning.addView(type("警示案例 ›",18f,INK,true),LinearLayout.LayoutParams(0,-2,1f));clickable(warning,null) { info("警示案例") };tax.addView(warning);page.addView(tax)
    heading("查询")
    group(listOf(Service("收入纳税明细","查看个人所得税纳税明细",purple),Service("申报记录","查看历史申报记录",green),Service("异议申诉","查询最新申诉记录和结果",blue),Service("税务文书","查看电子税务文书信息",blue),Service("涉税专业服务机构","查询全国涉税专业服务机构信息",blue),Service("备案信息","查询备案信息",0xFF6279F7.toInt()),Service("票证查验","查验纳税记录等税收票证信息真伪",amber),Service("代办申报明细","查询月销售额达到10万元的代办申报明细数据",purple),Service("欠税信息","查询个人欠税信息",amber)))
    heading("发票管理")
    group(listOf(Service("扫码开票","扫描开票二维码提交开票信息",blue),Service("发票抬头","维护常用发票抬头信息",0xFF6279F7.toInt()),Service("我的票夹","查询和管理数电票",amber),Service("开票申请记录","查看历史代开发票申请记录",purple)))
    heading("公众服务")
    group(listOf(Service("热点问题","获取当下热点问题与解答",amber),Service("政策解读","了解最新税收政策与解读",0xFF6279F7.toInt()),Service("办税指南","为办税提供指南",blue)))
    heading("本机数据管理")
    group(listOf(Service("年度收入统计","按年度查看本机收入统计",blue),Service("数据备份与恢复","CSV 导出与 JSON 备份恢复",green)))
}

internal fun MainActivity.openService(title: String) {
    when(title) {
        "收入纳税明细" -> go(QueryActivity::class.java)
        "年度收入统计" -> go(StatsActivity::class.java,Rules.currentYear())
        "数据备份与恢复" -> go(BackupActivity::class.java)
        "更多功能" -> select(2)
        else -> info(title)
    }
}

internal fun MainActivity.buildSearchBar(): View {
    val bar=row().apply { background=gradient(0xFF287CEE.toInt(),0xFF3489F8.toInt(),0);setPadding(u(13),u(5),u(13),u(6)) }
    val field=row().apply { background=rounded(0x558ED2FF,10);setPadding(u(10),u(7),u(10),u(7)) };field.addView(ReferenceIcon(this,"search",Color.WHITE),LinearLayout.LayoutParams(u(20),u(20)).apply { marginEnd=u(7) });field.addView(type("请输入想搜索的功能/服务",15f,0xFFE6F4FF.toInt()));bar.addView(field,LinearLayout.LayoutParams(0,u(35),1f))
    clickable(field,null) { showServiceSearch() }
    val scan=ReferenceIcon(this,"scan",Color.WHITE);bar.addView(scan,LinearLayout.LayoutParams(u(25),u(25)).apply { marginStart=u(14);marginEnd=u(6) });clickable(scan,"扫一扫") { info("扫一扫") };return bar
}
private fun MainActivity.showServiceSearch() {
    val options=listOf("收入纳税明细","综合所得年度汇算","专项附加扣除","年度收入统计","数据备份与恢复","家庭成员信息","银行卡信息","帮助中心")
    val layout=column().apply { setPadding(u(20),u(10),u(20),u(10)) };val input=EditText(this).apply { hint="输入功能名称";isSingleLine=true };val results=ListView(this)
    layout.addView(input);layout.addView(results,LinearLayout.LayoutParams(-1,u(300)))
    val dialog=MaterialAlertDialogBuilder(this).setTitle("搜索功能 / 服务").setView(layout).setNegativeButton("取消",null).create()
    var filtered=options
    fun update(){filtered=options.filter { it.contains(input.text.toString().trim()) };results.adapter=ArrayAdapter(this,android.R.layout.simple_list_item_1,filtered)}
    input.addTextChangedListener { update() };results.setOnItemClickListener { _,_,index,_ -> dialog.dismiss();openService(filtered[index]) };update();dialog.show()
}
private fun light(color: Int)=Color.rgb((Color.red(color)*.12+255*.88).toInt(),(Color.green(color)*.12+255*.88).toInt(),(Color.blue(color)*.12+255*.88).toInt())
private fun serviceIcon(title: String) = when {
    title.contains("收入纳税") -> "income"
    title.contains("年度汇算") -> "annual"
    title.contains("纳税记录") -> "record"
    title.contains("扣除") -> "deduction"
    title.contains("扫码") || title.contains("票证") -> "invoice"
    title.contains("人员") || title.contains("机构") -> "personnel"
    title.contains("查询") || title.contains("申报记录") -> "lookup"
    else -> "document"
}
