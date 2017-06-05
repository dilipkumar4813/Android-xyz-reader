package com.example.xyzreader.ui;

import android.app.Activity;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created on 05/06/17.
 *
 * @author dilipkumar4813
 * @version 1.0
 */

class SimpleGestureFilter extends GestureDetector.SimpleOnGestureListener {

    final static int SWIPE_LEFT = 3;
    final static int SWIPE_RIGHT = 4;

    private final static int ACTION_FAKE = -13;

    private boolean tapIndicator = false;

    private GestureDetector detector;
    private SimpleGestureListener listener;

    SimpleGestureFilter(Activity activity, SimpleGestureListener sgl) {
        this.detector = new GestureDetector(activity, this);
        this.listener = sgl;
    }

    void onTouchEvent(MotionEvent event) {

        boolean result = this.detector.onTouchEvent(event);

        if (event.getAction() == ACTION_FAKE)
            event.setAction(MotionEvent.ACTION_UP);
        else if (result)
            event.setAction(MotionEvent.ACTION_CANCEL);
        else if (this.tapIndicator) {
            event.setAction(MotionEvent.ACTION_DOWN);
            this.tapIndicator = false;
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {

        int swipe_Min_Distance = 100;
        int swipe_Max_Distance = 350;
        int swipe_Min_Velocity = 100;

        final float xDistance = Math.abs(e1.getX() - e2.getX());
        final float yDistance = Math.abs(e1.getY() - e2.getY());

        if (xDistance > swipe_Max_Distance || yDistance > swipe_Max_Distance)
            return false;

        velocityX = Math.abs(velocityX);
        boolean result = false;

        if (velocityX > swipe_Min_Velocity && xDistance > swipe_Min_Distance) {
            if (e1.getX() > e2.getX()) // right to left
                this.listener.onSwipe(SWIPE_LEFT);
            else
                this.listener.onSwipe(SWIPE_RIGHT);

            result = true;
        }

        return result;
    }

    interface SimpleGestureListener {
        void onSwipe(int direction);
    }

}
