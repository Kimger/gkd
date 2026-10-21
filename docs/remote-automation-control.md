# 电脑通过 ADB 临时接管自动化

适用于本 Fork 的 Android 版本。入口为“高级设置 → 实时调试 → 允许电脑临时接管自动化”，默认关闭。不需要启动 HTTP 服务，也不需要 ADB 端口转发。Desktop 开发宿主显示相同入口，点击提示当前平台不支持。

开启后，在 GKD 自动化模式已经运行的情况下申请暂停。GKD 保留原有自动化开关，临时释放 UiAutomation 连接并阻止重连；不会改成系统无障碍模式。正常结束时先停止电脑任务使用的手机端 uiautomator2 服务，再释放暂停。

## 广播协议

接收器要求发送者持有 `android.permission.DUMP`，ADB shell 可调用；普通未授权应用不能发送。token 用于区分任务所有权，不是替代发送者权限的密码。以下为 release 包；debug 包的组件包名改为 `li.songe.gkd.debug`，类名和 action 不变。

```powershell
adb -s 手机地址:无线调试端口 shell am broadcast --user current `
  -n li.songe.gkd/li.gkd.app.receiver.RemoteAutomationReceiver `
  -a li.songe.gkd.REMOTE_AUTOMATION `
  --es command pause --es token 0123456789abcdef0123456789abcdef
```

| command | token | 含义 |
| --- | --- | --- |
| `pause` | 必填 | 申请暂停，等待 GKD 断开并确认没有其他 UiAutomation 占用；同凭证重复申请可续期 |
| `heartbeat` | 必填 | 延长当前暂停期限，建议每 5 秒调用一次 |
| `release` | 必填 | 释放当前任务的暂停，立即尝试恢复 |
| `status` | 不需要 | 返回当前状态；不会续期 |

token 为每次任务新生成的 16～128 位 ASCII 字母、数字、下划线或连字符，推荐 `uuid.uuid4().hex`。暂停时限固定为最后一次成功申请/续期后的 30 秒，使用手机单调时钟；维护任务约每秒检查，恢复时间还受系统调度影响。另一个 token 无法接管未释放的暂停，也不能续期或释放当前任务。过期后的心跳不能重新获得暂停。

必须同时检查 ADB 进程退出码和广播结果。成功示例：

```text
Broadcast completed: result=0, data="paused"
```

只有 `pause` 成功且 data 为 `paused` 后，电脑才能启动 uiautomator2。ADB 命令成功退出不代表广播业务成功。

状态：`paused` 正在由电脑接管；`waiting_for_automation` 等待恢复条件；`running` GKD 自动化已连接；`idle` GKD 自动化未运行、也没有待恢复请求。

失败返回 `result=1`，data 为原因，包括：`feature_disabled`、`invalid_token`、`automation_mode_required`、`automation_disabled`、`automation_not_running`、`another_task_owns_pause`、`automation_still_occupied`、`lease_expired_or_wrong_token`、`unknown_command`。若暂停申请已释放 GKD 但最终确认失败，不要启动任务，允许 30 秒超时恢复，或用同 token 发送 `release`。

## Python 接入示例

这是接入参考，不会在本项目中创建或修改电脑程序。仅使用 Python 标准库发送广播；uiautomator2 的停止接口需按电脑项目实际安装版本核对。本例的 `stop_uiautomator()` 已依据 [uiautomator2 当前源码](https://github.com/openatx/uiautomator2/blob/master/uiautomator2/core.py) 核对，旧版本不能直接套用。

```python
import logging
import re
import subprocess
import threading
import uuid

SERIAL = "192.168.1.100:12345"  # 改成 adb devices 中的设备序列号
TOKEN = uuid.uuid4().hex        # 每次任务重新生成
STOP = threading.Event()
HEARTBEAT_FAILED = threading.Event()


def notify_gkd(command):
    args = [
        "adb", "-s", SERIAL, "shell", "am", "broadcast", "--user", "current",
        "-n", "li.songe.gkd/li.gkd.app.receiver.RemoteAutomationReceiver",
        "-a", "li.songe.gkd.REMOTE_AUTOMATION",
        "--es", "command", command,
        "--es", "token", TOKEN,
    ]
    completed = subprocess.run(
        args, capture_output=True, text=True, encoding="utf-8",
        errors="replace", timeout=10, check=True,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
    )
    match = re.search(
        r'Broadcast completed: result=(-?\d+)(?:, data="([^"]*)")?',
        completed.stdout,
    )
    if not match or match.group(1) != "0":
        raise RuntimeError(completed.stdout + completed.stderr)
    return match.group(2)


def heartbeat():
    while not STOP.wait(5):
        try:
            if notify_gkd("heartbeat") != "paused":
                raise RuntimeError("GKD 已不再暂停")
        except Exception:
            HEARTBEAT_FAILED.set()
            logging.exception("GKD 心跳失败，请中止电脑任务并清理手机端服务")
            return


if notify_gkd("pause") != "paused":
    raise RuntimeError("GKD 未确认释放连接，不能启动 uiautomator2")

worker = threading.Thread(target=heartbeat, daemon=True)
worker.start()
device = None
try:
    import uiautomator2 as u2
    device = u2.connect(SERIAL)  # 必须在 pause 成功后连接

    # 在这里运行淘宝任务；循环/步骤之间检查 HEARTBEAT_FAILED。
    if HEARTBEAT_FAILED.is_set():
        raise RuntimeError("已失去 GKD 暂停租约")
finally:
    try:
        if device is not None:
            device.stop_uiautomator()  # 先释放手机端连接
    finally:
        STOP.set()
        worker.join(timeout=12)
        try:
            print("GKD:", notify_gkd("release"))
        except Exception:
            logging.exception("主动恢复失败，等待手机端超时恢复")
```

GUI/EXE 应让以上生命周期属于实际任务线程：任务停止时清理；窗口关闭时请求任务退出并清理。心跳应检查任务是否仍然正常运行，不能让一个与任务无关的常驻线程无限续期。强制结束进程无法保证 Python 的 finally 执行，由 GKD 的手机端超时处理兜底。

## 恢复规则与边界

- 正常释放或心跳超时后，仅恢复本次申请前已运行的 GKD 自动化；用户关闭自动化或改到其他模式后，不自动覆盖其设置。
- 关闭功能开关会结束当前暂停，并按原有启用状态尝试恢复；后续广播全部拒绝。
- 其他 UiAutomation 仍占用时保持等待，释放后重试；不杀死其他进程、不争抢连接。若关闭 EXE 后手机端 uiautomator2 残留，必须由电脑项目清理、重新连接后清理，或手动停止后才能恢复。
- 特权服务暂时断开时等待恢复；应用排除和按应用运行模式仍遵守 GKD 原有设置。
- 当前租约与待恢复状态仅存在于 GKD 进程内。手机重启、GKD 被杀或被强行停止时，不能保证当场执行恢复；重新运行 GKD 后按原有设置启动，并重新申请租约。该开关本身会持久保存。
- 调试先在测试任务中验证“正常退出、异常退出、结束 EXE、断网、另一个 token、关闭功能开关、手机端服务残留”等情况，再接入日常任务。

Android 端可在导出的界面日志中搜索 `Remote automation` 查看暂停、过期和恢复记录。
