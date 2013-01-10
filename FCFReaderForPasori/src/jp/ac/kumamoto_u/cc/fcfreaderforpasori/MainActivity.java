/*******************************************************************************
 * Copyright (c) 2013 Takayuki Nagai.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Takayuki Nagai - initial API and implementation
 ******************************************************************************/
package jp.ac.kumamoto_u.cc.fcfreaderforpasori;

import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TabHost.TabSpec;

// see. http://www.androidhive.info/2011/08/android-tab-layout-tutorial/
// http://stackoverflow.com/questions/3011361/alertdialog-input-text
// http://techbooster.jpn.org/andriod/ui/1140/
// https://sites.google.com/a/techdoctranslator.com/jp/android/guide/ui/dialogs

public class MainActivity extends TabActivity {

	private static final String TAG = "MainActivity";

	private String titleScreenLock;
	private String titleScreenUnlock;
	private MenuItem quitMenu;
	private MenuItem settingsMenu;
	private MenuItem screenLockMenu;

	static final int DIALOG_CHALLENGE_ID = 0;

	Dialog challengeDialog;
	private boolean challengeResult = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initializeTab();

		titleScreenLock = this.getString(R.string.screenlock_title);
		titleScreenUnlock = this.getString(R.string.screenunlock_title);

		quitMenu = (MenuItem) findViewById(R.id.quit);
		settingsMenu = (MenuItem) findViewById(R.id.settings);
	}

	@Override
	public void onStart() {
		super.onStart();
		MyApplication application = (MyApplication) getApplication();

		application.detectPasori();
	}

	@Override
	public void onResume() {
		super.onResume();

		Intent intent = getIntent();
		Log.d(TAG, "intent: " + intent);
		String action = intent.getAction();

		MyApplication application = (MyApplication) getApplication();
		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			application.pasoriAttached(intent);
		} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			application.pasoriDettached(intent);
		}

		invalidateOptionsMenu();
		if (application.isLockMode()) {
			setTabEnabled(false);
		} else {
			setTabEnabled(true);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.screenlock:
			MyApplication application = (MyApplication) getApplication();
			Log.d(TAG,
					"screenlockSelected: isLocked=" + application.isLockMode());

			// invalidateOptionsMenu();
			if (application.isLockMode()) {
				startActivity(new Intent(this, ChallengeActivity.class));
				
				/*
				if (success) {
					application.setLockMode(false);
					item.setTitle(titleScreenLock);

					// enable tabs
					setTabEnabled(true);
				}
				*/
			} else {
				application.setLockMode(true);
				item.setTitle(titleScreenUnlock);

				// disable tabs
				setTabEnabled(false);
			}
			return true;
		case R.id.settings:
			startActivity(new Intent(this, Prefs.class));

			return true;
		case R.id.quit:
			quit();

			return true;
		}

		return false;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		MyApplication application = (MyApplication) getApplication();

		Log.d(TAG, "onPrepareOptionsMenu: isLocked=" + application.isLockMode());
		Log.d(TAG, "onPrepareOptionsMenu: menu=" + menu);

		MenuItem quitMenu = menu.findItem(R.id.quit);
		MenuItem settingsMenu = menu.findItem(R.id.settings);
		MenuItem screenLockMenu = menu.findItem(R.id.screenlock);

		Log.d(TAG, "onPrepareOptionsMenu: quitMenu=" + quitMenu);
		Log.d(TAG, "onPrepareOptionsMenu: settingsMenu=" + settingsMenu);
		Log.d(TAG, "onPrepareOptionsMenu: settingsMenu=" + screenLockMenu);

		if(quitMenu == null) {
			getMenuInflater().inflate(R.menu.menu, menu);
		}
		
		if (application.isLockMode()) {
			if(screenLockMenu != null) {
				screenLockMenu.setTitle(titleScreenUnlock);
			}
			
			if(quitMenu != null) {
				quitMenu.setEnabled(false);
			}
			
			if(settingsMenu != null) {
				settingsMenu.setEnabled(false);
			}
		} else {
			if(screenLockMenu != null) {
				screenLockMenu.setTitle(titleScreenLock);
			}
			
			if(quitMenu != null) {
				quitMenu.setEnabled(true);
			}
			
			if(settingsMenu != null) {
				settingsMenu.setEnabled(true);
			}
		}

		return true;
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_CHALLENGE_ID:
			// do the work to define the pause Dialog
			challengeDialog = createChallengeDialog();
			dialog = challengeDialog;
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	Dialog createChallengeDialog() {

		AlertDialog dialog;
		String title = getString(R.string.logDelete_title);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to exit?")
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						challengeResult = true;
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								challengeResult = false;
							}
						});
		dialog = builder.create();

		return dialog;
	}

	public void initializeTab() {
		String title;

		// TabHostインスタンスを取得
		TabHost tabHost = getTabHost();

		// Tab for LogView
		title = this.getString(R.string.logview_title);
		TabSpec logViewSpec = tabHost.newTabSpec(title);
		// logViewSpec.setIndicator("Videos",
		// getResources().getDrawable(R.drawable.icon_videos_tab));
		logViewSpec.setIndicator(title);

		Intent logViewIntent = new Intent(this, LogViewActivity.class);
		logViewSpec.setContent(logViewIntent);

		// Tab for AttendanceView
		title = this.getString(R.string.attendance_title);
		TabSpec attendanceSpec = tabHost.newTabSpec(title);
		// attendanceSpec.setIndicator("Videos",
		// getResources().getDrawable(R.drawable.icon_videos_tab));
		attendanceSpec.setIndicator(title);

		Intent attendanceIntent = new Intent(this, AttendanceActivity.class);
		attendanceSpec.setContent(attendanceIntent);

		// Tab for Preference
		title = this.getString(R.string.preference_title);
		TabSpec preferenceSpec = tabHost.newTabSpec(title);
		// preferenceSpec.setIndicator("Videos",
		// getResources().getDrawable(R.drawable.icon_videos_tab));
		preferenceSpec.setIndicator(title);

		Intent preferenceIntent = new Intent(this, Prefs.class);
		preferenceSpec.setContent(preferenceIntent);

		// Tab for Config
		title = this.getString(R.string.title_activity_config);
		TabSpec configSpec = tabHost.newTabSpec(title);
		// preferenceSpec.setIndicator("Videos",
		// getResources().getDrawable(R.drawable.icon_videos_tab));
		configSpec.setIndicator(title);

		Intent configIntent = new Intent(this, ConfigActivity.class);
		configSpec.setContent(configIntent);

		tabHost.addTab(configSpec); // Adding config tab
		tabHost.addTab(attendanceSpec); // Adding attendance tab
		tabHost.addTab(logViewSpec); // Adding logView tab
		// tabHost.addTab(preferenceSpec); // Adding preference tab

		// Test: disable preference tab
		// getTabHost().getTabWidget().getChildAt(2).setEnabled(false);
	}

	public void setTabEnabled(boolean flag) {
		getTabHost().getTabWidget().getChildAt(0).setEnabled(flag);
		getTabHost().getTabWidget().getChildAt(1).setEnabled(flag);
		getTabHost().getTabWidget().getChildAt(2).setEnabled(flag);
	}

	public void quit() {
		/*
		 * handler = null; if(pasori != null){ pasori.reset(); }
		 */
		MyApplication application = (MyApplication) getApplication();
		application.flushExternalStorage();

		finish();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		MyApplication application = (MyApplication) getApplication();

		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
				if (application.isLockMode()) {
					return false;
				}
			}
		}
		return super.dispatchKeyEvent(event);
	}

	public boolean challenge() {
		// challengeDialog.show();
		showDialog(DIALOG_CHALLENGE_ID);

		// return true;

		return challengeResult;
	}

}
