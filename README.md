# Android-Simple-Anti-Snooping

[简体中文](#android-simple-anti-snooping-中文) | [English](#android-simple-anti-snooping-en)

---

## Android-Simple-Anti-Snooping (中文)

这是一个基于 Android 平台 Xposed/LSPosed 框架开发的强力防偷窥模块。

### 核心功能

* ~~后台全屏纯白贴图覆盖：当检测到目标应用启动时，模块会自动向当前的 Activity 顶层注入一个与屏幕等宽等高的纯白 View 布局进行完全遮挡。~~（未完善）
* 在覆盖白屏的同时，会弹出一个~~完全继承当前手机系统原生样式（例如 MIUI / HyperOS 风格）的~~警告弹窗。用户点击确定后，模块将直接调用底层接口退出该应用并清理后台进程。（未完善）
* 完备的变砖防护机制：代码层层面过滤了系统核心进程（如 SystemUI、系统桌面等）以及模块自身包名，防止因范围扩大导致手机死机或卡成砖的情况。（待测试）

### 使用方法

1. 下载安装。
2. 在 LSPosed 管理器中启用模块。
3. 勾选对应作用域。
4. 重启作用域。

### 画饼

1. 仿 Lsposed 界面
2. APP 内切换黑白名单
3. 定时拦截
4. 伪装反偷窥行为

### 写在最后

本项目完全由 AI 负责，本人只负责思路和测试，如有不专业之处敬请谅解！

---

## Android-Simple-Anti-Snooping (EN)

This is a powerful anti-snooping module based on the Android platform Xposed/LSPosed framework.

### Core Features

* ~~Background full-screen pure white layout coverage: When the target application is detected to start, the module will automatically inject a pure white View layout with the same width and height as the screen to the top layer of the current Activity for complete occlusion.~~ (Incomplete)
* While covering the white screen, ~~a warning pop-up window that completely inherits the native style of the current mobile phone system (such as MIUI / HyperOS style) will pop up. After the user clicks confirm, the module will directly call the underlying interface to exit the application and clean up the background process.~~ (Incomplete)
* Complete brick-proof mechanism: The code layer filters the core processes of the system (such as SystemUI, system desktop, etc.) and the module's own package name to prevent the phone from crashing or turning into a brick due to the expansion of the scope. (To be tested)

### Usage

1. Download and install.
2. Enable the module in LSPosed Manager.
3. Check the corresponding scope.
4. Restart the scope.

### Roadmap

1. Simulated LSPosed interface
2. Toggle black and white list within the APP
3. Scheduled interception
4. Camouflage anti-snooping behavior

### Postscript

This project is entirely handled by AI. I am only responsible for ideas and testing. Please forgive any unprofessional aspects!
