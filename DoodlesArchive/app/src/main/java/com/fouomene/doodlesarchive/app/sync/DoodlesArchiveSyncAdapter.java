package com.fouomene.doodlesarchive.app.sync;

/**
 * Created by FOUOMENE on 19/03/2015. EmailAuthor: fouomenedaniel@gmail.com .
 */
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.fouomene.doodlesarchive.app.MainActivity;
import com.fouomene.doodlesarchive.app.R;
import com.fouomene.doodlesarchive.app.data.DoodleContract;
import com.fouomene.doodlesarchive.app.utils.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Vector;

public class DoodlesArchiveSyncAdapter extends AbstractThreadedSyncAdapter {

    public final String LOG_TAG = DoodlesArchiveSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the doodle, in seconds.
    // 60 seconds (1 minute) * 720 = 12 hours
    public static final int SYNC_INTERVAL = 60 * 720;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;


    private static final String[] NOTIFY_DOODLE_PROJECTION = new String[] {
            DoodleContract.DoodleEntry.TABLE_NAME + "."+DoodleContract.DoodleEntry._ID,
            DoodleContract.DoodleEntry.COLUMN_TITLE,
            DoodleContract.DoodleEntry.COLUMN_RUN_DATE
    };

    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int DOODLE_NOTIFICATION_ID = 3004;

    // these indices must match the projection
    private static final int INDEX_ID = 0;
    private static final int INDEX_TITLE = 1;
    private static final int INDEX_RUN_DATE = 2;

    public DoodlesArchiveSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");

        String yearQuery  = Utility.getPreferredYear(getContext());
        String monthQuery = Utility.getPreferredMonth(getContext());

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String doodlesJsonStr = null;


        try {
            // Construct the URL for the Doodles Archive query
            // http://www.google.com/doodles/json/2015/3
            Uri builtUri  = new Uri.Builder()
                    .scheme("http")
                    .authority("google.com")
                    .path("doodles/json/" + yearQuery + "/" + monthQuery)
                    .build();

            Log.d(LOG_TAG, "URI = "+ builtUri.toString());

            URL url = new URL(builtUri.toString());

            // Create the request to Doodles Archive, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            doodlesJsonStr = buffer.toString();
            getDoodlesDataFromJson(doodlesJsonStr, yearQuery, monthQuery);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the doodles data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        return;
    }

