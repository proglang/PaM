/*
 * Copyright: Universität Freiburg, 2015
 * Authors: Marc Pfeifer <pfeiferm@tf.uni-freiburg.de>, GUI and user inputs.
 * 			Christian Schönweiß <schoenwc@tf.uni-freiburg.de>, Connection/Communication with Client
 */

package monitor.pack;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A activity representing the entering-screen to a
 * patient-monitor-simulator-app. After a successful connection to a simulation
 * controller, a on-button could be pressed to start the actual patient-monitor.
 */
public class EnterScreen extends Activity {

	// PRIVATE:
	protected static final String TAG = "EnterScreen";
	// FINAL MEMBERS:
	private final boolean DEBUG = false; // De(Activate) the debug-messages.
	private final int _hideSystemBarDelay = 2000; // Delay-time for hiding the system-bar.
	// Control-pattern to check ip user input.
	private final String _ipAdressPattern = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	// MEMBERS:
	private boolean _connectionAvailable;
	private static Context _context;
	static Client _client = null;
	private Pattern _ipPattern;
	private Matcher _ipMatcher;
	private String _sessionName;

	/**
	 * Do all the initializations and add Listeners.
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Deactivate the title-bar.
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Minimize the System-Bar and hide the Action-Bar if necessary.
        final View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 16) {
        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        // Minimize the System-Bar (and the Action-Bar) again after hideSystemBarDelay ms after a click on it.
        decorView.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
        	@Override
            public void onSystemUiVisibilityChange(int visibility) {
                new Handler().postDelayed(new Runnable() {
                    public void run(){
                    	// Minimize the System-Bar and hide the Action-Bar if necessary.
                    	if (Build.VERSION.SDK_INT >= 16) {
                        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
                        } else {
                        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                        }
                    }
                }, _hideSystemBarDelay);
            }
        });
        
        // Set the context.
		EnterScreen._context = getApplicationContext();

		setContentView(R.layout.activity_enter_screen);

		// Set some members.
		_connectionAvailable = false;

		// Hide the ip input section as default.
		showHideIPSettings(false);

		// Initialize Client
		_client = new Client(EnterScreen.this);
	}

	// PUBLIC:

	/**
	 * Show/Hides the manually ip input section.
	 * 
	 * @param show - states if the ip input section should be shown or not.
	 * 
	 */
	public void showHideIPSettings(boolean show) {
		EditText ipEditText = (EditText) this.findViewById(R.id.ipEditText);
		Button ipDoneButton = (Button) this.findViewById(R.id.ipDoneButton);
		if (show) {
			// Show the section and the keyboard.
			ipEditText.setVisibility(View.VISIBLE);
			ipDoneButton.setVisibility(View.VISIBLE);
			ipEditText.requestFocus();
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.toggleSoftInputFromWindow(
					ipEditText.getApplicationWindowToken(),
					InputMethodManager.SHOW_FORCED, 0);
		} else {
			// Hide the section and the keyboard.
			ipEditText.setVisibility(View.INVISIBLE);
			ipDoneButton.setVisibility(View.INVISIBLE);
			ipEditText.clearFocus();
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(
					ipEditText.getWindowToken(), 0);
		}
	}

