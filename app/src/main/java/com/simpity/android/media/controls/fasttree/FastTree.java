package com.simpity.android.media.controls.fasttree;

import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;

public class FastTree extends View implements View.OnClickListener, View.OnLongClickListener {

	private final static String EXPAND_STATE_KEY = "EXPAND_STATE"; 
	private final static String FILTER_KEY = "FILTER";
	private final static String SCROLL_POS_KEY = "SCROLL_POS";
	
	private ElementState[] mRootElementState;
	private String mFilter = null, mFilterSrc = null;;
	private int mTopVisibleBorder, mBottomVisibleBorder;
	private int mItemHeight = 48, mIconPadding = 4, mTextParagraph = 8;
	private int mTouchX, mTouchY;
	private FastTreeListener mTreeListener;
	private Bitmap mIconOpen, mIconClose;
	private Paint mTitlePaint, mSubTitlePaint, mSeparatorPaint;
	private int mTitleColor = Color.WHITE;
	private int mSubTitleColor = Color.LTGRAY;
	private int mTitleFontSize = 16;
	private int mSubTitleFontSize = 12;
	private ScrollView mScrollView;
	private ElementState mSelectedItem;
	private int toRestoreScrollPosition = -1;

	//--------------------------------------------------------------------------
	private final class ElementState {
		private final FastTreeItem mElement;
		private boolean mExpanded = true;
		private ElementState[] mChildren = null;
		private boolean mVisible = true;
		private final int mLevel;
		private boolean mSelected;

		//----------------------------------------------------------------------
		ElementState(FastTreeItem element, int level) {
			mElement = element;
			mLevel = level;
			rebuildTree();
		}

		//----------------------------------------------------------------------
		FastTreeItem getElement() {
			return mElement;
		}

		//----------------------------------------------------------------------
		void rebuildTree() {

			if(mElement == null)
				return;
			
			int child_count = mElement.getChildCount();

			mChildren = new ElementState[child_count];
			for (int i=0; i<child_count; i++) {
				mChildren[i] = new ElementState(mElement.getChild(i), mLevel+1);
				mChildren[i].rebuildTree();
			}
		}

		//----------------------------------------------------------------------
		void storeState(StringBuilder builder) {
			
			if (mElement != null && mElement.isGroup()) {			
				if (mExpanded) {
					
					builder.append('1');
					for (ElementState e : mChildren) {
						e.storeState(builder);
					}
					
				} else {
					
					builder.append('0');
				}
			}
		}
		
		//----------------------------------------------------------------------
		int restoreState(String state, int index) {
			
			if (mElement != null && mElement.isGroup() && index < state.length()) {

				mExpanded = state.charAt(index) == '1';
				index++;
				
				if (mExpanded) {
					for (ElementState e : mChildren) {
						index = e.restoreState(state, index);
					}
				}
			}
			
			return index;
		}
		
		//----------------------------------------------------------------------
		boolean isExpanded() {
			return mExpanded;
		}

		//----------------------------------------------------------------------
		void expand() {
			mExpanded = true;
		}

		//----------------------------------------------------------------------
		void collapse() {
			mExpanded = false;
		}

		//----------------------------------------------------------------------
		private boolean isVisible() {
			return mVisible;
		}

		//----------------------------------------------------------------------
		private boolean hasVisibleChildren() {

			if (mElement != null && mElement.isGroup()) {

				for (ElementState e : mChildren) {
					if (e.getElement().isGroup()) {
						if (e.hasVisibleChildren()) {
							return true;
						}
					} else {
						if (e.isVisible()) {
							return true;
						}
					}
				}
			}

			return false;
		}

		//----------------------------------------------------------------------
		int getVisibleCount() {

			int result = 0;

			if (mVisible && mElement != null) {

				if (!mElement.isGroup()) {

					result = 1;

				} else if (mExpanded) {

					for (ElementState e : mChildren) {
						result += e.getVisibleCount();
					}

					if (result > 0)
						result++;

				} else if (hasVisibleChildren()) {

					result = 1;
				}
			}

			return result;
		}

		//----------------------------------------------------------------------
		void applyFilter(String filter) {

			if (mElement != null && !mElement.isGroup()) {

				if (filter == null) {

					mVisible = true;

				} else {

					String title = mElement.getTitle();
					String sub_title = mElement.getSubTitle();

					mVisible = (title != null && title.toUpperCase().contains(filter)) ||
							(sub_title != null && sub_title.toUpperCase().contains(filter));
				}
				
			} else if (filter != null) {
				
				mExpanded = true;
			}

			if (mChildren != null) {
				for (ElementState e : mChildren) {
					e.applyFilter(filter);
				}
			}
		}

