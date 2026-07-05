package com.fuck.snooping;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateScopeInfo();
    }

    private void initView() {
        MaterialCardView statusCard = findViewById(R.id.card_status);
        ImageView statusIcon = findViewById(R.id.iv_status_icon);
        TextView statusTitle = findViewById(R.id.tv_status_title);
        TextView statusSummary = findViewById(R.id.tv_status_summary);

        boolean isDarkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        if (isModuleActive()) {
            statusCard.setCardBackgroundColor(isDarkMode ? Color.parseColor("#1B5E20") : Color.parseColor("#C8E6C9"));
            statusIcon.setImageResource(android.R.drawable.checkbox_on_background);
            statusTitle.setText("模块已激活");
            statusTitle.setTextColor(isDarkMode ? Color.parseColor("#81C784") : Color.parseColor("#2E7D32"));
            statusSummary.setText("LSPosed 框架运行正常");
            statusSummary.setTextColor(isDarkMode ? Color.parseColor("#81C784") : Color.parseColor("#2E7D32"));
        } else {
            statusCard.setCardBackgroundColor(isDarkMode ? Color.parseColor("#B71C1C") : Color.parseColor("#FFCCBC"));
            statusIcon.setImageResource(android.R.drawable.ic_delete);
            statusTitle.setText("模块未激活");
            statusTitle.setTextColor(isDarkMode ? Color.parseColor("#E57373") : Color.parseColor("#BF360C"));
            statusSummary.setText("请在 LSPosed 管理器中启用模块并重启");
            statusSummary.setTextColor(isDarkMode ? Color.parseColor("#E57373") : Color.parseColor("#BF360C"));
        }

        updateScopeInfo();

        MaterialCardView scopeCard = findViewById(R.id.card_scope);
        scopeCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScopeActivity.class);
            startActivity(intent);
        });

        TextView tvDeviceInfo = findViewById(R.id.tv_device_info);
        String deviceInfo = "型号: " + Build.MODEL + "\n" +
                "厂商: " + Build.MANUFACTURER + "\n" +
                "Android 版本: " + Build.VERSION.RELEASE + "\n" +
                "SDK: " + Build.VERSION.SDK_INT;
        tvDeviceInfo.setText(deviceInfo);

        MaterialCardView githubCard = findViewById(R.id.card_about);
        githubCard.setOnClickListener(v -> {
            String url = "https://github.com/nanjimenwai41-oss/Android-Simple-Anti-Snooping";
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateScopeInfo() {
        TextView tvScopeCount = findViewById(R.id.tv_scope_count);
        int count = Config.getEnabledApps(this).size();
        tvScopeCount.setText(String.valueOf(count));
    }

    /**
     * 该方法会被 Xposed 钩子拦截并返回 true
     */
    public boolean isModuleActive() {
        return false;
    }
}
