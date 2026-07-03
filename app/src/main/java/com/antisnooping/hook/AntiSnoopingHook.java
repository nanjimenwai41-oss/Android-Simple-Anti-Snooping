package com.antisnooping.hook;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AntiSnoopingHook implements IXposedHookLoadPackage {

    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    private static final int DIALOG_SHOWN_TAG_ID = 0x7f010001; // 手动定义一个不容易冲突的 ID

    // 原有的目标微信 Activity 集合 (保持原样)
    private static final Set<String> TARGET_ACTIVITIES = new HashSet<>(Arrays.asList(
            "com.tencent.mm.plugin.sns.ui.Improve.ImproveSnsTimelineUI",
            "com.tencent.mm.plugin.sns.ui.SnsTimelineUI",
            "com.tencent.mm.plugin.sns.ui.SnsUserUI",
            "com.tencent.mm.ui.AlbumUI"
    ));

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!WECHAT_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        // --- 方案一：Activity 生命周期拦截 (增强型) ---
        // 拦截 onCreate
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                String className = activity.getClass().getName();
                if (isMomentsTimeline(className) || TARGET_ACTIVITIES.contains(className)) {
                    showSecurityDialog(activity);
                }
            }
        });

        // 拦截 onResume (兜底生命周期，防止任务切换绕过)
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                String className = activity.getClass().getName();
                if (isMomentsTimeline(className)) {
                    showSecurityDialog(activity);
                }
            }
        });

        // --- 方案二：Intent 启动拦截 (高级方案，零闪现) ---
        try {
            XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader,
                    "execStartActivity",
                    Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Intent intent = (Intent) param.args[4];
                            if (intent != null && intent.getComponent() != null) {
                                String className = intent.getComponent().getClassName();
                                if (isMomentsTimeline(className)) {
                                    // 找到当前 Activity 以显示对话框
                                    Activity currentActivity = (Activity) param.args[3];
                                    if (currentActivity != null) {
                                        showSecurityDialog(currentActivity);
                                    }
                                    // 阻止跳转
                                    param.setResult(null);
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("AntiSnooping: Failed to hook execStartActivity: " + t.getMessage());
        }

        // --- 方案三：通用 UI 元素检测 (终极兜底方案) ---
        XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                boolean hasFocus = (boolean) param.args[0];
                if (hasFocus) {
                    final Activity activity = (Activity) param.thisObject;
                    checkViewTreeRecursive(activity.getWindow().getDecorView(), activity);
                }
            }
        });
    }

    /**
     * 判断是否为朋友圈时间线相关的类 (模糊匹配)
     */
    private boolean isMomentsTimeline(String className) {
        if (className == null) return false;
        String lower = className.toLowerCase();
        // 核心特征：包含 sns.ui 且包含 timeline
        return lower.contains(".sns.ui.") && lower.contains("timeline");
    }

    /**
     * 递归检查 View 树中是否存在“朋友圈”等关键标识
     */
    private void checkViewTreeRecursive(View view, final Activity activity) {
        if (view == null) return;

        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null) {
                String content = text.toString();
                // 检测是否包含“朋友圈”文字。注意：微信在不同语言下可能不同。
                if ("朋友圈".equals(content) || "SnsTimeLine".equalsIgnoreCase(content)) {
                    showSecurityDialog(activity);
                    return;
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                checkViewTreeRecursive(group.getChildAt(i), activity);
            }
        }
    }

    /**
     * 调用系统风格对话框提示安全信息
     */
    private void showSecurityDialog(final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) return;

                // 使用 android.R.id.content 的 tag 来标记是否已显示
                View contentView = activity.findViewById(android.R.id.content);
                if (contentView != null) {
                    if (contentView.getTag(DIALOG_SHOWN_TAG_ID) != null) {
                        return;
                    }
                    contentView.setTag(DIALOG_SHOWN_TAG_ID, true);
                }

                new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setTitle("安全提示")
                        .setMessage("检测到他人偷窥，已停止运行。")
                        .setCancelable(false)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 点击确定：退出当前活动/返回上一级
                                activity.finish();
                            }
                        })
                        .show();
            }
        });
    }
}