		//----------------------------------------------------------------------
		ElementState getElementByPos(int top, int y) {
			if (!mVisible || mElement == null || (mElement.isGroup() && !hasVisibleChildren()))
				return null;

			top += mItemHeight;
			if (y < top)
				return this;

			if (mChildren != null) {
				for (ElementState e : mChildren) {
					int bottom = top + e.getVisibleCount() * mItemHeight;

					if (y < bottom) {
						return e.getElementByPos(top, y);
					}

					top = bottom;
				}
			}

			return null;
		}

		//----------------------------------------------------------------------
		private String correctString(String text, int width, Paint paint) {
			if (paint.measureText(text) <= width)
				return text;

			width -= paint.measureText("...");
			int len = text.length() - 1;
			while (len > 0 && paint.measureText(text.substring(0, len)) > width) {
				len--;
			}

			return len > 0 ? text.substring(0, len) + "..." : "...";
		}

		//----------------------------------------------------------------------
		int draw(Canvas canvas, int y) {

			if (!mVisible || mElement == null || (mElement.isGroup() && !hasVisibleChildren()))
				return y;

			if (y < mBottomVisibleBorder && y + mItemHeight >= mTopVisibleBorder) {
				Bitmap icon;
				int x = mLevel * 16;
				int width = mScrollView != null ? mScrollView.getWidth() : getWidth();

				if (mSelected) {
					Paint paint = new Paint();
					int[] colors = new int[] {0xFFFFAA00, 0xFFFFC700, 0xFFFFB600}; 
					float[] pos = new float[] {0.0f, 0.5f, 1.0f};
					
					paint.setShader(new LinearGradient(0, y, 0, y + mItemHeight, colors, pos, Shader.TileMode.REPEAT));
					canvas.drawRect(new Rect(0, y, width, y + mItemHeight), paint);
				}
				
				canvas.drawLine(0, y + mItemHeight - 1,
						width, y + mItemHeight - 1, mSeparatorPaint);

				if (mElement.isGroup()) {
					icon = mExpanded ? mIconOpen : mIconClose;
					mTitlePaint.setTypeface(Typeface.DEFAULT_BOLD);
				} else {
					icon = mElement.getIcon();
					mTitlePaint.setTypeface(Typeface.DEFAULT);
				}
				
				if(!mElement.isAvailable()){
					mTitlePaint.setColor(Color.DKGRAY);
					mSubTitlePaint.setColor(Color.DKGRAY);
				}
				else{
					mTitlePaint.setColor(mTitleColor);
					mSubTitlePaint.setColor(mSubTitleColor);
				}
				
				if (icon != null) {
					canvas.drawBitmap(icon, x + mIconPadding, 
											mElement.isGroup() ? y + (mItemHeight - icon.getHeight()) / 2 : y + (mItemHeight - icon.getHeight()) / 4,
											mTitlePaint);

					x += mIconPadding + icon.getWidth() + (mElement.isGroup() ? mTextParagraph : mTextParagraph / 4);
				}

				if(mElement.isGroup())
					width -= mIconPadding * 2;
				else
					width -= mIconPadding;
				
				Bitmap[] right_icons = mElement.getRightIcons();
				if (right_icons != null && right_icons.length > 0) {
					for (int i=right_icons.length-1; i>=0; i--) {
						icon = right_icons[i];
						if(icon != null){
							width -= icon.getWidth();
							canvas.drawBitmap(icon, width,
									y + (mItemHeight - icon.getHeight()) / 2, mTitlePaint);
	
							width -= mIconPadding;
						}
					}
				}

				width -= x;

				String title = mElement.getTitle();
				if (title != null) {
					title = correctString(title, width, mTitlePaint);
				}

				String sub_title = mElement.getSubTitle();
				if (sub_title != null && mSubTitlePaint.measureText(sub_title) > width) {

					int pos1 = sub_title.indexOf("://");
					if (pos1 >= 0) {

						int pos2 = sub_title.indexOf('/', pos1 + 3);
						int pos3 = sub_title.lastIndexOf('/');

						if (pos2 > 0 && pos2 < pos3) {

							String server = sub_title.substring(0, pos2+1);
							String filename = sub_title.substring(pos3);
							int server_width = (int)mSubTitlePaint.measureText(server);
							int dots_width = (int)mSubTitlePaint.measureText("...");

							if (server_width + dots_width >= width) {

								sub_title = correctString(sub_title, width, mSubTitlePaint);

							} else {

								width -= server_width + dots_width;

								sub_title = server + "..." + correctString(filename, width, mSubTitlePaint);
							}

						} else {

							sub_title = correctString(sub_title, width, mSubTitlePaint);
						}

					} else {

						sub_title = correctString(sub_title, width, mSubTitlePaint);
					}
				}

				if (title != null) {

					FontMetrics title_metrics = mTitlePaint.getFontMetrics();
					int title_height = (int)(title_metrics.bottom - title_metrics.top);
					int title_y;

					if (sub_title != null) {

						FontMetrics sub_title_metrics = mSubTitlePaint.getFontMetrics();
						int sub_title_height = (int)(sub_title_metrics.bottom - sub_title_metrics.top);

						title_y = y + (mItemHeight - title_height - sub_title_height) / 2 - (int)title_metrics.top;
						canvas.drawText(sub_title, x, title_y + (int)title_metrics.bottom -
								(int)sub_title_metrics.top, mSubTitlePaint);

					} else {

						title_y = y + (mItemHeight - title_height) / 2 - (int)title_metrics.top;
					}

					canvas.drawText(title, x, title_y, mTitlePaint);

				} else if (sub_title != null) {

					FontMetrics sub_title_metrics = mSubTitlePaint.getFontMetrics();
					int sub_title_height = (int)(sub_title_metrics.bottom - sub_title_metrics.top);

					canvas.drawText(sub_title, x, y + (mItemHeight - sub_title_height) / 2 -
							(int)sub_title_metrics.top, mSubTitlePaint);
				}
			}

			y += mItemHeight;
			if (mChildren != null && mExpanded) {
				if (y < mBottomVisibleBorder) {

					int index = 0;
					for (ElementState e : mChildren) {
						y = e.draw(canvas, y);
						index++;

						if (y >= mBottomVisibleBorder) {
							y += mChildren.length - index;
							break;
						}
					}

				} else {

					y += getVisibleCount() * mItemHeight;
				}
			}

			return y;
		}

