package com.simpity.android.media;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.simpity.android.media.Res;

public abstract class FourTabActivity extends Activity {

	private final static String PAGE_ID = "PAGE_ID";
	
	protected final static int ALL_LINKS_PAGE = 0;
	protected final static int FAVORITES_PAGE = 1;
	protected final static int HISTORY_PAGE = 2;
	protected final static int NEW_PAGE = 3;
	protected final static int SEARCH_COMMAND = 4;
	
	private int mCurrentPage = -1;
	private onTabClickListener mOnTabClickListener = null;
	
	public interface onTabClickListener{
		public void onTabClicked(int prevTabNumber, int tabNumber, int settedPage);
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(PAGE_ID, mCurrentPage);
	}
	
	//--------------------------------------------------------------------------
	protected void initTabs(Bundle savedState, onTabClickListener onTabClickListener) {

		mOnTabClickListener = onTabClickListener;
		
		int number = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(Res.string.pref_links_list_default_tab_key), "0"));
		if(getAllLinksPageId() == -1){
			number = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(Res.string.pref_local_media_default_tab_key), "2"));
		}
			
		View view = findViewById(getHistoryTabId());
		if(view != null){
			view.setOnClickListener(new TabClickListener(HISTORY_PAGE));
		}
		
		view = findViewById(getNewTabId());
		if(view != null){
			view.setOnClickListener(new TabClickListener(NEW_PAGE));
		}
		
		view = findViewById(getSearchTabId());
		if(view != null){
			view.setOnClickListener(new TabClickListener(SEARCH_COMMAND));
		}
		
		view = findViewById(getAllLinksTabId());
		if(view != null){
			view.setOnClickListener(new TabClickListener(ALL_LINKS_PAGE));
		}
		
		view = findViewById(getFavoritesTabId());
		if(view != null){
			view.setOnClickListener(new TabClickListener(FAVORITES_PAGE));
		}
		
		if (savedState != null) {
			number = savedState.getInt(PAGE_ID, number);
		}
		
		setCurrentPage(number, true);
	}
	
	//--------------------------------------------------------------------------
	abstract protected int getAllLinksPageId();
	abstract protected int getAllLinksTabId();
	abstract protected int getFavoritesPageId();
	abstract protected int getFavoritesTabId();
	abstract protected int getHistoryPageId();
	abstract protected int getHistoryTabId();
	abstract protected int getNewPageId();
	abstract protected int getNewTabId();
	abstract protected int getSearchTabId();
	abstract protected int getSearchViewId();
	
	//--------------------------------------------------------------------------
	private class TabClickListener implements View.OnClickListener {
		
		private final int mPage;
		
		TabClickListener(int page) {
			mPage = page;
		}
		
		@Override
		public void onClick(View v) {
			int prevTabNum = getCurrentPage();
			setCurrentPage(mPage, false);
			if(mOnTabClickListener != null)
			{
				mOnTabClickListener.onTabClicked(prevTabNum, mPage, mCurrentPage);
			}
			
			//Keyboard is hidden
			InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(),  0);
		}
	}
	
	//--------------------------------------------------------------------------
	protected void setCurrentPage(int number, boolean isRestoring) {
		
		View history_tab	= findViewById(getHistoryTabId());
		View history_page	= findViewById(getHistoryPageId());
		View new_tab		= findViewById(getNewTabId());
		View new_page		= findViewById(getNewPageId());
		View all_tab		= findViewById(getAllLinksTabId());
		View all_page		= findViewById(getAllLinksPageId());
		View favorites_tab	= findViewById(getFavoritesTabId());
		View favorites_page	= findViewById(getFavoritesPageId());
		View search_tab		= findViewById(getSearchTabId());
		View search_view	= findViewById(getSearchViewId());
		
		/*
		if(number == SEARCH_COMMAND && mCurrentPage == SEARCH_COMMAND){
			number = ALL_LINKS_PAGE;
		}
		*/
		if (mCurrentPage != number || isRestoring) {
			
			Display display = getWindowManager().getDefaultDisplay();
			
			boolean top = display.getWidth() < display.getHeight();   
			
			mCurrentPage = number;
			
			if (number == NEW_PAGE) {
				
				mCurrentPage = NEW_PAGE;
				
				//-----------------------------------------------
				if(all_tab != null)
					all_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if( all_page != null)	
					all_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(favorites_tab != null)
					favorites_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(favorites_page != null)
					favorites_page.setVisibility(View.INVISIBLE);
				
				
				//-----------------------------------------------
				if(history_tab != null)
					history_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(history_page != null)
					history_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(search_tab != null)
					search_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(search_view != null)
					search_view.setVisibility(View.GONE);
				
				//-----------------------------------------------
				if(new_tab != null)
					new_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_pushed : Res.drawable.tab_left_button_pushed);
				if(new_page != null)
					new_page.setVisibility(View.VISIBLE);
				
			} else if(number == HISTORY_PAGE){
				
				mCurrentPage = HISTORY_PAGE;
				
				//-----------------------------------------------
				if(all_tab != null) 
					all_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(all_page != null)
					all_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(favorites_tab != null) 
					favorites_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(favorites_page != null)
					favorites_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(new_tab != null) 
					new_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(new_page != null)
					new_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(search_tab != null)
					search_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(search_view != null)
					search_view.setVisibility(View.GONE);
				
				//-----------------------------------------------
				if(history_tab != null)
					history_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_pushed : Res.drawable.tab_left_button_pushed);
				if(history_page != null)
					history_page.setVisibility(View.VISIBLE);
				
			} else if(number == ALL_LINKS_PAGE){
				
				mCurrentPage = ALL_LINKS_PAGE;
				
				//-----------------------------------------------
				if(favorites_tab != null)
					favorites_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(favorites_page != null)
					favorites_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(history_tab != null) 
					history_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(history_page != null)
					history_page.setVisibility(View.INVISIBLE);
				//-----------------------------------------------
				if(new_tab != null)
					new_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(new_page != null)
					new_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(search_tab != null)
					search_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(search_view != null)
					search_view.setVisibility(View.GONE);
				
				//-----------------------------------------------
				if(all_tab != null)
					all_tab.setBackgroundResource(top ?	Res.drawable.tab_top_button_pushed : Res.drawable.tab_left_button_pushed);
				if(all_page != null)
					all_page.setVisibility(View.VISIBLE);
				
			}else if(number == FAVORITES_PAGE){
				
				mCurrentPage = FAVORITES_PAGE;
				
				//-----------------------------------------------
				if(all_tab != null)
					all_tab.setBackgroundResource(top ?	Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(all_page != null)
					all_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(history_tab != null)
					history_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(history_page != null)
					history_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(new_tab != null)
					new_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(new_page != null)
					new_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(search_tab != null)
					search_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(search_view != null)
					search_view.setVisibility(View.GONE);
				
				//-----------------------------------------------
				if(favorites_tab != null)
					favorites_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_pushed : Res.drawable.tab_left_button_pushed);
				if(favorites_page != null)
					favorites_page.setVisibility(View.VISIBLE);
				
			}else if(number == SEARCH_COMMAND){
				
				mCurrentPage = SEARCH_COMMAND;
				
				//-----------------------------------------------
				if(history_tab != null)
					history_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(history_page != null)
					history_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(new_tab != null)
					new_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(new_page != null)
					new_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(favorites_tab != null)
					favorites_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(favorites_page != null)
					favorites_page.setVisibility(View.INVISIBLE);
				
				//-----------------------------------------------
				if(all_tab != null)
					all_tab.setBackgroundResource(top ?	Res.drawable.tab_top_button_normal : Res.drawable.tab_left_button_normal);
				if(all_page != null)
					all_page.setVisibility(View.VISIBLE);
				
				//-----------------------------------------------
				if(search_tab != null)
					search_tab.setBackgroundResource(top ? Res.drawable.tab_top_button_pushed : Res.drawable.tab_left_button_pushed);
				if(search_view != null)
					search_view.setVisibility(View.VISIBLE);
			}
		}
	}
	
	//--------------------------------------------------------------------------
	public int getCurrentPage() {
		return mCurrentPage;
	}
}
