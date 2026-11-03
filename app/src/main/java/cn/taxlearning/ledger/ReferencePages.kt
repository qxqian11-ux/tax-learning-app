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
    section.addView(HomeNoticeView(this),LinearLayout.LayoutParams(-1,u(45)).apply {topMargin=u(12);bottomMargin=u(11)})
    val hero=FrameLayout(this).apply {id=R.id.home_hero;background=gradient(0xFFE9F8FF.toInt(),0xFFCEE5FF.toInt(),9);clipToOutline=true}
    hero.addView(ServiceArtwork(this,"hero"),FrameLayout.LayoutParams(-1,-1))
    val topic=type("进入专题页",14f,Color.TRANSPARENT).apply {id=R.id.home_hero_topic;gravity=Gravity.CENTER;background=HomeBitmapDrawable(this@buildReferenceHome,"home_topic_tab.png")}
    hero.addView(topic,FrameLayout.LayoutParams(u(133),u(54),Gravity.TOP or Gravity.END).apply {topMargin=-u(10);rightMargin=-u(13)});clickable(topic,null){info("综合所得年度汇算")}
    val heroText=column().apply {setPadding(u(17),u(55),u(12),0)}
    heroText.addView(type("${Rules.currentYear()-1}综合所得年度汇算",18.5f,INK,true))
    heroText.addView(type("模拟提交申报，无需缴税或退税",16f),LinearLayout.LayoutParams(-1,-2).apply {topMargin=u(12)})
    val record=type("查看记录",17f,Color.WHITE).apply {id=R.id.home_hero_record;gravity=Gravity.CENTER;background=gradient(0xFF247EEF.toInt(),0xFF4B96FF.toInt(),26)}
    heroText.addView(record,LinearLayout.LayoutParams(u(167),u(47)).apply {topMargin=u(20)});clickable(record,null){go(IncomeListActivity::class.java,Rules.currentYear()-1)}
    hero.addView(heroText,FrameLayout.LayoutParams(-1,-1));section.addView(hero,LinearLayout.LayoutParams(-1,u(if(largeText)280 else 208)))
    section.addView(type("重点服务推荐",18f,INK,true),LinearLayout.LayoutParams(-1,-2).apply {topMargin=u(47);bottomMargin=u(16)})
    val deduction=FrameLayout(this).apply {id=R.id.home_deduction;background=HomeBitmapDrawable(this@buildReferenceHome,"home_deduction_surface.png",android.graphics.Rect(36,33,1089,510))}
    val label=row();label.addView(ReferenceIcon(this,"flame",0xFFFF4B40.toInt()),LinearLayout.LayoutParams(u(24),u(24)).apply {marginEnd=u(4)});label.addView(type("专项附加扣除",16f,INK,true))
    deduction.addView(label,FrameLayout.LayoutParams(-2,-2).apply {leftMargin=u(13);topMargin=u(12)})
    val policy=row().apply {gravity=Gravity.CENTER;addView(type("相关政策",15f,Color.WHITE,true));addView(ChevronView(this@buildReferenceHome,Color.WHITE),LinearLayout.LayoutParams(u(10),u(16)).apply {marginStart=u(5)})}
    deduction.addView(policy,FrameLayout.LayoutParams(u(119),u(39),Gravity.TOP or Gravity.END));clickable(policy,null){info("专项附加扣除")}
    val filled=type("模拟填报 3 项专项附加扣除",17f,INK,true).apply {gravity=Gravity.CENTER;text=android.text.SpannableString(text).apply {setSpan(android.text.style.RelativeSizeSpan(1.6f),5,6,0);setSpan(android.text.style.ForegroundColorSpan(0xFFFFB54F.toInt()),5,6,0)}}
    deduction.addView(filled,FrameLayout.LayoutParams(-1,u(if(largeText)64 else 34)).apply {topMargin=u(63)})
    val actions=FrameLayout(this).apply {clipChildren=false;clipToPadding=false}
    val fill=HomeRibbonLabel(this,"填报",true).apply {id=R.id.home_deduction_fill;gravity=Gravity.CENTER;setLayerType(View.LAYER_TYPE_SOFTWARE,null);background=HomeRibbonDrawable(true,u(3).toFloat())}
    val lookup=HomeRibbonLabel(this,"我要查询",false).apply {id=R.id.home_deduction_lookup;gravity=Gravity.CENTER;setLayerType(View.LAYER_TYPE_SOFTWARE,null);background=HomeRibbonDrawable(false,u(3).toFloat());setPadding(u(9),0,0,0)}
    actions.addView(fill,FrameLayout.LayoutParams(u(138),-1));actions.addView(lookup,FrameLayout.LayoutParams(u(180),-1,Gravity.END))
    clickable(fill,null){info("专项附加扣除填报")};clickable(lookup,null){info("专项附加扣除查询")}
    deduction.addView(actions,FrameLayout.LayoutParams(-1,u(52),Gravity.BOTTOM).apply {leftMargin=u(39);rightMargin=u(39);bottomMargin=u(15)})
    section.addView(deduction,LinearLayout.LayoutParams(-1,u(if(largeText)230 else 171)))
    val horizontal=HorizontalScrollView(this).apply {id=R.id.home_shortcuts;isHorizontalScrollBarEnabled=false;clipToPadding=false}
    val tiles=row().apply {setPadding(0,u(17),0,0)}
    val entries=listOf(Service("综合所得年度汇算","申报与查询综合所得年度汇算",blue),Service("收入纳税明细","查看个人所得税纳税明细",purple),Service("纳税记录开具","生成或查看纳税记录",green),Service("专项附加扣除","填报与查询7项专项附加扣除信息",amber),Service("更多功能","设置在首页展示的常用功能入口",blue))
    entries.forEachIndexed {index,item ->
        val frame=FrameLayout(this)
        val card=column().apply {background=RaisedCardDrawable(Color.WHITE,u(15).toFloat());setPadding(u(9),u(16),u(8),u(12))}
        card.addView(type(item.title,14f));card.addView(type(item.subtitle,11f,0xFF999B9D.toInt()),LinearLayout.LayoutParams(-1,-2).apply {topMargin=u(6)})
        frame.addView(card,FrameLayout.LayoutParams(-1,-1).apply {topMargin=u(28)})
        frame.addView(ReferenceIcon(this,serviceIcon(item.title),item.color),FrameLayout.LayoutParams(u(39),u(39)).apply {leftMargin=u(7)})
        val tagColors=listOf(0xFF5CA5E8.toInt(),0xFFA47CDD.toInt(),0xFF47C7B4.toInt(),0xFFE7A330.toInt(),0xFF5CA5E8.toInt())
        val tagFills=listOf(0xFFB8DDF9.toInt(),0xFFE4D7FB.toInt(),0xFFA7F0E7.toInt(),0xFFFFE9C0.toInt(),0xFFB8DDF9.toInt())
        val tag=type(listOf("去申报","去查询","去开具","去填报","去使用")[index],13f,tagColors[index],true).apply {background=HomeCornerTagDrawable(tagFills[index]);gravity=Gravity.CENTER;setPadding(u(6),0,u(2),0)}
        frame.addView(tag,FrameLayout.LayoutParams(u(60),u(24),Gravity.BOTTOM or Gravity.END));clickable(frame,null){openService(item.action)}
        tiles.addView(frame,LinearLayout.LayoutParams(u(if(largeText)145 else 108),u(if(largeText)210 else 153)).apply {if(index<entries.lastIndex)marginEnd=u(8)})
    }
    horizontal.addView(tiles);section.addView(horizontal,LinearLayout.LayoutParams(-1,-2));horizontal.post {horizontal.scrollTo(homeCardScroll,0)};horizontal.setOnScrollChangeListener {_,x,_,_,_ -> homeCardScroll=x}
    val promo=FrameLayout(this).apply {background=gradient(0xFF52CEF0.toInt(),0xFF08BDE9.toInt(),9);clipToOutline=true}
    promo.addView(ServiceArtwork(this,"promo"),FrameLayout.LayoutParams(-1,-1))
    val promoText=column().apply {setPadding(u(18),u(21),u(8),u(12))};promoText.addView(type("经营所得申报功能介绍",20f,0xFF055C9B.toInt(),true));promoText.addView(type("业务详情  快速了解",14f,0xFF237DA8.toInt(),true),LinearLayout.LayoutParams(-2,-2).apply {topMargin=u(12)});promoText.alpha=0f;promo.addView(promoText)
    clickable(promo,null){info("经营所得申报")};section.addView(promo,LinearLayout.LayoutParams(-1,u(96)).apply {topMargin=u(27);bottomMargin=u(16)})
    val feedTabs=row().apply {id=R.id.home_news_tabs};val articles=column()
    fun fillArticles(){
        feedTabs.removeAllViews();articles.removeAllViews()
        listOf("警示案例","通知公告","热点问题","政策解读").forEachIndexed {index,s ->
            val t=type(s,if(largeText)14f else 16f,if(index==homeFeedTab)0xFF008CE2.toInt() else 0xFF55575B.toInt(),index==homeFeedTab).apply {gravity=Gravity.CENTER}
            feedTabs.addView(t,LinearLayout.LayoutParams(0,u(if(largeText)50 else 35),1f));clickable(t,null){homeFeedTab=index;fillArticles()}
        }
        val titles=when(homeFeedTab){0->listOf("办理汇算想便捷 APP密码要记牢","获取退税想及时 准确卡号必须有","未依法办理个人所得税综合所得汇算清缴案件","未在法定期限内办理综合所得汇算清缴、虚假填报子女教育专项附加扣除案件");1->listOf("学习版使用说明","模拟记录备份与恢复提醒");2->listOf("如何查询收入纳税明细？","如何备份本机收入数据？");else->listOf("综合所得年度汇算学习","专项附加扣除学习介绍")}
        titles.forEachIndexed {index,titleText ->
            val line=FrameLayout(this).apply {background=rounded(Color.WHITE,19)}
            val art=ServiceArtwork(this,"article$index")
            line.addView(art,FrameLayout.LayoutParams(u(154),u(98),if(index<2)Gravity.END else Gravity.START).apply {topMargin=u(10)})
            val copy=column().apply {setPadding(if(index<2)u(17) else u(12),0,if(index<2)u(13) else u(10),0)}
            copy.addView(type(titleText,14f));line.addView(copy,FrameLayout.LayoutParams(-1,-2).apply {topMargin=u(20);if(index<2)rightMargin=u(154) else leftMargin=u(154)})
            val date=type(if(index<2)"2025-02-28" else "2024-03-15",13f,0xFFAAAAAA.toInt()).apply {setCompoundDrawablesWithIntrinsicBounds(HomeClockDrawable(u(12)),null,null,null);compoundDrawablePadding=u(4)}
            line.addView(date,FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM).apply {leftMargin=u(if(index<2)17 else 166);bottomMargin=u(19)})
            if(largeText){art.alpha=.28f;copy.layoutParams=copy.layoutParams.apply {width=-1};(copy.layoutParams as FrameLayout.LayoutParams).apply {leftMargin=0;rightMargin=0}}
            clickable(line,null){info(titleText)};articles.addView(line,LinearLayout.LayoutParams(-1,u(if(largeText)190 else 120)).apply {topMargin=u(13)})
        }
    }
    section.addView(feedTabs);section.addView(articles);fillArticles()
    val more=type("查看更多 ›",15f,0xFFA3A7AD.toInt()).apply {gravity=Gravity.CENTER;setPadding(0,u(19),0,u(26))};clickable(more,null){info(listOf("警示案例","通知公告","热点问题","政策解读")[homeFeedTab])};section.addView(more)
}

