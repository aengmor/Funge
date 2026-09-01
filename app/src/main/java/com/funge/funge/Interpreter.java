package com.funge.funge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Interpreter{
    private int[] keyAmounts; // 初始按键数表，用于清屏时恢复按键数

    private int ROW = 12;
    private int COLUMN = 16;
    private boolean SGML = true;
    private Random random = new Random(); // ?指令

    private Map<XYZ, Character> code = new HashMap<>(); // 代码
    private Map<XYZ, Character> obstacle = new HashMap<>(); // 地图障碍
    private LevelData currentLevel = null; // 当前关卡
    private List<Key> keyList = new ArrayList<>(); // 按键表，用于修改按键数
    private Deque<HistorySnapshot> history = new ArrayDeque<>();

    private List<IP> ips = new ArrayList<>(); // 指针列表
    private IP cip = null; // 当前指针
    private IP cloneIP = null; // t指令分裂出的ip
    private int nextuid = 0;
    private int cycleCount = 0; // 时间
    private Callback callback;

    // getter & setter
    public String getStack() {
        StringBuilder stackOutput = new StringBuilder();
        if (cip == null)
            return "noCIP";
        stackOutput.append(cip.uid).append(":");
        if (ips == null || ips.isEmpty())
            return "noIP";
        for (Integer num : cip.stack) {
            stackOutput.append(num).append("|");
        }
        return stackOutput.toString();
    }

    private void move(IP cip) {
        char p = getCommand(cip.x, cip.y);
        int flipCount = 0;
        if (cip.stringMode) {
            if (p != ' ') {
                wrap(cip);
                return;
            }
            while (SGML && p == ' ') {
                flipCount += wrap(cip) ? 1 : 0;
                p = getCommand(cip.x, cip.y);
                if (flipCount == 1000) {
                    restart();
                    callback.onError("死循环");
                    return;
                }
            }
            return;
        }

        wrap(cip);
        while (p == ';') {
            do {
                flipCount += wrap(cip) ? 1 : 0;
                p = getCommand(cip.x, cip.y);
                if (flipCount == 1000) {
                    restart();
                    callback.onError("死循环");
                    return;
                }
            } while (p != ';');
            p = getCommand(cip.x, cip.y);
        }
    }

    // 单步执行
    public void execute(IP cip, char c) {
        // 字符串模式
        if (cip.stringMode) {
            if (c == '"') {
                cip.stringMode = false;
            } else {
                cip.push(c);
            }
            return;
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
                cip.push(c - '0');
                break;
            case 'a' :
            case 'b' :
            case 'c' :
            case 'd' :
            case 'e' :
            case 'f' :
                cip.push(c - 'W');
                break;

            // 栈中数运算
            case '+' :
                cip.push(cip.pop() + cip.pop());
                break;
            case '-' : {
                int a = cip.pop();
                int b = cip.pop();
                cip.push(b - a);
            }
            break;
            case '*' :
                cip.push(cip.pop() * cip.pop());
                break;
            case '/' : {
                int a = cip.pop();
                int b = cip.pop();
                cip.push(a == 0 ? 0 : b / a);
            }
            break;
            case '%' : {
                int a = cip.pop();
                int b = cip.pop();
                cip.push(a == 0 ? 0 : b % a);
            }
            break;
            case '!' :
                cip.push(cip.pop() == 0 ? 1 : 0);
                break;
            case '`' : {
                int a = cip.pop();
                int b = cip.pop();
                cip.push(b > a ? 1 : 0);
            }
            break;

            // 其他栈操作
            case ':' : {
                int top = cip.pop();
                cip.push(top);
                cip.push(top);
            }
            break;
            case '\\' : {
                int a = cip.pop();
                int b = cip.pop();
                cip.push(a);
                cip.push(b);
            }
            break;
            case '$' :
                cip.pop();
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
                cip.dy = cip.pop();
                cip.dx = cip.pop();
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
                cip.dx = (cip.pop() == 0 ? 1 : -1);
                cip.dy = 0;
                break;
            case '|' :
                cip.dy = (cip.pop() == 0 ? 1 : -1);
                cip.dx = 0;
                break;
            case 'w' : {
                int a = cip.pop();
                int b = cip.pop();
                if (a > b)
                    cip.turnRight();
                else if (b > a)
                    cip.turnLeft();
                break;
            }

            // 指针移位
            case '#' :
                move(cip);
                break;
            case 'j' : {
                int a = cip.pop();
                if (a > 0)
                    for (int i = 0; i < a; i = i + 1)
                        move(cip);
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
                move(cip);
                cip.push(getCommand(cip.x, cip.y));
                break;
            case ' ' :
                break;
            case 'p' : {
                int yVal = cip.pop();
                int xVal = cip.pop();
                int v = cip.pop();
                setObstacle(xVal, yVal, (char) v);
            }
            break;
            case 'g' : {
                int yVal = cip.pop();
                int xVal = cip.pop();
                char ch = getCommand(xVal, yVal);
                cip.push(ch);
            }
            break;
            case 's' : {
                move(cip);
                int v = cip.pop();
                setObstacle(cip.x, cip.y, (char) v);
                break;
            }
            case 't' : {
                cloneIP = new IP(nextuid++, cip.x, cip.y, -cip.dx, -cip.dy);
                return;
            }
            case 'z' :
                return;
            case 'k' : {
                int n = cip.pop(); // 执行次数
                if (n <= 0) {
                    move(cip);
                    break;
                }
                int tx = cip.x;
                int ty = cip.y;
                move(cip);
                char next = getCommand(cip.x, cip.y);
                while (next == ' ' || next == ';') {
                    if (next == ';') {
                        move(cip);
                        while (getCommand(cip.x, cip.y) != ';')
                            move(cip);
                        move(cip);
                    } else {
                        move(cip);
                    }
                    next = getCommand(cip.x, cip.y);
                }
                cip.x = tx;
                cip.y = ty;
                for (int i = 0; i < n; i++) {
                    execute(cip, next);
                }
            }
            break;
            case '"' :
                cip.stringMode = true;
                break;
            //            case '.': System.out.print(cip.pop() + " "); break;
            //            case ',': System.out.print((char) cip.pop()); break;

            // 时间旅行
            case 'G': // EX_TRDS+02 获取当前时间
                cip.push(cycleCount);
                break;
            case 'D': // EX_TRDS+03 设定绝对空间跳跃目标
                cip.SpMode = 'A';
                cip.TSpY = cip.pop();
                cip.TSpX = cip.pop();
                break;
            case 'E': // EX_TRDS+05 设定相对空间跳跃目标
                cip.SpMode = 'R';
                cip.TSpY = cip.pop();
                cip.TSpX = cip.pop();
                break;
            case 'V': // EX_TRDS+06 设定目标方向向量
                cip.VMode = 'Y';
                cip.TvY = cip.pop();
                cip.TvX = cip.pop();
                break;
            case 'J': // EX_TRDS+04 执行时空跳跃!
                // 记录返回点
                cip.RTSpX = cip.x; cip.RTSpY = cip.y;
                cip.RTvX = cip.dx; cip.RTvY = cip.dy;
                cip.RTTime = cycleCount;
                // 空间跳跃
                if (cip.SpMode == 'A') { cip.x = cip.TSpX; cip.y = cip.TSpY; }
                else if (cip.SpMode == 'R') { cip.x += cip.TSpX; cip.y += cip.TSpY; }
                // 方向改变
                if (cip.VMode == 'Y') { cip.dx = cip.TvX; cip.dy = cip.TvY; }
                // 时间跳跃
                int targetTime = cip.TTime;
                if (cip.TMode == 'R') targetTime = cycleCount + cip.TTime; // 相对时间
                if (cip.TMode != 'D') {
                    if (targetTime >= cycleCount) { // 去未来
                    } else { // 去过去
                        travelToPast(cip, targetTime);
                        return; // 中断当前 tick
                    }
                }
                break;
            case 'T': // EX_TRDS+09 设定绝对时间目标
                cip.TMode = 'A';
                cip.TTime = cip.pop();
                break;
            case 'U': // EX_TRDS+10 设定相对时间目标
                cip.TMode = 'R';
                cip.TTime = cip.pop();
                break;
            case 'I': // EX_TRDS+11 跳回存档点
                cip.SpMode = 'A'; cip.VMode = 'Y'; cip.TMode = 'A';
                cip.TSpX = cip.RTSpX; cip.TSpY = cip.RTSpY;
                cip.TvX = cip.RTvX; cip.TvY = cip.RTvY;
                cip.TTime = cip.RTTime;
                break;

            default :
                break;
        }
    }

    public Status tick() { // 单步执行
        saveHistory();     // 保存历史快照
        char c;
        for (int i = 0; i < ips.size(); i++) {
            cip = ips.get(i);
            if (cip.uid >= 0) {
                c = getCommand(cip.x, cip.y);
                if (!cip.stringMode && c == 'z')
                    return Status.FAILED;
                execute(cip, c);
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
            move(ip);
        }
        if (ips == null || ips.isEmpty())
            return Status.CLEAR;
        cycleCount++;      // 时间 +1
        return Status.RUNNING;
    }

    public void setLevel(LevelData level) {
        // 接收Funge传来的关卡数据
        currentLevel = level;

        // 初始化关卡
        ROW = currentLevel.w;
        COLUMN = currentLevel.h;
        obstacle = stringToCode(currentLevel.map); // 加载障碍物
        keyList = new ArrayList<>(currentLevel.keyList);
        keyAmounts = copyKeyAmount();
        loadIP(currentLevel.ip);
    }

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

    // 辅助方法
    // 深拷贝 IP 列表
    private List<IP> deepCopyIPs(List<IP> original) {
        List<IP> copy = new ArrayList<>();
        for (IP ip : original) {
            IP newIp = new IP(ip.uid, ip.x, ip.y, ip.dx, ip.dy);
            newIp.stack = new ArrayDeque<>(ip.stack);
            newIp.stringMode = ip.stringMode;
            copy.add(newIp);
        }
        return copy;
    }

    public enum Status { RUNNING, CLEAR, FAILED, PAUSED } // 游戏状态，供解释器处理

    // 回调接口，在Funge实现
    public interface Callback {
        void onCommandPlaced(List<Key> keyList, int pos); // 放置、收回指令时键盘响应，与keyboardAdapter交互，通知它改UI

        void onError(String msg); // 代替 Toast 传递消息
    }

    // Befunge解释器部分

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

    // 时间旅行函数
    // 每一 tick 结束时保存快照
    private void saveHistory() {
        HistorySnapshot snap = new HistorySnapshot();
        snap.codeCopy = new HashMap<>(code);
        snap.ipsCopy = deepCopyIPs(ips); // 深拷贝当前所有 IP
        snap.keyAmountsCopy = copyKeyAmount();
        snap.time = cycleCount;
        history.push(snap);

        if (history.size() > 1000) history.poll(); // 防止内存溢出，只保留最近 1000 个时间点
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

    // 回到过去
    private void travelToPast(IP traveler, int targetTime) {
        if (targetTime < 0) targetTime = 0; // 时间起点
        // 时间悖论检测：如果与上次跳跃完全相同，抹杀 IP 防止死循环
        if (traveler.lastJumpX == traveler.x &&
                traveler.lastJumpY == traveler.y &&
                traveler.lastTargetTime == targetTime) {
            traveler.uid = -1; // 抹杀
            return;
        }
        traveler.lastJumpX = traveler.x;
        traveler.lastJumpY = traveler.y;
        traveler.lastTargetTime = targetTime;
        // 获取历史快照
        HistorySnapshot pastSnap = null;
        for (HistorySnapshot snap : history) { // 遍历查找目标时间
            if (snap.time <= targetTime) {
                pastSnap = snap;
                break;
            }
        }
        if (pastSnap == null) return;
        // 时空重置
        this.code = new HashMap<>(pastSnap.codeCopy);
        // 插入时间旅行者
        this.ips = deepCopyIPs(pastSnap.ipsCopy);
        this.ips.add(traveler); // 旅行者降临在过去的时间线
        setKeyAmount(pastSnap.keyAmountsCopy);
        this.cycleCount = pastSnap.time;
    }

    // 历史快照，用于时间旅行
    private static class HistorySnapshot {
        Map<XYZ, Character> codeCopy;
        List<IP> ipsCopy;
        int[] keyAmountsCopy;
        int time;
    }

    // 拷贝指令数量
    private int[] copyKeyAmount() {
        int[] amountsCopy = new int[keyList.size()];
        for (int i = 0; i < keyList.size(); i++) {
            amountsCopy[i] = keyList.get(i).amount;
        }
        return amountsCopy;
    }

    // 恢复指令数量
    public void setKeyAmount(int[] amountsCopy) {
        if (amountsCopy != null) {
            for (int i = 0; i < keyList.size() && i < amountsCopy.length; i++) {
                keyList.get(i).setAmount(amountsCopy[i]);
            }
            // 通知 UI 全部刷新
            callback.onCommandPlaced(this.keyList, -1);
        }
    }

    // 撤回一步
    public void undo() {
        if (!history.isEmpty()) {
            HistorySnapshot snap = history.pop();
            code = new HashMap<>(snap.codeCopy);
            ips = deepCopyIPs(snap.ipsCopy);
            setKeyAmount(snap.keyAmountsCopy);
            cycleCount = snap.time; //  this.cycleCount--
        }
    }

    public void clear() {
        currentLevel.toKeyList(); // 重置关卡
        ips.clear();
        loadIP(currentLevel.ip); // 重置指针
        code = new HashMap<>(); // 清除指令
        setKeyAmount(keyAmounts); // 重置按键数
        cycleCount = 0;
    }

    public void restart() {
        ips.clear();
        loadIP(currentLevel.ip); // 重置指针
        obstacle = new HashMap<>(stringToCode(currentLevel.map)); // 重置障碍物
        List<XYZ> placedKeys = new ArrayList<>(this.code.keySet());
        for (XYZ xyz : placedKeys) { // 回收被障碍物占据的指令
            if (obstacle.containsKey(xyz)) {
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
}