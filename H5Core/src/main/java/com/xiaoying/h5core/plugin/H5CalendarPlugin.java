package com.xiaoying.h5core.plugin;

import com.xiaoying.h5api.api.H5Intent;
import com.xiaoying.h5api.api.H5IntentFilter;
import com.xiaoying.h5api.api.H5Plugin;
import com.xiaoying.h5api.util.H5Environment;
import com.xiaoying.h5api.util.H5Log;
import com.xiaoying.h5api.util.H5Utils;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class H5CalendarPlugin implements H5Plugin {

    private static final String TAG = "H5CalendarPlugin";

    private static final String SYS_ERROR = "10";
    private static final String NO_PERMISSION = "11";
    private static final String CAL_ERROR = "12";
    private static final String INVALID_PARAM = "13";

    @Override
    public void onRelease() {

    }

    @Override
    public void getFilter(H5IntentFilter filter) {
        filter.addAction(ADD_EVENT_CAL);
    }

    @Override
    public boolean interceptIntent(H5Intent intent) {
        return false;
    }

    @Override
    public boolean handleIntent(H5Intent intent) {
        JSONObject param = intent.getParam();
        String code = addEventCal(param);

        JSONObject result = new JSONObject();
        try {
            result.put("error", code);
        } catch (JSONException e) {
            H5Log.e(TAG, "exception", e);
        }
        intent.sendBack(result);

        return true;
    }

    @SuppressLint("SimpleDateFormat")
    private String addEventCal(JSONObject param) {
        String title = H5Utils.getString(param, "title");
        String startTime = H5Utils.getString(param, "startDate");
        String endTime = H5Utils.getString(param, "endDate");
        String location = H5Utils.getString(param, "location");
        String notes = H5Utils.getString(param, "notes");
        int alarmOffset = H5Utils.getInt(param, "alarmOffset");
        int recurrenceTimes = H5Utils.getInt(param, "recurrenceTimes");
        String frequency = H5Utils.getString(param, "frequency");

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(startTime)
                || TextUtils.isEmpty(endTime)) {
            return INVALID_PARAM;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setLenient(false);
            ParsePosition startPos = new ParsePosition(0);
            ParsePosition endPos = new ParsePosition(0);
            Date startDate = sdf.parse(startTime, startPos);
            Date endDate = sdf.parse(endTime, endPos);
            if (!isDate(startDate, startPos, startTime)) {
                return INVALID_PARAM;
            }
            if (!isDate(endDate, endPos, endTime)) {
                return INVALID_PARAM;
            }

            return addEvent(title, startDate.getTime(), endDate.getTime(),
                    location, notes, alarmOffset, recurrenceTimes, frequency);

        } catch (Exception e) {
            e.printStackTrace();
            return INVALID_PARAM;
        }
    }

    private boolean isDate(Date date, ParsePosition pos, String dttm) {
        if (date == null || pos.getErrorIndex() > 0) {
            return false;
        }

        if (pos.getIndex() != dttm.length()) {
            return false;
        }

        return true;
    }

    public String addEvent(String title, long startDate, long endDate,
                           String location, String notes, int alarmOffset,
                           int recurrenceTimes, String frequency) {
        H5Log.d("calander  title=" + title + " startDate=" + startDate
                + " endDate=" + endDate + " location=" + location + " notes="
                + notes + " alarmOffset=" + alarmOffset + " recurrenceTimes="
                + recurrenceTimes + " frequency=" + frequency);

        Context context = H5Environment.getContext();
        String calanderURL = "";
        String calanderEventURL = "";
        String calanderRemiderURL = "";
        if (Build.VERSION.SDK_INT >= 8) {
            calanderURL = "content://com.android.calendar/calendars";
            calanderEventURL = "content://com.android.calendar/events";
            calanderRemiderURL = "content://com.android.calendar/reminders";
        } else {
            calanderURL = "content://calendar/calendars";
            calanderEventURL = "content://calendar/events";
            calanderRemiderURL = "content://calendar/reminders";
        }
        double calId = 0;
        try {
            Cursor c = context.getContentResolver().query(
                    Uri.parse(calanderURL), null, null, null, null);
            if (c.moveToFirst()) {
                calId = c.getDouble(c.getColumnIndex("_id"));
            } else {
                ContentValues cValues = new ContentValues();
                cValues.put("_id", calId);
                cValues.put("name", "JSH5");
                context.getContentResolver().insert(Uri.parse(calanderURL),
                        cValues);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            return NO_PERMISSION;
        } catch (Exception e) {
            e.printStackTrace();
            return SYS_ERROR;
        }
        ContentValues eValues = new ContentValues();
        eValues.put("dtstart", startDate);
        eValues.put("dtend", endDate);
        eValues.put("title", title);
        eValues.put("description", notes);
        eValues.put("calendar_id", calId);
        eValues.put("hasAlarm", 1);
        eValues.put("eventTimezone", "GMT+8");
        eValues.put("eventLocation", location);

        if (null != frequency && !"".equals(frequency)) {
            String freq = null;
            int count = 1;
            if (recurrenceTimes != 0) {
                count = recurrenceTimes;
            }
            // year、 month、week、day
            if ("year".equals(frequency)) {
                freq = "YEARLY";
            } else if ("month".equals(frequency)) {
                freq = "MONTHLY";
            } else if ("week".equals(frequency)) {
                freq = "WEEKLY";
            } else if ("day".equals(frequency)) {
                freq = "DAILY";
            }
            String rule = "FREQ=" + freq + ";COUNT=" + count + ";";
            eValues.put("rrule", rule);
        }
        String myEventsId = "";
        try {
            Uri uri = context.getContentResolver().insert(
                    Uri.parse(calanderEventURL), eValues);
            myEventsId = uri.getLastPathSegment(); // 得到当前表的_id
        } catch (SecurityException e) {
            e.printStackTrace();
            return NO_PERMISSION;
        } catch (Exception e) {
            e.printStackTrace();
            return CAL_ERROR;
        }
        ContentValues rValues = new ContentValues();
        rValues.put("event_id", myEventsId);
        rValues.put("minutes", alarmOffset);
        rValues.put("method", 1);
        try {
            context.getContentResolver().insert(Uri.parse(calanderRemiderURL),
                    rValues);
        } catch (SecurityException e) {
            e.printStackTrace();
            return NO_PERMISSION;
        } catch (Exception e) {
            e.printStackTrace();
            return CAL_ERROR;
        }

        H5Log.d("calander  insert ok");
        return "";
    }
}
