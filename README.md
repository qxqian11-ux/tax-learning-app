# 税知账 · 个人所得税学习版

当前版本 **1.1.0 / versionCode 2**。独立的 Kotlin / Android 原生课程项目，参考用户提供的页面截图实现蓝白界面，所有身份、收入与税额均用于模拟学习，不连接真实税务系统。

下载与历史版本见 [GitHub Releases](https://github.com/qxqian11-ux/tax-learning-app/releases)。新版交付集中在 `output/v1.1.0/`，原 `output/税知账-1.0.0-release.apk` 和旧二维码保留。安装时可用同包名、同签名 APK 覆盖升级，升级前建议先导出 JSON 备份。

## 页面与功能

- 首页、待办、办&查、消息、我的五项固定导航；页面滚动、首页横向卡片和资讯分类状态保留。
- 个人中心包含蓝色渐变头像区、浅橙模拟身份卡、三个数量卡片、五项菜单、关怀版及模拟退出/登录。
- 首页和办&查按照参考图组织卡片及服务分组；收入查询、统计和文件备份连接真实本地业务功能。
- 收入明细按年查询、增删改查、月份/所得项目筛选、全年和分类统计，使用 Room + SQLite。
- 年度范围为 2019 至系统当前年份。原所得类别继续兼容，另补充参考图中的所得类别名称。
- 金额以 Long 整数分存储，使用 BigDecimal 校验和转换；原数据库 schema 版本 1 保持不变。
- 系统文件选择器导出 CSV / JSON；JSON 校验、重复跳过、内容冲突事务回滚与异常恢复保护。
- 首次示例与初始化标记在同一事务中写入；后续启动不重新生成已删记录。
- 自定义图标、头像及矢量插画，无 Google Play 依赖，无联网、通讯录、定位或广泛存储权限。

非核心税务入口为明确标注的学习展示，不提供真实申报、银行或实名认证。详情中原模型没有维护的扣除与机构信息显示为“—”或“未维护”。新增和修改页尚无参考截图，采用统一的蓝白分组表单，待视觉核对。

## 构建

Android Studio 打开项目，JDK 21（字节码目标 Java 17）、SDK 34、AGP 8.4.2、Gradle 8.7。最低 Android 7.0 / API 24。开发机 JDK 位于 `D:\java\jdk\jdk-21.0.11`，SDK 位于 `D:\tax_project\Android SDK`。本机 `local.properties` 不包含在公开源码包中。

执行 `scripts/build.ps1` 可运行单元测试、Lint 并生成 Debug / Release。签名保存在本机 `signing/`；创建独立开发副本时使用自己的密钥，原应用升级必须保留原正式密钥。签名私钥与密码不包含在源码包或 Git 仓库中。

## 代码结构

- `Ui.kt`：通用 Screen、布局和交互辅助。
- `ReferenceMain.kt`：五项导航、个人中心、待办和消息。
- `ReferencePages.kt`：首页、服务目录、搜索和资讯。
- `ReferenceIcon.kt`、`ServiceArtwork.kt`：自定义本地矢量素材。
- `IncomeScreens.kt`：收入查询、年度选择、列表与详情。
- `EditActivity.kt`：录入/修改表单、日期、字段校验与保存。
- `Model.kt`、`Repository.kt`、`BackupCodec.kt`：实体/DAO、精确金额、数据持久化和备份事务。
- `app/src/test`、`app/src/androidTest`：可独立运行的 JVM 与 Android 集成测试。

`acceptance/` 是独立 UI 验收工具，按测试方法执行并保留了历史阶段方法和中断清理方法；不属于发布 APK，也不应作为普通应用安装。正式 UI 数据回归针对专用模拟器中的 1.0.0 既有模拟记录，依赖对应备份文件。通用开发验证可运行应用自身的测试。

## 回退与验证

重构前备份位于 `backup/before-ui-1.0.0-*`，Git 基线为 `1ce9b28d87a0dc053b1815af5a6dfabe5dc10b98`。原数据库结构、Repository、备份算法和金额精度保留。

本轮说明、原始文件校验和运行截图见 `docs/*-1.1.0.txt` 与 `output/v1.1.0/evidence`。HarmonyOS 4.2.0 真机待用户测试；模拟器通过不等同于该手机已通过。HarmonyOS NEXT 不在本 APK 的目标范围内。
