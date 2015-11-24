package com.example.mypulltorefresh;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

public abstract class PullToRefreshBase<T extends View> extends LinearLayout
		implements IPullToRefresh<T> {

	private int mTouchSlop;
	private Mode mMode;
	T mRefreshableView;

	public PullToRefreshBase(Context context) {
		this(context, null);
	}

	public PullToRefreshBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	@SuppressLint("NewApi")
	public PullToRefreshBase(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		switch (getPullToRefreshScrollDirection()) {
		case HORIZONTAL:
			setOrientation(LinearLayout.HORIZONTAL);
			break;
		case VERTICAL:
		default:
			setOrientation(LinearLayout.VERTICAL);
			break;
		}
		setGravity(Gravity.CENTER);

		ViewConfiguration config = ViewConfiguration.get(context);
		mTouchSlop = config.getScaledTouchSlop();

		// Styleables from XML
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.PullToRefresh);

		if (a.hasValue(R.styleable.PullToRefresh_ptrMode)) {
			mMode = Mode.mapIntToValue(a.getInteger(
					R.styleable.PullToRefresh_ptrMode, 0));
		}
	}

	private boolean mIsBeingDragged = false;
	private float mLastMotionX, mLastMotionY;
	private float mInitialMotionX, mInitialMotionY;
	private boolean mScrollingWhileRefreshingEnabled = false; // 刷新时是否滚动
	private boolean mFilterTouchEvents = true;
	private Mode mCurrentMode;
	private State mState;
	private OnRefreshListener mOnRefreshListener;
	private OnRefreshListener2 mOnRefreshListener2;
	static final float FRICTION = 2.0f; //下拉或者上拉比例超过这个比例触发刷新
	private boolean mLayoutVisibilityChangesEnabled = true;
	
	private LoadingLayout mHeaderLayout;
	private LoadingLayout mFooterLayout;
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (!isPullToRefreshEnabled()) {
			return false;
		}

		final int action = MotionEventCompat.getActionMasked(event);
		if (action == MotionEvent.ACTION_CANCEL
				|| action == MotionEvent.ACTION_UP) {
			mIsBeingDragged = false;
			return false;
		}

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (isReadyForPull()) {
				mLastMotionY = mInitialMotionY = event.getY();
				mLastMotionX = mInitialMotionX = event.getX();
				mIsBeingDragged = false;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			// If we're refreshing, and the flag is set. Eat all MOVE events
			if (!mScrollingWhileRefreshingEnabled && isRefreshing()) {
				return true;
			}

			if (isReadyForPull()) {
				final float y = event.getY(), x = event.getX();
				final float diff, oppositeDiff, absDiff;

				// We need to use the correct values, based on scroll direction
				switch (getPullToRefreshScrollDirection()) {
				case HORIZONTAL:
					diff = x - mLastMotionX;
					oppositeDiff = y - mLastMotionY;
					break;
				case VERTICAL:
				default:
					diff = y - mLastMotionY;
					oppositeDiff = x - mLastMotionX;
					break;
				}
				absDiff = Math.abs(diff);

				if (absDiff > mTouchSlop
						&& (!mFilterTouchEvents || absDiff > Math
								.abs(oppositeDiff))) {
					if (mMode.showHeaderLoadingLayout() && diff >= 1f
							&& isReadyForPullStart()) {
						mLastMotionY = y;
						mLastMotionX = x;
						mIsBeingDragged = true;
						if (mMode == Mode.BOTH) {
							mCurrentMode = Mode.PULL_FROM_START;
						}
					} else if (mMode.showFooterLoadingLayout() && diff <= -1f
							&& isReadyForPullEnd()) {
						mLastMotionY = y;
						mLastMotionX = x;
						mIsBeingDragged = true;
						if (mMode == Mode.BOTH) {
							mCurrentMode = Mode.PULL_FROM_END;
						}
					}
				}
			}
			break;
		default:
			break;
		}
		return mIsBeingDragged;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isPullToRefreshEnabled()) {
			return false;
		}

		// If we're refreshing, and the flag is set. Eat the event
		if (!mScrollingWhileRefreshingEnabled && isRefreshing()) {
			return true;
		}

		final int action = MotionEventCompat.getActionMasked(event);
		// 触摸试图的边缘
		if (action == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
			return false;
		}

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (isReadyForPull()) {
				mLastMotionY = mInitialMotionY = event.getY();
				mLastMotionX = mInitialMotionX = event.getX();
				return true;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mIsBeingDragged) {
				mLastMotionY = event.getY();
				mLastMotionX = event.getX();
				pullEvent();
				return true;
			}
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if (mIsBeingDragged) {
				mIsBeingDragged = false;

				if (mState == State.RELEASE_TO_REFRESH
						&& (null != mOnRefreshListener || null != mOnRefreshListener2)) {
					setState(State.REFRESHING, true);
					return true;
				}

				// If we're already refreshing, just scroll back to the top
				if (isRefreshing()) {
					smoothScrollTo(0);
					return true;
				}

				// If we haven't returned by here, then we're not in a state
				// to pull, so just reset
				setState(State.RESET);
				return true;
			}
			break;
		default:
			break;
		}
		return super.onTouchEvent(event);
	}

	private void setState(State reset) {

	}

	private void smoothScrollTo(int i) {

	}

	private void setState(State refreshing, boolean b) {

	}

	private void pullEvent() {
		final int newScrollValue; //原始拉动距离的比例值
		final int itemDimension; //header 或 footer 大小
		final float initialMotionValue, lastMotionValue; //初始触摸位置和最后触摸位置

		//获取已下拉或上拉距离
		switch (getPullToRefreshScrollDirection()) {
		case HORIZONTAL:
			initialMotionValue = mInitialMotionX;
			lastMotionValue = mLastMotionX;
			break;
		case VERTICAL:
		default:
			initialMotionValue = mInitialMotionY;
			lastMotionValue = mLastMotionY;
			break;
		}

		//newScrollValue  获取已下拉或上拉距离的比例值和FRICTION相关
		switch (mCurrentMode) {
		case PULL_FROM_END:
			newScrollValue = Math.round(Math.max(initialMotionValue - lastMotionValue, 0) / FRICTION);
			itemDimension = getFooterSize();
			break;
		case PULL_FROM_START:
		default:
			newScrollValue = Math.round(Math.min(initialMotionValue - lastMotionValue, 0) / FRICTION);
			itemDimension = getHeaderSize();
			break;
		}
		
		//滚动Header
		setHeaderScroll(newScrollValue);
		
		if (newScrollValue != 0 && !isRefreshing()) {
			float scale = Math.abs(newScrollValue) / (float) itemDimension;
			switch (mCurrentMode) {
				case PULL_FROM_END:
					mFooterLayout.onPull(scale);
					break;
				case PULL_FROM_START:
				default:
					mHeaderLayout.onPull(scale);
					break;
			}

			if (mState != State.PULL_TO_REFRESH && itemDimension >= Math.abs(newScrollValue)) {
				setState(State.PULL_TO_REFRESH);
			} else if (mState == State.PULL_TO_REFRESH && itemDimension < Math.abs(newScrollValue)) {
				setState(State.RELEASE_TO_REFRESH);
			}
		}
	}

	/**
	 * Helper method which just calls scrollTo() in the correct scrolling
	 * direction.
	 * @param value - New Scroll value
	 */
	private void setHeaderScroll(int value) {
		if (true) {
			Log.d("LOG_TAG", "setHeaderScroll: " + value);
		}

		// Clamp value to with pull scroll range
		final int maximumPullScroll = getMaximumPullScroll();
		value = Math.min(maximumPullScroll, Math.max(-maximumPullScroll, value));

		if (mLayoutVisibilityChangesEnabled) {
			if (value < 0) {
				mHeaderLayout.setVisibility(View.VISIBLE);
			} else if (value > 0) {
				mFooterLayout.setVisibility(View.VISIBLE);
			} else {
				mHeaderLayout.setVisibility(View.INVISIBLE);
				mFooterLayout.setVisibility(View.INVISIBLE);
			}
		}

		switch (getPullToRefreshScrollDirection()) {
			case VERTICAL:
				scrollTo(0, value);
				break;
			case HORIZONTAL:
				scrollTo(value, 0);
				break;
		}
	}

	private int getMaximumPullScroll() {
		return 0;
	}

	private int getHeaderSize() {
		return 0;
	}

	private int getFooterSize() {
		return 0;
	}

	private boolean isReadyForPull() {
		return false;
	}

	/**
	 * Implemented by derived class to return whether the View is in a state
	 * where the user can Pull to Refresh by scrolling from the end.
	 * 
	 * @return true if the View is currently in the correct state (for example,
	 *         bottom of a ListView)
	 */
	protected abstract boolean isReadyForPullEnd();

	/**
	 * Implemented by derived class to return whether the View is in a state
	 * where the user can Pull to Refresh by scrolling from the start.
	 * 
	 * @return true if the View is currently the correct state (for example, top
	 *         of a ListView)
	 */
	protected abstract boolean isReadyForPullStart();

	public abstract Orientation getPullToRefreshScrollDirection();

	public interface OnRefreshListener<V extends View> {
		public void onRefresh(final PullToRefreshBase<V> refreshView);
	}

	public interface OnRefreshListener2<V extends View> {
		public void onPullDownToRefresh(final PullToRefreshBase<V> refreshView);

		public void onPullUpToRefresh(final PullToRefreshBase<V> refreshView);

	}

	public enum Orientation {
		VERTICAL, HORIZONTAL;
	}

	public static enum State {
		/**
		 * When the UI is in a state which means that user is not interacting
		 * with the Pull-to-Refresh function.
		 */
		RESET(0x0),

		/**
		 * When the UI is being pulled by the user, but has not been pulled far
		 * enough so that it refreshes when released.
		 */
		PULL_TO_REFRESH(0x1),

		/**
		 * When the UI is being pulled by the user, and <strong>has</strong>
		 * been pulled far enough so that it will refresh when released.
		 */
		RELEASE_TO_REFRESH(0x2),

		/**
		 * When the UI is currently refreshing, caused by a pull gesture.
		 */
		REFRESHING(0x8),

		/**
		 * When the UI is currently refreshing, caused by a call to
		 * {@link PullToRefreshBase#setRefreshing() setRefreshing()}.
		 */
		MANUAL_REFRESHING(0x9),

		/**
		 * When the UI is currently overscrolling, caused by a fling on the
		 * Refreshable View.
		 */
		OVERSCROLLING(0x10);

		/**
		 * Maps an int to a specific state. This is needed when saving state.
		 * 
		 * @param stateInt
		 *            - int to map a State to
		 * @return State that stateInt maps to
		 */
		static State mapIntToValue(final int stateInt) {
			for (State value : State.values()) {
				if (stateInt == value.getIntValue()) {
					return value;
				}
			}
			// If not, return default
			return RESET;
		}

		private int mIntValue;

		State(int intValue) {
			mIntValue = intValue;
		}

		int getIntValue() {
			return mIntValue;
		}
	}

	public static enum Mode {

		/**
		 * Disable all Pull-to-Refresh gesture and Refreshing handling
		 */
		DISABLED(0x0),

		/**
		 * Only allow the user to Pull from the start of the Refreshable View to
		 * refresh. The start is either the Top or Left, depending on the
		 * scrolling direction.
		 */
		PULL_FROM_START(0x1),

		/**
		 * Only allow the user to Pull from the end of the Refreshable View to
		 * refresh. The start is either the Bottom or Right, depending on the
		 * scrolling direction.
		 */
		PULL_FROM_END(0x2),

		/**
		 * Allow the user to both Pull from the start, from the end to refresh.
		 */
		BOTH(0x3),

		/**
		 * Disables Pull-to-Refresh gesture handling, but allows manually
		 * setting the Refresh state via
		 * {@link PullToRefreshBase#setRefreshing() setRefreshing()}.
		 */
		MANUAL_REFRESH_ONLY(0x4);

		/**
		 * Maps an int to a specific mode. This is needed when saving state, or
		 * inflating the view from XML where the mode is given through a attr
		 * int.
		 * 
		 * @param modeInt
		 *            - int to map a Mode to
		 * @return Mode that modeInt maps to, or PULL_FROM_START by default.
		 */
		static Mode mapIntToValue(final int modeInt) {
			for (Mode value : Mode.values()) {
				if (modeInt == value.getIntValue()) {
					return value;
				}
			}

			// If not, return default
			return getDefault();
		}

		static Mode getDefault() {
			return PULL_FROM_START;
		}

		private int mIntValue;

		// The modeInt values need to match those from attrs.xml
		Mode(int modeInt) {
			mIntValue = modeInt;
		}

		/**
		 * @return true if the mode permits Pull-to-Refresh
		 */
		boolean permitsPullToRefresh() {
			return !(this == DISABLED || this == MANUAL_REFRESH_ONLY);
		}

		/**
		 * @return true if this mode wants the Loading Layout Header to be shown
		 */
		public boolean showHeaderLoadingLayout() {
			return this == PULL_FROM_START || this == BOTH;
		}

		/**
		 * @return true if this mode wants the Loading Layout Footer to be shown
		 */
		public boolean showFooterLoadingLayout() {
			return this == PULL_FROM_END || this == BOTH
					|| this == MANUAL_REFRESH_ONLY;
		}

		int getIntValue() {
			return mIntValue;
		}

	}

	@Override
	public T getRefreshableView() {
		return null;
	}

	@Override
	public boolean isPullToRefreshEnabled() {
		return false;
	}

	@Override
	public boolean isRefreshing() {
		return false;
	}

	@Override
	public void onRefreshComplete() {

	}

	@Override
	public void setOnRefreshListener(OnRefreshListener<T> listener) {

	}

	@Override
	public void setOnRefreshListener(OnRefreshListener2<T> listener) {

	}

	protected T createRefreshableView(Context context,
			AttributeSet attrs) {
		return null;
	}
}
