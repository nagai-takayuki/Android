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

import java.io.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class Prefs extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	private static final String TAG = "Prefs";

	private static final String OPT_READ_KUMADAI_ID = "readKumadaiID";
	private static final boolean DEFAULT_READ_KUMADAI_ID = false;

	private static final String OPT_ENABLE_BEEP = "enableBeep";
	private static final boolean DEFAULT_ENABLE_BEEP = true;

	private static final String OPT_LOCK_NUMBER = "lockNumber";
	private static final String DEFAULT_LOCK_NUMBER = "0000";

	private Preference detectPasoriPreference;
	private Preference logDeletePreference;
	AlertDialog.Builder logDeleteDialog;

	String LOG_FILENAME = "FCF.log";

	void createAlertDialog() {
		
		String title = getString(R.string.logDelete_title);
		logDeleteDialog = new AlertDialog.Builder(this);
		logDeleteDialog.setTitle(title);
		
		String	msg = getString(R.string.msg_really_delete_log);
		logDeleteDialog.setMessage(msg);
		logDeleteDialog.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						deleteLogData();
					}
				});
		logDeleteDialog.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Do nothing.
					}
				});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		OnPreferenceClickListener logDeleteListner = new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Log.d(TAG, "OnPreferenceClickListener: clicked preference="
						+ preference);
				logDeleteDialog.show();

				return true;
			}
		};

		OnPreferenceClickListener detectPasoriListener = new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Log.d(TAG, "OnPreferenceClickListener: clicked preference="
						+ preference);

				MyApplication application = (MyApplication) getApplication();
				application.detectPasori();

				if(detectPasoriPreference != null) {
					detectPasoriPreference.setSummary(application.deviceListString());	
				}
				return true;
			}
		};

		createAlertDialog();

		logDeletePreference = (Preference) findPreference("logDeletePreference");
		logDeletePreference.setOnPreferenceClickListener(logDeleteListner);

		detectPasoriPreference = (Preference) findPreference("detectPasoriPreference");
		detectPasoriPreference
				.setOnPreferenceClickListener(detectPasoriListener);

		refreshPreference();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	public void refreshPreference() {
		EditTextPreference readerNamePreference = (EditTextPreference) getPreferenceScreen()
				.findPreference("readerName");
		readerNamePreference.setSummary(readerNamePreference.getText());
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d(TAG, "onSharedPreferenceChanged: key=" + key);

		refreshPreference();
	}

	public static boolean doesReadKumadaiID(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(OPT_READ_KUMADAI_ID, DEFAULT_READ_KUMADAI_ID);
	}

	public static boolean isBeepEnabled(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(OPT_ENABLE_BEEP, DEFAULT_ENABLE_BEEP);
	}

	public static String lockNumber(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(OPT_LOCK_NUMBER, DEFAULT_LOCK_NUMBER);
	}

	void deleteLogData() {
		File sdcardDirectory = getBaseContext().getExternalFilesDir(null);
		String filePath = sdcardDirectory.getAbsolutePath() + File.separator
				+ LOG_FILENAME;

		try {
			File logFile = new File(filePath);
			if (logFile.exists()) {
				boolean flag = logFile.delete();
			}
		} catch (Exception e) {
			Log.d(TAG, "deleteLogData: exception=" + e);
		}
	}

}
