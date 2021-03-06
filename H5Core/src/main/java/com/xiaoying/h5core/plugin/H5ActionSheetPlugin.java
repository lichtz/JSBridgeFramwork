package com.xiaoying.h5core.plugin;

import com.xiaoying.h5api.api.H5Intent;
import com.xiaoying.h5api.api.H5IntentFilter;
import com.xiaoying.h5api.api.H5Page;
import com.xiaoying.h5api.api.H5Plugin;
import com.xiaoying.h5api.util.H5Log;
import com.xiaoying.h5api.util.H5Utils;
import com.xiaoying.h5core.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class H5ActionSheetPlugin implements H5Plugin {

    public static final String TAG = "H5ActionSheetPlugin";
    private H5Page h5Page;
    private PopupWindow popupWindow;

    public H5ActionSheetPlugin(H5Page page) {
        this.h5Page = page;
    }

    @Override
    public void onRelease() {
        h5Page = null;
        hide();
        popupWindow = null;
    }

    @Override
    public void getFilter(H5IntentFilter filter) {
        filter.addAction(ACTION_SHEET);
        filter.addAction(H5_PAGE_PHYSICAL_BACK);
    }

    @Override
    public boolean handleIntent(H5Intent intent) {
        String action = intent.getAction();
        if (ACTION_SHEET.equals(action)) {
            hide();
            show(intent);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean interceptIntent(H5Intent intent) {
        String action = intent.getAction();
        if (H5_PAGE_PHYSICAL_BACK.equals(action)) {
            if (hide()) {
                return true;
            }
        }
        return false;
    }

    private boolean hide() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            return true;
        }
        return false;
    }

    private void show(final H5Intent h5Intent) {
        JSONObject params = h5Intent.getParam();
        Context context = h5Page.getContext().getContext();

        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup contentView = (ViewGroup) inflater.inflate(
                R.layout.h5_action_sheet, null, false);

        String title = H5Utils.getString(params, "title");
        String cancelBtn = H5Utils.getString(params, "cancelBtn");
        JSONArray jaButtons = H5Utils.getJSONArray(params, "btns", null);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = H5Utils.dip2px(13);
        lp.setMargins(margin, 0, margin, margin);

        OnClickListener outsideClick = new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                h5Intent.sendBack("index", -1);
            }
        };

        View rlSheet = contentView.findViewById(R.id.rl_h5_action_sheet);
        LinearLayout llContent = (LinearLayout) contentView
                .findViewById(R.id.h5_action_sheet_content);

        rlSheet.setOnClickListener(outsideClick);
        llContent.setOnClickListener(outsideClick);

        TextView tvTitle = (TextView) contentView
                .findViewById(R.id.h5_action_sheet_title);

        if (!TextUtils.isEmpty(title)) {
            tvTitle.setText(title);
            tvTitle.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setVisibility(View.GONE);
        }

        OnClickListener clickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                int index = (Integer) v.getTag();
                h5Intent.sendBack("index", index);
                popupWindow.dismiss();
            }
        };

        int viewIndex = 0;
        try {
            if (jaButtons != null && jaButtons.length() != 0) {
                for (int index = 0; index < jaButtons.length(); index++) {
                    String otherButton = (String) jaButtons.get(index);
                    H5Log.d(TAG, "otherButton =" + otherButton);
                    Button Button = (Button) inflater.inflate(
                            R.layout.h5_action_sheet_cancel, null);
                    Button.setText(otherButton);
                    Button.setTag(viewIndex);
                    Button.setOnClickListener(clickListener);
                    Button.setLayoutParams(lp);
                    llContent.addView(Button, ++viewIndex);
                }
            }
        } catch (JSONException e) {
            H5Log.e(TAG, "exception", e);
        }

        if (!TextUtils.isEmpty(cancelBtn)) {
            Button cancelButton = (Button) inflater.inflate(
                    R.layout.h5_action_sheet_cancel, null);
            cancelButton.setText(cancelBtn);
            cancelButton.setTag(viewIndex);
            cancelButton.setOnClickListener(clickListener);
            cancelButton.setLayoutParams(lp);
            llContent.addView(cancelButton, ++viewIndex);
        }

        popupWindow = new PopupWindow(contentView, LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT, false);

        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAtLocation(contentView, Gravity.BOTTOM, 0, 0);

    }
}
