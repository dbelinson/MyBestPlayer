package com.simpity.android.media.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.simpity.android.media.AboutActivity;
import com.simpity.android.media.HelpActivity;
import com.simpity.android.media.PreferencesActivity;
import com.simpity.android.media.Res;

public final class DefaultMenu {

	//--------------------------------------------------------------------------
	public static void create (Menu menu) {
		MenuItem item;

		item = menu.add(Menu.NONE, Command.HELP, Menu.NONE, Res.string.help);
		item.setIcon(Res.drawable.icon_help);

		item = menu.add(Menu.NONE, Command.ABOUT, Menu.NONE, Res.string.about);
		item.setIcon(Res.drawable.icon_psa);
		
		item = menu.add(Menu.NONE, Command.SETTINGS, Menu.NONE, Res.string.settings);
		item.setIcon(Res.drawable.settings);
	}

	//--------------------------------------------------------------------------
	public static boolean onItemSelected (Activity activity, MenuItem item) {
		switch (item.getItemId()) {
		case Command.HELP:
			activity.startActivityForResult(new Intent(activity, HelpActivity.class), Command.HELP);
			return true;

		case Command.ABOUT:
			activity.startActivityForResult(new Intent(activity, AboutActivity.class), Command.ABOUT);
			return true;
		
		case Command.SETTINGS:
			activity.startActivityForResult(new Intent(activity, PreferencesActivity.class), Command.SETTINGS);
			return true;	
		}

		return false;
	}
}
