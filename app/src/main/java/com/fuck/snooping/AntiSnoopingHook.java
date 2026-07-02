package com.fuck.snooping;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AntiSnoopingHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // 变砖保护：排除系统级关键进程与桌面，以及本应用自身
        if ("android".equals(lpparam.packageName)
                || "com.android.systemui".equals(lpparam.packageName)
                || "com.miui.home".equals(lpparam.packageName)
                || lpparam.packageName.equals("com.fuck.snooping")
                || lpparam.packageName.contains("hardware")
                || lpparam.packageName.contains("miui")) {
            return;
        }

        // Hook 目标应用 Activity 的 onCreate 方法
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;

                // 1. 全屏纯白贴图覆盖
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                View blankView = new View(activity);
                blankView.setBackgroundColor(Color.WHITE);
                activity.addContentView(blankView, layoutParams);

                // 2. 弹出由模块内置库提供的 MIUIX 风格弹窗
                try {
                    // 1. 获取模块自身的 Context，以加载内置资源
                    Context moduleContext = activity.createPackageContext("com.fuck.snooping", 
                            Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                    
                    // 2. 使用模块的 ClassLoader 加载内置的 MIUIX 类
                    Class<?> builderClass = moduleContext.getClassLoader().loadClass("miuix.appcompat.app.AlertDialog$Builder");
                    Object builder = XposedHelpers.newInstance(builderClass, activity);
                    
                    XposedHelpers.callMethod(builder, "setTitle", "安全提示");
                    XposedHelpers.callMethod(builder, "setMessage", "检测到他人偷窥，已停止运行。");
                    XposedHelpers.callMethod(builder, "setCancelable", false);
                    XposedHelpers.callMethod(builder, "setPositiveButton", "确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finishAffinity();
                            System.exit(0);
                        }
                    });
                    
                    XposedHelpers.callMethod(builder, "show");
                } catch (Throwable t) {
                    // 报错时记录日志并回退
                    XposedBridge.log("AntiSnooping: Failed to use bundled MIUIX: " + t.getMessage());

                    new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                            .setTitle("安全提示")
                            .setMessage("检测到他人偷窥，已停止运行。")
                            .setCancelable(false)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.finishAffinity();
                                    System.exit(0);
                                }
                            })
                            .show();
                }
            }
        });
    }
}