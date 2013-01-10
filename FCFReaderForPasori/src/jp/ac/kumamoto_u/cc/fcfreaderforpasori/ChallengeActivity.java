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

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ChallengeActivity extends Activity implements View.OnClickListener {

	private static final String TAG = "ChallengeActivity";

	TextView challengeNumberView;
	TextView challengeResultView;
	Button challengeButton;
	Button cancelButton;

	String msgWrongLockNumber;
	String msgCorrectLockNumber;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_challenge);

		challengeNumberView = (TextView) findViewById(R.id.textChallengeNumber);
		challengeResultView = (TextView) findViewById(R.id.textChallengeResult);

		challengeButton = (Button) findViewById(R.id.buttonChallengeOK);
		challengeButton.setOnClickListener(this);

		cancelButton = (Button) findViewById(R.id.buttonChallengeCancel);
		cancelButton.setOnClickListener(this);

		msgWrongLockNumber = this.getString(R.string.msg_wrong_lockNumber);
		msgCorrectLockNumber = this.getString(R.string.msg_correct_lockNumber);

	}

	@Override
	public void onResume() {
		super.onResume();

		challengeResultView.setText("");
	}

	public void onClick(View v) {
		if (v == challengeButton) {
			doChallenge();
		}

		if (v == cancelButton) {
			// Back to previous activity
			finish();
		}
	}

	public void doChallenge() {
		String challengeNumber = challengeNumberView.getText().toString();
		String lockNumber = Prefs.lockNumber(this);

		Log.d(TAG, "doChallenge challengeNumber=" + challengeNumber);
		Log.d(TAG, "doChallenge lockNumber=" + lockNumber);

		if ((challengeNumber == null) || !challengeNumber.equals(lockNumber)) {
			challengeResultView.setText(msgWrongLockNumber);
		} else {
			// Lock Number is correct. Unlock application
			challengeResultView.setText(msgCorrectLockNumber);
			MyApplication application = (MyApplication) getApplication();
			application.setLockMode(false);

			// Back to previous activity
			finish();
		}

	}
}
