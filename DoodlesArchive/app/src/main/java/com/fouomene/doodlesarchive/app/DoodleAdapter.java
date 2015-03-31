/**
 * Created by FOUOMENE on 15/03/2015. EmailAuthor: fouomenedaniel@gmail.com .
 */
package com.fouomene.doodlesarchive.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fouomene.doodlesarchive.app.utils.Utility;

/**
 * {@link DoodleAdapter} exposes a list of Doodles Archive
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class DoodleAdapter extends CursorAdapter {


    /**
     * Cache of the children views for a doodle list item.
     */
    public static class ViewHolder {

        public final TextView titleView;
        public final TextView runDateView;


        public ViewHolder(View view) {
            titleView = (TextView) view.findViewById(R.id.list_item_title_textview);
            runDateView = (TextView) view.findViewById(R.id.list_item_rundate_textview);
        }
    }

    public DoodleAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }


    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int layoutId = -1;

         layoutId = R.layout.list_item_doodle;

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Read doodle title from cursor
        String title = cursor.getString(DoodleFragment.COL_DOODLE_TITLE);
        // Find TextView and set doodle title on it
        viewHolder.titleView.setText(title);

        // Read doodle run-date from cursor
        String runDate = cursor.getString(DoodleFragment.COL_DOODLE_RUN_DATE);

        // Find TextView and set doodle run date on it
        viewHolder.runDateView.setText(Utility.formatDate(runDate));

    }


}