package cn.taxlearning.ledger

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.roundToInt

/** Reference-driven application shell. Income data remains in the existing repository. */
class MainActivity : Screen() {
    private var tab = 0
    private lateinit var scroll: ScrollView
    private lateinit var navigation: LinearLayout
    private val positions = IntArray(5)
    private var revealIdentity = false
    internal var homeCardScroll=0
    internal var homeFeedTab=0
    private lateinit var sticky: FrameLayout
    private val settings by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private val scale get() = (resources.displayMetrics.widthPixels / resources.displayMetrics.density / 400f).coerceIn(.85f, 1.2f)
    private val care get() = settings.getBoolean("care_mode", false)
    internal fun u(value: Int) = dp((value * scale).roundToInt())
    internal fun type(value: String, size: Float = 15f, color: Int = INK, bold: Boolean = false) = text(value, size * scale * if(care) 1.18f else 1f, color, bold).apply {
        includeFontPadding = false; setLineSpacing(u(3).toFloat(), 1f)
    }
    internal fun rounded(color: Int = Color.WHITE, radius: Int = 14) = GradientDrawable().apply { setColor(color); cornerRadius=u(radius).toFloat() }
    internal fun gradient(a: Int, b: Int, radius: Int = 14) = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(a,b)).apply { cornerRadius=u(radius).toFloat() }
    internal fun clickable(view: View, description: String? = null, block: () -> Unit) {
        view.isClickable=true; view.isFocusable=true; if(description!=null)view.contentDescription=description
        val attrs=obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground));view.foreground=attrs.getDrawable(0);attrs.recycle()
        view.setOnClickListener { block() }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        tab=state?.getInt("tab")?.coerceIn(0,4)?:0
        state?.getIntArray("positions")?.copyInto(positions)
        homeCardScroll=state?.getInt("homeCardScroll")?:0;homeFeedTab=state?.getInt("homeFeedTab")?:0
        window.statusBarColor=Color.BLACK;window.navigationBarColor=BG
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        root=column().apply { setBackgroundColor(BG) };setContentView(root)
        val stage=FrameLayout(this)
        body=column()
        scroll=ScrollView(this).apply { isFillViewport=true;isVerticalScrollBarEnabled=false;clipToPadding=false;addView(body) }
        stage.addView(scroll,FrameLayout.LayoutParams(-1,-1))
        sticky=FrameLayout(this);stage.addView(sticky,FrameLayout.LayoutParams(-1,-2,Gravity.TOP))
        navigation=row().apply { background=rounded(0xFAFFFFFF.toInt(),40);elevation=u(3).toFloat();setPadding(u(16),u(7),u(16),u(7));clipToOutline=true }
        stage.addView(navigation,FrameLayout.LayoutParams(-1,u(68),Gravity.BOTTOM).apply { setMargins(u(17),0,u(17),u(15)) })
        root.addView(stage,LinearLayout.LayoutParams(-1,0,1f));render()
    }
    override fun onSaveInstanceState(out: Bundle) { positions[tab]=scroll.scrollY;out.putInt("homeCardScroll",homeCardScroll);out.putInt("homeFeedTab",homeFeedTab);out.putInt("tab",tab);out.putIntArray("positions",positions);super.onSaveInstanceState(out) }
    internal fun select(index: Int) { if(index==tab)return;positions[tab]=scroll.scrollY;tab=index;render() }
    private fun renderNavigation() {
        navigation.removeAllViews()
        listOf("首页","待办","办&查","消息","我的").forEachIndexed { index, title ->
            val active=index==tab
            val item=column().apply { gravity=Gravity.CENTER;id=resources.getIdentifier("nav_$index","id",packageName);isSelected=active }
            item.addView(ReferenceIcon(this,listOf("home","todo","services","message","mine")[index],if(active)ACCENT else 0xFF282C31.toInt(),active),LinearLayout.LayoutParams(u(27),u(27)))
            item.addView(type(title,12f,if(active)ACCENT else INK).apply { gravity=Gravity.CENTER },LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(4) })
            clickable(item,null) { select(index) };navigation.addView(item,LinearLayout.LayoutParams(0,-1,1f))
        }
    }
    private fun render() {
        body.removeAllViews();body.setPadding(0,0,0,u(106));renderNavigation()
        sticky.removeAllViews();scroll.setPadding(0,0,0,0);body.background=rounded(BG,0)
        if(tab==0){sticky.addView(buildSearchBar());scroll.setPadding(0,u(46),0,0);body.background=PageTopBackground(u(148))}
        if(tab==2){val top=row().apply { background=gradient(0xFF287CF5.toInt(),0xFF72A6F9.toInt(),0);setPadding(u(13),u(28),u(13),u(16)) };top.addView(type("办&查",26f,Color.WHITE,true));top.addView(type("请选择您需要办理或查询的事项",12f,0xFFBFE0FF.toInt()),LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=u(12) });sticky.addView(top,FrameLayout.LayoutParams(-1,u(73)));scroll.setPadding(0,u(73),0,0)}
        when(tab) { 0 -> renderHome();1 -> renderTodo();2 -> renderServices();3 -> renderMessages();else -> renderProfile() }
        scroll.post { scroll.scrollTo(0,positions[tab]) }
    }
    internal fun paneTitle(title: String, subtitle: String) {
        val header=row().apply { background=gradient(0xFF287CF5.toInt(),0xFF72A6F9.toInt(),0);setPadding(u(14),u(27),u(14),u(27)) }
        header.addView(type(title,26f,Color.WHITE,true));header.addView(type(subtitle,13f,0xFFCEE4FF.toInt()),LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=u(12) });body.addView(header)
    }
    private fun renderProfile() {
        val active=settings.getBoolean("session_active",true)
        val header=FrameLayout(this)
        header.addView(ProfileBackdrop(this),FrameLayout.LayoutParams(-1,-1))
        header.addView(type("税知账 · 学习版",10f,0xFFD9ECFF.toInt()),FrameLayout.LayoutParams(-2,-2,Gravity.END).apply { topMargin=u(12);rightMargin=u(14) })
        val avatar=ProfileAvatar(this).apply { contentDescription="学习版自定义人物头像" }
        header.addView(avatar,FrameLayout.LayoutParams(u(120),u(120),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin=u(55) })
        clickable(avatar,null) { if(active)info("个人信息") else showDemoLogin() }
        val identity=FrameLayout(this).apply { background=gradient(0xFFFFEBD6.toInt(),0xFFEFCAA1.toInt(),14) }
        val identityTexts=column().apply { setPadding(u(26),u(25),u(16),0) }
        identityTexts.addView(type(if(active)"学习同学" else "未登录",15f,INK,true))
        val number=row().apply { setPadding(0,u(10),0,0) }
        number.addView(type(if(active)"纳税人识别号  ${if(revealIdentity)"DEMO-LEARNING-2026" else "D***************6"}" else "使用模拟身份体验学习功能",13f,0xFFC28C47.toInt()))
        if(active)number.addView(ReferenceIcon(this,"eye",0xFFCF974B.toInt(),revealIdentity).apply { id=R.id.profile_identity_toggle;contentDescription=if(revealIdentity)"隐藏模拟识别号" else "显示模拟识别号";setOnClickListener { revealIdentity=!revealIdentity;render() } },LinearLayout.LayoutParams(u(28),u(24)).apply { marginStart=u(9) })
        identityTexts.addView(number);identity.addView(identityTexts,FrameLayout.LayoutParams(-1,-1))
        val personal=type(if(active)"个人信息 ›" else "登录 / 注册 ›",13f,Color.WHITE).apply { background=gradient(0xFFEAA251.toInt(),0xFFD89241.toInt(),18);setPadding(u(12),u(5),u(9),u(5));id=R.id.profile_personal }
        identity.addView(personal,FrameLayout.LayoutParams(-2,-2,Gravity.TOP or Gravity.END).apply { topMargin=u(22) })
        clickable(personal,null) { if(active)info("个人信息") else showDemoLogin() }
        header.addView(identity,FrameLayout.LayoutParams(-1,u(102),Gravity.TOP).apply { topMargin=u(188);leftMargin=u(23);rightMargin=u(23) })
        body.addView(header,LinearLayout.LayoutParams(-1,u(284)))
        val shortcuts=row().apply { background=rounded(Color.WHITE,25);setPadding(u(12),u(12),u(12),u(12)) }
        listOf(Triple("家庭成员","4人","family"),Triple("任职受雇","3家","job"),Triple("银行卡","1张","bank")).forEachIndexed { i, item ->
            val tile=column().apply { gravity=Gravity.CENTER;background=rounded(0xFFFCFDFF.toInt(),12);id=listOf(R.id.profile_family,R.id.profile_employment,R.id.profile_bank)[i] }
            tile.addView(ReferenceIcon(this,item.third,listOf(0xFFFF8965.toInt(),0xFFFFC052.toInt(),0xFF08D6BB.toInt())[i]),LinearLayout.LayoutParams(u(46),u(46)))
            tile.addView(type(item.first,15f,INK,true),LinearLayout.LayoutParams(-2,-2).apply { topMargin=u(8) })
            tile.addView(type(if(active)item.second else "—",12f,Color.WHITE).apply { background=rounded(0xFF458DF8.toInt(),12);setPadding(u(9),u(2),u(9),u(2));gravity=Gravity.CENTER },LinearLayout.LayoutParams(-2,-2).apply { topMargin=u(12) })
            clickable(tile,null) { if(active)info(listOf("家庭成员信息","任职受雇信息","银行卡信息")[i]) else showDemoLogin() }
            shortcuts.addView(tile,LinearLayout.LayoutParams(0,u(if(care)140 else 118),1f).apply { if(i<2)marginEnd=u(12) })
        }
        body.addView(shortcuts,LinearLayout.LayoutParams(-1,-2).apply { topMargin=-u(18) })
        val menus=column().apply { background=rounded();setPadding(u(19),u(7),u(19),u(7)) }
        val titles=listOf(if(care)"切换标准版" else "切换关怀版","安全中心","帮助中心","我要咨询","关于&更新")
        titles.forEachIndexed { index, title ->
            val line=row().apply { id=listOf(R.id.profile_care,R.id.profile_security,R.id.profile_help,R.id.profile_consult,R.id.profile_about)[index];minimumHeight=u(if(care)56 else 48) }
            line.addView(ReferenceIcon(this,listOf("care","security","help","consult","about")[index],0xFF242C32.toInt()),LinearLayout.LayoutParams(u(26),u(26)).apply { marginEnd=u(15) })
            line.addView(type(title,15f),LinearLayout.LayoutParams(0,-2,1f));line.addView(type("›",28f,INK))
            clickable(line,null) { if(index==0){settings.edit().putBoolean("care_mode",!care).apply();render()}else info(titles[index]) };menus.addView(line)
        }
        body.addView(menus,LinearLayout.LayoutParams(-1,-2).apply { setMargins(u(13),u(13),u(13),0) })
        val logout=type(if(active)"退出登录" else "使用模拟身份登录",17f,0xFF2686D7.toInt(),true).apply { id=R.id.profile_logout;gravity=Gravity.CENTER;background=rounded() }
        clickable(logout,null) { if(!active)showDemoLogin() else MaterialAlertDialogBuilder(this).setTitle("退出登录").setMessage("确认退出当前模拟身份？本机收入记录会保留。")
            .setNegativeButton("取消",null).setPositiveButton("确认退出") { _,_ -> settings.edit().putBoolean("session_active",false).apply();revealIdentity=false;render() }.show() }
        body.addView(logout,LinearLayout.LayoutParams(-1,u(56)).apply { setMargins(u(13),u(13),u(13),0) })
    }
    private fun showDemoLogin() { MaterialAlertDialogBuilder(this).setTitle("学习版模拟登录").setMessage("使用虚构的学习身份，无需注册或输入真实个人资料。")
        .setNegativeButton("取消",null).setPositiveButton("使用模拟身份") { _,_ -> settings.edit().putBoolean("session_active",true).apply();render() }.show() }
    private fun renderHome() { buildReferenceHome() }
    private fun renderTodo() {
        paneTitle("待办","及时查看待办事项")
        val empty=column().apply { gravity=Gravity.CENTER;setPadding(u(20),u(90),u(20),u(40)) }
        empty.addView(ReferenceIcon(this,"todo",0xFFB8CFEA.toInt()),LinearLayout.LayoutParams(u(78),u(78)));empty.addView(type("暂无待办事项",17f,MUTED),LinearLayout.LayoutParams(-2,-2).apply { topMargin=u(20) });body.addView(empty)
    }
    private fun renderServices() { buildReferenceServices() }
    private fun renderMessages() {
        paneTitle("消息","服务消息与通知")
        val content=column().apply { setPadding(u(14),u(20),u(14),0) };body.addView(content)
        card(content) { item("欢迎使用税知账学习版","本机消息 · 点击查看",parent=this) { info("欢迎使用") };item("数据备份提醒","导出备份，妥善保存模拟记录",R.drawable.ic_folder,this) { go(BackupActivity::class.java) } }
    }
}

