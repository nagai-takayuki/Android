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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LogViewActivity extends Activity implements View.OnClickListener {

	private static final String TAG = "LogViewActivity";

	String LOG_FILENAME = "FCF.log";

	private static String msgLoadingData;
	private static String msgNoDataFound;

	TextView logView;
	private Button deleteButton;
	private Button reloadButton;
	AlertDialog.Builder logDeleteDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.logview);

		msgNoDataFound = this.getString(R.string.msg_no_data_found);
		msgLoadingData = this.getString(R.string.msg_loading_data);

		logView = (TextView) findViewById(R.id.logTextView);

		/*
		 * deleteButton = (Button) findViewById(R.id.deleteButton);
		 * deleteButton.setOnClickListener(this);
		 * 
		 * reloadButton = (Button) findViewById(R.id.reloadButton);
		 * reloadButton.setOnClickListener(this);
		 */
		logView.setText(msgLoadingData);
		
//		createAlertDialog();

		showLogData(logView);
	}

	@Override
	public void onResume() {
		super.onResume();
		showLogData(logView);
	}

	/*
	void createAlertDialog() {
		logDeleteDialog = new AlertDialog.Builder(this);
		logDeleteDialog.setTitle("Log Delete");
		logDeleteDialog.setMessage("Are really deleting log ?");
		logDeleteDialog.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						deleteLogData();
						logView.setText("deleted.");
					}
				});
		logDeleteDialog.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Do nothing.
					}
				});
	}
*/
	
	public void onClick(View v) {
		if (v == deleteButton) {
			logDeleteDialog.show();
		}

		if (v == reloadButton) {
			showLogData(logView);
		}
	}

	/*
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
*/
	
	void showLogData(TextView logView) {
		File sdcardDirectory = getBaseContext().getExternalFilesDir(null);
		String filePath = sdcardDirectory.getAbsolutePath() + File.separator
				+ LOG_FILENAME;
		String logStr = null;
		try {
			File logFile = new File(filePath);

			if (logFile.exists()) {
				FileInputStream istream = new FileInputStream(filePath);
				byte[] buffer = new byte[istream.available()];
				istream.read(buffer);
				istream.close();
				// �擾�ł��镶����
				// new String(readBytes);
				logStr = new String(buffer);
			} else {
				logStr = msgNoDataFound;
			}
		} catch (Exception e) {
			logStr = "Exception=" + e;
		}

		logView.setText(logStr);
	}
}
