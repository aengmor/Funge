package com.funge.funge;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.*;
import androidx.preference.*;
import androidx.recyclerview.widget.*;
import java.util.*;

public class Funge extends AppCompatActivity {
	private Paints board = null; // 游戏板
	private Interpreter interpreter = new Interpreter();
    private TextView stackOutput = null; // 栈显示
    private TextView IPoutput = null; // 指针
    private Button executeButton = null; // 执行按钮
	private SpecialKeyboardAdapter keyboardAdapter; // 键盘适配器
	private String result = " ^0[r#]?|_w+;j:\\$n\'pgstx@";
	private List<Key> keyList = new ArrayList<>();
	
	// 处理Execute按钮长按
	private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable longPressExecute; // 长按Execute按钮
    private Runnable repeatExecute; // 长按Execute按钮重复执行
	private Runnable run; // 供Run键使用
    private boolean isLongPressing = false;
	private boolean isRunning = false;
	private int RUNSPEED = 50;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_funge);
		
		// 接受Level传来的关卡数据
		LevelData currentLevel = (LevelData) getIntent().getSerializableExtra("current_level");
		keyList = currentLevel.keyList;
		
		// 标题栏
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setSubtitle(currentLevel.name);
			actionBar.setTitle(String.valueOf(currentLevel.id));
		}
			
		// 传给该页面的Paints
		board = findViewById(R.id.board);
		board.setInterpreter(interpreter);
		interpreter.setLevel(currentLevel);
		
		// 键盘界面
        RecyclerView keyboardView = findViewById(R.id.keys);
        keyboardView.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false));
		
		// 点击按键的监听器
		keyboardAdapter = new SpecialKeyboardAdapter(this, keyList, new SpecialKeyboardAdapter.OnItemClickListener() {
				// 调取keyboardAdapter，传入keyList和触控监听器
				@Override
				public void onItemClick(int pos, char key) { 
				// 点击事件设计：点击pos号按钮，就得到该按钮的状态并传给board
					board.getCommandInput(String.valueOf(key));
					
				}		
			});
		keyboardView.setAdapter(keyboardAdapter);
	
		// 放置指令的监听器，从Paints接收keyList和pos传给keyboardAdapter
		board.setOnCommandPlacedListener(new Paints.OnCommandPlacedListener() {
				@Override
				public void onCommandPlaced(List<Key> keyList, int pos)
				{
					keyboardAdapter.getKeyList(keyList);
					keyboardAdapter.updateCommandCount(pos);
				}
			});
			
        stackOutput = findViewById(R.id.screen);
        IPoutput = findViewById(R.id.ip);

		// execute按钮长/短按
        executeButton = findViewById(R.id.executeButton);

        longPressExecute = new Runnable() {
            @Override
            public void run() {
                isLongPressing = true;
                execute();
                handler.post(repeatExecute);
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
		
		// 重复执行
		repeatExecute = new Runnable() {
			@Override
            public void run() {
				execute();
				handler.postDelayed(this, 30);
				}
		};
		
		// 按Execute键的监听器
        executeButton.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							isLongPressing = false;
							handler.postDelayed(longPressExecute, 467); 
							// 检测长按延时
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
				}
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
		stackOutput.setText("Restart");
		isRunning = false;
		handler.removeCallbacks(repeatExecute);
        IPoutput.setText("0,0");
	}

    public void clear(View view) {
        interpreter.clear();
        stackOutput.setText("Clear");
		isRunning = false;
		handler.removeCallbacks(repeatExecute);
        IPoutput.setText("0,0");
    }

	public void execute() {
		Interpreter.Status status = interpreter.tick();
		IP cip = interpreter.getCip(); // 需要在 Engine 暴露刚执行完的 cip

		if (cip != null) {
			IPoutput.setText(cip.uid + ":" + cip.x + "," + cip.y);
		}
		stackOutput.setText(interpreter.getStack());
		board.invalidate(); // 触发重绘

		// 根据引擎返回的状态处理弹窗
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
}
