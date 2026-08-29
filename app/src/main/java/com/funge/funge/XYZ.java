package com.funge.funge;

// 坐标类
public class XYZ {
	int x, y;
	XYZ(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		XYZ xyz = (XYZ) o;
		return x == xyz.x && y == xyz.y;
	}
	
	@Override
	public int hashCode() {
		return 31 * Integer.hashCode(x) + Integer.hashCode(y);
	}
}
