package com.funge.funge;

import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import java.util.*;
import android.content.*;

public class SpecialKeyboardAdapter extends RecyclerView.Adapter<SpecialKeyboardAdapter.ViewHolder> {
	// 指令输入界面适配器，供Funge取用
	private Context context;
	private OnItemClickListener listener; // 点击监听器
	private List<Key> keyList = new ArrayList<>(); // 所有按键
	private Map<Character, String> tooltipMap; // 长按显示提示
	private int selectedKey = -1; // 选中的按键

	public interface OnItemClickListener { // 公开接口，供Funge实现并调用
        void onItemClick(int pos, char key); // 点击的响应
		// void onItemLongClick(int pos, char key); // 长按的响应，备用
    }

	public SpecialKeyboardAdapter(Context context, List<Key> keyList, OnItemClickListener listener) {
		// 构造函数：构造新LevelAdapter，传入keyList和监听器
		this.context = context;
		this.keyList = keyList;
		this.listener = listener;
		initTooltipMap();
	}

	public void getKeyList(List<Key> keyList) {
		this.keyList = keyList;
	}

	public void updateCommandCount(int pos) {
        if (pos >= 0)
            notifyItemChanged(pos);  // 刷新对应 item
        else {
			notifyDataSetChanged();  // 刷新全部
		}
    }
	
	private void initTooltipMap() {
		tooltipMap = new HashMap<>();
		//tooltipMap.put(' ', context.getString(R.string.cmd_space));
		tooltipMap.put('!', context.getString(R.string.cmd_not));
		//tooltipMap.put('"', context.getString(R.string.cmd_stringmode));
		tooltipMap.put('#', context.getString(R.string.cmd_trampoline));
		tooltipMap.put('$', context.getString(R.string.cmd_pop));
		tooltipMap.put('%', context.getString(R.string.cmd_remainder));
		//tooltipMap.put('&', context.getString(R.string.cmd_input_int));
		tooltipMap.put('\'', context.getString(R.string.cmd_fetch));
		//tooltipMap.put('(', context.getString(R.string.cmd_load_fp));
		//tooltipMap.put(')', context.getString(R.string.cmd_unload_fp));
		tooltipMap.put('*', context.getString(R.string.cmd_mul));
		tooltipMap.put('+', context.getString(R.string.cmd_add));
		//tooltipMap.put(',', context.getString(R.string.cmd_output_char));
		tooltipMap.put('-', context.getString(R.string.cmd_sub));
		//tooltipMap.put('.', context.getString(R.string.cmd_output_int));
		tooltipMap.put('/', context.getString(R.string.cmd_div));
		// 数字 0-9
		for (char c = '0'; c <= '9'; c++) {
			tooltipMap.put(c, String.format(context.getString(R.string.cmd_push_number), c));
		}
		tooltipMap.put(':', context.getString(R.string.cmd_duplicate));
		tooltipMap.put(';', context.getString(R.string.cmd_skip));
		tooltipMap.put('<', context.getString(R.string.cmd_west));
		//tooltipMap.put('=', context.getString(R.string.cmd_execute));
		tooltipMap.put('>', context.getString(R.string.cmd_east));
		tooltipMap.put('?', context.getString(R.string.cmd_random));
		tooltipMap.put('@', context.getString(R.string.cmd_stop));
		//for (char c = 'A'; c <= 'Z'; c++) {
		//	tooltipMap.put(c, context.getString(R.string.cmd_fingerprint));
		//}
		tooltipMap.put('[', context.getString(R.string.cmd_left_turn));
		tooltipMap.put('\\', context.getString(R.string.cmd_swap));
		tooltipMap.put(']', context.getString(R.string.cmd_right_turn));
		tooltipMap.put('^', context.getString(R.string.cmd_north));
		tooltipMap.put('_', context.getString(R.string.cmd_if_horiz));
		tooltipMap.put('`', context.getString(R.string.cmd_greater));
		// a-f
		for (char c = 'a'; c <= 'f'; c++) {
			tooltipMap.put(c, String.format(context.getString(R.string.cmd_push_hex), c));
		}
		tooltipMap.put('g', context.getString(R.string.cmd_get));
		//tooltipMap.put('h', context.getString(R.string.cmd_high));
		//tooltipMap.put('i', context.getString(R.string.cmd_input_file));
		tooltipMap.put('j', context.getString(R.string.cmd_jump));
		tooltipMap.put('k', context.getString(R.string.cmd_iterate));
		//tooltipMap.put('l', context.getString(R.string.cmd_low));
		//tooltipMap.put('m', context.getString(R.string.cmd_if_vert_3d));
		tooltipMap.put('n', context.getString(R.string.cmd_clear_stack));
		//tooltipMap.put('o', context.getString(R.string.cmd_output_file));
		tooltipMap.put('p', context.getString(R.string.cmd_put));
		//tooltipMap.put('q', context.getString(R.string.cmd_quit));
		tooltipMap.put('r', context.getString(R.string.cmd_reflect));
		tooltipMap.put('s', context.getString(R.string.cmd_store));
		tooltipMap.put('t', context.getString(R.string.cmd_split));
		//tooltipMap.put('u', context.getString(R.string.cmd_stack_under));
		tooltipMap.put('v', context.getString(R.string.cmd_south));
		tooltipMap.put('w', context.getString(R.string.cmd_compare));
		tooltipMap.put('x', context.getString(R.string.cmd_abs_delta));
		//tooltipMap.put('y', context.getString(R.string.cmd_sysinfo));
		tooltipMap.put('z', context.getString(R.string.cmd_noop));
		//tooltipMap.put('{', context.getString(R.string.cmd_begin_block));
		tooltipMap.put('|', context.getString(R.string.cmd_if_vert));
		//tooltipMap.put('}', context.getString(R.string.cmd_end_block));
		//tooltipMap.put('~', context.getString(R.string.cmd_input_char));
	}
	
