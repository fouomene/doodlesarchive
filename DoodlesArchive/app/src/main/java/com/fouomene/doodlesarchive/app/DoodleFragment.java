/**
 * Created by FOUOMENE on 15/03/2015.  EmailAuthor: fouomenedaniel@gmail.com .
 */
package com.fouomene.doodlesarchive.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.fouomene.doodlesarchive.app.data.DoodleContract;
import com.fouomene.doodlesarchive.app.sync.DoodlesArchiveSyncAdapter;
import com.fouomene.doodlesarchive.app.utils.Utility;

/**
 * Encapsulates fetching the Doodle and displaying it as a {@link ListView} layout.
 */
public class DoodleFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private DoodleAdapter mDoodleAdapter;

    private final String LOG_TAG = DoodleFragment.class.getSimpleName();

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;

    private static final String SELECTED_KEY = "selected_position";

    private static final int DOODLE_LOADER = 0;
    // For the doodle view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] DOODLE_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the yearmonth & doodle tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the doodle table
            // using the year and month set by the user, which is only in the yearmonth table.
            // So the convenience is worth it.
            DoodleContract.DoodleEntry.TABLE_NAME + "." + DoodleContract.DoodleEntry._ID,
            DoodleContract.DoodleEntry.COLUMN_NAME,
            DoodleContract.DoodleEntry.COLUMN_TITLE,
            DoodleContract.DoodleEntry.COLUMN_URL,
            DoodleContract.DoodleEntry.COLUMN_RUN_DATE,
            DoodleContract.YearMonthEntry.COLUMN_YEAR_SETTING,
            DoodleContract.YearMonthEntry.COLUMN_MONTH_SETTING
    };

    // These indices are tied to DOODLE_COLUMNS.  If DOODLE_COLUMNS changes, these
    // must change.
    static final int COL_DOODLE_ID = 0;
    static final int COL_DOODLE_NAME = 1;
    static final int COL_DOODLE_TITLE= 2;
    static final int COL_DOODLE_URL = 3;
    static final int COL_DOODLE_RUN_DATE = 4;
    static final int COL_YEAR_MONTH_SETTING = 5;


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }

    public DoodleFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.doodlefragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The DoodleAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mDoodleAdapter = new DoodleAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_doodle);
        mListView.setAdapter(mDoodleAdapter);
        // We'll call our MainActivity
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String yearSetting = Utility.getPreferredYear(getActivity());
                    String monthSetting = Utility.getPreferredMonth(getActivity());
                    Log.d(LOG_TAG, "URI = "+DoodleContract.DoodleEntry.buildDoodleYearMonthWithIdDoodle(
                            yearSetting, monthSetting, cursor.getLong(COL_DOODLE_ID)
                    ).toString());
                    ((Callback) getActivity())
                            .onItemSelected(DoodleContract.DoodleEntry.buildDoodleYearMonthWithIdDoodle(
                                    yearSetting,monthSetting,cursor.getLong(COL_DOODLE_ID)
                            ));
                }
                mPosition = position;
            }
        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DOODLE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    // since we read the year month when we create the loader, all we need to do is restart things
    void onYearMonthChanged() {
        updateDoodle();
        getLoaderManager().restartLoader(DOODLE_LOADER, null, this);
    }

    private void updateDoodle() {

        DoodlesArchiveSyncAdapter.syncImmediately(getActivity());

    }


    @Override
    public void onStart() {
        super.onStart();
        updateDoodle();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.


        // Sort order:  Ascending, by id.
       // String sortOrder = DoodleContract.DoodleEntry.COLUMN_RUN_DATE + " ASC";

        String yearSetting = Utility.getPreferredYear(getActivity());
        String monthSetting = Utility.getPreferredMonth(getActivity());

        Uri doodleForYearMonthUri = DoodleContract.DoodleEntry.buildDoodleYearMonth(
                yearSetting, monthSetting);
        Log.e(LOG_TAG,"URI =  "+ doodleForYearMonthUri.toString());
        return new CursorLoader(getActivity(),
                doodleForYearMonthUri,
                DOODLE_COLUMNS,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mDoodleAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mDoodleAdapter.swapCursor(null);
    }


}