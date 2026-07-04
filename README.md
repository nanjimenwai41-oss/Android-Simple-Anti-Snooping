# AntiSnooping

[简体中文](#antisnooping-中文) | [English](#antisnooping-en)

---

## AntiSnooping (中文)

这是一个基于 Android 平台 Xposed/LSPosed 框架开发的强力防偷窥模块。

### 核心功能

*   **微信朋友圈拦截**: 深度拦截微信朋友圈（SnsTimelineUI 等），支持 Intent 启动拦截与 UI 元素动态扫描，防止“闪现”。
*   **安全弹窗提示**: 检测到非法访问时，弹出系统级安全对话框。
*   **防护机制**: 自动过滤系统核心进程，确保模块运行安全，不导致系统崩溃。

### 使用方法

1. 下载并安装模块。
2. 在 LSPosed 管理器中启用模块。
3. 勾选对应作用域。
4. 重启目标应用。

### 开源协议

本项目采用 [AGPL-3.0](LICENSE) 协议开源。

---

## AntiSnooping (EN)

A powerful anti-snooping module based on the Android Xposed/LSPosed framework.

### Core Features

*   **Moments Interception**: Deeply intercepts WeChat Moments (SnsTimelineUI, etc.), supporting Intent interception and UI scanning to prevent leaks.
*   **Security Dialog**: Pops up a system-style security dialog when unauthorized access is detected.
*   **Brick Protection**: Filters system core processes to ensure stability.

### Usage

1. Download and install.
2. Enable the module in LSPosed Manager.
3. Check the scope.
4. Restart the target app.

### License

This project is licensed under the [AGPL-3.0](LICENSE).
