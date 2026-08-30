package com.funge.funge;

import java.util.ArrayList;
import java.util.List;

public class IP { // 指针类
	int uid, x, y, dx, dy;
	List<Integer> stack;
	boolean stringMode = false;

	// 时空旅行
	public char SpMode = 'D'; // 'D'禁用, 'A'绝对, 'R'相对
	public char VMode = 'D';  // 'D'禁用, 'Y'改变方向
	public char TMode = 'D';  // 'D'禁用, 'A'绝对, 'R'相对
	public int TTime = 0;
	public int TvX = 0, TvY = 0;
	public int TSpX = 0, TSpY = 0;
	// 记录跳跃前的返回坐标 (用于 'I' 指令)
	public int RTSpX, RTSpY, RTvX, RTvY, RTTime;
	// 悖论检测记录
	public int lastJumpX = -1, lastJumpY = -1, lastTargetTime = -1;

	IP(int uid, int x, int y, int dx, int dy) {
		this.uid = uid;
		this.x = x;
		this.y = y;
		this.dx = dx;
		this.dy = dy;
		stack = new ArrayList<>();
	}
	
	public void step() {
		this.x += this.dx;
		this.y += this.dy;
	}
	
	public void reflect() {
		this.dx = -this.dx;
		this.dy = -this.dy;
	}
	
	public void turnLeft() {
		int tmp = this.dx;
		this.dx = this.dy;
		this.dy = -tmp;
	}

	public void turnRight() {
		int tmp = this.dx;
		this.dx = -this.dy;
		this.dy = tmp;
	}
}
