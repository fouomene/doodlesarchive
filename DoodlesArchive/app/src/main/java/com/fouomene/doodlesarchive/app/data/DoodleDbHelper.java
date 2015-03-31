/**
 * Created by FOUOMENE on 15/03/2015. EmailAuthor: fouomenedaniel@gmail.com
 */
package com.fouomene.doodlesarchive.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.fouomene.doodlesarchive.app.data.DoodleContract.YearMonthEntry;
import com.fouomene.doodlesarchive.app.data.DoodleContract.DoodleEntry;

/**
 * Manages a local database for doodle data.
 */
public class DoodleDbHelper extends SQLiteOpenHelper {


    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 34;

    public static final String DATABASE_NAME = "doodle.db";

    public DoodleDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        // Create a table to hold yearmonth.
        final String SQL_CREATE_YEARMONTH_TABLE = "CREATE TABLE " + YearMonthEntry.TABLE_NAME + " (" +
                YearMonthEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                YearMonthEntry.COLUMN_YEAR_SETTING+ " TEXT NOT NULL, " +
                YearMonthEntry.COLUMN_MONTH_SETTING+ " TEXT NOT NULL " +
                " );";

        final String SQL_CREATE_DOODLE_TABLE = "CREATE TABLE " + DoodleEntry.TABLE_NAME + " (" +

                DoodleEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                // the ID of the yearmonth entry associated with this doodle data
                DoodleEntry.COLUMN_YEARMONTH_KEY + " INTEGER NOT NULL, " +
                DoodleEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                DoodleEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                DoodleEntry.COLUMN_URL + " TEXT NOT NULL," +
                DoodleEntry.COLUMN_RUN_DATE + " TEXT NOT NULL, " +

                // Set up the yearmonth column as a foreign key to yearmonth table.
                " FOREIGN KEY (" + DoodleEntry.COLUMN_YEARMONTH_KEY + ") REFERENCES " +
                YearMonthEntry.TABLE_NAME + " (" + YearMonthEntry._ID + ") );";

        sqLiteDatabase.execSQL(SQL_CREATE_YEARMONTH_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_DOODLE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + YearMonthEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DoodleEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
