/**
 * 
 */
package com.example.mypulltorefresh;

import com.example.mypulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.example.mypulltorefresh.PullToRefreshBase.OnRefreshListener2;

import android.view.View;

/**
 * @author wangyonghua
 */
public interface IPullToRefresh<T extends View> {

	/**
	 * Get the Wrapped Refreshable View. Anything returned here has already been
	 * added to the content view.
	 * @return The View which is currently wrapped
	 */
	public T getRefreshableView();
	
	/**
	 * Whether Pull-to-Refresh is enabled
	 * 
	 * @return enabled
	 */
	public boolean isPullToRefreshEnabled();
	
	/**
	 * Returns whether the Widget is currently in the Refreshing mState
	 * 
	 * @return true if the Widget is currently refreshing
	 */
	public boolean isRefreshing();
	
	/**
	 * Mark the current Refresh as complete. Will Reset the UI and hide the
	 * Refreshing View
	 */
	public void onRefreshComplete();
	
	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener - Listener to be used when the Widget is set to Refresh
	 */
	public void setOnRefreshListener(OnRefreshListener<T> listener);

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener - Listener to be used when the Widget is set to Refresh
	 */
	public void setOnRefreshListener(OnRefreshListener2<T> listener);
	
	
}