    /**
     * Take the String representing the complete Doodles in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getDoodlesDataFromJson(String doodlesJsonStr, String yearQuery, String monthQuery)
            throws JSONException {

        // Now we have a String representing the complete Doodle in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // doodle information
        final String OWM_NAME = "name";
        final String OWM_TITLE = "title";
        final String OWM_URL = "url";
        final String OWM_RUN_DATE_ARRAY = "run_date_array";

        try {

            long yearmonthId = addYearMonth(yearQuery, monthQuery) ;

            JSONArray doodlesJsonArray = new JSONArray(doodlesJsonStr);
            // Insert the new doodle information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(doodlesJsonArray.length());

            for(int i = 0; i < doodlesJsonArray.length(); i++) {
                // These are the values that will be collected.
                String name;
                String title;
                String url;
                String run_date;

                // Get the JSON object representing the
                JSONObject doodleObject = doodlesJsonArray.getJSONObject(i);

                name = doodleObject.getString(OWM_NAME);
                title = doodleObject.getString(OWM_TITLE);
                url = doodleObject.getString(OWM_URL);
                run_date = doodleObject.getJSONArray(OWM_RUN_DATE_ARRAY).getInt(0)+"/"+doodleObject.getJSONArray(OWM_RUN_DATE_ARRAY).getInt(1)+"/"+doodleObject.getJSONArray(OWM_RUN_DATE_ARRAY).getInt(2);

                ContentValues doodleValues = new ContentValues();

                doodleValues.put(DoodleContract.DoodleEntry.COLUMN_YEARMONTH_KEY, yearmonthId);
                doodleValues.put(DoodleContract.DoodleEntry.COLUMN_NAME, name);
                doodleValues.put(DoodleContract.DoodleEntry.COLUMN_TITLE, title);
                doodleValues.put(DoodleContract.DoodleEntry.COLUMN_URL, url);
                doodleValues.put(DoodleContract.DoodleEntry.COLUMN_RUN_DATE, run_date);

                cVVector.add(doodleValues);

            }

            // add to database
            if ( cVVector.size() > 0 ) {

                Uri doodleUri = DoodleContract.DoodleEntry.buildDoodleYearMonth(
                        yearQuery, monthQuery);

                Cursor cursorDoodle = getContext().getContentResolver().query(doodleUri,null, null, null, null);

                if (cursorDoodle.getCount()<cVVector.size()) {

                    //delete Doodle where YEARMONTH_KEY = yearmonthId
                    Log.d(LOG_TAG, "Delete Doodle where YEARMONTH_KEY ="+yearmonthId);
                    getContext().getContentResolver().delete(DoodleContract.DoodleEntry.CONTENT_URI,
                            DoodleContract.DoodleEntry.COLUMN_YEARMONTH_KEY+ " = ? ", new String[]{Long.toString(yearmonthId)});

                    ContentValues[] cvArray = new ContentValues[cVVector.size()];
                    cVVector.toArray(cvArray);
                    getContext().getContentResolver().bulkInsert(DoodleContract.DoodleEntry.CONTENT_URI, cvArray);

                    Log.d(LOG_TAG, "Doodles Archive  Complete. " + cVVector.size() + " Inserted");
                    notifyDoodle();
                }
            }



        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }


    /**
     * Helper method to handle insertion of a new Year Month in the yearmonth database.
     *
     * @param yearSetting The year string used to request updates from the server.
     * @param monthSetting The year string used to request updates from the server.
     * @return the row ID of the added year Month.
     */
    long addYearMonth(String yearSetting,String monthSetting) {
        long yearmonthId;

        // First, check if the yearmonth exists in the db
        Cursor yearmonthCursor = getContext().getContentResolver().query(
                DoodleContract.YearMonthEntry.CONTENT_URI,
                new String[]{DoodleContract.YearMonthEntry._ID},
                DoodleContract.YearMonthEntry.COLUMN_YEAR_SETTING + " = ? AND "+DoodleContract.YearMonthEntry.COLUMN_MONTH_SETTING + " = ? ",
                new String[]{yearSetting,monthSetting},
                null);

        if (yearmonthCursor.moveToFirst()) {
            int yearmonthIdIndex = yearmonthCursor.getColumnIndex(DoodleContract.YearMonthEntry._ID);
            yearmonthId = yearmonthCursor.getLong(yearmonthIdIndex);
            Log.d(LOG_TAG, "Year and Month already exit with Id=" + yearmonthId);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues yearmonthValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            yearmonthValues.put(DoodleContract.YearMonthEntry.COLUMN_YEAR_SETTING, yearSetting);
            yearmonthValues.put(DoodleContract.YearMonthEntry.COLUMN_MONTH_SETTING, monthSetting);

            // Finally, insert YearMonth data into the database.
            Uri insertedUri = getContext().getContentResolver().insert(
                    DoodleContract.YearMonthEntry.CONTENT_URI,
                    yearmonthValues
            );

            // The resulting URI contains the ID for the row.  Extract the yearmonthId from the Uri.
            yearmonthId = ContentUris.parseId(insertedUri);

            Log.d(LOG_TAG, "Insert new Year and Month with Id = " + yearmonthId);

        }

        yearmonthCursor.close();
        // Wait, that worked?  Yes!
        return yearmonthId;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        DoodlesArchiveSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }


    private void notifyDoodle() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if ( displayNotifications ) {

            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                // Last sync was more than 1 day ago, let's send a notification with the doodle.

                //set year and month to current value
                Utility.setPreferredYear(context, "" + Calendar.getInstance().get(Calendar.YEAR));
                int monthCurrent = Calendar.getInstance().get(Calendar.MONTH)+1;
                Utility.setPreferredMonth(context, "" + monthCurrent);

                String yearQuery = Utility.getPreferredYear(context);
                String monthQuery = Utility.getPreferredMonth(context);
                String daysQuery = "" + Calendar.getInstance().get(Calendar.DATE);


                Uri doodleUri = DoodleContract.DoodleEntry.buildDoodleYearMonthWithRunDate(yearQuery, monthQuery, yearQuery + "/" + monthQuery + "/" + daysQuery);

                // we'll query our contentProvider, as always
                Cursor cursor = context.getContentResolver().query(doodleUri, NOTIFY_DOODLE_PROJECTION, null, null, null);

                if (cursor.moveToFirst()) {
                    String titleDoodle = cursor.getString(INDEX_TITLE);
                    String runDate = Utility.formatDate(cursor.getString(INDEX_RUN_DATE));
                    int iconId = R.drawable.ic_launcher;

                    // Define the text of the Doodle.
                    String contentText = String.format(context.getString(R.string.format_notification),
                            titleDoodle,
                            runDate);

                    // NotificationCompatBuilder is a very convenient way to build backward-compatible
                    // notifications.  Just throw in some data.
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getContext())
                                    .setSmallIcon(iconId)
                                    .setContentTitle("Doodles Current")
                                    .setContentText(contentText);

                    // Make something interesting happen when the user clicks on the notification.
                    // In this case, opening the app is sufficient.
                    Intent resultIntent = new Intent(context, MainActivity.class);

                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager =
                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    // DOODLE_NOTIFICATION_ID allows you to update the notification later on.
                    mNotificationManager.notify(DOODLE_NOTIFICATION_ID, mBuilder.build());


                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();
                }

                cursor.close();
            }
        }

    }



}