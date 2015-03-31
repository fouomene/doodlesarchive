package com.fouomene.doodlesarchive.app.custom;

/**
 * Created by FOUOMENE on 26/03/2015. EmailAuthor: fouomenedaniel@gmail.com
 */
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class DoodleImageView extends ImageView {

    public DoodleImageView(Context context) {
        super(context);
    }

    public DoodleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DoodleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getDrawable();
        if (d != null) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = w * d.getIntrinsicHeight() / d.getIntrinsicWidth();
            setMeasuredDimension(w, h);
        }
        else super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
