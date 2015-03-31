/**
 * Created by FOUOMENE on 15/03/2015. EmailAuthor: fouomenedaniel@gmail.com .
 */
package com.fouomene.doodlesarchive.app;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.fouomene.doodlesarchive.app.sync.DoodlesArchiveSyncAdapter;
import com.fouomene.doodlesarchive.app.utils.Utility;

import java.util.Calendar;


public class MainActivity extends ActionBarActivity implements DoodleFragment.Callback{

    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private boolean mTwoPane;
    private String mYear;
    private String mMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set year and month to current value
        Utility.setPreferredYear(this, "" + Calendar.getInstance().get(Calendar.YEAR));
        int monthCurrent = Calendar.getInstance().get(Calendar.MONTH)+1;
        Utility.setPreferredMonth(this, "" + monthCurrent);

        mYear = Utility.getPreferredYear(this);
        mMonth = Utility.getPreferredMonth(this);

        // to display Icon launcher
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);


        setContentView(R.layout.activity_main);
        if (findViewById(R.id.doodle_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.doodle_detail_container, new DetailFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }

        // Make sur you have gotten an accound created
        DoodlesArchiveSyncAdapter.initializeSyncAdapter(this);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        String year = Utility.getPreferredYear(this);
        String month = Utility.getPreferredMonth(this);
        // update the year month in our second pane using the fragment manager
        if ((year != null && !year.equals(mYear)) || (month != null && !month.equals(mMonth))) {
            DoodleFragment ff = (DoodleFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_doodle);
            if ( null != ff ) {
                ff.onYearMonthChanged();
            }
            DetailFragment df = (DetailFragment)getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if ( null != df ) {
                df.onYearMonthChanged(year,month);
            }
            mYear = year;
            mMonth = month;
        }
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, contentUri);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.doodle_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }

}
