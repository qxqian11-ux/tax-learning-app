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
                "任职受雇信息" -> "星河科技（模拟）\n任职状态：在职（示例）\n任职类型：课程实践\n\n此页面为学习版展示。实际收入记录中的扣缴义务人可在收入明细中修改。"
                "家庭成员信息" -> "模拟成员：小禾\n关系：学习示例\n\n这里只展示虚构资料，不采集家庭成员的真实信息。"
                "银行卡信息" -> "演示卡：学习银行卡\n状态：未绑定\n\n无需也无法绑定真实银行卡。不要输入真实卡号。"
                "安全中心" -> "本机离线存储\n数据库位于应用私有目录，不申请联网、通讯录或广泛存储权限。\n\n应用不设账户和登录，设备解锁后可使用。请通过手机锁屏保护设备，并妥善保存导出的备份。"
                "通用设置" -> "使用系统字体大小与显示比例。\n年份依据手机日期动态生成；若年份不正确，请检查手机系统时间。\n\n卸载应用会清除记录，升级安装同签名版本可保留数据库。"
                "综合所得年度汇算" -> "学习流程：核对年度收入 → 了解扣除项目 → 检查记录。\n\n本应用可记录和统计模拟收入，不计算应补退税额，不提交真实申报。可从首页进入“年度收入统计”练习查看汇总。"
                "专项附加扣除" -> "学习主题包括子女教育、继续教育、大病医疗、住房贷款利息、住房租金、赡养老人、3岁以下婴幼儿照护。\n\n这里仅介绍项目名称，不进行真实扣除填报或资格判断。"
                "纳税记录" -> "学习版提供 CSV 明细导出及 JSON 备份。\n\n这些文件是模拟记录，不能作为真实纳税证明。请在“我的 → 数据备份与恢复”操作。"
                else -> "税知账 · 个人所得税学习版\n版本 1.0.0 · 课程实践项目\n\n独立设计的学习应用，不属于国家税务总局或其他政府机构。所有个人信息、收入与纳税记录仅用于模拟演示。\n\n技术：Kotlin + Material 原生 UI + Room / SQLite。\n不连接真实税务系统，不依赖 Google Play 服务。\n\n支持 Android 7.0 及以上；HarmonyOS 4.2.0 的 APK 兼容环境为目标，需在目标真机上验收；HarmonyOS NEXT 不在支持范围内。"
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
        if(title=="纳税记录")button("前往导出与备份") { go(BackupActivity::class.java) }
    }
}
