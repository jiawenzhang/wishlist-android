package com.wish.wishlist.view;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.widget.ImageView;
import android.util.AttributeSet;

public class ZoomPanImageView extends ImageView {

Matrix _matrix = new Matrix();

// We can be in one of these 3 states
static final int NONE = 0;
static final int DRAG = 1;
static final int ZOOM = 2;
int mode = NONE;

// Remember some things for zooming
PointF last = new PointF();
PointF start = new PointF();
float minScale = 1f;
float maxScale = 3f;
float[] mArray;

float redundantXSpace, redundantYSpace;

float width, height;
static final int CLICK = 3;
float _saveScale = 1f;
float right, bottom, origWidth, origHeight, bmWidth, bmHeight;

ScaleGestureDetector mScaleDetector;

Context context;


public ZoomPanImageView(Context context, AttributeSet attr) {
    super(context, attr);
    super.setClickable(true);
    this.context = context;
    mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    _matrix.setTranslate(1f, 1f);
    mArray = new float[9];
    setImageMatrix(_matrix);
    setScaleType(ScaleType.MATRIX);

    setOnTouchListener(new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mScaleDetector.onTouchEvent(event);

            _matrix.getValues(mArray);
            float x = mArray[Matrix.MTRANS_X];
            float y = mArray[Matrix.MTRANS_Y];
            PointF curr = new PointF(event.getX(), event.getY());

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    last.set(event.getX(), event.getY());
                    start.set(last);
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        float deltaX = curr.x - last.x;
                        float deltaY = curr.y - last.y;
                        float scaleWidth = Math.round(origWidth * _saveScale);
                        float scaleHeight = Math.round(origHeight * _saveScale);
                        if (scaleWidth < width) {
                            deltaX = 0;
                            if (y + deltaY > 0)
                                deltaY = -y;
                            else if (y + deltaY < -bottom)
                                deltaY = -(y + bottom); 
                        } else if (scaleHeight < height) {
                            deltaY = 0;
                            if (x + deltaX > 0)
                                deltaX = -x;
                            else if (x + deltaX < -right)
                                deltaX = -(x + right);
                        } else {
                            if (x + deltaX > 0)
                                deltaX = -x;
                            else if (x + deltaX < -right)
                                deltaX = -(x + right);

                            if (y + deltaY > 0)
                                deltaY = -y;
                            else if (y + deltaY < -bottom)
                                deltaY = -(y + bottom);
                        }
                        _matrix.postTranslate(deltaX, deltaY);
                        last.set(curr.x, curr.y);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    mode = NONE;
                    int xDiff = (int) Math.abs(curr.x - start.x);
                    int yDiff = (int) Math.abs(curr.y - start.y);
                    if (xDiff < CLICK && yDiff < CLICK)
                        performClick();
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
            }
            setImageMatrix(_matrix);
            invalidate();
            return true; // indicate event was handled
        }

    });//end of setOnTouchListener;
}

@Override
public void setImageBitmap(Bitmap bm) { 
    super.setImageBitmap(bm);
    bmWidth = bm.getWidth();
    bmHeight = bm.getHeight();
}

public void setMaxZoom(float x)
{
    maxScale = x;
}

private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mode = ZOOM;
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float mScaleFactor = (float)Math.min(Math.max(.95f, detector.getScaleFactor()), 1.05);
        float origScale = _saveScale;
        _saveScale *= mScaleFactor;
        if (_saveScale > maxScale) {
            _saveScale = maxScale;
            mScaleFactor = maxScale / origScale;
        } else if (_saveScale < minScale) {
            _saveScale = minScale;
            mScaleFactor = minScale / origScale;
        }
        right = width * _saveScale - width - (2 * redundantXSpace * _saveScale);
        bottom = height * _saveScale - height - (2 * redundantYSpace * _saveScale);
        if (origWidth * _saveScale <= width || origHeight * _saveScale <= height) {
            _matrix.postScale(mScaleFactor, mScaleFactor, width / 2, height / 2);
            if (mScaleFactor < 1) {
                _matrix.getValues(mArray);
                float x = mArray[Matrix.MTRANS_X];
                float y = mArray[Matrix.MTRANS_Y];
                if (mScaleFactor < 1) {
                    if (Math.round(origWidth * _saveScale) < width) {
                        if (y < -bottom)
                            _matrix.postTranslate(0, -(y + bottom));
                        else if (y > 0)
                            _matrix.postTranslate(0, -y);
                    } else {
                        if (x < -right) 
                            _matrix.postTranslate(-(x + right), 0);
                        else if (x > 0) 
                            _matrix.postTranslate(-x, 0);
                    }
                }
            }
        } else {
            _matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
            _matrix.getValues(mArray);
            float x = mArray[Matrix.MTRANS_X];
            float y = mArray[Matrix.MTRANS_Y];
            if (mScaleFactor < 1) {
                if (x < -right) 
                    _matrix.postTranslate(-(x + right), 0);
                else if (x > 0) 
                    _matrix.postTranslate(-x, 0);
                if (y < -bottom)
                    _matrix.postTranslate(0, -(y + bottom));
                else if (y > 0)
                    _matrix.postTranslate(0, -y);
            }
        }
        return true;

    }
}

@Override
protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
{
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    width = MeasureSpec.getSize(widthMeasureSpec);
    height = MeasureSpec.getSize(heightMeasureSpec);
    //Fit to screen.
    float scale;
    float scaleX =  (float)width / (float)bmWidth;
    float scaleY = (float)height / (float)bmHeight;
    scale = Math.min(scaleX, scaleY);

	//Log.d("wish", "width" + String.valueOf(width));
	//Log.d("wish", "bmWidth" + String.valueOf(bmWidth));
	//Log.d("wish", "scaleX" + String.valueOf(scaleX));

	//Log.d("wish", "height" + String.valueOf(height));
	//Log.d("wish", "bmHeight" + String.valueOf(bmHeight));
	//Log.d("wish", "scaleY" + String.valueOf(scaleY));

    _matrix.setScale(scale, scale);

    setImageMatrix(_matrix);
    _saveScale = 1f;

    // Center the image
    redundantYSpace = (float)height - (scale * (float)bmHeight) ;
    redundantXSpace = (float)width - (scale * (float)bmWidth);
    redundantYSpace /= (float)2;
    redundantXSpace /= (float)2;

    _matrix.postTranslate(redundantXSpace, redundantYSpace);

    origWidth = width - 2 * redundantXSpace;
    origHeight = height - 2 * redundantYSpace;
    right = width * _saveScale - width - (2 * redundantXSpace * _saveScale);
    bottom = height * _saveScale - height - (2 * redundantYSpace * _saveScale);
    setImageMatrix(_matrix);
}

}
