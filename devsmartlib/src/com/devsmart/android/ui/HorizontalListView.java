/*
 * HorizontalListView.java v1.5
 *
 * 
 * The MIT License
 * Copyright (c) 2011 Paul Soucy (paul@dev-smart.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.devsmart.android.ui;

import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Scroller;

import com.devsmart.android.R;

public class HorizontalListView extends AdapterView<ListAdapter> {
  public static final int POSITION_START = 0;
  public static final int POSITION_MIDDLE = 1;
  public static final int POSITION_END = 2;

	public boolean mAlwaysOverrideTouch = true;
	protected ListAdapter mAdapter;
	private int mLeftViewIndex = -1;
	private int mRightViewIndex = 0;
  private int mSelection = 0;
  private int mItemWidth = 0;
  private int mItemHeight = 0;
	protected int mCurrentX;
	protected int mNextX;
	private int mMaxX = Integer.MAX_VALUE;
	private int mDisplayOffset = 0;
	protected Scroller mScroller;
	private GestureDetector mGesture;
	private Queue<View> mRemovedViewQueue = new LinkedList<View>();
	private OnItemSelectedListener mOnItemSelected;
	private OnItemClickListener mOnItemClicked;
	private OnItemLongClickListener mOnItemLongClicked;
  private OnScrollListener mOnScrollListener;
	private boolean mDataChanged = false;

	public HorizontalListView(Context context, AttributeSet attrs) {
		super(context, attrs);

    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HorizontalListView, 0, 0);
    mItemWidth = a.getDimensionPixelSize(R.styleable.HorizontalListView_itemWidth, mItemWidth);
    mItemHeight = a.getDimensionPixelSize(R.styleable.HorizontalListView_itemHeight, mItemHeight);
		
		initView();
	}

	private synchronized void initView() {
		mLeftViewIndex = -1;
		mRightViewIndex = 0;
		mDisplayOffset = 0;
		mCurrentX = 0;
		mNextX = 0;
		mMaxX = Integer.MAX_VALUE;
		mScroller = new Scroller(getContext());
		mGesture = new GestureDetector(getContext(), mOnGesture);
	}

	@Override
	public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
		mOnItemSelected = listener;
	}

	@Override
	public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
		mOnItemClicked = listener;
	}

	@Override
	public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener) {
		mOnItemLongClicked = listener;
	}

	private DataSetObserver mDataObserver = new DataSetObserver() {

		@Override
		public void onChanged() {
			synchronized (HorizontalListView.this) {
				mDataChanged = true;
			}
			invalidate();
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			reset();
			invalidate();
			requestLayout();
		}

	};

	@Override
	public ListAdapter getAdapter() {
		return mAdapter;
	}

	@Override
	public View getSelectedView() {
	  for(int i = 0; i < getChildCount(); i++) {
	    View v = getChildAt(i);
	    if(v.isSelected()) return v;
	  }
	  
	  for(View v : mRemovedViewQueue) {
      if(v.isSelected()) return v;
	  }
	  
	  return null;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		if (mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mDataObserver);
		}
		mAdapter = adapter;
		mAdapter.registerDataSetObserver(mDataObserver);
		reset();
	}
	
	public void setOnScrollListener(OnScrollListener scrollListener) {
	  mOnScrollListener = scrollListener;
	}

	private synchronized void reset() {
		initView();
		removeAllViewsInLayout();
		requestLayout();
	}

	@Override
	public synchronized int getFirstVisiblePosition() {
		Rect viewRect = new Rect();
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			View child = getChildAt(i);
			int left = child.getLeft();
			int right = child.getRight();
			int top = child.getTop();
			int bottom = child.getBottom();
			viewRect.set(left, top, right, bottom);

			if (viewRect.contains(2, 2)) {
				return mLeftViewIndex + 1 + i;
			}
		}
		
		return 0;
	}
	
	

	@Override
  public int getLastVisiblePosition() {
	  return getFirstVisiblePosition() + getChildCount();
  }

  @Override
	public void setSelection(int position) {
	  mSelection = position;
	  
	  View v = null;
	  v = getSelectedView();
	  if(v!= null) v.setSelected(false);
	  
	  int idx = mSelection - mLeftViewIndex - 1;
	  if(idx >= 0) {
	    v = getChildAt(idx);
	    if(v != null) v.setSelected(true);
	  }
	}
	
	

	@Override
  public int getSelectedItemPosition() {
	  return mSelection;
  }

  @Override
  public Object getSelectedItem() {
    return getAdapter().getItem(getSelectedItemPosition());
  }

  private void addChild(final View child, int viewPos) {
		LayoutParams params = child.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		}

		addViewInLayout(child, viewPos, params, true);
	}

	@Override
	protected synchronized void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		if (mAdapter == null) {
			return;
		}

		if (mDataChanged) {
			int oldCurrentX = mCurrentX;
			initView();
			removeAllViewsInLayout();
			mNextX = oldCurrentX;
			mDataChanged = false;
		}

		if (mScroller.computeScrollOffset()) {
			int scrollx = mScroller.getCurrX();
			mNextX = scrollx;
		}

    int position = POSITION_MIDDLE;
		if (mNextX <= 0) {
			mNextX = 0;
      position = POSITION_START;
			mScroller.forceFinished(true);
		}
		if (mNextX >= mMaxX) {
			mNextX = mMaxX;
      position = POSITION_END;
			mScroller.forceFinished(true);
		}

		int dx = mCurrentX - mNextX;

		removeNonVisibleItems(dx);
		fillList(dx);
		positionItems(dx);

		mCurrentX = mNextX;

		if (!mScroller.isFinished()) {
		  postDelayed(new Runnable() {
				public void run() {
					requestLayout();
				}
			}, 10);

		}
		
		if(mOnScrollListener != null) {
		  mOnScrollListener.onScroll(mNextX, position);
		}
	}

	private void fillList(final int dx) {
		int edge = 0;
		View child = getChildAt(getChildCount() - 1);
		if (child != null) {
			edge = child.getRight();
		}
		fillListRight(edge, dx);

		edge = 0;
		child = getChildAt(0);
		if (child != null) {
			edge = child.getLeft();
		}
		fillListLeft(edge, dx);

	}

	private void fillListRight(int rightEdge, final int dx) {
		while (rightEdge + dx < getWidth() && mRightViewIndex < mAdapter.getCount()) {

			View child = mAdapter.getView(mRightViewIndex, mRemovedViewQueue.poll(), this);

      
      if(mRightViewIndex == mSelection) {
        child.setSelected(true);
      } else {
        child.setSelected(false);
      }
			
			addChild(child, -1);

      int spWidth = 
          mItemWidth != 0 ? MeasureSpec.makeMeasureSpec(mItemWidth, MeasureSpec.EXACTLY) 
              : MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE, MeasureSpec.AT_MOST);

      int spHeight = 
          mItemHeight != 0 ? MeasureSpec.makeMeasureSpec(mItemHeight, MeasureSpec.EXACTLY) 
              : MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY);
			child.measure(spWidth, spHeight);
			
			rightEdge += child.getMeasuredWidth();

			if (mRightViewIndex == mAdapter.getCount() - 1) {
				mMaxX = mCurrentX + rightEdge - getWidth();
			}

			if (mMaxX < 0) {
				mMaxX = 0;
			}
			mRightViewIndex++;
		}

	}

	private void fillListLeft(int leftEdge, final int dx) {
		while (leftEdge + dx > 0 && mLeftViewIndex >= 0) {
			View child = mAdapter.getView(mLeftViewIndex, mRemovedViewQueue.poll(), this);

      if(mLeftViewIndex == mSelection) {
        child.setSelected(true);
      } else {
        child.setSelected(false);
      }
			
			addChild(child, 0);


      int spWidth = 
          mItemWidth != 0 ? MeasureSpec.makeMeasureSpec(mItemWidth, MeasureSpec.EXACTLY) 
              : MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE, MeasureSpec.AT_MOST);

      int spHeight = 
          mItemHeight != 0 ? MeasureSpec.makeMeasureSpec(mItemHeight, MeasureSpec.EXACTLY) 
              : MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY);
      child.measure(spWidth, spHeight);
      
			leftEdge -= child.getMeasuredWidth();
			mLeftViewIndex--;
			mDisplayOffset -= child.getMeasuredWidth();
		}
	}

	private void removeNonVisibleItems(final int dx) {
		View child = getChildAt(0);
		while (child != null && child.getRight() + dx <= 0) {
			mDisplayOffset += child.getMeasuredWidth();
			mRemovedViewQueue.offer(child);
			removeViewInLayout(child);
			mLeftViewIndex++;
			child = getChildAt(0);

		}

		child = getChildAt(getChildCount() - 1);
		while (child != null && child.getLeft() + dx >= getWidth()) {
			mRemovedViewQueue.offer(child);
			removeViewInLayout(child);
			mRightViewIndex--;
			child = getChildAt(getChildCount() - 1);
		}
	}

	private void positionItems(final int dx) {
		if (getChildCount() > 0) {
			mDisplayOffset += dx;
			int left = mDisplayOffset;
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				int childWidth = child.getMeasuredWidth();
				child.layout(left, 0, left + childWidth, child.getMeasuredHeight());
				left += childWidth;
			}
		}
	}

  public synchronized void scrollTo(int x) {
    mScroller.abortAnimation();
    requestLayout();
    mScroller.startScroll(mNextX, 0, x - mNextX, 0);
    requestLayout();
  }

  public synchronized void scrollBy(int x) {
    mScroller.abortAnimation();
    requestLayout();
    mScroller.startScroll(mNextX, 0, x, 0);
    requestLayout();
  }
  
  public int getNextX()
  {
    return mNextX;
  }

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean handled = super.dispatchTouchEvent(ev);
		handled |= mGesture.onTouchEvent(ev);
		return handled;
	}

	protected boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		synchronized (HorizontalListView.this) {
			mScroller.fling(mNextX, 0, (int) -velocityX, 0, 0, mMaxX, 0, 0);
		}
		requestLayout();

		return true;
	}

	protected boolean onDown(MotionEvent e) {
		mScroller.forceFinished(true);
		return true;
	}

	private OnGestureListener mOnGesture = new GestureDetector.SimpleOnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			return HorizontalListView.this.onDown(e);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return HorizontalListView.this.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

			synchronized (HorizontalListView.this) {
				mNextX += (int) distanceX;
			}
			requestLayout();

			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			Rect viewRect = new Rect();
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				int left = child.getLeft();
				int right = child.getRight();
				int top = child.getTop();
				int bottom = child.getBottom();
				viewRect.set(left, top, right, bottom);
				if (viewRect.contains((int) e.getX(), (int) e.getY())) {
					if (mOnItemClicked != null) {
						mOnItemClicked.onItemClick(HorizontalListView.this, child, mLeftViewIndex + 1 + i,
								mAdapter.getItemId(mLeftViewIndex + 1 + i));
					}
					if (mOnItemSelected != null) {
						mOnItemSelected.onItemSelected(HorizontalListView.this, child, mLeftViewIndex + 1 + i,
								mAdapter.getItemId(mLeftViewIndex + 1 + i));
					}
					break;
				}

			}
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			Rect viewRect = new Rect();
			int childCount = getChildCount();
			for (int i = 0; i < childCount; i++) {
				View child = getChildAt(i);
				int left = child.getLeft();
				int right = child.getRight();
				int top = child.getTop();
				int bottom = child.getBottom();
				viewRect.set(left, top, right, bottom);
				if (viewRect.contains((int) e.getX(), (int) e.getY())) {
					if (mOnItemLongClicked != null) {
						mOnItemLongClicked.onItemLongClick(HorizontalListView.this, child, mLeftViewIndex + 1 + i,
								mAdapter.getItemId(mLeftViewIndex + 1 + i));
					}
					break;
				}

			}
		}

	};

	public static interface OnScrollListener {
	  public void onScroll(int nextX, int position);
	}
}
