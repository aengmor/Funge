package com.funge.funge;

import android.content.*;
import android.graphics.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import androidx.preference.*;
import java.util.*;

public class Paints extends View { // 负责绘制游戏板的类
	private int ROW = 12;
	private int COLUMN = 16;
	private int SIZE = 100;
	private boolean SGML = true;

	private char command = ' ';
	private Random random = new Random(); // ?指令

	// 画笔
	private Paint paint = new Paint(); // 图格画笔
	private Paint borderPaint = new Paint(); // 边框画笔
	private Paint ipPaint = new Paint(); // 指针画笔
	private Paint stringPaint = new Paint(); // 图格填实
	private Paint codePaint = new Paint(); // 代码文本
	private Paint obstaclePaint = new Paint(); // 障碍物文本

	private Map<XYZ, Character> code = new HashMap<>(); // 代码
	private Map<XYZ, Character> obstacle = new HashMap<>(); // 地图障碍
	private LevelData currentLevel = null; // 当前关卡
	private List<Key> keyList = new ArrayList<>(); // 按键表，用于修改按键数

	private List<IP> ips = new ArrayList<>(); // 指针列表
	private IP cip = null; // 当前指针
	private IP cloneIP = null; // t指令分裂出的ip
	private int nextuid = 0;

	// 触控辅助
	private float downX, downY;
	private boolean hasMoved = false;
	private int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

	public Paints(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	// 回调接口，在Funge实现，用于Painter与keyboardAdapter交互以通知它改UI
	public interface OnCommandPlacedListener {
		void onCommandPlaced(List<Key> keyList, int pos); // 放置指令时触发
	}
	private OnCommandPlacedListener commandPlacedListener;
	public void setOnCommandPlacedListener(OnCommandPlacedListener listener) {
		this.commandPlacedListener = listener;
	}

	// 读取设置
	public void loadSettings() {
		// 从PreferenceManager获取 SharedPreferences 实例以读取设置
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		String sizeStr = prefs.getString("grid_size", "100");
		SIZE = Integer.parseInt(sizeStr);
		SGML = prefs.getBoolean("sgml", true);
		// 重算尺寸、重绘
		codePaint.setTextSize(SIZE);
		obstaclePaint.setTextSize(SIZE);
		requestLayout();
		invalidate();
	}

	public void setLevel(LevelData level) {
		// 接收Funge传来的关卡数据
		currentLevel = level;

		// 初始化关卡
		ROW = currentLevel.w;
		COLUMN = currentLevel.h;
		obstacle = stringToCode(currentLevel.map); // 加载障碍物
		keyList = new ArrayList<>(currentLevel.keyList);

		loadIP(currentLevel.ip);
	}

	public void loadIP(int[][] xys) {
		for (int[] xy : xys) {
			ips.add(new IP(nextuid++, xy[0], xy[1], 1, 0));
		}
	}

	public void init() {
		// 初始化图格画笔
		paint.setColor(Color.RED);
		paint.setStrokeWidth(4);
		paint.setStyle(Paint.Style.STROKE);

		stringPaint = new Paint(paint);
		stringPaint.setColor(Color.MAGENTA);

		borderPaint = new Paint(paint);
		borderPaint.setColor(Color.CYAN);
		borderPaint.setStrokeWidth(2);

		ipPaint = new Paint(paint);
		ipPaint.setColor(Color.GREEN);

		// 初始化文本画笔
		obstaclePaint.setColor(Color.BLACK);
		obstaclePaint.setTextAlign(Paint.Align.CENTER);
		obstaclePaint.setTextSize(SIZE);
		obstaclePaint.setTypeface(Typeface.MONOSPACE);

		codePaint = new Paint(obstaclePaint);
		codePaint.setColor(Color.BLUE);
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		for (int i = 0; i < ROW; i = i + 1) {
			// 绘制游戏板
			for (int j = 0; j < COLUMN; j = j + 1) {
				XYZ xy = new XYZ(i, j);
				char obs = obstacle.getOrDefault(xy, ' ');
				if (obs == 'z')
					continue;
				char cod = code.getOrDefault(xy, ' ');
				canvas.drawRect(i * SIZE, j * SIZE, (i + 1) * SIZE, (j + 1) * SIZE, paint);

				drawCenterText(canvas, obs, i, j, obstaclePaint);
				drawCenterText(canvas, cod, i, j, codePaint);
			}
		}

		canvas.drawRect(0, 0, ROW * SIZE, COLUMN * SIZE, borderPaint); // 标记边框

		for (IP ip : ips) {
			if (ip.stringMode)
				canvas.drawRect((ip.x + 0.1f) * SIZE, (ip.y + 0.1f) * SIZE, (ip.x + 0.9f) * SIZE, (ip.y + 0.9f) * SIZE,
						stringPaint);
			else
				canvas.drawRect((ip.x + 0.1f) * SIZE, (ip.y + 0.1f) * SIZE, (ip.x + 0.9f) * SIZE, (ip.y + 0.9f) * SIZE,
						ipPaint);
		}
	}

	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 测量出整个游戏板的完整宽高
        int width = ROW * SIZE;
        int height = COLUMN * SIZE;
        setMeasuredDimension(width, height);
    }
	
