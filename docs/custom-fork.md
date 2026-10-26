# 自用版本说明

本仓库 [Kimger/gkd](https://github.com/Kimger/gkd) 是 [gkd-kit/gkd](https://github.com/gkd-kit/gkd) 的个人自用 Fork。保留上游作者信息和 [GPL-3.0-only 许可证](../LICENSE)，在少量独立提交中维护自定义功能。

- `main`：用于跟随上游，不直接加入自定义功能。
- `dev/custom-enhancements`：自用功能分支，包含随机延时、日志页面、HyperOS 4 前台识别兼容修复、电脑临时接管自动化及猫猫木鱼触发提示。
- README 中的官方安装链接指向上游版本；本仓库功能需从自用分支构建。

## 猫猫木鱼触发提示

入口：**设置 → 触发提示 → 猫猫木鱼**。选择后保存；“播放提示效果”可在保存前预览。

- 使用灰白猫猫木鱼，透明背景，显示在原自定义 Toast 的底部位置。
- 每次提示轻敲一次猫猫头，顶部浮出“功德 +1”，约 1.8 秒内渐隐；无声音，不拦截触摸。
- 木鱼大小可在 20～80dp 间调节，默认 40dp；猫猫和木槌同比例缩放，文字保持可读字号，位置不变。调整后可预览，保存后用于实际触发。
- 系统样式和原文字自定义样式保留，旧配置不会自动切换。此样式固定显示“功德 +1”，不改写原有提示文案。
- Android 沿用无障碍悬浮层或悬浮窗权限；两者都不可用时回退为系统文字提示。触发提示沿用原有频率限制，不影响规则执行。
- Desktop 复用相同动画组件用于开发预览，不代表 Android 真机显示验收。

美术资源：`gkd-app/src/commonMain/composeResources/drawable/cat_woodfish.png`，由内置 ImageGen 根据本次确认的浅灰蓝猫猫木鱼生成透明本体；木槌由共享 Kotlin Canvas 独立绘制和旋转。生成提示词：保留参考图的猫猫木鱼造型、灰白配色、笑脸和木质纹理，去除木槌与背景光晕，输出透明背景、干净边缘的独立本体，不添加文字或其他物体。

## 规则级随机延时

在需要延时的单条规则中添加 `actionDelayRange`，例如：

```json5
{
  key: 2,
  name: '点击红包-开（随机延时）',
  preKeys: [1],
  matches: [
    '@Button[desc="开"] -3 LinearLayout >2 [text$="红包"]',
  ],
  actionDelayRange: {
    minimum: 500,
    maximum: 2000,
  },
  actionCd: 100,
  matchRoot: true,
}
```

此示例需放入现有微信规则组，并保留前置规则 `key: 1`；节点选择器仍需与实际微信界面匹配。

字段语义：

- 单位为毫秒，区间两端均包含；示例每次生成 `500～2000ms` 的整数。
- 仅对显式配置该字段的单条规则生效，支持应用规则和全局规则，不支持规则组继承。
- 第一次匹配到目标后抽取本次延时，等待结束后重新查询，仍匹配才执行动作。
- 同一轮等待不会反复抽取；动作成功或规则重置后，下次匹配重新抽取。
- 未配置时保留原有 `actionDelay` 行为；同时配置时 `actionDelayRange` 优先，不叠加。
- `minimum`、`maximum` 均必填且为整数，满足 `0 ≤ minimum ≤ maximum < Long.MAX_VALUE`；两值相等时相当于固定延时，抽到 `0` 时不等待。

`actionDelayRange` 是本 Fork 的扩展，官方 GKD 不支持，不能依赖官方客户端执行该延时。

## 查看随机延时日志

入口：**高级设置 → 日志排查 → 随机延时日志**。

- 按日期和应用分组、时间倒序展示；点击记录查看详情并复制。
- 点击“刷新”重新读取文件，查看新增记录。
- 兼容之前的随机延时日志；旧记录没有应用标识时显示“应用未记录”。
- 读取现有诊断日志文件，不增加数据库表，也不改变触发记录结构。历史记录随现有文件日志清理策略保留，不永久保存。

记录表示“开始等待”，并不代表点击或领取成功；实际动作请对照“触发记录”。抽到 `0ms` 直接执行时不会生成等待日志。

导出日志后，也可以搜索 `randomActionDelayMs`：

```text
13:48:26.930 A11yRuleEngine, ...
[0]: Random action delay scheduled
[1]: appId=com.tencent.mm, subsId=-2, groupKey=5, ruleKey=2, ruleIndex=1
[2]: randomActionDelayMs=539
```

日志时间是开始等待时间；重新查询、执行和调度会使实际点击时间略晚于设定延时。

## 签名与 Release 构建

Android 模块为 `gkd-android`，默认只构建 `gkd` 渠道。配置 Android SDK 后，通过环境变量或本地 Gradle 属性提供以下字段：

| 字段 | 用途 |
| --- | --- |
| `GKD_STORE_FILE` | 自己的 keystore 绝对路径 |
| `GKD_STORE_PASSWORD` | keystore 密码 |
| `GKD_KEY_ALIAS` | 签名密钥别名 |
| `GKD_KEY_PASSWORD` | 私钥密码 |

这些字段可通过现有私有配置在构建进程中注入。不要将密码、`signing.properties` 或 keystore 提交到仓库；本 Fork 不内置个人签名信息。未提供 `GKD_STORE_FILE` 时，现有构建脚本会回退到本机 Debug 签名。

在仓库根目录执行：

```powershell
git switch dev/custom-enhancements
.\gradlew.bat :gkd-android:assembleGkdRelease --no-configuration-cache
```

默认 APK 位于 `gkd-android/build/outputs/apk/gkd/release/`。版本和文件名以该目录的 `output-metadata.json` 为准。

本机交付目录固定为 `Y:\GKD`：打包并验证后，将 APK 复制到该目录，保留构建目录原文件。交付文件名附版本号或时间戳，避免覆盖历史包；目标盘不可用时需明确提示。

Release 包名为 `li.songe.gkd`。官方版与自签名版签名不同，首次切换应先导出备份，再卸载官方版并安装自签名版。以后保持同一签名及可升级的版本号即可覆盖安装。Debug 版包名带 `.debug` 后缀，可与 Release 共存。

安装升级后，若 Shizuku 已授权但自动化未运行，可在 GKD 中停止并重新启动特权服务，再确认工作模式与规则开关。

## 自动化模式下的辅助无障碍

入口：**高级设置 → 后台运行 → 自动化模式下允许无障碍辅助运行**，默认关闭。

开启后，自动化启动成功或打开该开关时，如果 GKD 普通无障碍未运行，会使用特权服务授予的写入安全设置权限自动开启。缺少权限或系统受限设置阻止启动时提示原因，不持续轮询或反复强制开启。

普通无障碍仅用于辅助兼容，自动化区域的规则、动作和快照仍由 UiAutomation 负责；自动化断开、暂停时不回退到辅助服务执行，停止时关闭辅助服务。原有无障碍作用域、屏蔽名单继续生效；显式使用普通无障碍的作用域仍按原模式切换。关闭开关恢复原来的互斥行为。

这来自 HyperOS 4 / Android 17 上的对照结果：微信在自动化单独运行时无法提供有效节点，而自动化与自身普通无障碍同时运行时可以读取。不保证其他系统或版本也需要、或能通过此方式改善；不将现象归因为未经证实的系统白名单。

## 电脑临时接管自动化

电脑与 GKD 轮流使用 UiAutomation 的可选功能见 [ADB 远程自动化接管协议与 Python 示例](remote-automation-control.md)，默认关闭，不依赖 HTTP。

## 开屏前台识别兼容

针对小米 OS4 上已复现的“开屏窗口仍属于应用，但任务栈短暂报告为桌面”情况，增加活动窗口核对：窗口仍属于当前应用时保留其匹配状态；若已误记为桌面，后续应用事件与活动窗口一致时允许立即纠正，不等待原有任务栈保护时间。无法确认窗口归属时保留原有判断，不按应用包名单独写特例。

窗口核对在前台状态锁外异步执行，最多等待 100ms；出现新前台事件后丢弃旧核对结果。确认纠正后同步服务运行模式所用的前台应用状态，并主动恢复规则查询，避免等待下一次无障碍事件。

相关实现位于 `A11yState.kt` 和 `TaskForegroundPolicy.kt`；回归测试为 `TaskForegroundPolicyTest`。诊断日志可搜索 `Ignored conflicting launcher task report` 或 `Corrected launcher task report from active window`。具体 ROM 和应用的真机表现仍需验证。

## 同步上游

本地 `origin` 指向自己的 Fork，`upstream` 指向官方仓库。先确认工作区无未提交修改：

```powershell
git status --short
git remote -v
git fetch upstream --tags
```

建议按需要同步经过选择的稳定版本，不必每次追随 beta。同步官方最新 `main` 时，可使用保留历史的合并方式：

```powershell
git switch main
git merge --ff-only upstream/main
git push origin main

git switch dev/custom-enhancements
git merge main
```

遇到冲突时先解决并核对，不能直接用某一方覆盖全部文件。自定义改动主要位于订阅字段解析、随机延时状态机、Android 延时日志，以及独立日志页面和导航入口。上游重构这些位置时需检查语义，即使 Git 没有冲突也要验证。

合并后按实际改动做验证，再推送功能分支：

```powershell
.\gradlew.bat :gkd-app:jvmTest --tests "li.gkd.app.rule.RuleRuntimeTest" --tests "li.gkd.app.rule.TaskForegroundPolicyTest" --tests "li.gkd.app.logging.RandomDelayLogTest"
# 完成当前会话的 SDK 与签名配置后：
.\gradlew.bat :gkd-android:assembleGkdRelease --no-configuration-cache
git push origin dev/custom-enhancements
```

以上命令展示日常同步流程，不能替代上游变更所需的其他验证。项目开发约束仍遵循 [AGENTS.md](../AGENTS.md)。
