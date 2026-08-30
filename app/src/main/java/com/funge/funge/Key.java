package com.funge.funge;

import java.io.Serializable;
import java.util.Objects;

import lombok.Setter;

public class Key implements Serializable {
	private static final long serialVersionUID = 1L;
	String states = "#"; // 状态列表，
	int currentState = 0;
	@Setter
    int amount = 50; // 可用数量
	int stateAmount = 1;
	
	public Key() {} // 为了Gson能读取的无参构造函数
	
	public Key(String states, int amount) {
		this.states = states;
		this.amount = amount;
		stateAmount = states.length();
	}

    @Override
	public boolean equals(Object o) {
		if (this == o) return true; // 引用相同
		if (o == null || getClass() != o.getClass()) return false; // 为null或类型不同
		return Objects.equals(this.states, ((Key)o).states);// 返回比较结果
	}

	@Override
	public int hashCode() {
		return states == null ? 0: states.hashCode();
	}
	
	public char toggle() {
		if (stateAmount == 1) return states.charAt(0);
		currentState = (currentState + 1) % stateAmount; // 循环切换现在状态
		return states.charAt(currentState);
	}
	
	public char getCommand() {
		return states.charAt(currentState);
	}
	
	public boolean in(char c) {
		return states.contains(String.valueOf(c));
	}
	
	public void alter(int n) {
		if (amount + n >= 0) amount += n;
	}
}