	public void drawCenterText(Canvas canvas, char c, int x, int y, Paint paint) {
		float x_pos = x * SIZE;
		float y_pos = y * SIZE;
		float centerX = x_pos + SIZE / 2f;
		float centerY = y_pos + SIZE / 2f;

		Paint.FontMetrics fm = paint.getFontMetrics();
		float baseline = centerY - (fm.ascent + fm.descent) / 2f;

		canvas.drawText(String.valueOf(c), centerX, baseline, paint);
	}

	// 指令相关
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN) {
			downX = event.getX();
			downY = event.getY();
			hasMoved = false;
			getParent().requestDisallowInterceptTouchEvent(true);
		} else if (action == MotionEvent.ACTION_MOVE) {
			if (!hasMoved) {
				if (Math.abs(event.getX() - downX) > touchSlop || Math.abs(event.getY() - downY) > touchSlop) {
					hasMoved = true;
					getParent().requestDisallowInterceptTouchEvent(false);
				}
			}
		} else if (action == MotionEvent.ACTION_UP) {
			if (!hasMoved) {
				int tile_x = (int) (event.getX() / SIZE);
				int tile_y = (int) (event.getY() / SIZE);
				setCommand(tile_x, tile_y, command);
				invalidate();
			}
		}
		return true;
	}

	// 取得所按指令
	public void getCommandInput(String c) {
		this.command = c.charAt(0);
	}

	public String getStack() {
		StringBuilder stackOutput = new StringBuilder();
		stackOutput.append(cip.uid + ":");
		if (ips == null || ips.isEmpty())
			return "noIP";
		for (Integer num : cip.stack) {
			stackOutput.append(num + "|");
		}
		return stackOutput.toString();
	}

	public char getCommand(int xVal, int yVal) { // 取得指令
		return code.getOrDefault(new XYZ(xVal, yVal), obstacle.getOrDefault(new XYZ(xVal, yVal), ' '));
	}

	public void setCommand(int xVal, int yVal, char c) { // 存入指令
		// 边界检查
		if (xVal < 0 || xVal >= ROW || yVal < 0 || yVal >= COLUMN)
			return;

		XYZ xy = new XYZ(xVal, yVal);

		// 若有障碍物，不能放置
		if (obstacle.containsKey(xy)) {
			Toast.makeText(getContext(), "Cannot place here", Toast.LENGTH_SHORT).show();
			return;
		}

		int pos = -1;
		Character old_c = code.getOrDefault(new XYZ(xVal, yVal), ' ');
		if (c == ' ' || old_c == c) {
			code.put(xy, ' ');
			recover(old_c);
		} else {
			pos = alter(c);
			if (pos >= 0) {
				Key key = keyList.get(pos);
				if (key.amount > 0) {
					code.put(xy, c);
					key.alter(-1);
					recover(old_c);
				}
			}
		}

		// 监听器响应变化
		if (commandPlacedListener != null && pos >= 0) {
			commandPlacedListener.onCommandPlaced(keyList, pos);
		}
	}

	// 收回被替换的指令
	private void recover(char old_c) {
		if (old_c != ' ') {
			int old_pos = alter(old_c);
			if (old_pos >= 0) {
				keyList.get(old_pos).alter(1);
				if (commandPlacedListener != null)
					commandPlacedListener.onCommandPlaced(keyList, old_pos);
			}
		}
	}

	public void setObstacle(int xVal, int yVal, char c) {
		if (xVal < 0 || xVal >= ROW || yVal < 0 || yVal >= COLUMN)
			return;

		XYZ xy = new XYZ(xVal, yVal);
		if (obstacle.containsKey(xy)) {
			if (c == ' ')
				obstacle.remove(xy);
			else
				obstacle.put(xy, c);
			return;
		}

		Character old_c = code.remove(xy);
		obstacle.put(xy, c);
		if (old_c == null || old_c == ' ')
			return;
		int old_pos = alter(old_c);
		if (old_pos < 0)
			return;
		keyList.get(old_pos).alter(1);
		commandPlacedListener.onCommandPlaced(keyList, old_pos);
	}

	// 辅助函数，由字符找按键
	public int alter(char c) {
		for (int pos = 0; pos < keyList.size(); pos++) {
			Key key = keyList.get(pos);
			if (key.in(c)) {
				return pos;
			}
		}
		return -1;
	}

	public Map<XYZ, Character> stringToCode(String[] codeString) {
		Map<XYZ, Character> mapMap = new HashMap<>();
		for (int y = 0; y < codeString.length; y++) {
			String line = codeString[y];
			for (int x = 0; x < line.length(); x++) {
				char c = line.charAt(x);
				if (c != ' ')
					mapMap.put(new XYZ(x, y), c);
			}
		}
		return mapMap;
	}

	public void clear() {
		code.clear(); // 清理代码
		currentLevel.toKeyList(); // 重置关卡
		restart(); // 重置指针
		keyList = new ArrayList<>(currentLevel.keyList); // 重置键盘
		if (commandPlacedListener != null)
			commandPlacedListener.onCommandPlaced(keyList, -1);
	}

	public void restart() {
		ips.clear();
		loadIP(currentLevel.ip);
		obstacle = new HashMap<>();
		obstacle = stringToCode(currentLevel.map);
		invalidate();
	}

	// Befunge解释器部分    
	// 栈与指针
	private int pop() {
		return cip.stack.isEmpty() ? 0 : cip.stack.remove(cip.stack.size() - 1);
	}

	private void push(int val) {
		cip.stack.add(val);
	}

	private void move() {
		char p;
		int flipCount = 0;
		if (cip.stringMode) {
			p = getCommand(cip.x, cip.y);
			if (p != ' ') {
				wrap(cip);
				return;
			}
			while (SGML && p == ' ') {
				flipCount += wrap(cip) ? 1 : 0;
				p = getCommand(cip.x, cip.y);
			}
			return;
		}

		flipCount += wrap(cip) ? 1 : 0;
		p = getCommand(cip.x, cip.y);
		if (!cip.stringMode) {
			while (p == ';') {
				do {
					flipCount += wrap(cip) ? 1 : 0;
					p = getCommand(cip.x, cip.y);
				} while (p != ';');
				flipCount += wrap(cip) ? 1 : 0;
				p = getCommand(cip.x, cip.y);
			}
		}
		if (flipCount == 1000) {
			restart();
			Toast.makeText(getContext(), "死循环", Toast.LENGTH_SHORT).show();
		}

	}

	public void retreat() {
		cip.x = (cip.x - cip.dx + ROW) % ROW;
		cip.y = (cip.y - cip.dy + COLUMN) % COLUMN;
	}

	public boolean wrap(IP cip) {
		cip.step();
		if (cip.x >= 0 && cip.x < ROW && cip.y >= 0 && cip.y < COLUMN)
			return false;
		// 反向移动直到进入空间
		while (cip.x >= ROW || cip.x < 0 || cip.y >= COLUMN || cip.y < 0) {
			cip.x -= cip.dx;
			cip.y -= cip.dy;
		}
		// 继续反向移动直到再次离开空间
		while (cip.x < ROW && cip.x >= 0 && cip.y < COLUMN && cip.y >= 0) {
			cip.x -= cip.dx;
			cip.y -= cip.dy;
		}
		// 最后正向一步
		cip.x += cip.dx;
		cip.y += cip.dy;
		return true;
	}

	// 单步执行
	public int execute(char c) {
		// 字符串模式
		if (cip.stringMode) {
			if (c == '"') {
				cip.stringMode = false;
			} else {
				push((char) c);
			}
			return 0;
		}

		switch (c) {
			// 栈存数
			case '0' :
			case '1' :
			case '2' :
			case '3' :
			case '4' :
			case '5' :
			case '6' :
			case '7' :
			case '8' :
			case '9' :
				push(c - '0');
				break;
			case 'a' :
			case 'b' :
			case 'c' :
			case 'd' :
			case 'e' :
			case 'f' :
				push(c - 'W');
				break;

			// 栈中数运算
			case '+' :
				push(pop() + pop());
				break;
			case '-' : {
				int a = pop();
				int b = pop();
				push(b - a);
			}
				break;
			case '*' :
				push(pop() * pop());
				break;
			case '/' : {
				int a = pop();
				int b = pop();
				push(a == 0 ? 0 : b / a);
			}
				break;
			case '%' : {
				int a = pop();
				int b = pop();
				push(a == 0 ? 0 : b % a);
			}
				break;
			case '!' :
				push(pop() == 0 ? 1 : 0);
				break;
			case '`' : {
				int a = pop();
				int b = pop();
				push(b > a ? 1 : 0);
			}
				break;

			// 其他栈操作     
			case ':' : {
				int top = pop();
				push(top);
				push(top);
			}
				break;
			case '\\' : {
				int a = pop();
				int b = pop();
				push(a);
				push(b);
			}
				break;
			case '$' :
				pop();
				break;
			case 'n' :
				cip.stack.clear();
				break;

			// 指针转向
			case '^' :
				cip.dx = 0;
				cip.dy = -1;
				break;
			case '<' :
				cip.dx = -1;
				cip.dy = 0;
				break;
			case 'v' :
				cip.dx = 0;
				cip.dy = 1;
				break;
			case '>' :
				cip.dx = 1;
				cip.dy = 0;
				break;
			case '[' : {
				cip.turnLeft();
				break;
			}
			case ']' : {
				cip.turnRight();
				break;
			}
			case 'r' :
				cip.reflect();
				break;
			case 'x' : {
				cip.dy = pop();
				cip.dx = pop();
				break;
			}

			// 指针随机/条件转向
			case '?' : {
				int dir = random.nextInt(4);
				cip.dx = (dir == 0 ? 1 : dir == 1 ? -1 : 0);
				cip.dy = (dir == 2 ? 1 : dir == 3 ? -1 : 0);
			}
				break;
			case '_' :
				cip.dx = (pop() == 0 ? 1 : -1);
				cip.dy = 0;
				break;
			case '|' :
				cip.dy = (pop() == 0 ? 1 : -1);
				cip.dx = 0;
				break;
			case 'w' : {
				int a = pop();
				int b = pop();
				if (a > b)
					cip.turnRight();
				else if (b > a)
					cip.turnLeft();
				break;
			}

			// 指针移位
			case '#' :
				move();
				break;
			case 'j' : {
				int a = pop();
				if (a > 0)
					for (int i = 0; i < a; i = i + 1)
						move();
				else if (a < 0)
					for (int i = 0; i < -a; i = i + 1)
						retreat();
				break;
			}
			//			case ';' : {
			//				move();
			//				while (getCommand(cip.x, cip.y) != ';')
			//					move();
			//				break;
			//			}

			// 杂项
			case '@' :
				cip.uid = -1;
				break;
			case '\'' :
				move();
				push(getCommand(cip.x, cip.y));
				break;
			case ' ' :
				break;
			case 'p' : {
				int yVal = pop();
				int xVal = pop();
				int v = pop();
				setObstacle(xVal, yVal, (char) v);
			}
				break;
			case 'g' : {
				int yVal = pop();
				int xVal = pop();
				char ch = getCommand(xVal, yVal);
				push((int) ch);
			}
				break;
			case 's' : {
				move();
				int v = pop();
				setObstacle(cip.x, cip.y, (char) v);
				break;
			}
			case 't' : {
				cloneIP = new IP(nextuid++, cip.x, cip.y, -cip.dx, -cip.dy);
				return 2;
			}
			case 'z' :
				return 2;
			case 'k' : {
				int n = pop(); // 执行次数
				if (n <= 0) {
					move();
					break;
				}
				int tx = cip.x;
				int ty = cip.y;
				move();
				char next = getCommand(cip.x, cip.y);
				while (next == ' ' || next == ';') {
					if (next == ';') {
						move();
						while (getCommand(cip.x, cip.y) != ';')
							move();
						move();
					} else {
						move();
					}
					next = getCommand(cip.x, cip.y);
				}
				cip.x = tx;
				cip.y = ty;
				for (int i = 0; i < n; i++) {
					execute(next);
				}
			}
				break;
			case '"' :
				cip.stringMode = true;
				break;
			//            case '.': System.out.print(pop() + " "); break;
			//            case ',': System.out.print((char) pop()); break;

			default :
				break;
		}
		return 0;
	}

	public String tick() {
		char c = ' ';
		for (int i = 0; i < ips.size(); i++) {
			cip = ips.get(i);
			if (cip.uid >= 0) {
				c = getCommand(cip.x, cip.y);
				if (!cip.stringMode && c == 'z') {
					restart();
					return getContext().getString(R.string.game_failed);
				}
				execute(c);
			}
			if (cip.uid < 0) {
				ips.remove(cip);
				Toast.makeText(getContext(), "IP deleted", Toast.LENGTH_SHORT).show();
				i--;
			}
			if (cloneIP != null) {
				ips.add(i++, cloneIP);
				cloneIP = null;
			}
		}
		for (IP ip : ips) {
			cip = ip;
			move();
		}
		invalidate();
		if (ips == null || ips.isEmpty())
			return getContext().getString(R.string.game_clear);
		return cip.uid + ":" + cip.x + "," + cip.y + ' ' + c;
	}
}