internal fun MainActivity.buildReferenceServices() {
    val page=column().apply { setPadding(u(13),0,u(13),u(12));background=rounded(BG,25) };body.addView(page)
    fun heading(title: String) { page.addView(type(title,18f,INK,true),LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(21);bottomMargin=u(11) }) }
    fun group(items: List<Service>,parent: LinearLayout=page){
        val tax=parent!==page
        val outer=column().apply {clipChildren=false;clipToPadding=false;background=rounded();setPadding(if(tax)0 else u(12),u(if(tax)22 else 12),if(tax)0 else u(12),if(tax)0 else u(12))}
        items.chunked(3).forEachIndexed {lineIndex,chunk ->
            val line=row().apply {gravity=Gravity.TOP;clipChildren=false;clipToPadding=false}
            chunk.forEachIndexed {index,item ->
                val tile=FrameLayout(this).apply {minimumHeight=u(if(tax)124 else 135)}
                tile.addView(View(this).apply {background=if(tax)RaisedCardDrawable(0xFFF8F9FB.toInt(),u(12).toFloat()) else rounded(0xFFF8F9FB.toInt(),9)},FrameLayout.LayoutParams(-1,-1).apply {if(tax)topMargin=u(20)})
                val copy=column().apply {setPadding(u(9),0,u(8),u(8))}
                copy.addView(type(item.title,13f,INK,true));copy.addView(type(item.subtitle,11f,0xFF999B9D.toInt()),LinearLayout.LayoutParams(-1,-2).apply {topMargin=u(8)})
                tile.addView(copy,FrameLayout.LayoutParams(-1,-2).apply {topMargin=u(if(tax)40 else 54)})
                tile.addView(ReferenceIcon(this,serviceIcon(item.title),item.color),FrameLayout.LayoutParams(u(29),u(29)).apply {leftMargin=u(10);topMargin=u(if(tax)0 else 14)})
                if(item.title in listOf("经营所得申报","涉税服务人员信息管理","代办申报明细")){
                    val badge=type("NEW",10f,Color.WHITE).apply {background=rounded(0xFFFF5449.toInt(),12);gravity=Gravity.CENTER}
                    tile.addView(badge,FrameLayout.LayoutParams(u(33),u(17)).apply {leftMargin=u(29);topMargin=u(if(tax)-8 else 4)})
                }
                tile.clipChildren=false;clickable(tile,null){openService(item.action)}
                line.addView(tile,LinearLayout.LayoutParams(0,-2,1f).apply {if(index<2)marginEnd=u(7)})
            }
            repeat(3-chunk.size){line.addView(View(this),LinearLayout.LayoutParams(0,1,1f).apply {if(it<2-chunk.size)marginEnd=u(7)})}
            outer.addView(line,LinearLayout.LayoutParams(-1,-2).apply {if(lineIndex>0)topMargin=u(if(tax)13 else 8)})
            line.doOnLayout {measured -> for(index in 0 until line.childCount){val child=line.getChildAt(index);if(child.measuredHeight<measured.height)child.layoutParams=child.layoutParams.apply {height=measured.height}}}
        }
        parent.addView(outer)
    }
    heading("办税")
    val tax=column().apply { background=rounded();setPadding(u(13),u(13),u(13),u(2)) }
    listOf(Service("综合所得年度汇算","申报与查询综合所得年度汇算",blue),Service("专项附加扣除","填报与查询7项专项附加扣除信息",amber)).forEachIndexed { index,item ->
        val feature=FrameLayout(this).apply {background=rounded(if(index==0)0xFFE7F0FF.toInt() else 0xFFFFF5E5.toInt(),5);clipToOutline=true}
        feature.addView(ReferenceIcon(this,if(index==0)"annual_feature" else "deduction_feature",item.color),FrameLayout.LayoutParams(u(80),u(80),Gravity.BOTTOM or Gravity.END).apply {bottomMargin=-u(9);rightMargin=-u(5)})
        val copy=column().apply {setPadding(u(16),u(14),u(7),u(10))};copy.addView(type(item.title,18f,if(index==0)0xFF1C5D91.toInt() else 0xFF996116.toInt(),true));copy.addView(type(item.subtitle,13f,if(index==0)0xFF668FB4.toInt() else 0xFFBC965B.toInt()),LinearLayout.LayoutParams(-1,-2).apply {topMargin=u(8)})
        feature.addView(copy,FrameLayout.LayoutParams(-1,-1))
        clickable(feature,null){openService(item.action)};tax.addView(feature,LinearLayout.LayoutParams(-1,u(if(largeText)115 else 75)).apply {if(index==0)bottomMargin=u(13)})
    }
    group(listOf(Service("个人养老金扣除管理","录入或查看个人养老金扣除信息",blue),Service("经营所得申报","取得经营所得后办理申报",green),Service("纳税记录开具","生成或查看纳税记录",green,"纳税记录"),Service("公益慈善捐赠扣除填报","录入或查看准予扣除的捐赠记录",purple),Service("委托代理关系管理","委托代理机构或个人代办年度汇算",amber),Service("涉税服务人员信息管理","填报涉税服务人员信息与申领信用码",blue)),tax)
    val warning=row().apply { setPadding(u(2),u(4),0,0) };warning.addView(ServiceArtwork(this,"warning"),LinearLayout.LayoutParams(u(107),u(74)));warning.addView(type("警示案例 ›",18f,INK,true),LinearLayout.LayoutParams(0,-2,1f));clickable(warning,null) { info("警示案例") };tax.addView(warning);page.addView(tax)
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
    val bar=row().apply {id=R.id.home_search_bar;background=HomeHeaderDrawable(this@buildSearchBar,0);setPadding(u(13),u(5),u(13),u(6))}
    val field=row().apply {id=R.id.home_search_field;background=android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,intArrayOf(0x88E3E4FD.toInt(),0x88AED1FC.toInt())).apply {cornerRadius=u(9).toFloat()};setPadding(u(8),0,u(8),0)}
    field.addView(ReferenceIcon(this,"search",Color.WHITE),LinearLayout.LayoutParams(u(25),u(26)).apply {marginEnd=u(1)})
    field.addView(type("请输入想搜索的功能/服务",16f,0xFFF3F7FF.toInt()).apply {isSingleLine=true})
    bar.addView(field,LinearLayout.LayoutParams(0,u(35),1f));clickable(field,null){showServiceSearch()}
    val scan=ReferenceIcon(this,"scan",Color.WHITE).apply {id=R.id.home_search_scan}
    bar.addView(scan,LinearLayout.LayoutParams(u(28),u(26)).apply {marginStart=u(12);marginEnd=u(4)})
    clickable(scan,"扫一扫"){info("扫一扫")};return bar
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
private fun serviceIcon(title: String) = when(title) {
    "综合所得年度汇算" -> "annual"
    "收入纳税明细" -> "income"
    "纳税记录开具" -> "record"
    "专项附加扣除" -> "deduction"
    "更多功能" -> "more"
    "个人养老金扣除管理" -> "pension"
    "经营所得申报" -> "business"
    "公益慈善捐赠扣除填报" -> "donation"
    "委托代理关系管理" -> "agency"
    "涉税服务人员信息管理" -> "personnel"
    "申报记录" -> "lookup"
    "异议申诉" -> "appeal"
    "税务文书" -> "tax_document"
    "涉税专业服务机构" -> "institution"
    "备案信息" -> "filing"
    "票证查验" -> "voucher"
    "代办申报明细" -> "agent_declaration"
    "欠税信息" -> "arrears"
    "扫码开票" -> "invoice"
    "发票抬头" -> "invoice_title"
    "我的票夹" -> "invoice_wallet"
    "开票申请记录" -> "invoice_application"
    "热点问题" -> "hot"
    "政策解读" -> "policy"
    "办税指南" -> "guide"
    else -> "document"
}
