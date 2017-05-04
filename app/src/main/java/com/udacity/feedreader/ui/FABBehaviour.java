package com.udacity.feedreader.ui;

import android.content.Context;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class FABBehaviour extends CoordinatorLayout.Behavior<FloatingActionButton> {
	private final static String TAG = FABBehaviour.class.getSimpleName();
	Handler mHandler;

	public FABBehaviour(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean layoutDependsOn(final CoordinatorLayout parent, final FloatingActionButton child, final View dependency) {
		return dependency instanceof ObservableScrollView;
	}

	@Override
	public void onStopNestedScroll(final CoordinatorLayout coordinatorLayout, final FloatingActionButton child, final View target) {
		super.onStopNestedScroll(coordinatorLayout, child, target);
		if (mHandler == null) {
			mHandler = new Handler();
		}
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				child.animate().translationY(0).setInterpolator(new LinearInterpolator()).start();
				Log.d(TAG, "startHandler()");
			}
		}, 1000);

	}

	@Override
	public void onNestedScroll(final CoordinatorLayout coordinatorLayout, final FloatingActionButton child, final View target,
	                           int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
		super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
		//child -> Floating Action Button
		if (dyConsumed > 0) {
			Log.d(TAG, "Scrolling Up");
			final CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
			int fab_bottomMargin = layoutParams.bottomMargin;
			child.animate().translationY(child.getHeight() + fab_bottomMargin).setInterpolator(new LinearInterpolator()).start();
		} else if (dyConsumed < 0) {
			Log.d(TAG, "Scrolling Down");
			child.animate().translationY(0).setInterpolator(new LinearInterpolator()).start();
		}
	}

	@Override
	public boolean onStartNestedScroll(final CoordinatorLayout coordinatorLayout, FloatingActionButton child,
	                                   final View directTargetChild, final View target, int nestedScrollAxes) {
		if (mHandler != null) {
			mHandler.removeMessages(0);
			Log.d(TAG, "Scrolling stopHandler()");
		}
		return (nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL);
	}
}
