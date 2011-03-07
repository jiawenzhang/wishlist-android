package com.androiddev.mywishlist;

import com.androiddev.mywithlist.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;



public class WishListItemView extends TextView {
	private Paint marginPaint;
	private Paint linePaint;
	private int paperColor;
	private float margin;
	public WishListItemView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init();
	}

	public WishListItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init();
	}

	public WishListItemView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init();
	}
	
	private void init(){
		Resources myResources = getResources();
		marginPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		marginPaint.setColor(myResources.getColor(R.color.notepad_margin));
		linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		linePaint.setColor(myResources.getColor(R.color.notepad_lines));
		
		paperColor = myResources.getColor(R.color.notepad_paper);
		margin = myResources.getDimension(R.dimen.notepad_margin);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		canvas.drawColor(paperColor);
		canvas.drawLine(0, 0, getMeasuredHeight(), 0, linePaint);
		canvas.drawLine(0, getMeasuredHeight(), getMeasuredWidth(),
				getMeasuredHeight(), linePaint);
		// Draw margin
		canvas.drawLine(margin, 0, margin, getMeasuredHeight(), marginPaint);
		// Move the text across from the margin
		canvas.save();
		canvas.translate(margin, 0);
		super.onDraw(canvas);
		canvas.restore();
	}
	

}
