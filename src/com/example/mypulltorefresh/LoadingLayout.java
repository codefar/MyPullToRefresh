package com.example.mypulltorefresh;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class LoadingLayout extends FrameLayout implements ILoadingLayout{
	
	/**
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public LoadingLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public LoadingLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * @param context
	 */
	public LoadingLayout(Context context) {
		super(context);
	}

	public final void onPull(float scaleOfLayout) {
	}
}
