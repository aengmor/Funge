package com.funge.funge;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.preference.PreferenceManager;

import lombok.Setter;

@Setter
public class Paints extends View { // 负责绘制游戏板的类
	private int SIZE = 100;

	private Interpreter interpreter = new Interpreter();
	private char command = ' ';

	// 画笔
	private Paint paint = new Paint(); // 图格画笔
	private Paint borderPaint = new Paint(); // 边框画笔
	private Paint ipPaint = new Paint(); // 指针画笔
	private Paint stringPaint = new Paint(); // 图格填实
	private Paint codePaint = new Paint(); // 代码文本
	private Paint obstaclePaint = new Paint(); // 障碍物文本

	// 触控辅助
	private float downX, downY;
	private boolean hasMoved = false;
	private int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

	public Paints(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	// 读取设置
	public void loadSettings() {
		// 从PreferenceManager获取 SharedPreferences 实例以读取设置
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		String sizeStr = prefs.getString("grid_size", "100");
		SIZE = Integer.parseInt(sizeStr);
		interpreter.setSGML(prefs.getBoolean("sgml", true));
		// 重算尺寸、重绘
		codePaint.setTextSize(SIZE);
		obstaclePaint.setTextSize(SIZE);
		requestLayout();
		invalidate();
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

		int ROW = interpreter.getROW();
		int COLUMN = interpreter.getCOLUMN();

		for (int i = 0; i < ROW; i = i + 1) {
			// 绘制游戏板
			for (int j = 0; j < COLUMN; j = j + 1) {
				XYZ xy = new XYZ(i, j);
				char obs = interpreter.getObstacle().getOrDefault(xy, ' ');
				if (obs == 'z')
					continue;
				char cod = interpreter.getCode().getOrDefault(xy, ' ');
				canvas.drawRect(i * SIZE, j * SIZE, (i + 1) * SIZE, (j + 1) * SIZE, paint);

				drawCenterText(canvas, obs, i, j, obstaclePaint);
				drawCenterText(canvas, cod, i, j, codePaint);
			}
		}

		canvas.drawRect(0, 0, ROW * SIZE, COLUMN * SIZE, borderPaint); // 标记边框

		for (IP ip : interpreter.getIps()) {
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
        int width = interpreter.getROW()* SIZE;
        int height = interpreter.getCOLUMN() * SIZE;
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
				interpreter.setCommand(tile_x, tile_y, command);
				invalidate();
			}
		}
		return true;
	}

	// 取得所按指令
	public void getCommandInput(String c) {
		this.command = c.charAt(0);
	}
}

