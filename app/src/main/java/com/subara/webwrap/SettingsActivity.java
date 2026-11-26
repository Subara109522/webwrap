package com.subara.webwrap;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xFF111318);

        TextView title = new TextView(this);
        title.setText("起始网址");
        title.setTextSize(20);
        title.setTextColor(0xFFFFFFFF);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("填入要打开的网址，不带 https:// 也会自动补上。");
        hint.setTextSize(13);
        hint.setTextColor(0xFF9AA0A6);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(8);
        hint.setLayoutParams(hintLp);
        root.addView(hint);

        final EditText input = new EditText(this);
        input.setTextSize(16);
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF666B73);
        input.setSingleLine(false);
        SharedPreferences sp = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        input.setText(sp.getString(MainActivity.KEY_URL, MainActivity.DEFAULT_URL));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputLp.topMargin = dp(16);
        input.setLayoutParams(inputLp);
        root.addView(input);

        Button save = new Button(this);
        save.setText("保存");
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveLp.topMargin = dp(24);
        save.setLayoutParams(saveLp);
        save.setOnClickListener(v -> {
            String url = MainActivity.normalize(input.getText().toString());
            if (url.length() <= 8) {
                Toast.makeText(this, "网址不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                    .edit().putString(MainActivity.KEY_URL, url).apply();
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            finish();
        });
        root.addView(save);

        Button reset = new Button(this);
        reset.setText("恢复默认");
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resetLp.topMargin = dp(8);
        reset.setLayoutParams(resetLp);
        reset.setOnClickListener(v -> input.setText(MainActivity.DEFAULT_URL));
        root.addView(reset);

        setContentView(root);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
