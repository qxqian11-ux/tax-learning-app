# 税知账 · 个人所得税学习版

独立的 Kotlin / Android 原生课程项目，仅记录模拟收入，不属于任何政府机构，不连接税务系统。

已发布：[下载页面](https://github.com/qxqian11-ux/tax-learning-app/releases/tag/v1.0.0) · [签名 Release APK](https://github.com/qxqian11-ux/tax-learning-app/releases/download/v1.0.0/tax-learning-1.0.0-release.apk)。二维码在 `output/APK下载二维码.png`。远程下载内容已与本地 SHA-256 比对一致。

## 快速开始

Android Studio 打开本目录，使用 JDK 21（编译目标 Java 17）、SDK 34、Gradle 8.7。最低 Android 7.0 / API 24。开发机已存在的 SDK 在 `D:\tax_project\Android SDK`，JDK 在 `D:\java\jdk\jdk-21.0.11`。`local.properties` 是本机路径，不在源码包中。

首次发布前执行 `scripts/create-signing.ps1`（仅执行一次），再执行 `scripts/build.ps1`。独立 Release 签名位于 `signing/`，不能丢失或提交到代码仓库。Debug 与 Release 使用不同包名，数据彼此独立。

## 已实现的功能

- 首页 / 我的双底部导航，所有入口均可操作；辅助业务使用明确标注的学习说明页。
- 2019 年至系统当前年份的年度选择、所得类型选择、月度及所得项目筛选。
- 明细完整增删改查，删除确认，退出编辑确认，每页 50 笔。
- 全年收入、已申报、已缴税额，月份与所得项目条形图；数据库变化自动刷新。
- Room / SQLite 私有数据库，UUID 主键，年度索引；首次初始化标记与示例记录处于同一事务，删除后不会重生。
- BigDecimal 校验转换为 Long 整数分，全程精确求和，输入最多两位小数。
- 系统文件选择器导出 CSV / JSON，按年或全量；JSON 验证、重复跳过、冲突整次回滚。
- 自定义矢量与自适应图标，屏幕截屏保护开关，无网络或存储权限。

## 项目结构

```text
app/src/main/java/cn/taxlearning/ledger/
  Model.kt          实体、DAO、金额与业务校验
  Repository.kt     数据库、首次初始化、事务与异步数据接口
  BackupCodec.kt    JSON 校验和序列化、CSV 转义
  Ui.kt             通用 UI、主页、查询、列表和详情
  EditActivity.kt   增改表单及错误提示
  StatsActivity.kt  月份及所得项目统计图
  BackupActivity.kt 系统文件选择器导入导出
  InfoActivity.kt   模拟资料、设置及业务说明
app/src/test/       金额、日期、备份及汇总单元测试
app/src/androidTest/  SQLite 持久化及事务集成测试
app/schemas/        Room schema
assets/             原创图标导出
docs/               架构、验收、安装和签名说明
scripts/            构建、签名、发布及二维码工具
output/             交付包（忽略 Git）
signing/            本地密钥和密码（忽略 Git，禁止公开）
```

## 数据及导入规则

数据库 `learning-ledger.db` 位于应用私有目录。每条记录必须带纳税年度，查询始终按年限定。收入日期必须有效且年份与纳税年度一致，纳税月份可独立指定。扣缴义务人为必填，无扣缴方填写“自行申报（模拟）”。单金额上限 9,999,999,999.99 元，数据库上限 10,000 条，避免 Long 汇总溢出。所有金额仅为用户录入，应用不进行法定税额计算。

备份格式为 `format=tax-learning-backup`、`version=1`，金额字段以整数分表示。导入上限 64 MB / 10,000 条，覆盖数据库全部容量；先检查所有记录，再在单一事务中合并。相同 UUID 和内容跳过；同 UUID 不同内容拒绝整次导入。复制记录应从界面新增，不应修改备份 UUID。CSV 使用 UTF-8 BOM、CRLF、双引号转义，并防护常见表格公式前缀；CSV 不用于恢复。

修改一条记录的年度后，该记录会从旧年度移入新年度。列表年度汇总始终为全年数据；筛选合计只统计所选月份及项目，二者分别标注。

## 环境与交付状态

项目已迁移到 `D:\PersonalTaxLearningApp`，并在该目录构建成功。初期曾因自动审批服务连接中断暂存到授权工作区；审批恢复后已完成 D 盘迁移，无需重复安装 Android Studio 或 SDK。新下载的无 Google 服务 Android 11 测试镜像位于 D 盘现有 SDK，测试虚拟机位于项目 `.android/`（不包含在源码包中）。

实际构建、测试和发布结果以 `docs/验证结果.md` 为准；不可仅凭源码或任务书判断已经通过真机测试。HarmonyOS 4.2.0 以 APK 兼容环境为目标；HarmonyOS NEXT 不支持此 APK。

## 参考资料

- [税务机关关于最新版 App 查询入口的说明（2026-01）](https://guangxi.chinatax.gov.cn/nsfw/nszx/rdwd/202601/t20260115_427903.html)：参考首页、年度与所得类型查询流程，不复制官方品牌。
- [Android Room](https://developer.android.com/jetpack/androidx/releases/room)：本项目固定使用 2.6.1 以适配现有工具链。
- [Android 系统文件选择器](https://developer.android.com/training/data-storage/shared/documents-files)：使用 SAF，无需广泛存储权限。
- [AGP 8.4 兼容说明](https://developer.android.com/build/releases/agp-8-4-0-release-notes)：使用现有 AGP 8.4.2 与 SDK 34。

用户提供的任务书原文在第 14.1 节第 7 项处结束；该处之后的要求未包含在附件中。
