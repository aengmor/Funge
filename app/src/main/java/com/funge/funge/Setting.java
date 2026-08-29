package com.funge.funge;

import android.os.*;
import androidx.appcompat.app.*;

public class Setting extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // 将 SettingsFragment 添加到 Activity 中
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment())
				.commit();
        }

        // 设置 ActionBar 标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("设置");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
}
