package com.example.rob.socialmusic.onboarding;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.example.rob.socialmusic.R;
import java.util.ArrayList;

class OnBoard_Adapter extends PagerAdapter {
    private Context mContext;
    ArrayList<OnBoardItem> onBoardItems;

    OnBoard_Adapter(Context mContext, ArrayList<OnBoardItem> items) {
        this.mContext = mContext;
        this.onBoardItems = items;
    } // End OnBoard_Adapter

    @Override
    public int getCount() {
        return onBoardItems.size();
    }
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.onboard_item, container, false);
        ImageView imageView = itemView.findViewById(R.id.iv_onboard);
        TextView tv_title= itemView.findViewById(R.id.tv_header);
        TextView tv_content= itemView.findViewById(R.id.tv_desc);

        OnBoardItem item = onBoardItems.get(position);

        imageView.setImageResource(item.getImageID());
        tv_title.setText(item.getTitle());
        tv_content.setText(item.getDescription());

        container.addView(itemView);
        return itemView;
    } // End instantiateItem

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // Previously Linear Layout, crashing on onBoarding
        container.removeView((RelativeLayout) object);
    } // End destroyItem
} // End class OnBoard_Adapter