	@Override
	public int getItemCount() {
		return keyList.size();
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
        public Button keyButton;
		public TextView keyAmount;
        public ViewHolder(View keysView) {
            super(keysView);
            keyButton = itemView.findViewById(R.id.key_with_amount); // 位于special_key.xml
			keyAmount = itemView.findViewById(R.id.key_amount);
        }
    }

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		// 根据keyboard.xml创建并缓存ViewHolder，供onBindVuewHolder取用
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.special_key, parent, false);
        return new ViewHolder(view);
    }

	@Override
	public void onBindViewHolder(ViewHolder vH, final int pos) {
		// 从ViewHolder取出第pos个按钮，设置文本和选中状态
		Key currentKey = keyList.get(pos);
		final char cmd = currentKey.getCommand(); // 拿出第pos个按钮的当前状态
		final int amount = currentKey.amount; // 第pos个按钮，即对应key指令的按钮，其数量
		vH.keyButton.setText(String.valueOf(cmd)); // 设置按钮文本
		vH.keyAmount.setText(String.valueOf(amount)); // 设置按钮数量文本
		vH.keyButton.setTooltipText(tooltipMap.getOrDefault(cmd, context.getString(R.string.cmd_unknown))); // 设置气泡提示文本
		vH.keyButton.setSelected(selectedKey == pos); // 是否选中
		vH.keyButton.setEnabled(amount > 0);
		vH.keyButton.setOnClickListener(new View.OnClickListener() {
				// 点击指令按钮触发效果
				@Override
				public void onClick(View v)
				{
					if (selectedKey == pos) { // 已选中
						char nextKey = currentKey.toggle();
						if (listener != null)
							listener.onItemClick(pos, nextKey);
					} else {
						notifyItemChanged(selectedKey); // 取消原选中按键的高亮
						selectedKey = pos; // 设为选中
						if (listener != null)
							listener.onItemClick(pos, cmd);				
					}
					notifyItemChanged(pos); // 更新选中按键状态
				}	
			});
		
//		vH.keyButton.setOnLongClickListener(new View.OnLongClickListener() {
//				@Override
//				public boolean onLongClick(View v) {
//					// 长按逻辑，备用
//					char cmd = keyList.get(pos).getCommand();
//						return true;
//				}
//		});
		
	}
		
}