internal class ProfileBackdrop(context: android.content.Context) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val w=width.toFloat();val h=height.toFloat()
        p.shader=LinearGradient(0f,0f,w,h,intArrayOf(0xFF0039FC.toInt(),0xFF0077F7.toInt(),0xFF02BEEB.toInt()),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p)
        p.shader=RadialGradient(w*.94f,h*.02f,w*.68f,intArrayOf(0xFF9BC9FF.toInt(),0x000073FF),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p)
        val wave=Path().apply { moveTo(0f,h*.79f);cubicTo(w*.3f,h*.35f,w*.58f,h*.46f,w,h*.32f);lineTo(w,h*.39f);cubicTo(w*.45f,h*.45f,w*.25f,h*.66f,0f,h*.87f);close() }
        p.shader=LinearGradient(0f,h,w,0f,intArrayOf(0x0000C7E9,0x654AD9EF,0x004CCEEB),null,Shader.TileMode.CLAMP);c.drawPath(wave,p);p.shader=null
    }
}

/** Original local vector portrait, independent of the official app's default avatar. */
internal class ProfileAvatar(context: android.content.Context) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        c.save();c.scale(width/120f,height/120f)
        fun fill(color: Int) { p.color=color;p.shader=null;p.style=Paint.Style.FILL }
        fill(0xFF7DAAFA.toInt());c.drawCircle(60f,60f,59f,p);fill(0xFFD8F1FF.toInt());c.drawCircle(60f,60f,50f,p)
        val clip=Path().apply { addCircle(60f,60f,49f,Path.Direction.CW) };c.save();c.clipPath(clip)
        fill(0xFF6C91D6.toInt());c.drawOval(27f,89f,94f,139f,p);fill(0xFFF4B6A6.toInt());c.drawRoundRect(50f,75f,70f,97f,8f,8f,p)
        fill(0xFFFFFFFF.toInt());c.drawPath(Path().apply { moveTo(37f,92f);lineTo(50f,86f);lineTo(60f,99f);lineTo(70f,86f);lineTo(83f,92f);lineTo(78f,121f);lineTo(42f,121f);close() },p)
        fill(0xFFB5D8F1.toInt());c.drawPath(Path().apply { moveTo(51f,89f);lineTo(59f,99f);lineTo(52f,104f);lineTo(45f,93f);close() },p)
        fill(0xFFF0B0A1.toInt());c.drawOval(33f,53f,44f,72f,p);c.drawOval(78f,53f,88f,72f,p)
        p.shader=LinearGradient(38f,36f,81f,81f,intArrayOf(0xFFFFD2B8.toInt(),0xFFF5B4A0.toInt()),null,Shader.TileMode.CLAMP);c.drawRoundRect(39f,32f,82f,86f,23f,23f,p);p.shader=null
        fill(0xFF39264D.toInt());c.drawPath(Path().apply { moveTo(39f,58f);cubicTo(21f,28f,55f,6f,75f,24f);cubicTo(96f,27f,86f,56f,80f,61f);lineTo(77f,42f);cubicTo(68f,47f,54f,43f,47f,36f);lineTo(43f,61f);close() },p)
        fill(0xFF4A2D5B.toInt());c.drawPath(Path().apply { moveTo(35f,38f);cubicTo(44f,18f,66f,19f,76f,28f);cubicTo(58f,28f,62f,42f,37f,46f);close() },p)
        fill(0xFF392E3A.toInt());c.drawRoundRect(46f,56f,50f,63f,2f,2f,p);c.drawRoundRect(69f,56f,73f,63f,2f,2f,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=2.3f;p.strokeCap=Paint.Cap.ROUND;c.drawLine(45f,51f,51f,50f,p);c.drawLine(68f,50f,74f,51f,p)
        p.style=Paint.Style.FILL;fill(0xFFE99483.toInt());c.drawOval(56f,64f,63f,68f,p);fill(0xFFBF6E63.toInt());c.drawArc(50f,65f,70f,78f,0f,180f,false,p);fill(Color.WHITE);c.drawArc(52f,66f,68f,74f,0f,180f,false,p)
        c.restore();c.restore()
    }
}
