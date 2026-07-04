package com.fuck.snooping;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScopeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private AppAdapter adapter;
    private List<AppInfo> fullList = new ArrayList<>();
    private List<AppInfo> displayList = new ArrayList<>();
    private String currentSearchQuery = "";
    private int currentSortMode = 0; // 0: Name, 1: Package
    private int filterType = 0; // 0: All, 1: User, 2: System
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scope);

        setupImmersion();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadingProgress = findViewById(R.id.loading_progress);
        recyclerView = findViewById(R.id.rv_apps);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, v.getPaddingTop(), systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        setupSearch();
        loadApps();
    }

    private void setupImmersion() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        View decorView = window.getDecorView();
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void setupSearch() {
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                filterAndSort();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu sortMenu = menu.addSubMenu(0, 100, 0, "排序方式");
        sortMenu.add(0, 1, 0, "按名称");
        sortMenu.add(0, 2, 0, "按包名");
        SubMenu typeMenu = menu.addSubMenu(0, 200, 0, "应用类型");
        typeMenu.add(1, 10, 0, "全部");
        typeMenu.add(1, 11, 0, "用户");
        typeMenu.add(1, 12, 0, "系统");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == 1) currentSortMode = 0;
        else if (id == 2) currentSortMode = 1;
        else if (id == 10) filterType = 0;
        else if (id == 11) filterType = 1;
        else if (id == 12) filterType = 2;
        else return super.onOptionsItemSelected(item);
        filterAndSort();
        return true;
    }

    private void loadApps() {
        loadingProgress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(0); // 不带 meta_data 更快
            
            SharedPreferences scopePrefs = getSharedPreferences("active_scope_apps", 0);
            Set<String> activeScopes = scopePrefs.getStringSet("apps", new HashSet<>());

            List<AppInfo> tempFullList = new ArrayList<>();
            for (ApplicationInfo appInfo : installedApps) {
                AppInfo info = new AppInfo();
                info.appInfo = appInfo; // 保存对象，稍后按需获取图标
                info.name = appInfo.loadLabel(pm).toString();
                info.packageName = appInfo.packageName;
                info.isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                info.isWechat = info.packageName.equals("com.tencent.mm");
                info.isActivated = activeScopes.contains(info.packageName);
                info.enabled = info.isWechat || Config.isAppEnabled(this, info.packageName);
                tempFullList.add(info);
            }

            mainHandler.post(() -> {
                fullList.clear();
                fullList.addAll(tempFullList);
                loadingProgress.setVisibility(View.GONE);
                filterAndSort();
            });
        });
    }

    private void filterAndSort() {
        displayList.clear();
        for (AppInfo info : fullList) {
            if (filterType == 1 && info.isSystem) continue;
            if (filterType == 2 && !info.isSystem) continue;
            if (currentSearchQuery.isEmpty() || 
                info.name.toLowerCase().contains(currentSearchQuery.toLowerCase()) || 
                info.packageName.toLowerCase().contains(currentSearchQuery.toLowerCase())) {
                displayList.add(info);
            }
        }

        if (currentSortMode == 0) {
            Collections.sort(displayList, (a, b) -> a.name.compareToIgnoreCase(b.name));
        } else {
            Collections.sort(displayList, (a, b) -> a.packageName.compareToIgnoreCase(b.packageName));
        }

        if (adapter == null) {
            adapter = new AppAdapter(displayList);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private static class AppInfo {
        ApplicationInfo appInfo;
        String name, packageName;
        Drawable icon;
        boolean enabled, isWechat, isSystem, isActivated;
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
        private final List<AppInfo> list;
        AppAdapter(List<AppInfo> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppInfo info = list.get(position);
            holder.tvName.setText(info.name);
            holder.tvPackage.setText(info.packageName);
            
            // 异步加载图标，防止滚动卡顿
            holder.ivIcon.setImageDrawable(null); // 先清除旧图标
            if (info.icon != null) {
                holder.ivIcon.setImageDrawable(info.icon);
            } else {
                executor.execute(() -> {
                    Drawable icon = info.appInfo.loadIcon(getPackageManager());
                    info.icon = icon;
                    mainHandler.post(() -> {
                        if (holder.getAdapterPosition() == position) {
                            holder.ivIcon.setImageDrawable(icon);
                        }
                    });
                });
            }

            holder.switchEnable.setOnCheckedChangeListener(null);
            if (info.isWechat) {
                holder.switchEnable.setChecked(true);
                holder.switchEnable.setEnabled(false);
                holder.tvWechatTip.setVisibility(View.VISIBLE);
                holder.itemView.setAlpha(0.8f);
                holder.itemView.setOnClickListener(null);
            } else if (!info.isActivated) {
                holder.switchEnable.setChecked(false);
                holder.switchEnable.setEnabled(false);
                holder.tvWechatTip.setVisibility(View.GONE);
                holder.itemView.setAlpha(0.5f);
                holder.itemView.setOnClickListener(v -> Toast.makeText(ScopeActivity.this, "请在 LSPosed 中勾选对应应用！", Toast.LENGTH_SHORT).show());
            } else {
                holder.switchEnable.setChecked(info.enabled);
                holder.switchEnable.setEnabled(true);
                holder.tvWechatTip.setVisibility(View.GONE);
                holder.itemView.setAlpha(1.0f);
                holder.itemView.setOnClickListener(null);
            }
            
            holder.switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                info.enabled = isChecked;
                Config.setAppEnabled(ScopeActivity.this, info.packageName, isChecked);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvName, tvPackage, tvWechatTip;
            MaterialSwitch switchEnable;
            ViewHolder(View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_app_icon);
                tvName = itemView.findViewById(R.id.tv_app_name);
                tvPackage = itemView.findViewById(R.id.tv_package_name);
                tvWechatTip = itemView.findViewById(R.id.tv_wechat_tip);
                switchEnable = itemView.findViewById(R.id.switch_enable);
            }
        }
    }
}
