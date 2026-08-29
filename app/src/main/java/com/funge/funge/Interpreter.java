package com.funge.funge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Interpreter{
    public enum Status { RUNNING, CLEAR, FAILED, PAUSED }

    private int ROW = 12;
    private int COLUMN = 16;
    private boolean SGML = true;
    private char command = ' ';
    private Random random = new Random(); // ?指令

    private Map<XYZ, Character> code = new HashMap<>(); // 代码

    private Map<XYZ, Character> obstacle = new HashMap<>(); // 地图障碍
    private LevelData currentLevel = null; // 当前关卡
    private List<Key> keyList = new ArrayList<>(); // 按键表，用于修改按键数

    private List<IP> ips = new ArrayList<>(); // 指针列表
    private IP cip = null; // 当前指针
    private IP cloneIP = null; // t指令分裂出的ip
    private int nextuid = 0;

    // 回调接口
    public interface Callback {
        void onCommandPlaced(List<Key> keyList, int pos);

        void onError(String msg); // 代替 Toast
    }
    private Callback callback;

    // getter&setter
    public void loadIP(int[][] xys) {
        if (xys == null) return;
        for (int[] xy : xys) {
            if (xy == null || xy.length < 2) continue; // 跳过无效
            int x = xy[0];
            int y = xy[1];
            // 边界检查
            if (x < 0 || x >= ROW || y < 0 || y >= COLUMN) {
                continue;
            }
            ips.add(new IP(nextuid++, x, y, 1, 0));
        }
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

    // 辅助函数
    public void clear() {
        currentLevel.toKeyList(); // 重置关卡
        ips.clear();
        loadIP(currentLevel.ip); // 重置指针
        List<XYZ> placedKeys = new ArrayList<>(this.code.keySet());
        for (XYZ xyz : placedKeys) {
            recover(code.remove(xyz));
        } // 回收所有指令以重置键盘
    }

    public void restart() {
        ips.clear();
        loadIP(currentLevel.ip); // 重置指针
        obstacle = new HashMap<>(stringToCode(currentLevel.map)); // 重置障碍物
        List<XYZ> placedKeys = new ArrayList<>(this.code.keySet());
        for (XYZ xyz : placedKeys) { // 回收被障碍物占据的指令
            if (this.obstacle.containsKey(xyz)) {
                recover(code.remove(xyz));
            }
        }
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

    // Befunge解释器部分
    // 栈与指针
    private int pop() {
        return cip.stack.isEmpty() ? 0 : cip.stack.remove(cip.stack.size() - 1);
    }

    private void push(int val) {
        cip.stack.add(val);
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
            callback.onError("Cannot place here");
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
        callback.onCommandPlaced(keyList, pos);
    }

    // 收回被替换的指令
    private void recover(char old_c) {
        if (old_c != ' ') {
            int old_pos = alter(old_c);
            if (old_pos >= 0) {
                keyList.get(old_pos).alter(1);
                callback.onCommandPlaced(keyList, old_pos);
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
        callback.onCommandPlaced(keyList, old_pos);
    }

    // 辅助函数，由字符找按键
    public int alter(char c) {
        if (keyList == null) {
            return -1;
        }
        for (int pos = 0; pos < keyList.size(); pos++) {
            Key key = keyList.get(pos);
            if (key.in(c)) {
                return pos;
            }
        }
        return -1;
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
            callback.onError("死循环");
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
                push(c);
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
                push(ch);
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

    public Status tick() {
        char c;
        for (int i = 0; i < ips.size(); i++) {
            cip = ips.get(i);
            if (cip.uid >= 0) {
                c = getCommand(cip.x, cip.y);
                if (!cip.stringMode && c == 'z')
                    return Status.FAILED;
                execute(c);
            }
            if (cip.uid < 0) {
                ips.remove(cip);
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
        if (ips == null || ips.isEmpty())
            return Status.CLEAR;
        return Status.RUNNING;
    }
}