package com.funge.funge;

import android.content.*;
import android.graphics.*;
import android.os.*;
import androidx.appcompat.app.*;
import androidx.recyclerview.widget.*;
import androidx.swiperefreshlayout.widget.*;
import com.google.gson.*;
import com.google.gson.reflect.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import android.widget.*;

public class Level extends AppCompatActivity {
    private LevelData currentLevel = null; // 当前关卡，供Funge取用
	private List<LevelData> levelList = null; // 关卡列表
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level);
		getSupportActionBar().setTitle("Levels");
		
		levelList = loadLevels(this); // 载入所有关卡
		
        RecyclerView recyclerView = findViewById(R.id.levelView); // 取用选关界面
        recyclerView.setLayoutManager(new GridLayoutManager(this, 7)); // 网格布局
		LevelAdapter levelAdapter = new LevelAdapter(levelList, new LevelAdapter.OnItemClickListener() {
			// 调取LevelAdapter，需传入关卡列表和监听器匿名类，并实现公开接口
			@Override
			public void onItemClick(int pos) { // 点击pos号按钮
				currentLevel = levelList.get(pos);	// 载入第pos关
				currentLevel.id = pos;
				Intent intent = new Intent(Level.this, Funge.class);
				intent.putExtra("current_level", currentLevel); // 将该关卡传给Funge
				startActivity(intent); // 跳转到Funge
			}		
		});
		recyclerView.setAdapter(levelAdapter);
	
	// 下拉刷新
	SwipeRefreshLayout swipeRefresh = findViewById(R.id.levels);
	swipeRefresh.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE);
	
	swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
   		@Override
    	public void onRefresh() {
        // 刷新关卡
			levelList = loadLevels(Level.this);
			levelAdapter.getLevelList(levelList);
			swipeRefresh.setRefreshing(false);
			Toast.makeText(Level.this, String.valueOf(levelList.size()), Toast.LENGTH_SHORT).show();
    		}
		});}
	
	public List<LevelData> loadLevels(Context context) {
		// 从levels.json读取各关卡成LevelData列表输出，借助gson
		File externalFile = new File(context.getExternalFilesDir(null), "levels.json");
		InputStream is = null;
		boolean needCopyToExternal = false; // 是否复制内部文件到外部
		
		try {
			if (externalFile.exists()) {
				is = new FileInputStream(externalFile);
			} else {
				is = context.getAssets().open("levels.json");
				needCopyToExternal = true;  // 标记需要复制
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			Type type = new TypeToken<List<LevelData>>(){}.getType();
			Gson levelgson = new Gson();
			List<LevelData> levels = levelgson.fromJson(reader, type);
			for (LevelData level : levels) {
				level.toKeyList();
			}
			reader.close();
			is.close();
			
			if (needCopyToExternal) {
				copyDefaultLevelsToExternal(context);
			}
			return levels;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void copyDefaultLevelsToExternal(Context context) {
		try {
			InputStream defaultIs = context.getAssets().open("levels.json");
			File outFile = new File(context.getExternalFilesDir(null), "levels.json");
			FileOutputStream fos = new FileOutputStream(outFile);
			byte[] buffer = new byte[4096];
			int len;
			while ((len = defaultIs.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}
			fos.close();
			defaultIs.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
