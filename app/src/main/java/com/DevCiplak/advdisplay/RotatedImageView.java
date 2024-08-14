package com.DevCiplak.advdisplay;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;
public class RotatedImageView extends AppCompatImageView{

    public RotatedImageView(Context context) {
        super(context);
    }

    public RotatedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RotatedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.rotate(0, getWidth() / 2, getHeight() / 2);
        super.onDraw(canvas);
        canvas.restore();
    }

}
