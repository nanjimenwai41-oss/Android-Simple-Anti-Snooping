package com.fuck.snooping;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AntiSnoopingHook implements IXposedHookLoadPackage {

    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    private static final int DIALOG_SHOWN_TAG_ID = 0x7f010001;
    private static final int SCAN_STATE_TAG_ID = 0x7f010002;
    private static final int TARGET_VIEW_TAG_ID = 0x7f010003;
    private static boolean globalSkipScan = false; 
    private static WeakReference<View> lastClickedView;

    /**
     * 实时查询主程序开关状态
     */
    private boolean isEnabledRemote(Context context, String packageName) {
        if (context == null) return WECHAT_PACKAGE.equals(packageName);
        try {
            // 使用 Context 的 ContentResolver 调用模块的 Provider
            Bundle res = context.getContentResolver().call(
                    Uri.parse("content://com.fuck.snooping.scope"),
                    "is_enabled", packageName, null);
            return res != null && res.getBoolean("enabled", false);
        } catch (Throwable t) {
            // 通讯失败时，微信默认开启，其他应用默认关闭，防止误拦
            return WECHAT_PACKAGE.equals(packageName);
        }
    }

    private static final Set<String> TARGET_ACTIVITIES = new HashSet<>(Arrays.asList(
            "com.tencent.mm.plugin.sns.ui.Improve.ImproveSnsTimelineUI",
            "com.tencent.mm.plugin.sns.ui.SnsTimelineUI",
            "com.tencent.mm.plugin.sns.ui.SnsUserUI",
            "com.tencent.mm.ui.AlbumUI",
            "com.tencent.mm.plugin.favorite.ui.FavoriteIndexUI",
            "com.tencent.mm.plugin.setting.ui.setting.SettingsUI",
            "com.tencent.mm.plugin.mall.ui.MallIndexUI",
            "com.tencent.mm.plugin.brandservice.ui.BrandServiceIndexUI"
    ));

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // 注册激活状态
        if (!"android".equals(lpparam.packageName) && !lpparam.packageName.equals("com.fuck.snooping")) {
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Context context = (Context) param.thisObject;
                        context.getContentResolver().call(Uri.parse("content://com.fuck.snooping.scope"), 
                                "register_scope", lpparam.packageName, null);
                    } catch (Throwable ignored) {}
                }
            });
        }

        if ("com.fuck.snooping".equals(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod("com.fuck.snooping.MainActivity", lpparam.classLoader, "isModuleActive", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });
            return;
        }

        // --- 生命周期拦截 ---
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                if (!isEnabledRemote(activity, lpparam.packageName)) return;

                View contentView = activity.findViewById(android.R.id.content);
                if (contentView != null) contentView.setTag(SCAN_STATE_TAG_ID, "READY");

                String className = activity.getClass().getName();
                if (WECHAT_PACKAGE.equals(lpparam.packageName)) {
                    if (isTargetActivity(className)) showSecurityDialog(activity);
                } else {
                    // 普通 APP：拦截除首页外的所有启动或恢复（也可以在此加特定包名过滤）
                    showSecurityDialog(activity);
                }
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onRestart", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                globalSkipScan = true;
            }
        });

        if (WECHAT_PACKAGE.equals(lpparam.packageName)) {
            hookWeChatClickEvents(lpparam);
            try {
                XC_MethodHook skipHook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) { globalSkipScan = true; }
                };
                XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI", lpparam.classLoader, "finish", skipHook);
                XposedHelpers.findAndHookMethod("com.tencent.mm.ui.chatting.TextPreviewUI", lpparam.classLoader, "finish", skipHook);
            } catch (Throwable ignored) {}
        }

        // --- 启动拦截 ---
        try {
            XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader,
                    "execStartActivity",
                    Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Context context = (Context) param.args[0];
                            if (context == null || !isEnabledRemote(context, lpparam.packageName)) return;

                            Intent intent = (Intent) param.args[4];
                            if (intent == null || intent.getComponent() == null) return;
                            String className = intent.getComponent().getClassName();

                            if (WECHAT_PACKAGE.equals(lpparam.packageName)) {
                                if (className.contains("TextPreviewUI")) return;
                                Activity src = (Activity) param.args[3];
                                if (src != null && src.getClass().getName().contains("TextPreviewUI")) return;
                                
                                if (isTargetActivity(className)) {
                                    Activity current = (Activity) param.args[3];
                                    if (current != null) showSecurityDialog(current);
                                    param.setResult(null);
                                }
                            } else {
                                // 普通应用：启动界面即拦截
                                Activity current = (Activity) param.args[3];
                                if (current != null) {
                                    showSecurityDialog(current);
                                    param.setResult(null);
                                }
                            }
                        }
                    });
        } catch (Throwable ignored) {}

        // --- UI 扫描 ---
        XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                boolean hasFocus = (boolean) param.args[0];
                if (hasFocus) {
                    final Activity activity = (Activity) param.thisObject;
                    if (!isEnabledRemote(activity, lpparam.packageName)) return;

                    if (globalSkipScan) {
                        globalSkipScan = false;
                        View cv = activity.findViewById(android.R.id.content);
                        if (cv != null) cv.setTag(SCAN_STATE_TAG_ID, "DONE");
                        return;
                    }

                    View cv = activity.findViewById(android.R.id.content);
                    if (cv != null && "READY".equals(cv.getTag(SCAN_STATE_TAG_ID))) {
                        cv.setTag(SCAN_STATE_TAG_ID, "DONE");
                        checkViewTreeRecursive(activity.getWindow().getDecorView(), activity);
                    }
                }
            }
        });
    }

    private boolean isTargetActivity(String className) {
        if (className == null) return false;
        if (className.contains("plugin.sns.ui.SnsTimelineUI") || 
            className.contains("plugin.sns.ui.Improve.ImproveSnsTimelineUI") ||
            className.contains("plugin.sns.ui.SnsUserUI") ||
            className.contains("com.tencent.mm.ui.AlbumUI")) return true;
        return TARGET_ACTIVITIES.contains(className);
    }

    private void hookWeChatClickEvents(final LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod(View.class, "performClick", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                View view = (View) param.thisObject;
                lastClickedView = new WeakReference<>(view);
                if (isMomentsEntryView(view)) {
                    Context context = view.getContext();
                    if (context instanceof Activity) {
                        view.setPressed(false);
                        showSecurityDialog((Activity) context);
                        param.setResult(true);
                    }
                }
            }
        });
    }

    private boolean isMomentsEntryView(View view) {
        Context context = view.getContext();
        if (!(context instanceof Activity)) return false;
        Activity activity = (Activity) context;
        String name = activity.getClass().getName();
        if (name.contains(".chatting.ChattingUI") || name.contains(".chatting.TextPreviewUI")) return false;
        if (name.contains(".ui.LauncherUI") && !isDiscoverTabContext(activity.getWindow().getDecorView())) return false;
        if (view.getTag(TARGET_VIEW_TAG_ID) != null) return true;
        return checkViewForText(view, "朋友圈") || checkViewForText(view, "收藏") || checkViewForText(view, "服务") || checkViewForText(view, "支付");
    }

    private boolean checkViewForText(View view, String targetText) {
        if (view instanceof TextView) return targetText.equals(((TextView) view).getText().toString());
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (checkViewForText(group.getChildAt(i), targetText)) return true;
            }
        }
        return false;
    }

    private void checkViewTreeRecursive(View view, final Activity activity) {
        if (view == null) return;
        String name = activity.getClass().getName();
        if (name.contains(".chatting.ChattingUI") || name.contains(".chatting.TextPreviewUI")) return;
        
        // --- LauncherUI 专项修复：点击触发逻辑 ---
        boolean isLauncher = name.contains(".ui.LauncherUI");
        if (isLauncher && !isDiscoverTabContext(activity.getWindow().getDecorView())) return;

        boolean isContactInfo = name.contains("ContactInfoUI") || name.contains("ContactlnfoU");

        if (view instanceof TextView) {
            String content = ((TextView) view).getText().toString();
            if ("朋友圈".equals(content) || "SnsTimeLine".equalsIgnoreCase(content)) {
                // 在 LauncherUI 中，扫描到不弹窗，只打标记，依靠点击拦截
                if (isContactInfo || isLauncher) markClickableParentAsTarget(view);
                else showSecurityDialog(activity);
                return;
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                checkViewTreeRecursive(group.getChildAt(i), activity);
            }
        }
    }

    private boolean isDiscoverTabContext(View root) {
        return findTextInTree(root, "扫一扫") || findTextInTree(root, "看一看") || findTextInTree(root, "附近");
    }

    private boolean findTextInTree(View view, String target) {
        if (view instanceof TextView && target.equals(((TextView) view).getText().toString())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup gp = (ViewGroup) view;
            for (int i = 0; i < gp.getChildCount(); i++) {
                if (findTextInTree(gp.getChildAt(i), target)) return true;
            }
        }
        return false;
    }

    private void markClickableParentAsTarget(View view) {
        View current = view;
        while (current != null) {
            if (current.isClickable()) { current.setTag(TARGET_VIEW_TAG_ID, true); return; }
            if (current.getParent() instanceof View) current = (View) current.getParent();
            else break;
        }
    }

    private void showSecurityDialog(final Activity activity) {
        final String packageName = activity.getPackageName();
        final boolean isWeChat = WECHAT_PACKAGE.equals(packageName);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 123云盘兼容性：增加 Activity 存活状态强效检查
                    if (activity.isFinishing() || (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
                    
                    if (lastClickedView != null) {
                        View v = lastClickedView.get();
                        if (v != null) { v.setPressed(false); v.clearFocus(); }
                    }

                    final ViewGroup contentView = activity.findViewById(android.R.id.content);
                    if (contentView == null) return;
                    
                    if (contentView.getTag(DIALOG_SHOWN_TAG_ID) != null) return;
                    contentView.setTag(DIALOG_SHOWN_TAG_ID, true);

                    int nightModeFlags = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    boolean isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
                    int theme = isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;

                    // 普通应用：全屏遮罩覆盖
                    if (!isWeChat && contentView.findViewWithTag("ANTI_SNOOP_MASK") == null) {
                        View maskView = new View(activity);
                        maskView.setTag("ANTI_SNOOP_MASK");
                        maskView.setLayoutParams(new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 
                                ViewGroup.LayoutParams.MATCH_PARENT));
                        maskView.setBackgroundColor(isDarkMode ? Color.BLACK : Color.WHITE);
                        maskView.setClickable(true);
                        maskView.setFocusable(true);
                        contentView.addView(maskView);
                    }

                    AlertDialog dialog = new AlertDialog.Builder(activity, theme)
                            .setTitle("安全提示")
                            .setMessage("检测到他人偷窥，已停止运行。")
                            .setCancelable(false)
                            .setPositiveButton("确定", (dialogInterface, which) -> {
                                globalSkipScan = true;
                                contentView.setTag(DIALOG_SHOWN_TAG_ID, null);
                                resetAllViewsState(activity.getWindow().getDecorView());
                                String className = activity.getClass().getName();
                                if (!isWeChat || (!className.equals("com.tencent.mm.ui.LauncherUI") && !className.contains("ContactInfoUI"))) {
                                    activity.finish();
                                }
                            })
                            .create();
                    dialog.setOnShowListener(d -> {
                        if (isDarkMode) {
                            android.widget.Button btn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            if (btn != null) btn.setTextColor(Color.parseColor("#90CAF9"));
                        }
                    });
                    dialog.show();
                } catch (Throwable ignored) {}
            }
        });
    }

    private void resetAllViewsState(View view) {
        if (view == null) return;
        view.setPressed(false);
        view.setSelected(false);
        view.clearFocus();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) resetAllViewsState(group.getChildAt(i));
        }
    }
}
