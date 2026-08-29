package com.funge.funge;
import java.util.*;

public class IP { // 指针类
	int uid, x, y, dx, dy;
	List<Integer> stack;
	boolean stringMode = false;

	//		IP() {
	//			this.uid = nextuid;
	//			this.x = 0;
	//			this.y = 0;
	//			this.dx = 1;
	//			this.dy = 0;		
	//			stack = new ArrayList<>();
	//		}

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
