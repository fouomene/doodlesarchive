/**
 * Created by FOUOMENE on 15/03/2015.  EmailAuthor: fouomenedaniel@gmail.com .
 */
package com.fouomene.doodlesarchive.app;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fouomene.doodlesarchive.app.custom.DoodleImageView;
import com.fouomene.doodlesarchive.app.data.DoodleContract;
import com.fouomene.doodlesarchive.app.data.DoodleContract.DoodleEntry;

import java.io.InputStream;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";


    private static final String DOODLE_SHARE_HASHTAG = " #DoodlesArcchiveApp";

    private ShareActionProvider mShareActionProvider;
    private String mDoodle;
    private Uri mUri;



    private static final int DETAIL_LOADER = 0;

    private static final String[] DETAIL_COLUMNS = {
            DoodleEntry.TABLE_NAME + "." + DoodleEntry._ID,
            DoodleEntry.COLUMN_TITLE,
            DoodleEntry.COLUMN_URL,

            // This works because the DoodleProvider returns yearmonth data joined with
            // doodle data, even though they're stored in two different tables.
            DoodleContract.YearMonthEntry.COLUMN_YEAR_SETTING,
            DoodleContract.YearMonthEntry.COLUMN_MONTH_SETTING

    };

    // These indices are tied to DETAIL_COLUMNS.  If DETAIL_COLUMNS changes, these
    // must change.
    public static final int COLUMN_TITLE =1 ;
    public static final int COLUMN_URL = 2;

    private TextView mload_textview;
    private DoodleImageView mIconView;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mload_textview = (TextView) rootView.findViewById(R.id.load_textview);
        //mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mIconView = (DoodleImageView) rootView.findViewById(R.id.detail_icon);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mDoodle != null) {
            mShareActionProvider.setShareIntent(createShareDoodleIntent());
        }
    }

    private Intent createShareDoodleIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mDoodle + DOODLE_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        getLoaderManager().initLoader(DETAIL_LOADER, null, this);

        super.onActivityCreated(savedInstanceState);
    }

    void onYearMonthChanged( String newYear, String newMonth ) {
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (null != uri) {

            mUri = DoodleEntry.buildDoodleYearMonth(newYear,newMonth);

            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if ( null != mUri ) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {

            mload_textview.setText("Loading image from internet ...");
            // Read URL from cursor and update view
            String url = data.getString(COLUMN_URL);
            // Construct the URL for the Doodles Archive query
            //www.google.com/logos/doodles/2015/cricket-world-cup-2015-quarterfinals-1-sri-lanka-vs-south-africa-5730984632778752-hp.jpg
            Log.e(LOG_TAG, "URL = "+url.replaceFirst("//www.google.com",""));
            Uri builtUri  = new Uri.Builder()
                    .scheme("http")
                    .authority("google.com")
                    .path(url.replaceFirst("//www.google.com",""))
                    .build();

            Log.e(LOG_TAG, "URI = "+builtUri.toString());

            // Read title from cursor
            String mTitle = data.getString(COLUMN_TITLE);

             //to load image from internet
             new DownloadImageTask(mIconView, mload_textview, mTitle)
                          .execute(builtUri.toString());


            // We still need this for the share intent
            mDoodle = mTitle;

            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareDoodleIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }


    //to load image from internet
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        DoodleImageView bmImage;
        TextView bmload_textview;
        String bmTitle;

        public DownloadImageTask(DoodleImageView bmImage, TextView bmload_textview, String bmTitle) {
            this.bmImage = bmImage;
            this.bmload_textview = bmload_textview;
            this.bmTitle = bmTitle;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
            bmload_textview.setText(bmTitle);
        }


    }


}