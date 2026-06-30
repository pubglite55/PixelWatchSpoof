package io.github.pixelwatchspoof;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(Color.parseColor("#1a1a2e"));

        TextView title = new TextView(this);
        title.setText("PixelWatchSpoof");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Pixel Watch 1 → Xiaomi Watch 5");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.parseColor("#4CAF50"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 16, 0, 48);
        layout.addView(subtitle);

        addStatusItem(layout, "模块状态", isLsposedActive() ? "已激活" : "未激活");
        addStatusItem(layout, "作用域", "com.mi.health");
        addStatusItem(layout, "目标设备", "Xiaomi Watch 5 (M2301B2PN)");
        addStatusItem(layout, "设备类型", "BLE Watch (type=2)");

        TextView instructions = new TextView(this);
        instructions.setText("\n使用说明:\n\n" +
            "1. 打开 LSPosed 管理器\n" +
            "2. 进入「模块」页面\n" +
            "3. 启用 PixelWatchSpoof 模块\n" +
            "4. 勾选作用域: 小米运动健康\n" +
            "5. 强制停止小米运动健康 App\n" +
            "6. 重新打开小米运动健康");
        instructions.setTextSize(14);
        instructions.setTextColor(Color.parseColor("#cccccc"));
        instructions.setLineSpacing(0, 1.3f);
        instructions.setPadding(0, 32, 0, 32);
        layout.addView(instructions);

        Button openLsposed = new Button(this);
        openLsposed.setText("打开 LSPosed");
        openLsposed.setOnClickListener(v -> {
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage("org.lsposed.manager");
                if (intent == null) {
                    intent = getPackageManager().getLaunchIntentForPackage("io.github.libxposed.manager");
                }
                if (intent != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "未找到 LSPosed 管理器", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "打开失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 16, 0, 16);
        openLsposed.setLayoutParams(btnParams);
        layout.addView(openLsposed);

        Button openHealth = new Button(this);
        openHealth.setText("打开小米运动健康");
        openHealth.setOnClickListener(v -> {
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage("com.mi.health");
                if (intent != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "未找到小米运动健康", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "打开失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        openHealth.setLayoutParams(btnParams);
        layout.addView(openHealth);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);
        setContentView(scrollView);
    }

    private void addStatusItem(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);

        TextView labelView = new TextView(this);
        labelView.setText(label + ": ");
        labelView.setTextSize(14);
        labelView.setTextColor(Color.parseColor("#888888"));
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));
        row.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(value.contains("已激活") || value.contains("Xiaomi") ?
            Color.parseColor("#4CAF50") : Color.parseColor("#ff9800"));
        valueView.setTypeface(null, Typeface.BOLD);
        row.addView(valueView);

        parent.addView(row);
    }

    private boolean isLsposedActive() {
        try {
            getPackageManager().getPackageInfo("org.lsposed.manager", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            try {
                getPackageManager().getPackageInfo("io.github.libxposed.manager", 0);
                return true;
            } catch (PackageManager.NameNotFoundException e2) {
                return false;
            }
        }
    }
}
