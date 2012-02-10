package com.devsmart.android;

import android.os.Handler;

public class LightweightTimer {

	private Handler mHandler = new Handler();
	private Runnable mOnTickCallback = null;
	private long mInterval;
  private boolean mRunning = false;
  private boolean mTaskRunning = false;
	
	public LightweightTimer(Runnable r, long millisec) {
		setOnTick(r);
		setInterval(millisec);
	}
	
	public void setOnTick(Runnable r) {
		mOnTickCallback = r;
	}
	
	public void setInterval(long millisec) {
		if(millisec > 0) {
			mInterval = millisec;
		} else {
			throw new RuntimeException("timer delay must be > 0");
		}
	}
	
	public void start() {
		if(mOnTickCallback != null){
			mHandler.postDelayed(mOnTick, mInterval);
		}
		mRunning = true;
	}
	
	public void stop() {
		mRunning = false;
		mHandler.removeCallbacks(mOnTick);
	}
	
	public void runOnce() {
	  if(mTaskRunning) return;
	  mTaskRunning = true;
	  mOnTickCallback.run();
	  mTaskRunning = false;
	}
	
	private Runnable mOnTick = new Runnable() {
		public void run() {
			if(mOnTickCallback != null){
				runOnce();
				if(mRunning){
					start();
				}
			}
			
		}
	};
}
