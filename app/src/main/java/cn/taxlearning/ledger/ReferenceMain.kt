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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Reference-driven application shell. Income data remains in the existing repository. */
class MainActivity : Screen() {
    private var tab = 0
    private lateinit var scroll: ScrollView
    private lateinit var navigation: LinearLayout
    private var bottomInset=0
    private val positions = IntArray(5)
    private var revealIdentity = false
    private var employmentCount: Int? = null
    private var employmentBadge: TextView? = null
    internal var homeCardScroll=0
    internal var homeFeedTab=0
    private lateinit var sticky: FrameLayout
    private val settings by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private val scale get() = (resources.displayMetrics.widthPixels / resources.displayMetrics.density / 400f).coerceIn(.85f, 1.2f)
    internal val care get() = settings.getBoolean("care_mode", false)
    internal val largeText get() = care || resources.configuration.fontScale>1.12f
    internal fun u(value: Int) = ru(value)
    internal fun type(value: String, size: Float = 15f, color: Int = INK, bold: Boolean = false) = text(value, size * scale * if(care) 1.18f else 1f, color, bold).apply {
        includeFontPadding = false; setLineSpacing(u(-1).toFloat(), 1f)
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
        window.statusBarColor=Color.BLACK;window.navigationBarColor=Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window,false)
        if(android.os.Build.VERSION.SDK_INT>=29)window.isNavigationBarContrastEnforced=false
        window.decorView.systemUiVisibility=window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        root=column().apply { setBackgroundColor(BG) };setContentView(root)
        val stage=FrameLayout(this)
        body=column()
        scroll=ScrollView(this).apply { isFillViewport=true;isVerticalScrollBarEnabled=false;clipToPadding=false;addView(body) }
        stage.addView(scroll,FrameLayout.LayoutParams(-1,-1))
        sticky=FrameLayout(this);stage.addView(sticky,FrameLayout.LayoutParams(-1,-2,Gravity.TOP))
        navigation=row().apply { background=rounded(0xFAFFFFFF.toInt(),40);elevation=u(1).toFloat();setPadding(u(22),u(7),u(22),u(7));clipToOutline=true }
        stage.addView(navigation,FrameLayout.LayoutParams(-1,u(64),Gravity.BOTTOM).apply { setMargins(u(17),0,u(17),u(20)) })
        root.addView(stage,LinearLayout.LayoutParams(-1,0,1f))
        ViewCompat.setOnApplyWindowInsetsListener(root){view,insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bottomInset=bars.bottom;view.setPadding(0,bars.top,0,0)
            navigation.layoutParams=(navigation.layoutParams as FrameLayout.LayoutParams).apply {bottomMargin=maxOf(u(20),bars.bottom+u(2))}
            body.setPadding(0,0,0,u(106)+bottomInset);insets
        }
        ViewCompat.requestApplyInsets(root);render()
        work { employmentRepo.observeCount().collect { count ->
            employmentCount=count
            employmentBadge?.text=if(settings.getBoolean("session_active",true)) "${count}家" else "—"
        } }
    }
    override fun onSaveInstanceState(out: Bundle) { positions[tab]=scroll.scrollY;out.putInt("homeCardScroll",homeCardScroll);out.putInt("homeFeedTab",homeFeedTab);out.putInt("tab",tab);out.putIntArray("positions",positions);super.onSaveInstanceState(out) }
    internal fun select(index: Int) { if(index==tab)return;positions[tab]=scroll.scrollY;tab=index;render() }
    private fun renderNavigation() {
        navigation.background=if(tab==0)gradient(0xFFE7F8FC.toInt(),0xFFFFF4E8.toInt(),40) else rounded(0xF9FFFFFF.toInt(),40)
        navigation.removeAllViews()
        listOf("首页","待办","办&查","消息","我的").forEachIndexed { index, title ->
            val active=index==tab
            val item=column().apply { gravity=Gravity.CENTER;id=resources.getIdentifier("nav_$index","id",packageName);isSelected=active }
            item.addView(ReferenceIcon(this,listOf("home","todo","services","message","mine")[index],if(active)0xFF009CF1.toInt() else 0xFF282C31.toInt(),active),LinearLayout.LayoutParams(u(25),u(25)))
            item.addView(type(title,11.5f,if(active)0xFF009CF1.toInt() else INK).apply { gravity=Gravity.CENTER },LinearLayout.LayoutParams(-1,-2).apply { topMargin=u(4) })
            clickable(item,null) { select(index) };navigation.addView(item,LinearLayout.LayoutParams(0,-1,1f))
        }
    }
    private fun render() {
        employmentBadge=null
        body.removeAllViews();body.setPadding(0,0,0,u(106)+bottomInset);renderNavigation()
        sticky.removeAllViews();scroll.setPadding(0,0,0,0);body.background=rounded(BG,0)
        if(tab==0){sticky.addView(buildSearchBar());scroll.setPadding(0,u(46),0,0);body.background=PageTopBackground(u(148))}
        if(tab==2){val top=row().apply { background=gradient(0xFF287CF5.toInt(),0xFF72A6F9.toInt(),0);setPadding(u(13),u(28),u(13),u(16)) };top.addView(type("办&查",24f,Color.WHITE,true));top.addView(type("请选择您需要办理或查询的事项",12f,0xFFBFE0FF.toInt()),LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=u(12) });sticky.addView(top,FrameLayout.LayoutParams(-1,u(if(largeText)105 else 71)));scroll.setPadding(0,u(if(largeText)105 else 71),0,0)}
        if(tab==2)body.background=PageTopBackground(u(26))
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
        // The independent app name remains in the launcher and About page.
        val avatar=ProfileAvatar(this).apply { contentDescription="学习版模拟用户头像" }
        header.addView(avatar,FrameLayout.LayoutParams(u(120),u(120),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin=u(55) })
        clickable(avatar,null) { if(active)info("个人信息") else showDemoLogin() }
        val identity=FrameLayout(this).apply { background=gradient(0xFFFFEBD6.toInt(),0xFFEFCAA1.toInt(),14) }
        val identityTexts=column().apply { setPadding(u(26),u(23),u(16),0) }
        identityTexts.addView(type(if(active)"学习同学" else "未登录",14f,INK,true))
        val number=row().apply { setPadding(0,u(9),0,0) }
        number.addView(type(if(active)"纳税人识别号  ${if(revealIdentity)"DEMO-LEARNING-2026" else "D***************6"}" else "使用模拟身份体验学习功能",13f,0xFFCD954F.toInt()))
        if(active)number.addView(ReferenceIcon(this,"eye",0xFFCF974B.toInt(),revealIdentity).apply { id=R.id.profile_identity_toggle;contentDescription=if(revealIdentity)"隐藏模拟识别号" else "显示模拟识别号";setOnClickListener { revealIdentity=!revealIdentity;render() } },LinearLayout.LayoutParams(u(22),u(20)).apply { marginStart=u(9) })
        identityTexts.addView(number);identity.addView(identityTexts,FrameLayout.LayoutParams(-1,-1))
        val personal=type(if(active)"个人信息 ›" else "登录 / 注册 ›",13f,Color.WHITE).apply { background=gradient(0xFFEAA251.toInt(),0xFFD89241.toInt(),18);setPadding(u(12),u(5),u(9),u(5));id=R.id.profile_personal }
        identity.addView(personal,FrameLayout.LayoutParams(-2,-2,Gravity.TOP or Gravity.END).apply { topMargin=u(22) })
        clickable(personal,null) { if(active)info("个人信息") else showDemoLogin() }
        header.addView(identity,FrameLayout.LayoutParams(-1,u(102),Gravity.TOP).apply { topMargin=u(188);leftMargin=u(23);rightMargin=u(23) })
        body.addView(header,LinearLayout.LayoutParams(-1,u(284)))
        val shortcuts=row().apply { background=rounded(BG,25);setPadding(u(12),u(12),u(12),0) }
        listOf(Triple("家庭成员","4人","family"),Triple("任职受雇",employmentCount?.let { "${it}家" }?:"…","job"),Triple("银行卡","1张","bank")).forEachIndexed { i, item ->
            val tile=column().apply { gravity=Gravity.CENTER;background=rounded(Color.WHITE,12);id=listOf(R.id.profile_family,R.id.profile_employment,R.id.profile_bank)[i] }
            tile.addView(ReferenceIcon(this,item.third,listOf(0xFFFF8965.toInt(),0xFFFFC052.toInt(),0xFF08D6BB.toInt())[i]),LinearLayout.LayoutParams(u(50),u(50)))
            tile.addView(type(item.first,15f,INK,true),LinearLayout.LayoutParams(-2,-2).apply { topMargin=u(1) })
            tile.addView(type(if(active)item.second else "—",12f,Color.WHITE).apply { if(i==1) { employmentBadge=this;id=R.id.profile_employment_count }; background=rounded(0xFF458DF8.toInt(),12);setPadding(u(9),u(2),u(9),u(2));gravity=Gravity.CENTER },LinearLayout.LayoutParams(-2,-2).apply { topMargin=u(12) })
            clickable(tile,null) { if(active)info(listOf("家庭成员信息","任职受雇信息","银行卡信息")[i]) else showDemoLogin() }
            shortcuts.addView(tile,LinearLayout.LayoutParams(0,u(if(largeText)140 else 118),1f).apply { if(i<2)marginEnd=u(12) })
        }
        body.addView(shortcuts,LinearLayout.LayoutParams(-1,-2).apply { topMargin=-u(18) })
        val menus=column().apply { background=rounded();setPadding(u(19),u(8),u(19),u(8)) }
        val titles=listOf(if(care)"切换标准版" else "切换关怀版","安全中心","帮助中心","我要咨询","关于&更新")
        titles.forEachIndexed { index, title ->
            val line=row().apply { id=listOf(R.id.profile_care,R.id.profile_security,R.id.profile_help,R.id.profile_consult,R.id.profile_about)[index];minimumHeight=u(if(largeText)56 else 46) }
            line.addView(ReferenceIcon(this,listOf("care","security","help","consult","about")[index],0xFF242C32.toInt()),LinearLayout.LayoutParams(u(23),u(23)).apply { marginEnd=u(13) })
            line.addView(type(title,15f),LinearLayout.LayoutParams(0,-2,1f));line.addView(ChevronView(this,0xFF22262B.toInt()),LinearLayout.LayoutParams(u(12),u(18)))
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
    override fun onDraw(c: Canvas) {
        // The reference background is 1125 × 1464; preserve its aspect and top alignment.
        ReferenceAssets.draw(context,c,"profile_background.png",RectF(0f,0f,width.toFloat(),width*1464f/1125f))
    }
}

internal class ProfileAvatar(context: android.content.Context) : View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val size=minOf(width,height).toFloat()
        paint.shader=LinearGradient(0f,0f,size,size,intArrayOf(0xFF86AEFF.toInt(),0xFF66D2F1.toInt()),null,Shader.TileMode.CLAMP)
        c.drawCircle(width/2f,height/2f,size/2f,paint)
        val inset=size*.08f
        ReferenceAssets.draw(context,c,"avatar_man.png",RectF(inset,inset,width-inset,height-inset))
    }
}
