package com.funge.funge;
import java.io.*;
import java.util.*;

public class LevelData implements Serializable {
		// 关卡数据类
		// 用途：LevelAdapter，接收关卡列表；Level，载入关卡数据
	private static final long serialVersionUID = 1L;
	int id = 0, w = -1, h = -1;
	int[][] ip = {{0, 0}};
	String name = "";
    String[] map = {" "};	
	Map<Character, Integer> key = new HashMap<>();
	List<Key> keyList;
	
	public void toKeyList() {
		// 手动"构造函数"，从levels.json读取的keys转换为keyList，并计算地图大小
		keyList = new ArrayList<>();
		for ( Map.Entry<Character, Integer> entry : key.entrySet()) {
            char c = entry.getKey();    // 如 ">"
			String keysToAdd = createKeyState(c);
            int amount = entry.getValue();// 如 50
            keyList.add(new Key(keysToAdd, amount));
			}
		if (h == -1) h = map.length;
		if (w == -1) w = Arrays.stream(map).mapToInt(String::length).max().orElse(0);
        }
		
	public String createKeyState(char c) {
    	// 按照按钮文本，创建按钮状态组
		String keysToAdd = "";
		// 每个按键的状态列表，给予keyStates
        switch (c) {
            case '<':
            case '^':
            case '>':
            case 'v':
                // 方向键组：统一使用循环顺序 < ^ > v
                keysToAdd = "<^>v";
                break;
            case '0':
                // 十六进制数组：0-9 a-f
                keysToAdd = "0123456789abcdef";
                break;
			case '+': // 运算 +-*/%!`
				keysToAdd = "+-*/%!`";
				break;
			case '[': case ']':
				keysToAdd = "[]";
				break;
            default:
				keysToAdd = String.valueOf(c);
                // 其他按键只有自身，不可切换
                
                break;
        }
		return keysToAdd;
	}
}
	
	
	