	/**
	 * Show/Hides the session name change section.
	 * 
	 * @param show - states if the session name change section should be shown or not.
	 * 
	 */
	public void showHideConNameSettings(boolean show) {
		TextView conNameTextView = (TextView) this
				.findViewById(R.id.conNameTextView);
		Button changeConNameButton = (Button) this
				.findViewById(R.id.changeConNameButton);
		if (show) {
			// Show the section.
			conNameTextView.setVisibility(View.VISIBLE);
			changeConNameButton.setVisibility(View.VISIBLE);
		} else {
			// Hide the section.
			conNameTextView.setVisibility(View.INVISIBLE);
			changeConNameButton.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Is called after the controller ip was manually entered. Hides the
	 * keyboard and hands the ip over to the client.
	 * 
	 * @param view which called the method.
	 * 
	 */
	public void ipEnterDone(View view) {
		// Hide the keyboard.
		EditText ipEditText = (EditText) this.findViewById(R.id.ipEditText);
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(ipEditText.getWindowToken(), 0);
		// Get the ip.
		String ip = ipEditText.getText().toString();
		if (DEBUG) System.out.println("Entered controller ip: " + ip);
		// Check whether IP is valid
		_ipPattern = Pattern.compile(_ipAdressPattern);
		_ipMatcher = _ipPattern.matcher(ip);
		// If IP is in a valid format set Host IP and wake up ConnectionThread to connect.
		if (_ipMatcher.matches()) {
			try {
				_client.setHost(InetAddress.getByName(ip));
				_client.doNotifyOnConnect();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		} else {
			// If the IP is not valid, show it to the user.
			Toast inputIP = Toast.makeText(getApplicationContext(),
					"IP has not the right format: X.X.X.X", Toast.LENGTH_LONG);
			inputIP.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			inputIP.show();
		}
	}

	/**
	 * Is called when the session name should be changed. Opens a dailog where the user could
	 * input a new session name and handles the change of it.
	 * 
	 * (Based on code form Benjamin Voelker.)
	 * 
	 * @param view which called the method.
	 * 
	 */
	public void changeSessionName(View view) {
		// Open an alert dialog where the name of the session could be entered.
		final EditText sessionName = new EditText(this);
		new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
			.setTitle("Change session name")
			.setView(sessionName)
			// If Save button is pressed on the alert dialog.
			.setPositiveButton("Save",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int buttonId) {
						// Get the typed name.
						_sessionName = "" + sessionName.getText();
						if (TextUtils.isEmpty(_sessionName)) {
							// Display toast if the session name is empty.
							Toast input = Toast.makeText(
								getApplicationContext(),
								"Session name must not be empty",
								Toast.LENGTH_LONG);
							input.setGravity(Gravity.CENTER_VERTICAL,0, 0);
							input.show();
						} else {
							if (DEBUG) System.out.println("New service name: " + _sessionName);
							// Display new session name.
							TextView conNameTextView = (TextView) findViewById(R.id.conNameTextView);
							conNameTextView.setText("The session name is: \"" + _sessionName + "\"");
							_client.setmServiceName(_sessionName);
							Log.d(TAG, "Service Name was set on Monitor");
							_client.doNotifyOnDiscovery();
							Log.d(TAG, "Wakeup DiscoveryThread for discovery: " + _sessionName);
						}
					}
				})
				.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int ButtonId) {
							// Nothing to do here.
						}
					}).show();
	}

	/**
	 * Opens the main patient-monitor if connection to a controller is
	 * available.
	 * 
	 * @param view which called the method.
	 */
	public void openMonitorMain(View view) {
		if (_connectionAvailable) {
			final Intent mainMonitorIntent = new Intent(this, MonitorMainScreen.class);
			startActivity(mainMonitorIntent);
		}
	}

	/**
	 * Getter for the AppContext.
	 * 
	 * @return the app context.
	 */
	public static Context getAppContext() {
		return EnterScreen._context;
	}

	/**
	 * Changes the color of the on-button, hides/shows the
	 * "waiting for connection"-symbols and activates/deactivates the on-button,
	 * if a connection is available/not available.
	 * 
	 * @param value that states if connection is available or not
	 */
	public void setConnectionAvailable(final boolean value) {
		// Queue the UI-changes in the UI-thread.
		runOnUiThread(new Runnable() {
			public void run() {
				TextView waitForConTextView = (TextView) findViewById(R.id.waitForConTextView);
				ProgressBar waitForConProgressBar = (ProgressBar) findViewById(R.id.waitForConProgressBar);
				ImageButton onButton = (ImageButton) findViewById(R.id.onButton);
				if (value) {
					_connectionAvailable = true;
					waitForConTextView.setVisibility(View.INVISIBLE);
					waitForConProgressBar.setVisibility(View.INVISIBLE);
					onButton.setImageResource(R.drawable.onbuttonr_green);
				} else {
					_connectionAvailable = false;
					waitForConTextView.setVisibility(View.VISIBLE);
					waitForConProgressBar.setVisibility(View.VISIBLE);
					onButton.setImageResource(R.drawable.onbuttonr_gray);
				}
			}
		});

	}

}
