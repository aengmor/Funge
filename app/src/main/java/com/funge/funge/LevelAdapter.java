package com.funge.funge;

import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import java.util.*;

public class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.ViewHolder> {
	// 选关界面适配器
	private List<LevelData> levelList = new ArrayList<>();
	private OnItemClickListener listener;
	
	public interface OnItemClickListener { // 公开接口，等调用了再去实现
        void onItemClick(int pos);
    }
	
	public LevelAdapter(List<LevelData> levelList, OnItemClickListener listener) {
		// 构造函数：构造新LevelAdapter，传入levelList和监听器
		this.levelList = levelList;
		this.listener = listener;
	}

	public void getLevelList(List<LevelData> levelList) {
        this.levelList = levelList;
        notifyDataSetChanged();
    }
	
	@Override
	public int getItemCount() {
		return levelList == null ? 0 : levelList.size();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		// 创建并缓存ViewHolder，供onBindViewHolder取用
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.key, parent, false);
        return new ViewHolder(view);
    }

	@Override
	public void onBindViewHolder(ViewHolder vH, final int pos) {
		// 从ViewHolder取出第pos个按钮，其文本设为pos
			vH.levelButton.setText(String.valueOf(pos));
			vH.levelButton.setOnClickListener(new View.OnClickListener() {
				// 点击levelButton触发效果
				@Override
				public void onClick(View v)
				{
					if (listener != null)
						listener.onItemClick(pos);
				}	
	});}
	
	public static class ViewHolder extends RecyclerView.ViewHolder {
        public Button levelButton;
        public ViewHolder(View levelsView) {
            super(levelsView);
            levelButton = itemView.findViewById(R.id.key);
        }
    }
	
}
