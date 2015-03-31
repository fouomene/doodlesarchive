/**
 * Created by FOUOMENE on 15/03/2015. EmailAuthor: fouomenedaniel@gmail.com .
 */
package com.fouomene.doodlesarchive.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fouomene.doodlesarchive.app.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Utility {
    private static final String LOG_TAG = Utility.class.getSimpleName();

    public static String getPreferredYear(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_year_key),
                context.getString(R.string.pref_year_default));
    }

    public static String getPreferredMonth(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_month_key),
                context.getString(R.string.pref_month_default));
    }


    public static void setPreferredYear(Context context, String year) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.pref_year_key), year);
        editor.commit();
    }

    public static void setPreferredMonth(Context context, String month) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.pref_month_key) ,month);
        editor.commit();
    }




    public static String formatDate(String date) {

        String[] dateArray = date.split("/");

        try {
            Calendar cal = new GregorianCalendar(Integer.parseInt(dateArray[0]),Integer.parseInt(dateArray[1])-1,Integer.parseInt(dateArray[2]));
            return new SimpleDateFormat("MMMM dd, yyyy").format(cal.getTime());
        } catch(NumberFormatException nfe) {
            Log.e(LOG_TAG,"Could not parse " + nfe);
        }
        return  date;
    }



}