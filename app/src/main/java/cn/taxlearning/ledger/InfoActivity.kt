package cn.taxlearning.ledger

import android.os.Bundle
import com.google.android.material.switchmaterial.SwitchMaterial

class InfoActivity : Screen() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); val title=intent.getStringExtra("title")?:"关于应用"
        setup(title); banner()
        card {
            addView(text(title,23f,INK,true)); space(this,20)
            val message=when(title) {
                "个人信息" -> "昵称：学习同学\n用户类型：课程演示用户\n资料来源：本地模拟\n\n本应用不提供实名认证，不填写真实身份证号、手机号或住址。"
                "家庭成员信息" -> "模拟成员：小禾、小川、小林、小晴（共 4 人）\n关系：学习示例\n\n这里只展示虚构资料，不采集家庭成员的真实信息。"
                "银行卡信息" -> "演示卡：学习银行卡\n状态：未绑定\n\n无需也无法绑定真实银行卡。不要输入真实卡号。"
                "帮助中心" -> "常见问题\n\n如何查询收入？\n从首页或办&查进入收入纳税明细，选择年度和所得类型后查询。\n\n记录会丢失吗？\n记录保存在本机，退出模拟身份不会删除记录。卸载前请导出 JSON 备份。\n\n如何备份？\n从办&查中的数据管理，或安全中心进入数据备份与恢复。"
                "我要咨询" -> "学习咨询\n\n本地模拟咨询，不发送任何个人资料。\n\n收入查询、数据维护与文件导出的操作说明可在帮助中心查看。真实税务问题请通过正式税务服务渠道咨询。"
                "欢迎使用" -> "欢迎使用税知账学习版。\n\n这里的个人信息和服务消息为模拟内容，收入记录保存在本机。可从首页和办&查进入收入明细，或前往数据管理导出备份。"
                "安全中心" -> "本机离线存储\n数据库位于应用私有目录，不申请联网、通讯录或广泛存储权限。\n\n应用不设账户和登录，设备解锁后可使用。请通过手机锁屏保护设备，并妥善保存导出的备份。"
                "通用设置" -> "使用系统字体大小与显示比例。\n年份依据手机日期动态生成；若年份不正确，请检查手机系统时间。\n\n卸载应用会清除记录，升级安装同签名版本可保留数据库。"
                "综合所得年度汇算" -> "学习流程：核对年度收入 → 了解扣除项目 → 检查记录。\n\n本应用可记录和统计模拟收入，不计算应补退税额，不提交真实申报。可从首页进入“年度收入统计”练习查看汇总。"
                "专项附加扣除" -> "学习主题包括子女教育、继续教育、大病医疗、住房贷款利息、住房租金、赡养老人、3岁以下婴幼儿照护。\n\n这里仅介绍项目名称，不进行真实扣除填报或资格判断。"
                "纳税记录" -> "学习版提供 CSV 明细导出及 JSON 备份。\n\n这些文件是模拟记录，不能作为真实纳税证明。请在“我的 → 数据备份与恢复”操作。"
                "关于应用", "关于&更新" -> "税知账 · 个人所得税学习版\n版本 1.3.1 · 课程实践项目\n\n独立设计的学习应用，不属于国家税务总局或其他政府机构。所有个人信息、收入与纳税记录仅用于模拟演示。\n\n技术：Kotlin + Material 原生 UI + Room / SQLite。\n不连接真实税务系统，不依赖 Google Play 服务。\n\n支持 Android 7.0 及以上；HarmonyOS 4.2.0 的 APK 兼容环境为目标，需在目标真机上验收；HarmonyOS NEXT 不在支持范围内。"
                else -> learningInfo(title)
            }
            addView(text(message,15f))
        }
        if(title=="通用设置") {
            val prefs=getSharedPreferences("settings",MODE_PRIVATE)
            card {
                addView(SwitchMaterial(this@InfoActivity).apply {
                    text="防止系统截屏（应用内）"; isChecked=prefs.getBoolean("secure",false)
                    setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("secure",checked).apply();
                        if(checked)window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                        toast(if(checked)"已开启，返回其他页面后同样生效" else "已关闭截屏保护") }
                })
            }
        }
        if(title=="安全中心") { button("通用设置") { info("通用设置") }; button("数据备份与恢复",outline=true) { go(BackupActivity::class.java) } }
        if(title=="我要咨询")button("查看操作帮助") { info("帮助中心") }
        if(title=="纳税记录")button("前往导出与备份") { go(BackupActivity::class.java) }
    }
}

private fun learningInfo(title: String): String = when {
    title.contains("申诉") -> "模拟申诉流程\n\n先核对收入所属期、扣缴义务人与金额。如需修改本机记录，可在明细详情点击修改。\n\n本入口仅用于流程学习，不向税务系统提交申诉，也不产生真实处理结果。"
    title.contains("密码") -> "学习案例\n\n妥善保护账户密码与验证码，避免在不可信页面输入个人信息。当前学习版无需真实账号登录。"
    title.contains("卡号") -> "学习案例\n\n在真实业务中应通过正式服务渠道核对资料。本应用仅展示模拟银行卡，不收集真实卡号。"
    title.contains("案件") || title=="警示案例" -> "学习案例展示\n\n此入口用于理解资料核对与规范操作的重要性。截图中的案例标题作为课程参考，不对应本机用户或真实案件处理。"
    title.contains("备份") -> "本机数据管理\n\n在办&查页面底部进入数据备份与恢复，可导出 CSV 明细、保存 JSON 备份，或选择已有 JSON 文件恢复。"
    title.contains("扣除") -> "专项项目学习\n\n本页展示“$title”的业务入口与用途。学习资料均为模拟内容，不提交扣除申请。已有收入记录仍可从收入纳税明细维护和查看。"
    title.contains("发票") || title.contains("开票") || title=="我的票夹" -> "模拟发票服务\n\n$title\n\n当前没有真实发票或申请记录。本学习版不申请开票，也不连接电子发票系统。"
    title=="扫一扫" -> "扫一扫学习入口\n\n真实办税应用可通过扫码识别业务。本学习版不申请相机权限；你可以通过首页搜索或办&查选择相应服务。"
    title.contains("如何") || title=="办税指南" || title=="热点问题" -> "操作指南\n\n查询收入：选择年度和所得类型后查询。\n维护明细：在结果页点击新增，或进入详情修改和删除。\n数据备份：在办&查底部进入本机数据管理。"
    else -> "$title\n\n本入口提供课程学习展示，资料均为模拟内容，不连接真实税务系统。\n\n当前暂无相应模拟业务记录。你可以返回办&查，通过收入纳税明细维护本机收入数据。"
}
