package com.wish.wishlist.util;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateTimeFormatter {

    private static DateTimeFormatter instance = null;

    public static DateTimeFormatter getInstance() {
        if (instance == null){
            instance = new DateTimeFormatter();
        }
        return instance;
    }

    public String getDateTimeString(String dateTimeStr) {
        // Format the date_time and save it as a string
        int mYear = -1;
        int mMonth = -1;
        int mDay = -1;
        int mHour = 0;
        int mMin = 0;
        int mSec = 0;

        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);//24 hour format
        mMin = c.get(Calendar.MINUTE);
        mSec = c.get(Calendar.SECOND);

        String dateTimeStrNew = null;

        Date dateTimeNow = new Date(mYear - 1900, mMonth, mDay, mHour, mMin, mSec);
        SimpleDateFormat sdfFrom = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date dateTimeRecorded;
        try {
            dateTimeRecorded = sdfFrom.parse(dateTimeStr);
            long diffmSecs = dateTimeNow.getTime() - dateTimeRecorded.getTime();
            long diffHours = diffmSecs / (60 * 60 * 1000);
            long diffMins =  diffmSecs / (60 * 1000);

            if (diffHours >= 24) {
                SimpleDateFormat sdfTo = new SimpleDateFormat("MMM dd, yyyy");
                dateTimeStrNew = sdfTo.format(dateTimeRecorded);
            }
            else if (diffHours > 1) {
                dateTimeStrNew = Long.toString(diffHours) + " hours ago";
            }
            else if (diffHours == 1) {
                dateTimeStrNew = Long.toString(diffHours) + " hour ago";
            }
            else if (diffMins > 1) {
                dateTimeStrNew = Long.toString(diffMins) + " minutes ago";
            }
            else {
                dateTimeStrNew = "1 minute ago";
            }

        } catch (java.text.ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return dateTimeStrNew;
    }
}

