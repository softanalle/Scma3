package com.delektre.Scma3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EView;

@EView
public class CrosshairOverlay extends View {
    private Integer mnHorizStart, mnHorizEnd;
    private Integer mnVertStart, mnVertEnd;
    private float posX, posY;

    public CrosshairOverlay(Context context) {
        super(context);
    }

    public CrosshairOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CrosshairOverlay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void clearLimits() {
        mnHorizStart = mnHorizEnd = mnVertStart = mnVertEnd = null;
        posX = getWidth() / 2;
        posY = getHeight() / 2;
    }


    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);

        // draw a partially transparent crosshairs on the canvas

        Paint p = new Paint();
        p.setColor(Color.RED);
        p.setAlpha(128);
        c.drawLine(0.0f, getHeight() / 2, getWidth(), getHeight() / 2, p);
        c.drawLine(getWidth() / 2, 0.0f, getWidth() / 2, getHeight(), p);
        if (mnHorizStart != null) {
            c.drawLine(mnHorizStart, getHeight() / 2 - 5, mnHorizStart, getHeight() / 2 + 5, p);
            c.drawLine(mnHorizEnd, getHeight() / 2 - 5, mnHorizEnd, getHeight() / 2 + 5, p);
        }
        if (mnVertStart != null) {
            c.drawLine(getWidth() / 2 - 5, mnVertStart, getWidth() / 2 + 5, mnVertStart, p);
            c.drawLine(getWidth() / 2 - 5, mnVertEnd, getWidth() / 2 + 5, mnVertEnd, p);
        }

        // draw selection aid
        Paint f = new Paint(p);
        f.setStyle(Paint.Style.STROKE);
        f.setAntiAlias(true);
        f.setStrokeWidth(2);
        c.drawCircle(posX, posY, getHeight() / 4, f);
    }

    public void setHorizLimits(int nStart, int nEnd) {
        mnHorizStart = nStart;
        mnHorizEnd = nEnd;
    }

    public void setVertLimits(int nStart, int nEnd) {
        mnVertStart = nStart;
        mnVertEnd = nEnd;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        posX = w / 2;
        posY = h / 2;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        posX = event.getX();
        posY = event.getY();
        invalidate();
        return false;
    }
}