		//--------------------------------------------------------------------------
		void setSelected(boolean selected) {
			mSelected = selected;
		}

		//--------------------------------------------------------------------------
		boolean isSelected() {
			return mSelected;
		}
	}

	//--------------------------------------------------------------------------
	public FastTree(Context context) {
		super(context);
		init(context);
	}

	//--------------------------------------------------------------------------
	public FastTree(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	//--------------------------------------------------------------------------
	public FastTree(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	//--------------------------------------------------------------------------
	private void init(Context context) {

		setOnClickListener(this);
		setOnLongClickListener(this);

		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(dm);
		
		// TODO
		int min_size = Math.min(dm.widthPixels, dm.heightPixels);
		if (min_size >= 480) {
			
			mItemHeight = 64;
			mIconPadding = 6;
			mTextParagraph = 10;
			mTitleFontSize = 26;
			mSubTitleFontSize = 18;
			
		} else if (min_size == 240) {
			
			mItemHeight = 32;
			mIconPadding = 2;
			mTextParagraph = 6;
			mTitleFontSize = 12;
			mSubTitleFontSize = 8;
			
		} else {
			
			mItemHeight = 48;
			mIconPadding = 4;
			mTextParagraph = 8;
			mTitleFontSize = 18;
			mSubTitleFontSize = 12;
		}

	}

	//--------------------------------------------------------------------------
	public void setClosedFolderIcon(int res_id) {
		mIconClose = BitmapFactory.decodeResource(getContext().getResources(), res_id);
	}
	
	//--------------------------------------------------------------------------
	public void setOpenedFolderIcon(int res_id) {
		mIconOpen = BitmapFactory.decodeResource(getContext().getResources(), res_id);
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {

		int visible_count = getVisibleCount();

		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(dm);

		setMeasuredDimension(
				View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.UNSPECIFIED),
				View.MeasureSpec.makeMeasureSpec(visible_count * mItemHeight, View.MeasureSpec.UNSPECIFIED));
	}

	//--------------------------------------------------------------------------
	private int getVisibleCount() {
		int result = 0;

		if (mRootElementState != null) {
			for (ElementState item : mRootElementState)
				result += item.getVisibleCount();
		}

		return result;
	}

	//--------------------------------------------------------------------------
	public void setFastTreeListener(FastTreeListener listener) {
		mTreeListener = listener;
	}

	//--------------------------------------------------------------------------
	public void setRootElementsAndClearFilter(FastTreeItem[] root_elements) {
		mFilterSrc = null;
		mFilter = null;
		setRootElements(root_elements);
	}
	
	//--------------------------------------------------------------------------
	public void setRootElements(FastTreeItem[] root_elements) {
		if (root_elements == null) {
			mRootElementState = null;
			return;
		}
		int count = root_elements.length;
		ElementState[] oldElems = mRootElementState;

		toRestoreScrollPosition = mScrollView != null ? mScrollView.getScrollY() : -1;
		mRootElementState = new ElementState[count];
		int reminder = 0;
		for (int i = 0; i < count; i++) {
			ElementState newState = new ElementState(root_elements[i], 0);
			if(oldElems != null){
				for(int j = reminder; j < oldElems.length; j++){
					ElementState oldElem = oldElems[j];
					if(oldElem.mElement != null && newState.mElement != null){
						if(!oldElem.mElement.isGroup())
							break;
						if(oldElem.mElement == newState.mElement){
							reminder = j;
							if(oldElem.isExpanded()){
								newState.expand();
							}
							break;
						}
					}
				}
			}
			mRootElementState[i] = newState;
		}

		if (mFilterSrc != null) {
			applyFilter(mFilterSrc);
		} else {
			postRequestLayoutAndInvalidate();
		}
	}

	//--------------------------------------------------------------------------
	private void postRequestLayoutAndInvalidate() {
		post(new Runnable() {
			@Override
			public void run() {
				requestLayout();
				invalidate();
			}
		});
	}
	
	//--------------------------------------------------------------------------
	public void setRootElements(Vector<FastTreeItem> root_elements) {
		FastTreeItem[] elements_array = new FastTreeItem[root_elements.size()];
		root_elements.toArray(elements_array);
		setRootElements(elements_array);
	}

	//--------------------------------------------------------------------------
	public void rebuildTree() {
		for (ElementState e : mRootElementState) {
			e.rebuildTree();
		}
		
		if (mFilterSrc != null) {
			applyFilter(mFilterSrc);
		} else {
			postRequestLayoutAndInvalidate();
		}
	}
	
	//--------------------------------------------------------------------------
	public void applyFilter(String filter) {
		mFilterSrc = filter;
		mFilter = filter == null ? null : filter.toUpperCase();

		if (mRootElementState != null) {
			for (ElementState e : mRootElementState) {
				e.applyFilter(mFilter);
			}
		}

		postRequestLayoutAndInvalidate();
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onDraw(Canvas canvas) {

		if (mRootElementState == null) {
			return;
		}

		Rect clip_rect = canvas.getClipBounds();

		mTopVisibleBorder = clip_rect.top;
		mBottomVisibleBorder = (clip_rect.top == 0 && clip_rect.bottom == 0 ?
				Integer.MAX_VALUE : clip_rect.bottom);

		
		mSubTitlePaint = new Paint();

		mSubTitlePaint.setAntiAlias(true);
		mSubTitlePaint.setColor(mSubTitleColor);
		mSubTitlePaint.setTextSize(mSubTitleFontSize);

		mTitlePaint = new Paint();

		mTitlePaint.setAntiAlias(true);
		mTitlePaint.setColor(mTitleColor);
		mTitlePaint.setTextSize(mTitleFontSize);

		mSeparatorPaint = new Paint();
		mSeparatorPaint.setColor(Color.DKGRAY);
		mSeparatorPaint.setStrokeWidth(1);

		int y = 0;
		for (ElementState e : mRootElementState) {
			if (y >= mBottomVisibleBorder) {
				break;
			}
			y = e.draw(canvas, y);
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onTouchEvent (MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mTouchX = (int)event.getX();
			mTouchY = (int)event.getY();
			mSelectedItem = getElementByPos(mTouchY);
			if (mSelectedItem != null) {
				
				if (!mSelectedItem.getElement().isGroup() &&
					getRightIconByPos(mSelectedItem.getElement(), mTouchX) >= 0) {
					
					mSelectedItem = null;
					
				} else {
					
					mSelectedItem.setSelected(true);
					invalidate();
				}
			}
			break;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mSelectedItem != null) {
				if (mSelectedItem.isSelected()) {
					mSelectedItem.setSelected(false);
					invalidate();
				}
				
				mSelectedItem = null;
			}
			break;
		}

		return super.onTouchEvent(event);
	}

	//--------------------------------------------------------------------------
	private ElementState getElementByPos(int y) {

		if (mRootElementState == null) {
			return null;
		}

		int top = 0;
		for (ElementState e : mRootElementState) {
			int visible_count = e.getVisibleCount();
			int bottom = top + visible_count * mItemHeight;

			if (y < bottom) {
				return e.getElementByPos(top, y);
			}

			top = bottom;
		}

		return null;
	}

	//--------------------------------------------------------------------------
	private int getRightIconByPos(FastTreeItem item, int x) {
		Bitmap[] icons = item.getRightIcons();

		if (icons != null) {
			int width = mScrollView != null ? mScrollView.getWidth() : getWidth();

			for (int i=icons.length-1; i>=0; i--) {

				if (x >= width - icons[i].getWidth() - mIconPadding / 2)
					return i;

				width -= icons[i].getWidth() + mIconPadding;
			}
		}

		return -1;
	}

	//--------------------------------------------------------------------------
	private void expandGroup(ElementState group) {
		group.expand();
		
		if (mScrollView != null) {
			int y = 0;
			
			for (ElementState e : mRootElementState) {
				if (e == group) {
					final int y_pos = y;
					requestLayout();
					(new Handler()).post(new Runnable() {
						@Override
						public void run() {
							mScrollView.smoothScrollTo(0, y_pos);
						}
					});
					
					return;
				} else {
					y += e.getVisibleCount() * mItemHeight;
				}
			}
		}
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void onClick(View v) {
		
		if (mSelectedItem != null) {
			if (mSelectedItem.isSelected()) {
				mSelectedItem.setSelected(false);
				invalidate();
			}
			
			mSelectedItem = null;
		}
		
		ElementState e = getElementByPos(mTouchY);
		if (e != null) {
			FastTreeItem item = e.getElement();

			if (item.isGroup()) {
				
				if (e.isExpanded()) {
					e.collapse();
				} else {
					expandGroup(e);
				}
				requestLayout();
			} else {

				if (mTreeListener != null) {
					int icon_number = getRightIconByPos(item, mTouchX);

					if (icon_number >= 0)
						mTreeListener.onFastTreeRightIconClick(item, icon_number);
					else
						mTreeListener.onFastTreeItemClick(item);
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onLongClick(View v) {

		if (mSelectedItem != null) {
			if (mSelectedItem.isSelected()) {
				mSelectedItem.setSelected(false);
				invalidate();
			}
			
			mSelectedItem = null;
		}
		
		ElementState e = getElementByPos(mTouchY);
		if (e != null) {
			FastTreeItem item = e.getElement();

			if (item.isGroup()) {

				if (e.isExpanded()) {
					e.collapse();
				} else {
					expandGroup(e);
				}
				requestLayout();
			} else {

				if (mTreeListener != null) {
					int icon_number = getRightIconByPos(item, mTouchX);

					if (icon_number >= 0)
						mTreeListener.onFastTreeRightIconLongClick(item, icon_number);
					else
						mTreeListener.onFastTreeItemLongClick(item);
				}
			}

			return true;
		}

		return false;
	}
	
	//--------------------------------------------------------------------------
	public void storeState(Bundle state) {
		StringBuilder builder = new StringBuilder();
		
		for (ElementState e : mRootElementState) {
			e.storeState(builder);
		}
		
		state.putString(EXPAND_STATE_KEY, builder.toString());
		
		if (mFilterSrc != null)
			state.putString(FILTER_KEY, mFilterSrc);
		
		if (mScrollView != null)
			state.putInt(SCROLL_POS_KEY, mScrollView.getScrollY());
		
		Log.d("FastTree", "Scroll Y = " + getTop());
	}
	
	//--------------------------------------------------------------------------
	public void restoreState(Bundle state) {
		String state_str = state.getString(EXPAND_STATE_KEY);
		if (state_str != null) {
			int index = 0;
			
			for (ElementState e : mRootElementState) {
				index = e.restoreState(state_str, index);
			}
			
			String filter = state.getString(FILTER_KEY);
			applyFilter(filter);
			toRestoreScrollPosition = state.getInt(SCROLL_POS_KEY);
		}
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if(toRestoreScrollPosition != -1){
			mScrollView.scrollTo(0, toRestoreScrollPosition);
			toRestoreScrollPosition = -1;
		}
	}

	//--------------------------------------------------------------------------
	public void setTitleColor(int color) {
		mTitleColor = color;
	}

	//--------------------------------------------------------------------------
	public int getTitleColor() {
		return mTitleColor;
	}

	//--------------------------------------------------------------------------
	public void setSubTitleColor(int color) {
		mSubTitleColor = color;
	}

	//--------------------------------------------------------------------------
	public int getSubTitleColor() {
		return mSubTitleColor;
	}

	//--------------------------------------------------------------------------
	public String getFilter() {
		return mFilterSrc;
	}

	//--------------------------------------------------------------------------
	public void setScrollView(ScrollView mScrollView) {
		this.mScrollView = mScrollView;
	}

	//--------------------------------------------------------------------------
	public ScrollView getScrollView() {
		return mScrollView;
	}
	
	
}
