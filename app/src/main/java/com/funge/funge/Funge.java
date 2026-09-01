package com.funge.funge;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Funge extends AppCompatActivity {
	private Paints board = null; // 游戏板
	private final Interpreter interpreter = new Interpreter();
    private TextView stackOutput = null; // 栈显示
    private TextView IPoutput = null; // 指针
	private TextView output = null;
	// Execute按钮长按
	private final Handler handler = new Handler(Looper.getMainLooper());
	private String result = " ^0[r#]?|_w+;j:\\$n\'pgstx@";
	private List<Key> keyList = new ArrayList<>();
    private SpecialKeyboardAdapter keyboardAdapter; // 键盘适配器
    private Runnable longPressExecute; // 长按Execute按钮
    private Runnable repeatExecute; // 长按Execute按钮重复执行
	// 撤回按钮长按
	private Runnable longPressUndo; // 长按撤回检测
	private Runnable repeatUndo;    // 长按连续撤回
	private boolean isLongPressingUndo = false;

	private Runnable run; // 供Run键使用
    private boolean isLongPressing = false;
	private boolean isRunning = false;
	private int RUNSPEED = 50;
	
	@SuppressLint({"ClickableViewAccessibility", "SuspiciousIndentation"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_funge);
		
		// 接受Level传来的关卡数据
		LevelData currentLevel = (LevelData) getIntent().getSerializableExtra("current_level");
        if (currentLevel != null) {
            keyList = currentLevel.keyList;
        }

        // 传给该页面的Interpreter和Paints
		board = findViewById(R.id.board);
		interpreter.setLevel(currentLevel);
		board.setInterpreter(interpreter);

		// 输出界面
		output = findViewById(R.id.outputView);

		// 标题栏
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null && currentLevel != null) {
			actionBar.setSubtitle(currentLevel.name);
			actionBar.setTitle(String.valueOf(currentLevel.id));
		}
		
		// 键盘界面
        RecyclerView keyboardView = findViewById(R.id.keys);
        keyboardView.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false));
		
		// 点击按键的监听器，调取keyboardAdapter，传入keyList和触控监听器
        keyboardAdapter = new SpecialKeyboardAdapter(this, keyList, (pos, key) -> {
        // 点击事件设计：点击pos号按钮，就得到该按钮的状态并传给board
            board.getCommandInput(String.valueOf(key));
        });
		keyboardView.setAdapter(keyboardAdapter);

		// Interpreter的回调接口
		interpreter.setCallback(new Interpreter.Callback() {
			// 监听指令放置，从Interpreter接收keyList和pos传给keyboardAdapter
			@Override
			public void onCommandPlaced(List<Key> keyList, int pos) {
				keyboardAdapter.getKeyList(keyList);
				keyboardAdapter.updateCommandCount(pos);
			}

			@Override
			public void onError(String msg) {
				Toast.makeText(Funge.this, msg, Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onOutput(String str) { runOnUiThread(() -> { output.append(str); });}
		});

		// stackOutput = findViewById(R.id.screen);
        IPoutput = findViewById(R.id.ip);

		// execute按钮长/短按
        // 执行按钮
        Button executeButton = findViewById(R.id.executeButton);

        longPressExecute = () -> {
			isLongPressing = true;
			execute();
			handler.post(repeatExecute);
        };

		// 重复执行
		repeatExecute = new Runnable() {
			@Override
			public void run() {
				execute();
				handler.postDelayed(this, 30);
			}
		};

		// 撤回按钮同理
		Button undoButton = findViewById(R.id.undoButton);
		longPressUndo = () -> {
            isLongPressingUndo = true;
            undo();
            handler.post(repeatUndo);
        };
		repeatUndo = new Runnable() {
			@Override
			public void run() {
				undo();
				handler.postDelayed(this, 50); // 撤回速度，50ms撤一步，可以自己调
			}
		};

		// 运行，供Run按钮使用
        run = new Runnable() {
            @Override
            public void run() {
				if (!isRunning) return;
                execute();
				if (result.equals(getString(R.string.game_failed)) || result.equals(getString(R.string.game_clear))) {
					result = " ";
					Toast.makeText(Funge.this, "程序已终止", Toast.LENGTH_SHORT).show();
					handler.removeCallbacks(repeatExecute);
					isRunning = false;
					return;
				}
                if (isRunning) handler.postDelayed(this, RUNSPEED);
            }
        };
		
		// Execute键监听器
        executeButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isLongPressing = false;
                    handler.postDelayed(longPressExecute, 467); // 检测长按延时
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(longPressExecute);
                    if (!isLongPressing) { // 短按执行
                        if (run != null)
                            handler.removeCallbacks(run);
                        execute();
                    }
                    else { // 长按执行
                        handler.removeCallbacks(repeatExecute);
                        isLongPressing = false;
                    }
                    break;
            }
            return true;
        });

		// 撤回键监听器
		undoButton.setOnTouchListener((v, event) -> {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						isLongPressingUndo = false;
						handler.postDelayed(longPressUndo, 467); // 长按延时
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						handler.removeCallbacks(longPressUndo);
						if (!isLongPressingUndo) { // 短按撤回一步
							undo();
						} else { // 长按松开，停止连续撤回
							handler.removeCallbacks(repeatUndo);
							isLongPressingUndo = false;
						}
						break;
				}
				return true;
		});
    }

	@Override
    protected void onResume() {
        super.onResume();
        loadSettings();
		board.loadSettings();
    }

	// Actionbar添加设置菜单
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.setting_item) {
			// 启动承载 PreferenceFragmentCompat 的 Activity
			Intent intent = new Intent(this, Setting.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	// 读取设置
	private void loadSettings() {
        // 获取默认的 SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // 读取设置，第二个参数是默认值（当键不存在时返回）
        RUNSPEED = Integer.parseInt(prefs.getString("run_speed", "50"));
    }
	
	public void restart(View view) {
		interpreter.restart();
		board.invalidate();
		//stackOutput.setText("Restart");
		output.setText("");
		isRunning = false;
		handler.removeCallbacks(repeatExecute);
        IPoutput.setText("");
	}

    public void clear(View view) {
        interpreter.clear();
		board.invalidate();
        //stackOutput.setText("Clear");
		output.setText("");
		isRunning = false;
		handler.removeCallbacks(repeatExecute);
        IPoutput.setText("");
    }

	// 执行一步指令，并处理解释器返回的状态
	public void execute() {
		Interpreter.Status status = interpreter.tick();
		IP cip = interpreter.getCip();

		if (cip != null) {
			IPoutput.setText(interpreter.getCycleCount()+ ':' +  cip.uid + ":" + cip.x + "," + cip.y);
		}
		//stackOutput.setText(interpreter.getStack());
		board.invalidate(); // 触发重绘

		// 根据返回状态处理弹窗
		if (status == Interpreter.Status.CLEAR) {
			Toast.makeText(this, "程序执行完毕", Toast.LENGTH_SHORT).show();
			isRunning = false;
			handler.removeCallbacks(run);
		} else if (status == Interpreter.Status.FAILED) {
			Toast.makeText(this, getString(R.string.game_failed), Toast.LENGTH_SHORT).show();
			isRunning = false;
			handler.removeCallbacks(run);
		}
	}

	public void run(View view) {
		if (!isRunning) {
			isRunning = true;
			handler.post(run);
		} else {
			isRunning = false;
			handler.removeCallbacks(run);
		}
	}

    public void delete(View view) {
        board.getCommandInput(" ");
    }

	// 撤回
	public void undo() {
		// 如果正在自动运行，先停止
		if (isRunning) {
			isRunning = false;
			handler.removeCallbacks(run);
		}
		// 调用引擎撤回
		interpreter.undo();
		// 刷新 UI 显示
		board.invalidate();
		// 更新栈和坐标显示
		IP cip = interpreter.getCip(); // 获取当前 IP
		if (cip != null) {
			IPoutput.setText(interpreter.getCycleCount()+ ':'+ cip.uid + ":" + cip.x + "," + cip.y);
		} else {
			IPoutput.setText("");
		}
		// stackOutput.setText(interpreter.getStack());
		//Toast.makeText(this, "已撤回一步", Toast.LENGTH_SHORT).show();
	}
}
