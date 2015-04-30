/*
 * Copyright: Universität Freiburg, 2015
 * Authors: Marc Pfeifer <pfeiferm@tf.uni-freiburg.de> Everthing, except Defibirllator
 * 			Johannes Scherle <johannes.scherle@googlemail.com> Defibrillator
 */

package monitor.pack;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * A activity containing the main view of patient-monitor-simulation-app.
 */
public class MonitorMainScreen extends Activity {
	
  // PRIVATE:
	
	//FINAL MEMBERS:
	// private final boolean DEBUG = false;  // De(Activate) the debug-messages.
	private final int _hideSystemBarDelay = 2000;  // Delay-time for hiding the system-bar.
	private final int _heartBlinkDelay = 300;  // Blink time of the little heart that indicates the heart-beat (in ms).
	private final int _paramBlinkDelay = 300;  // Blink time of the parameters while ventfibri, ventflutter or asystole (in ms).
	private final int _paramBlinkRepeatTime = 1000; // Time between each blink of the parameters while ventfibri, ventflutter or asystole (in ms).
	private final int _nibpDelay = 12000;  // Time until the result of a NIBP-measurement is displayed after the start of it.
	private final int _incDecStepTime = 100;  // Delay time between each auto increase/decrease step of the alarm thresholds, when the buttons are pressed continuously.
	private final int _incDecAutoStartDelay = 500;  // Delay before the auto increase/decrease starts, when the buttons are pressed continuously.
	private final int _alarmPauseTime = 60; // Time in sec for which the alarm is paused if the alarm pause button is pressed.
	
	// Minimum and maximum alarm-thresholds for each parameter.
	// EKG upper value.
	private final int _ekgAlarmUpValueMax = 250;
	private final int _ekgAlarmUpValueMin = 20;
	// EKG lower value.
	private final int _ekgAlarmLowValueMax = 250;
	private final int _ekgAlarmLowValueMin = 20;
	// Diastolic blood pressure upper value.
	private final int _rrDiaAlarmUpValueMax = 150;
	private final int _rrDiaAlarmUpValueMin = 10;
	// Diastolic blood pressure lower value.
	private final int _rrDiaAlarmLowValueMax = 150;
	private final int _rrDiaAlarmLowValueMin = 10;
	// Systolic blood pressure upper value.
	private final int _rrSysAlarmUpValueMax = 250;
	private final int _rrSysAlarmUpValueMin = 20;
	// Systolic blood pressure lower value.
	private final int _rrSysAlarmLowValueMax = 250;
	private final int _rrSysAlarmLowValueMin = 20;
	// O2 saturation lower value.
	private final int _o2AlarmLowValueMax = 100;
	private final int _o2AlarmLowValueMin = 40;
	// CO2 upper value.
	private final int _co2AlarmUpValueMax = 100;
	private final int _co2AlarmUpValueMin = 15;
	// CO2 low value.
	private final int _co2AlarmLowValueMax = 100;
	private final int _co2AlarmLowValueMin = 15;
	
	// The increase/decrease step-size for the different alarm-thresholds.
	private final int _ekgAlarmIncDecStep = 5;
	private final int _rrAlarmIncDecStep = 5;
	private final int _o2AlarmIncDecStep = 1;
	private final int _co2AlarmIncDecStep = 1;
	
	// The threshold above a random must be to trigger a variation of a parameter.
	private final float _randThreshold = 0.7f;
	// The factor by which the current EKG value is divided to get the minimum number of
	// heart-beats with constant parameter values -> Means without a new random-variation.
	private final int _ekgDivFactor = 10;
	// The factor by which the current respiration value is divided to get the minimum number of
	// breaths with a constant parameter values -> Means without a new random-variation.
	private final int _respDivFactor = 10;
	// The multiplicators with which a random is multiplied to get a random offset for
	// the parameters -> Standard deviation.
	private final double _ekgRandMult = 1;
	private final double _rrRandMult = 1;
	private final double _o2RandMult = 1;
	private final double _co2RandMult = 1;
	private final double _respRandMult = 1;
	
	// MEMBERS:
	// Indicators if curves/values for each parameter are active.
	private boolean _ekgActive;
	private boolean _rrActive;
	private boolean _o2Active;
	private boolean _co2Active;
	private boolean _nibpActive;
	private boolean _respActive;
	// The current alarm threshold values for each parameter.
	private int _ekgAlarmUpValue;  // EKG upper value.
	private int _ekgAlarmLowValue;  // EKG lower value.
	private int _rrDiaAlarmUpValue;  // Diastolic blood pressure upper value.
	private int _rrDiaAlarmLowValue;  // Diastolic blood pressure lower value.
	private int _rrSysAlarmUpValue;  // Systolic blood pressure upper value.
	private int _rrSysAlarmLowValue;  // Sysstolic blood pressure lower value.
	private int _o2AlarmLowValue;  // O2 saturation lower value.
	private int _co2AlarmUpValue;  // CO2 upper value.
	private int _co2AlarmLowValue;  // CO2 lower value.
	// Indicator if the alarm in general is on or off.
	private boolean _alarmActive;
	// Indicators if the alarm for a parameter is active.
	private boolean _ekgAlarmOn;
	private boolean _rrAlarmOn;
	private boolean _o2AlarmOn;
	private boolean _co2AlarmOn;
	// Indicator if the alarm is currently paused.
	private boolean _alarmPaused;
	// Indicators if an alarm increase/decrease button is currently pressed.
	private boolean _firstAlarmUpperTHDownButtonPressed;
	private boolean _firstAlarmUpperTHUpButtonPressed;
	private boolean _firstAlarmLowerTHDownButtonPressed;
	private boolean _firstAlarmLowerTHUpButtonPressed;
	private boolean _secondAlarmUpperTHDownButtonPressed;
	private boolean _secondAlarmUpperTHUpButtonPressed;
	private boolean _secondAlarmLowerTHDownButtonPressed;
	private boolean _secondAlarmLowerTHUpButtonPressed;
	
	// Indicators if the settings-/defi-layouts are hidden.
	private boolean _settingsHidden;
	private boolean _defiHidden;
	// Settings- and defi-layouts.
	private LinearLayout _settingsLayout;
	private RelativeLayout _defiLayout;
	
	// Non inversive blood pressure measurement.
	private int _nibpAutoTime;  // The current auto-repeat-time.
	private boolean _nibpRunning;  // Indicates if a measurement is running.
	private boolean _autoNIBPRunning;  // Indicates if a auto-measurement is running.
	
	// The current vital-parameter-values.
	private int _diaBloodPressure;
	private int _sysBloodPressure;
	private int _ekgValue;
	private int _o2Value;
	private int _co2Value;
	private int _respValue;
	private Event.HeartPattern _heartPattern;
	// The current vital-parameter-values including some random variation.
	private int _ekgRandValue;
	private int _diaRandValue;
	private int _sysRandValue;
	private int _o2RandValue;
	private int _co2RandValue;
	private int _respRandValue;
	// Counters for the number of curve peaks of the different parameters.
	private int _ekgPeakCounter;
	private int _rrPeakCounter;
	private int _o2PeakCounter;
	private int _co2PeakCounter;
	private int _respPeakCounter;
	
	// Indicates if the EKG blinking while ventfibri or ventflutter is active.
	private boolean _ekgBlinkActive;
	// Indicates if the EKG, RR and O2 are blinking while asystole.
	private boolean _paramBlinkActive;
	
	// States which menu is currently open: 0 - EKG, 1 - RR, 2 - O2, 3 - CO2.
	private int _menuSelection;
	
	// Indicator if the EKG-sound is active.
	private boolean _ekgSoundOn;
	
	// Indicator if the settings are opened for the first time.
	private boolean _firstSettingsOpen;
	
	// Contains the time since the start of the timer.
	private int _timeInSec;
	// Indicates if the timer is running.
	private boolean _timerActive;
	
	// Some needed instances.
	private UpdateHandler _updateHandler;
	private Signalserver _signalServer;
	private GLActivity _glActivivty;
	private SoundHandler _soundHandler;
	private Timer _autoNibpTimer;
	private Timer _incDecTimer;
	private Timer _ekgBlinkTimer;
	private Timer _paramBlinkTimer;
	private Timer _alarmPauseTimer;
	private Timer _stopWatchTimer;
	private Random _randomGenerator;
	
	
	// Variables for the defi-mode
	private int _defiEnergy = 100;
	private int _defiEnergyUpperBound = 360;
	private int _defiEnergyLowerBound = 40;
	private int _defiEnergyThr = 150;
	private boolean _defiCharged = false;
	private boolean _defiCharging = false;
	Handler _handler; 
	/**
	 * Variables for the generation of the defi sound,
	 * not needed at the moment.
	private Timer _defiTimer;
	private int _defiTimerInterval = 500;
	private int _defiSoundFrequency = 100;
	private int _defiFrequencySteps = 40;
	private int _defi1kTime = 3000;
	private int _defi1kFrequency = 7000;
	private int _defiSteps = 0;
	private int _defiSoundIncrement = 0;
	*/
	
	
	/**
	 * Do all the initializations and add Listeners.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Hide the title-bar.
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Minimize the System-Bar and hide the Action-Bar if necessary.
        final View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 19) {
        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN 
        			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        			| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        			| View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else if (Build.VERSION.SDK_INT >= 16) {
        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN 
        			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        			| View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        
        // Minimize the System-Bar (and the Action-Bar) again after hideSystemBarDelay ms after a click on it.
        decorView.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
        	@Override
            public void onSystemUiVisibilityChange(int visibility) {
                new Handler().postDelayed(new Runnable() {
                    public void run(){
                    	if (Build.VERSION.SDK_INT >= 19) {
                        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN 
                        			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        			| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        			| View.SYSTEM_UI_FLAG_LOW_PROFILE);
                        } else if (Build.VERSION.SDK_INT >= 16) {
                        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN 
                        			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        			| View.SYSTEM_UI_FLAG_LOW_PROFILE);
                        } else {
                        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                        }
                    }
                }, _hideSystemBarDelay);
            }
        });
        
		setContentView(R.layout.activity_monitor_main_screen);
		
		// Initialize some members.
		
		// Deactivate all curves/values as default.
		_ekgActive = false;
		_rrActive = false;
		_o2Active = false;
		_co2Active = false;
		_nibpActive = false;
		_respActive = false;
		// Define the default alarm thresholds.
		_ekgAlarmUpValue = 110;  // EKG upper value.
		_ekgAlarmLowValue = 60;  // EKG lower value.
		_rrDiaAlarmUpValue = 100;  // Diastolic blood pressure upper value.
		_rrDiaAlarmLowValue = 40;  // Diastolic blood pressure lower value.
		_rrSysAlarmUpValue = 160;  // Systolic blood pressure upper value.
		_rrSysAlarmLowValue = 100;  // Sysstolic blood pressure lower value.
		_o2AlarmLowValue = 92;  // O2 saturation lower value.
		_co2AlarmUpValue = 45;  // CO2 upper value.
		_co2AlarmLowValue = 35;  // CO2 lower value.
		// Define the default activation states of the alarms.
		_alarmActive = false;
		_ekgAlarmOn = true;
		_rrAlarmOn = true;
		_o2AlarmOn = true;
		_co2AlarmOn = true;
		_alarmPaused = false;
		// Define the default pressed state of the alarm threshold increase/decrease buttons.
		_firstAlarmUpperTHDownButtonPressed = false;
		_firstAlarmUpperTHUpButtonPressed = false;
		_firstAlarmLowerTHDownButtonPressed = false;
		_firstAlarmLowerTHUpButtonPressed = false;
		_secondAlarmUpperTHDownButtonPressed = false;
		_secondAlarmUpperTHUpButtonPressed = false;
		_secondAlarmLowerTHDownButtonPressed = false;
		_secondAlarmLowerTHUpButtonPressed = false;
		
		// Deactivate the EKG and parameter blinking as default.
		_ekgBlinkActive = false;
		_paramBlinkActive = false;
		
		// Define the default hidden-state of the settings-/defi-layouts.
		_settingsHidden = true;
		_defiHidden = true;
		
		// Set the NIBP and the auto NIBP to "not running" as default.
		_nibpRunning = false;
		_autoNIBPRunning = false;
		
		// Initialize the number of curve peaks.
		_ekgPeakCounter = 0;
		_rrPeakCounter = 0;
		_o2PeakCounter = 0;
		_co2PeakCounter = 0;
		
		// Set the default menu selection to EKG.
		_menuSelection = 0;
		
		// Deactivate the EKG-sound as default.
		_ekgSoundOn = false;
		
		// Indicate the settings are opened for the first time.
		_firstSettingsOpen = true;
		
		// Set the time to 0 at start.
		_timeInSec = 0;
		// And deactivate the timer at start.
		_timerActive = false;
		
		// Create the instances.
		_settingsLayout = (LinearLayout) this.findViewById(R.id.settingsLayout);
		_defiLayout = (RelativeLayout) this.findViewById(R.id.defiLayout);
		_updateHandler = new UpdateHandler(this);
		_signalServer = new Signalserver(this);
		_soundHandler = new SoundHandler(this);
		_randomGenerator = new Random();
		_handler = new Handler();
		
		// Set the MainScreen in Client.
		EnterScreen._client.setMonitorMainScreen(this);
		
		// Set curve-view.
		_glActivivty = new GLActivity(this);
		RelativeLayout curveViewLayout = (RelativeLayout) this.findViewById(R.id.curveViewLayout);
		curveViewLayout.addView(_glActivivty);
		_glActivivty.setSignalserver(_signalServer);
		
		// Initialize the slider for the repeating-time of the autom. NIBP.
		SeekBar nibpAutoTimeSeekBar = (SeekBar) this.findViewById(R.id.nibpAutoTimeSeekBar);
		_nibpAutoTime = 180;  // Default value: 3 min.
		// Adjust the slider to this value.
		nibpAutoTimeSeekBar.setProgress(42);
		// Add a Listener for a change on the slider. 
		nibpAutoTimeSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
				nibpAutoTimeChange(progress);
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		
		// Hide the little bells in the parameter sections, which indicates that the alarm is active.
		ImageView ekgAlarmImageView = (ImageView) this.findViewById(R.id.ekgAlarmImageView);
		ekgAlarmImageView.setVisibility(View.INVISIBLE);
		ImageView ibpAlarmImageView = (ImageView) this.findViewById(R.id.ibpAlarmImageView);
		ibpAlarmImageView.setVisibility(View.INVISIBLE);
		ImageView nibpAlarmImageView = (ImageView) this.findViewById(R.id.nibpAlarmImageView);
		nibpAlarmImageView.setVisibility(View.INVISIBLE);
		ImageView o2AlarmImageView = (ImageView) this.findViewById(R.id.o2AlarmImageView);
		o2AlarmImageView.setVisibility(View.INVISIBLE);
		ImageView co2AlarmImageView = (ImageView) this.findViewById(R.id.co2AlarmImageView);
		co2AlarmImageView.setVisibility(View.INVISIBLE);
		// Hide the little speaker in the EKG parameter sections, which indicates that the EKG sound is active.
		ImageView ekgSoundImageView = (ImageView) this.findViewById(R.id.ekgSoundImageView);
		ekgSoundImageView.setVisibility(View.INVISIBLE);
		
		// Initialize the settings view with the EKG-settings.
		showAlarmSetting(true, false, false, false, "Frequency", "", "" + _ekgAlarmLowValue, "" + _ekgAlarmUpValue, "", "");
		setAlarmButton(_ekgAlarmOn);
		showSoundSetting(false);
		
		// Hide defi UI-elements.
		showHideDefiUiElements(false);
		
		// Move the little arrow in the menu on the right position after the first opening of the menu.
		// Therefore add a onDraw-Listener to recognize when the menu is fully extended for the first time.
		final LinearLayout settingsLayout = (LinearLayout)findViewById(R.id.settingsLayout);
		settingsLayout.getViewTreeObserver().addOnPreDrawListener(
				new ViewTreeObserver.OnPreDrawListener() {
			        public boolean onPreDraw() {
			        	// If the menu is extended for the first time.
					    if (settingsLayout.getMeasuredWidth() > 5 && _firstSettingsOpen) {
					    	// Move the arrow under the button.
					    	Button ekgSettingsButton = (Button) findViewById(R.id.ekgSettingsButton);
					    	ImageView setPointArrowImageView = (ImageView) findViewById(R.id.settingsPointingArrowImageView);
					    	int arrowPos = ekgSettingsButton.getLeft() + ekgSettingsButton.getWidth() / 2 - setPointArrowImageView.getWidth() / 2;
					    	RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) setPointArrowImageView.getLayoutParams();
					    	params.setMargins(arrowPos, 0, 0, 0); //Parameters for left, top, right and bottom margin.
					    	setPointArrowImageView.setLayoutParams(params);
					    	_firstSettingsOpen = false;
					    }
					    return true;
			        }
				});
		
		// Add Listeners to the 8 alarm increase/decrease buttons such that the auto increase/decrease while pressed could be stopped, if the button is released.
		final Button firstAlarmUpperTHDownButton = (Button) this.findViewById(R.id.firstAlarmUpperTHDownButton);
		firstAlarmUpperTHDownButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
			    if(event.getAction() == MotionEvent.ACTION_UP){
			    	_firstAlarmUpperTHDownButtonPressed = false;  // Publish the release of the button.
			    	v.setPressed(false);
			        return true;
			    } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
			    	v.performClick();
			    	v.setPressed(true);
			        return true;
			    }
			    return false;
			}
		});
		
		final Button firstAlarmUpperTHUpButton = (Button) this.findViewById(R.id.firstAlarmUpperTHUpButton);
		firstAlarmUpperTHUpButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
			    if(event.getAction() == MotionEvent.ACTION_UP){
			    	_firstAlarmUpperTHUpButtonPressed = false;  // Publish the release of the button.
			    	v.setPressed(false);
			        return true;
			    } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
			    	v.performClick();
			    	v.setPressed(true);
			        return true;
			    }
			    return false;
			}
		});
		
		final Button firstAlarmLowerTHDownButton = (Button) this.findViewById(R.id.firstAlarmLowerTHDownButton);
		firstAlarmLowerTHDownButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
			    if(event.getAction() == MotionEvent.ACTION_UP){
			    	_firstAlarmLowerTHDownButtonPressed = false;  // Publish the release of the button.
			    	v.setPressed(false);
			        return true;
			    } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
			    	v.performClick();
			    	v.setPressed(true);
			        return true;
			    }
			    return false;
			}
		});
		
		final Button firstAlarmLowerTHUpButton = (Button) this.findViewById(R.id.firstAlarmLowerTHUpButton);
		firstAlarmLowerTHUpButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
			    if(event.getAction() == MotionEvent.ACTION_UP){
			    	_firstAlarmLowerTHUpButtonPressed = false;  // Publish the release of the button.
			    	v.setPressed(false);
			        return true;
			    } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
			    	v.performClick();
			    	v.setPressed(true);
			        return true;
			    }
			    return false;
			}
		});
		
		final Button secondAlarmUpperTHDownButton = (Button) this.findViewById(R.id.secondAlarmUpperTHDownButton);
		secondAlarmUpperTHDownButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
			    if(event.getAction() == MotionEvent.ACTION_UP){
			    	_secondAlarmUpperTHDownButtonPressed = false;  // Publish the release of the button.
			    	v.setPressed(false);
			        return true;
			    } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
			    	v.performClick();
			    	v.setPressed(true);
			        return true;
			    }
			    return false;
			}
		});
		
		final Button secondAlarmUpperTHUpButton = (Button) this.findViewById(R.id.secondAlarmUpperTHUpButton);
		secondAlarmUpperTHUpButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
			    if(event.getAction() == MotionEvent.ACTION_UP){
			    	_secondAlarmUpperTHUpButtonPressed = false;  // Publish the release of the button.
			    	v.setPressed(false);
			        return true;
			    } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
			    	v.performClick();
			    	v.setPressed(true);
			        return true;
			    }
			    return false;
			}
		});
		
		final Button secondAlarmLowerTHDownButton = (Button) this.findViewById(R.id.secondAlarmLowerTHDownButton);
		secondAlarmLowerTHDownButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
			    if(event.getAction() == MotionEvent.ACTION_UP){
			    	_secondAlarmLowerTHDownButtonPressed = false;  // Publish the release of the button.
			    	v.setPressed(false);
			        return true;
			    } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
			    	v.performClick();
			    	v.setPressed(true);
			        return true;
			    }
			    return false;
			}
		});
		
		final Button secondAlarmLowerTHUpButton = (Button) this.findViewById(R.id.secondAlarmLowerTHUpButton);
		secondAlarmLowerTHUpButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
			    if(event.getAction() == MotionEvent.ACTION_UP){
			    	_secondAlarmLowerTHUpButtonPressed = false;  // Publish the release of the button.
			    	v.setPressed(false);
			        return true;
			    } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
			    	v.performClick();
			    	v.setPressed(true);
			        return true;
			    }
			    return false;
			}
		});
		
		// Write the default time (= 0) to the status-bar.
		updateTime();
		
		// Create a timer for a stop watch in the status bar which shows the time since the
		// start of the monitor. Not needed at the moment.
		/*Timer stopTimer = new Timer();
		stopTimer.schedule(new TimerTask() {
        	public void run(){
        		runOnUiThread(new Runnable() {
        			public void run() {
        				_timeInSec++;  // Increase the counter every second.
        				updateTime();  // Update the GUI.
        			}
        		});
    		}
    	}, 1000, 1000);*/
	}
	
	/**
	 * Hide the System-Bar (and the Action-Bar) again after reentering the app.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		// Minimize the System-Bar and hide the Action-Bar if necessary.
        final View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 19) {
        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN 
        			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        			| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        			| View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else if (Build.VERSION.SDK_INT >= 16) {
        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN 
        			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        			| View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
	}
	
	/**
	 * Destroy the soundHandler object when the app closed to stop the sounds.
	 */
	@Override
	protected void onDestroy() {
		if (_soundHandler != null) {
			_soundHandler.destroy();
		}
		super.onDestroy();
	}
	
	/**
     * Calculates, displays and stores the new auto-repeating-time for the NIBP. If necessary
     * the current running NIBP is updated with a new repeating time. This method
     * is called each time there is change on the auto-time-slider in the NIBP-section. 
     * 
     * @param progress of the slider.
     */
	private void nibpAutoTimeChange(int progress) {
		// Old calculation -> Not needed at the moment.
		/*_nibpAutoTime = 20 + progress * 2;
		int timeMin = _nibpAutoTime / 60;
		int timeSec = _nibpAutoTime % 60;
		TextView nibpAutoTimeTextView = (TextView) this.findViewById(R.id.nibpAutoTimeTextView);
		if (timeSec < 10) {
			nibpAutoTimeTextView.setText("" + timeMin + ":0" + timeSec);
		} else {
			nibpAutoTimeTextView.setText("" + timeMin + ":" + timeSec);
		}*/
		// Calculate and display the new auto repeat time.
		int timeMin = 0;
		if (progress <= 16) {
			_nibpAutoTime = 60;
			timeMin = 1;
		} else if (progress <= 33) {
			_nibpAutoTime = 120;
			timeMin = 2;
		} else if (progress <= 50) {
			_nibpAutoTime = 180;
			timeMin = 3;
		} else if (progress <= 67) {
			_nibpAutoTime = 300;
			timeMin = 5;
		} else if (progress <= 84) {
			_nibpAutoTime = 600;
			timeMin = 10;
		} else {
			_nibpAutoTime = 1200;
			timeMin = 20;
		}
		TextView nibpAutoTimeTextView = (TextView) this.findViewById(R.id.nibpAutoTimeTextView);
		nibpAutoTimeTextView.setText("" + timeMin + " min");
		// Update a running NIBP with a new auto repeat time if necessary.
		if (_autoNIBPRunning) {
			if (_autoNibpTimer != null) _autoNibpTimer.cancel();
			_autoNibpTimer = new Timer();
			_autoNibpTimer.scheduleAtFixedRate(new TimerTask() {
	        	public void run(){
	        		runOnUiThread(new Runnable() {
	        			public void run() {
	        				runNIBP();
	        			}
	        		});
	    		}
        	}, (_nibpAutoTime * 1000), (_nibpAutoTime * 1000));
		}
	}
	
	/**
     * Makes the EKG-heart blink once for heartBlinkDelay ms.
     */
	private void blinkHeart() {
		final ImageView ekgHeartImageView = (ImageView) this.findViewById(R.id.ekgHeartImageView);
		ekgHeartImageView.setVisibility(View.INVISIBLE);
		new Handler().postDelayed(new Runnable() {
            public void run(){
            	ekgHeartImageView.setVisibility(View.VISIBLE);
            }
        }, _heartBlinkDelay);
	}
	
	/**
     * Shows/hides the sound settings.
     * 
     * @param visible - states if the sound settings are visible or not.
     */
	private void showSoundSetting(boolean visible) {
		RelativeLayout soundSettingsLayout = (RelativeLayout) this.findViewById(R.id.soundSettingsLayout);
		if (visible) {
			soundSettingsLayout.setVisibility(View.VISIBLE);
		} else {
			soundSettingsLayout.setVisibility(View.INVISIBLE);
		}
	}
	
	/**
     * Shows/hides the alarm settings or only parts of it and fills it with given parameters.
     * 
     * @param alarm1 - States if the settings for the first alarm are visible or not. If alarm1 and alarm2 are set to false, the whole alarm settings section isn't visible.
     * @param oneValue - States if only one value (the upper or lower one) of the first alarm is determinable.
     * @param onlyUpper - States which of the values (the upper or lower one) is determinable if oneValue is true.
     * @param alarm2 - States if the settings for the second alarm are visible or not. If alarm1 and alarm2 are set to false, the whole alarm settings section isn't visible.
     * @param alarm1Title - The title of the first alarm which will be displayed.
     * @param alarm2Title - The title of the second alarm which will be displayed.
     * @param alarm1LowDef - The default lower value for the first alarm which will be displayed.
     * @param alarm1UpDef - The default upper value for the first alarm which will be displayed.
     * @param alarm2LowDef - The default lower value for the second alarm which will be displayed.
     * @param alarm2UpDef - The default upper value for the second alarm which will be displayed.
     */
	private void showAlarmSetting(boolean alarm1, boolean oneValue, boolean onlyUpper, boolean alarm2, String alarm1Title,
			String alarm2Title, String alarm1LowDef, String alarm1UpDef, String alarm2LowDef, String alarm2UpDef) {
		RelativeLayout alarmSettingsLayout = (RelativeLayout) this.findViewById(R.id.alarmSettingsLayout);
		if (!alarm1 && !alarm2) {
			// If no alarm is needed, hide all.
			alarmSettingsLayout.setVisibility(View.INVISIBLE);
		} else {
			// If one or more alarm are needed, check which one, display them and set the titles and default threshold-values. 
			alarmSettingsLayout.setVisibility(View.VISIBLE);
			TextView firstAlarmTitleTextView = (TextView) this.findViewById(R.id.firstAlarmTitleTextView);
			TextView firstAlarmUpperTHTextView = (TextView) this.findViewById(R.id.firstAlarmUpperTHTextView);
			TextView firstAlarmUpperTHValueTextView = (TextView) this.findViewById(R.id.firstAlarmUpperTHValueTextView);
			TextView firstAlarmLowerTHTextView = (TextView) this.findViewById(R.id.firstAlarmLowerTHTextView);
			TextView firstAlarmLowerTHValueTextView = (TextView) this.findViewById(R.id.firstAlarmLowerTHValueTextView);
			Button firstAlarmUpperTHUpButton = (Button) this.findViewById(R.id.firstAlarmUpperTHUpButton);
			Button firstAlarmUpperTHDownButton = (Button) this.findViewById(R.id.firstAlarmUpperTHDownButton);
			Button firstAlarmLowerTHUpButton = (Button) this.findViewById(R.id.firstAlarmLowerTHUpButton);
			Button firstAlarmLowerTHDownButton = (Button) this.findViewById(R.id.firstAlarmLowerTHDownButton);
			if (alarm1) {
				firstAlarmTitleTextView.setVisibility(View.VISIBLE);
				if (oneValue && onlyUpper) {
					firstAlarmUpperTHTextView.setVisibility(View.VISIBLE);
					firstAlarmUpperTHValueTextView.setVisibility(View.VISIBLE);
					firstAlarmLowerTHTextView.setVisibility(View.INVISIBLE);
					firstAlarmLowerTHValueTextView.setVisibility(View.INVISIBLE);
					firstAlarmUpperTHUpButton.setVisibility(View.VISIBLE);
					firstAlarmUpperTHDownButton.setVisibility(View.VISIBLE);
					firstAlarmLowerTHUpButton.setVisibility(View.INVISIBLE);
					firstAlarmLowerTHDownButton.setVisibility(View.INVISIBLE);
				} else if (oneValue && !onlyUpper) {
					firstAlarmUpperTHTextView.setVisibility(View.INVISIBLE);
					firstAlarmUpperTHValueTextView.setVisibility(View.INVISIBLE);
					firstAlarmLowerTHTextView.setVisibility(View.VISIBLE);
					firstAlarmLowerTHValueTextView.setVisibility(View.VISIBLE);
					firstAlarmUpperTHUpButton.setVisibility(View.INVISIBLE);
					firstAlarmUpperTHDownButton.setVisibility(View.INVISIBLE);
					firstAlarmLowerTHUpButton.setVisibility(View.VISIBLE);
					firstAlarmLowerTHDownButton.setVisibility(View.VISIBLE);
				} else {
					firstAlarmUpperTHTextView.setVisibility(View.VISIBLE);
					firstAlarmUpperTHValueTextView.setVisibility(View.VISIBLE);
					firstAlarmLowerTHTextView.setVisibility(View.VISIBLE);
					firstAlarmLowerTHValueTextView.setVisibility(View.VISIBLE);
					firstAlarmUpperTHUpButton.setVisibility(View.VISIBLE);
					firstAlarmUpperTHDownButton.setVisibility(View.VISIBLE);
					firstAlarmLowerTHUpButton.setVisibility(View.VISIBLE);
					firstAlarmLowerTHDownButton.setVisibility(View.VISIBLE);
				}
				firstAlarmTitleTextView.setText(alarm1Title);
				firstAlarmUpperTHValueTextView.setText(alarm1UpDef);
				firstAlarmLowerTHValueTextView.setText(alarm1LowDef);
			} else {
				firstAlarmTitleTextView.setVisibility(View.INVISIBLE);
				firstAlarmUpperTHTextView.setVisibility(View.INVISIBLE);
				firstAlarmUpperTHValueTextView.setVisibility(View.INVISIBLE);
				firstAlarmLowerTHTextView.setVisibility(View.INVISIBLE);
				firstAlarmLowerTHValueTextView.setVisibility(View.INVISIBLE);
				firstAlarmUpperTHUpButton.setVisibility(View.INVISIBLE);
				firstAlarmUpperTHDownButton.setVisibility(View.INVISIBLE);
				firstAlarmLowerTHUpButton.setVisibility(View.INVISIBLE);
				firstAlarmLowerTHDownButton.setVisibility(View.INVISIBLE);
			}
			
			TextView secondAlarmTitleTextView = (TextView) this.findViewById(R.id.secondAlarmTitleTextView);
			TextView secondAlarmUpperTHTextView = (TextView) this.findViewById(R.id.secondAlarmUpperTHTextView);
			TextView secondAlarmUpperTHValueTextView = (TextView) this.findViewById(R.id.secondAlarmUpperTHValueTextView);
			TextView secondAlarmLowerTHTextView = (TextView) this.findViewById(R.id.secondAlarmLowerTHTextView);
			TextView secondAlarmLowerTHValueTextView = (TextView) this.findViewById(R.id.secondAlarmLowerTHValueTextView);
			Button secondAlarmUpperTHUpButton = (Button) this.findViewById(R.id.secondAlarmUpperTHUpButton);
			Button secondAlarmUpperTHDownButton = (Button) this.findViewById(R.id.secondAlarmUpperTHDownButton);
			Button secondAlarmLowerTHUpButton = (Button) this.findViewById(R.id.secondAlarmLowerTHUpButton);
			Button secondAlarmLowerTHDownButton = (Button) this.findViewById(R.id.secondAlarmLowerTHDownButton);
			if (alarm2) {
				secondAlarmTitleTextView.setVisibility(View.VISIBLE);
				secondAlarmUpperTHTextView.setVisibility(View.VISIBLE);
				secondAlarmUpperTHValueTextView.setVisibility(View.VISIBLE);
				secondAlarmLowerTHTextView.setVisibility(View.VISIBLE);
				secondAlarmLowerTHValueTextView.setVisibility(View.VISIBLE);
				secondAlarmUpperTHUpButton.setVisibility(View.VISIBLE);
				secondAlarmUpperTHDownButton.setVisibility(View.VISIBLE);
				secondAlarmLowerTHUpButton.setVisibility(View.VISIBLE);
				secondAlarmLowerTHDownButton.setVisibility(View.VISIBLE);
				secondAlarmTitleTextView.setText(alarm2Title);
				secondAlarmUpperTHValueTextView.setText(alarm2UpDef);
				secondAlarmLowerTHValueTextView.setText(alarm2LowDef);
			} else {
				secondAlarmTitleTextView.setVisibility(View.INVISIBLE);
				secondAlarmUpperTHTextView.setVisibility(View.INVISIBLE);
				secondAlarmUpperTHValueTextView.setVisibility(View.INVISIBLE);
				secondAlarmLowerTHTextView.setVisibility(View.INVISIBLE);
				secondAlarmLowerTHValueTextView.setVisibility(View.INVISIBLE);
				secondAlarmUpperTHUpButton.setVisibility(View.INVISIBLE);
				secondAlarmUpperTHDownButton.setVisibility(View.INVISIBLE);
				secondAlarmLowerTHUpButton.setVisibility(View.INVISIBLE);
				secondAlarmLowerTHDownButton.setVisibility(View.INVISIBLE);
			}
		}
	}
	
	/**
	 * Toggles the alarm button title between "ON" and "OFF".
	 * 
	 * @param on - Flag which indicates if the alarm is on.
	 */
	private void setAlarmButton(boolean on) {
		Button alarmOnOffButton = (Button) this.findViewById(R.id.alarmOnOffButton);
		if (on) {
			alarmOnOffButton.setText("ON");
			alarmOnOffButton.setTextColor(Color.BLACK);
		} else {
			alarmOnOffButton.setText("OFF");
			alarmOnOffButton.setTextColor(Color.WHITE);
		}
	}
	
	/**
	 * Toggles the sound button title between "ON" and "OFF".
	 * 
	 * @param on - Flag which indicates if the sound is on.
	 */
	private void setSoundButton(boolean on) {
		Button soundOnOffButton = (Button) this.findViewById(R.id.soundOnOffButton);
		if (on) {
			soundOnOffButton.setText("ON");
			soundOnOffButton.setTextColor(Color.BLACK);
		} else {
			soundOnOffButton.setText("OFF");
			soundOnOffButton.setTextColor(Color.WHITE);
		}
	}
	
	/**
	 * A functions which activates/deactivates the corresponding alarm if the current vital-parameter-values are
	 * above or below the current alarm thresholds. The alarm is only triggered if the parameter itself is active, the
	 * alarm of the parameter is active and general alarm is active.
	 */
	private void checkAlarm() {
		// EKG:
		if ((_ekgRandValue < _ekgAlarmLowValue || _ekgRandValue > _ekgAlarmUpValue) && _ekgAlarmOn && _ekgActive && _alarmActive) {
			_soundHandler.setEKGAlarm(true);
		} else {
			_soundHandler.setEKGAlarm(false);
		}
		// Blood pressure:
		if ((_diaRandValue < _rrDiaAlarmLowValue || _diaRandValue > _rrDiaAlarmUpValue ||
				_sysRandValue < _rrSysAlarmLowValue || _sysRandValue > _rrSysAlarmUpValue) && _rrAlarmOn && _rrActive && _alarmActive) {
			_soundHandler.setRRAlarm(true);
		} else {
			_soundHandler.setRRAlarm(false);
		}
		// O2 Saturation:
		if (_o2RandValue < _o2AlarmLowValue && _o2AlarmOn && _o2Active && _alarmActive) {
			_soundHandler.setO2Alarm(true);
		} else {
			_soundHandler.setO2Alarm(false);
		}
		// CO2:
		if ((_co2RandValue < _co2AlarmLowValue || _co2RandValue > _co2AlarmUpValue) && _co2AlarmOn && _co2Active && _alarmActive) {
			_soundHandler.setCO2Alarm(true);
		} else {
			_soundHandler.setCO2Alarm(false);
		}
	}
	
	/**
	 * A function which simulates a non inversive blood pressure measurement by playing a pump
	 * sound and showing a delayed measurement result.
	 */
	private void runNIBP() {
		_nibpRunning = true;
		// Indicate the running measurement with a yellow button.
		Button nibpStartButton = (Button) findViewById(R.id.nibpStartButton);
		nibpStartButton.setBackgroundResource(R.drawable.yellow_button);
		// Play the pump sound and trigger a delayed result-display.
		_soundHandler.playBPSound();
		new Handler().postDelayed(new Runnable() {
            public void run(){
            	TextView ekgValueTextView = (TextView) findViewById(R.id.nibpValueTextView);
            	// Only show the actual value if NIBP is set to active.
            	if (_nibpActive) {
            		ekgValueTextView.setText("" + _sysBloodPressure + "/" + _diaBloodPressure);
            	} else {
            		ekgValueTextView.setText("0/0");
            	}
				// Change the button back to its normal green color.
				Button nibpStartButton = (Button) findViewById(R.id.nibpStartButton);
				nibpStartButton.setBackgroundResource(R.drawable.green_button);
				_nibpRunning = false;
            }
        }, _nibpDelay);
	}
	
	/**
	 * Writes the current time science the start of the timer in the status-bar. The format
	 * is mm:ss.
	 */
	private void updateTime() {
		// Schedule the update of the timer in main GUI-thread.
		runOnUiThread(new Runnable() {
			public void run() {
				TextView statusBarTitle = (TextView) findViewById(R.id.statusBarTitle);
				// Calculate the minute and second value out of the second-counter.
				int sec = _timeInSec % 60;
				int min = _timeInSec / 60;
				// If necessary add leading zeros to the well that it looks nice.
				if (sec < 10 && min < 10) {
					statusBarTitle.setText("Timer:   0" + min + ":0" + sec);
				} else if (sec < 10) {
					statusBarTitle.setText("Timer:   " + min + ":0" + sec);
				} else if (min < 10) {
					statusBarTitle.setText("Timer:   0" + min + ":" + sec);
				} else {
					statusBarTitle.setText("Timer:   " + min + ":" + sec);
				}
			}
		});
	}
	
	/**
     * Makes the EKG-parameter blink once for paramBlinkDelay ms.
     */
	private void blinkEKGParam() {
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView ekgValueTextView = (TextView) findViewById(R.id.ekgValueTextView);
				if(_ekgActive) ekgValueTextView.setVisibility(View.INVISIBLE);
				new Handler().postDelayed(new Runnable() {
		            public void run(){
		            	runOnUiThread(new Runnable() {
		        			public void run() {
		        				ekgValueTextView.setVisibility(View.VISIBLE);
		        			}
		        		});
		            }
		        }, _paramBlinkDelay);
			}
		});
	}
	
	/**
     * Makes the EKG, RR and O2-parameter blink once for paramBlinkDelay ms if they are active.
     */
	private void blinkParam() {
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView ekgValueTextView = (TextView) findViewById(R.id.ekgValueTextView);
				final TextView ibpValueTextView = (TextView) findViewById(R.id.ibpValueTextView);
				final TextView o2ValueTextView = (TextView) findViewById(R.id.o2ValueTextView);
				if(_ekgActive) ekgValueTextView.setVisibility(View.INVISIBLE);
				if(_rrActive) ibpValueTextView.setVisibility(View.INVISIBLE);
				if(_o2Active) o2ValueTextView.setVisibility(View.INVISIBLE);
				new Handler().postDelayed(new Runnable() {
		            public void run(){
		            	runOnUiThread(new Runnable() {
		        			public void run() {
		        				ekgValueTextView.setVisibility(View.VISIBLE);
		        				ibpValueTextView.setVisibility(View.VISIBLE);
		        				o2ValueTextView.setVisibility(View.VISIBLE);
		        			}
		        		});
		            }
		        }, _paramBlinkDelay);
			}
		});
	}
	
	/**
	 * Shows/Hides all Difibrillator GUI elements-
	 * 
	 * @param show - states if the elments should be shown or not.
	 */
	private void showHideDefiUiElements(boolean show) {
		RelativeLayout defiLayout = (RelativeLayout) this.findViewById(R.id.defiLayout);
		if (show) {
			defiLayout.setVisibility(View.VISIBLE);
		} else {
			defiLayout.setVisibility(View.INVISIBLE);
		}
	}
	
  // PUBLIC:
	
	/**
     * Opens/Closes the settings-view via its weight-factor. If the defi-view is already open,
     * close it first.
     * 
     * @param view which called the method.
     */
	public void openCloseSettings(View view) {
		LinearLayout.LayoutParams loParamsSettings = (LinearLayout.LayoutParams) _settingsLayout.getLayoutParams();
		Button openCloseSettingsButton = (Button) this.findViewById(R.id.openCloseSettingsButton);
		if (_settingsHidden) {
			loParamsSettings.weight = 0.6f;
			_settingsHidden = false;
			openCloseSettingsButton.setText("< Settings");
			if (!_defiHidden) {
				LinearLayout.LayoutParams loParamsDefi = (LinearLayout.LayoutParams) _defiLayout.getLayoutParams();
				loParamsDefi.weight = 0.001f;
				_defiHidden = true;
				_defiLayout.setLayoutParams(loParamsDefi);
				Button openCloseDefiButton = (Button) this.findViewById(R.id.openCloseDefiButton);
				openCloseDefiButton.setText("< Defibrillator");
				// Hide defi UI-elements.
				showHideDefiUiElements(false);
			}
		} else {
			loParamsSettings.weight = 0.001f;
			_settingsHidden = true;
			openCloseSettingsButton.setText("Settings >");
		}
		_settingsLayout.setLayoutParams(loParamsSettings);
	}
	
	/**
     * Opens/Closes the defi-view via its weight-factor. If the settings-view is already open,
     * close it first.
     * 
     * @param view which called the method.
     */
	public void openCloseDefi(View view) {
		LinearLayout.LayoutParams loParamsDefi = (LinearLayout.LayoutParams) _defiLayout.getLayoutParams();
		Button openCloseDefiButton = (Button) this.findViewById(R.id.openCloseDefiButton);
		if (_defiHidden) {
			loParamsDefi.weight = 0.4f;
			_defiHidden = false;
			openCloseDefiButton.setText("Defibrillator >");
			// Show defi UI-elements.
			showHideDefiUiElements(true);
			if (!_settingsHidden) {
				LinearLayout.LayoutParams loParamsSettings = (LinearLayout.LayoutParams) _settingsLayout.getLayoutParams();
				loParamsSettings.weight = 0.001f;
				_settingsHidden = true;
				_settingsLayout.setLayoutParams(loParamsSettings);
				Button openCloseSettingsButton = (Button) this.findViewById(R.id.openCloseSettingsButton);
				openCloseSettingsButton.setText("Settings >");
			}
		} else {
			loParamsDefi.weight = 0.001f;
			_defiHidden = true;
			openCloseDefiButton.setText("< Defibrillator");
			// Hide defi UI-elements.
			showHideDefiUiElements(false);
		}
		_defiLayout.setLayoutParams(loParamsDefi);
	}
	
	/**
     * Start/Stop an automatically NIBP measurement every _nibpAutoTime sec.
     * 
     * @param view which called the method.
     */
	public void autoNIBP(View view) {
		Button nibpAutoButton = (Button) this.findViewById(R.id.nibpAutoButton);
		if (_autoNIBPRunning) {
			// If the measurement is running, stop it by deleting the scheduled measuring task and reset the button.
			nibpAutoButton.setBackgroundResource(R.drawable.green_button);
			nibpAutoButton.setText("Start A.-NIBP");
			_autoNIBPRunning = false;
			if (_autoNibpTimer != null) _autoNibpTimer.cancel();
		} else {
			if(!_nibpRunning) {
				// If no measurement is running, schedule a measurement task every _nibpAutoTime and indicate
				// this by changing the button color to red.
				nibpAutoButton.setBackgroundResource(R.drawable.red_button);
				nibpAutoButton.setText("Stop A.-NIBP");
				_autoNIBPRunning = true;
				if (_autoNibpTimer != null) _autoNibpTimer.cancel();
				_autoNibpTimer = new Timer();
				_autoNibpTimer.scheduleAtFixedRate(new TimerTask() {
		        	public void run(){
		        		runOnUiThread(new Runnable() {
		        			public void run() {
		        				runNIBP();
		        			}
		        		});
		    		}
	        	}, 0, (_nibpAutoTime * 1000));
			}
		}
	}
	
	/**
     * Start a single NIBP measurement if no measurement is already running.
     * 
     * @param view which called the method.
     */
	public void startNIBP(View view) {
		if (!_nibpRunning) {
			runNIBP();
		}
	}
	
	/**
     * Sets and saves the EKG-value. If the heart pattern is ventfibri, ventflutter or asystole
     * sets the value to 0 and makes the EKG (and other values) blink.
     * 
     * @param value - current heart beat per minute value.
     */
	public void setEKG(int value) {
		_ekgValue = value;
		_ekgRandValue = value;
		// Only do something if the parameter is set active.
		if(_ekgActive) {
			// If the current heart pattern is ventfibri or ventflutter set EKG value to 0 and make it blink, if this isn't already the case. 
			if (_heartPattern == Event.HeartPattern.VENTFIBRI || _heartPattern == Event.HeartPattern.VENTFLUTTER) {
				if (!_ekgBlinkActive) {
					// Set EKG value to 0.
					_ekgValue = 0;
					_ekgRandValue = 0;
					runOnUiThread(new Runnable() {
						public void run() {
							TextView ekgValueTextView = (TextView) findViewById(R.id.ekgValueTextView);
							ekgValueTextView.setText("0");
						}
					});
					// Stop the timer if necessary.
					if (_ekgBlinkTimer != null) {
						_ekgBlinkTimer.cancel();
					}
					// Start blinking.
					_ekgBlinkTimer = new Timer();
					_ekgBlinkTimer.scheduleAtFixedRate(new TimerTask() {
				       	public void run(){
				       		blinkEKGParam();
				    	}
			        }, 0, _paramBlinkRepeatTime);
					_ekgBlinkActive = true;
					// Start alarm if necessary.
					checkAlarm();
				}
			} else if (_heartPattern == Event.HeartPattern.ASYSTOLE) {
				// If the current heart pattern is asystole set EKG value to 0 and make the EKG, RR and O2 blink, if this isn't already the case.
				_ekgValue = 0;
				_ekgRandValue = 0;
				runOnUiThread(new Runnable() {
					public void run() {
						TextView ekgValueTextView = (TextView) findViewById(R.id.ekgValueTextView);
						ekgValueTextView.setText("0");
					}
				});
				// Stop the EKG blink timer if necessary.
				if (_ekgBlinkTimer != null) {
					_ekgBlinkTimer.cancel();
					_ekgBlinkActive = false;
				}
				if (!_paramBlinkActive) {
					// Stop the timer if necessary.
					if (_paramBlinkTimer != null) {
						_paramBlinkTimer.cancel();
					}
					// Start blinking.
					_paramBlinkTimer = new Timer();
					_paramBlinkTimer.scheduleAtFixedRate(new TimerTask() {
			        	public void run(){
			        		blinkParam();
			    		}
		        	}, 0, _paramBlinkRepeatTime);
					_paramBlinkActive = true;
				}
			} else {
				// If there is an other heartpattern schedule the display in main GUI thread and stop the blinking (if necessary).
				if (_ekgBlinkTimer != null) {
					_ekgBlinkTimer.cancel();
					_ekgBlinkActive = false;
				}
				runOnUiThread(new Runnable() {
					public void run() {
						TextView ekgValueTextView = (TextView) findViewById(R.id.ekgValueTextView);
						ekgValueTextView.setText("" + _ekgValue);
					}
				});
				// Adjust the EKG curve to the new value.
				_signalServer.changeHeartRate(value);
				// Start alarm if necessary.
				checkAlarm();
			}
		}
	}
	
	/**
     * Sets and saves the invasible blood pressure values. If the heart pattern is asystole
     * sets the value to 0 and makes the RR (and other values) blink. If the heart pattern is
     * ventfibri or ventflutter sets the value to 0.
     * 
     * @param low - current low blood pressure value.
     * @param high - current high blood pressure value.
     */
	public void setIBP(int diaValue, int sysValue) {
		// If the current heart pattern is asystole set RR value to 0/0 and make the EKG, RR and O2 blink, if this isn't already the case.
		if (_heartPattern == Event.HeartPattern.ASYSTOLE) {
			// Save the values.
			_diaBloodPressure = 0;
			_diaRandValue = 0;
			_sysBloodPressure = 0;
			_sysRandValue = 0;
			// Only do something if the parameter is set active.
			if(_rrActive) {
				// Schedule the display of the new values in main GUI thread.
				runOnUiThread(new Runnable() {
					public void run() {
						TextView ibpValueTextView = (TextView) findViewById(R.id.ibpValueTextView);
						ibpValueTextView.setText("0/0");
					}
				});
				if (!_paramBlinkActive) {
					// Stop the timer if necessary.
					if (_paramBlinkTimer != null) {
						_paramBlinkTimer.cancel();
					}
					// Start blinking.
					_paramBlinkTimer = new Timer();
					_paramBlinkTimer.scheduleAtFixedRate(new TimerTask() {
			        	public void run(){
			        		blinkParam();
			    		}
		        	}, 0, _paramBlinkRepeatTime);
					_paramBlinkActive = true;
				}
				
			}
		// If the current heart pattern is ventfibri or ventflutter set RR value to 0/0.
		} else if (_heartPattern == Event.HeartPattern.VENTFIBRI || _heartPattern == Event.HeartPattern.VENTFLUTTER) { 
			// Save the values.
			_diaBloodPressure = 0;
			_diaRandValue = 0;
			_sysBloodPressure = 0;
			_sysRandValue = 0;
			// Only do something if the parameter is set active.
			if(_rrActive) {
				// Schedule the display of the new values in main GUI thread.
				runOnUiThread(new Runnable() {
					public void run() {
						TextView ibpValueTextView = (TextView) findViewById(R.id.ibpValueTextView);
						ibpValueTextView.setText("0/0");
					}
				});
				// Start alarm if necessary.
				checkAlarm();
			}
		} else {
			// Save the values.
			_diaBloodPressure = diaValue;
			_diaRandValue = diaValue;
			_sysBloodPressure = sysValue;
			_sysRandValue = sysValue;
			// Only do something if the parameter is set active.
			if(_rrActive) {
				// Schedule the display of the new values in main GUI thread.
				runOnUiThread(new Runnable() {
					public void run() {
						TextView ibpValueTextView = (TextView) findViewById(R.id.ibpValueTextView);
						ibpValueTextView.setText("" + _sysBloodPressure + "/" + _diaBloodPressure);
					}
				});
				// Adjust the blood pressure curve to the new value.
				_signalServer.changeBloodPressure(_sysBloodPressure, _diaBloodPressure);
				// Start alarm if necessary.
				checkAlarm();
			}
		}
	}
	
	/**
     * Sets and saves the oxygen saturation value. If the heart pattern is asystole
     * sets the value to 0 and makes the RR (and other values) blink. If the heart pattern is
     * ventfibri or ventflutter sets the value to 0.
     * 
     * @param value - current oxygen saturation value.
     */
	public void setO2(int value) {
		_o2Value = value;
		_o2RandValue = value;
		// If the current heart pattern is asystole set O2 value to 0 and make the EKG, RR and O2 blink, if this isn't already the case.
		if (_heartPattern == Event.HeartPattern.ASYSTOLE) {
			// Only do something if the parameter is set active.
			if(_o2Active) {
				// Save the value and schedule the display in main GUI thread.
				_o2Value = 0;
				_o2RandValue = 0;
				runOnUiThread(new Runnable() {
					public void run() {
						TextView o2ValueTextView = (TextView) findViewById(R.id.o2ValueTextView);
						o2ValueTextView.setText("0");
					}
				});
				if (!_paramBlinkActive) {
					// Stop the timer if necessary.
					if (_paramBlinkTimer != null) {
						_paramBlinkTimer.cancel();
					}
					// Start blinking.
					_paramBlinkTimer = new Timer();
					_paramBlinkTimer.scheduleAtFixedRate(new TimerTask() {
			        	public void run(){
			        		blinkParam();
			    		}
		        	}, 0, _paramBlinkRepeatTime);
					_paramBlinkActive = true;
				}
			}
		// If the current heart pattern is ventfibri or ventflutter set O2 value to 0.
		} else if (_heartPattern == Event.HeartPattern.VENTFIBRI || _heartPattern == Event.HeartPattern.VENTFLUTTER) { 
			// Only do something if the parameter is set active.
			if(_o2Active) {
				// Save the value and schedule the display in main GUI thread.
				_o2Value = 0;
				_o2RandValue = 0;
				runOnUiThread(new Runnable() {
					public void run() {
						TextView o2ValueTextView = (TextView) findViewById(R.id.o2ValueTextView);
						o2ValueTextView.setText("0");
					}
				});
				// Start alarm if necessary.
				checkAlarm();
			}
		} else {
			// Only do something if the parameter is set active.
			if(_o2Active) {
				// Schedule the display in main GUI thread.
				runOnUiThread(new Runnable() {
					public void run() {
						TextView o2ValueTextView = (TextView) findViewById(R.id.o2ValueTextView);
						o2ValueTextView.setText("" + _o2Value);
					}
				});
				// Adjust the O2 curve to the new value.
				_signalServer.changeO2Value(value);
				// Start alarm if necessary.
				checkAlarm();
			}
		}
	}
	
	/**
     * Sets and saves the CO2 value.
     * 
     * @param value - current CO2 value.
     */
	public void setCO2(int value) {
		_co2Value = value;
		_co2RandValue = value;
		// Only do something if the parameter is set active.
		if(_co2Active) {
			// Schedule the display in main GUI thread.
			runOnUiThread(new Runnable() {
				public void run() {
					TextView co2ValueTextView = (TextView) findViewById(R.id.co2ValueTextView);
					co2ValueTextView.setText("" + _co2Value);
				}
			});
			// Adjust the CO2 curve to the new value.
			_signalServer.changeCO2Value(value);
			// Start alarm if necessary.
			checkAlarm();
		}
	}
	
	/**
     * Sets and saves the respiration value.
     * 
     * @param value - current respiration value.
     */
	public void setResp(int value) {
		_respValue = value;
		_respRandValue = value;
		// Only do something if the parameter is set active.
		if(_respActive) {
			// Schedule the display in main GUI thread.
			runOnUiThread(new Runnable() {
				public void run() {
					TextView afValueTextView = (TextView) findViewById(R.id.afValueTextView);
					afValueTextView.setText("" + _respValue);
				}
			});
		}
		_signalServer.changeRespirationRate(value);
	}
	
	/**
     * Generates a gaussian variation on the ekg value. The value is kept constant until the 
     * number of heart-beats since the last change reaches a certain value and a random number
     * is greater than a threshold.
     */
	public void ekgVariation() {
		if(_ekgActive) {  // Only do something if EKG is active.
			_ekgPeakCounter++;
			// Check if new variation should be done and adjust the curve.
			if (Math.random() > _randThreshold && _ekgPeakCounter > (_ekgValue/_ekgDivFactor)) {
				double rand = _randomGenerator.nextGaussian();
				_ekgRandValue = _ekgValue + (int) (rand * _ekgRandMult);
				_signalServer.changeHeartRate(_ekgRandValue);
				_ekgPeakCounter = 0;
			}
			// One heart-beat after a change also update the parameter value and trigger
			// a alarm if necessary.
			if (_ekgPeakCounter == 1) {
				runOnUiThread(new Runnable() {
					public void run() {
						TextView ekgValueTextView = (TextView) findViewById(R.id.ekgValueTextView);
						ekgValueTextView.setText("" + _ekgRandValue);
					}
				});
				checkAlarm();
			}
		}
	}
	
	/**
     * Generates a gaussian variation on the blood pressure value. The value is kept constant 
     * until the number of heart-beats since the last change reaches a certain value and a random 
     * number is greater than a threshold.
     */
	public void rrVariation() {
		if (_rrActive) {  // Only do something if RR is active.
			_rrPeakCounter++;
			// Check if new variation should be done and adjust the curve.
			if (Math.random() > _randThreshold && _rrPeakCounter > (_ekgValue/_ekgDivFactor)) {
				double rand = _randomGenerator.nextGaussian();
				_diaRandValue = _diaBloodPressure + (int) (rand * _rrRandMult);
				rand = _randomGenerator.nextGaussian();
				_sysRandValue = _sysBloodPressure + (int) (rand * _rrRandMult);
				_signalServer.changeBloodPressure(_sysRandValue, _diaRandValue);
				_rrPeakCounter = 0;
			}
			// One heart-beat after a change also update the parameter value and trigger
			// a alarm if necessary.
			if (_rrPeakCounter == 1) {
				runOnUiThread(new Runnable() {
					public void run() {
						TextView ibpValueTextView = (TextView) findViewById(R.id.ibpValueTextView);
						ibpValueTextView.setText("" + _sysRandValue + "/" + _diaRandValue);
					}
				});
				checkAlarm();
			}
		}
	}
	
	/**
     * Generates a gaussian variation on the o2 value. The value is kept constant until the 
     * number of heart-beats since the last change reaches a certain value and a random number
     * is greater than a threshold.
     */
	public void o2Variation() {
		if (_o2Active) {  // Only do something if O2 is active.
			_o2PeakCounter++;
			// Check if new variation should be done and adjust the curve.
			if (Math.random() > _randThreshold && _o2PeakCounter > (_ekgValue/_ekgDivFactor)) {
				double rand = _randomGenerator.nextGaussian();
				_o2RandValue = _o2Value + (int) (rand * _o2RandMult);
				if (_o2RandValue > 100) {
					_o2RandValue = 100;
				}
				_signalServer.changeO2Value(_o2RandValue);
				_o2PeakCounter = 0;
			}
			// One heart-beat after a change also update the parameter value and trigger
			// a alarm if necessary.
			if (_o2PeakCounter == 1) {
				runOnUiThread(new Runnable() {
					public void run() {
						TextView o2ValueTextView = (TextView) findViewById(R.id.o2ValueTextView);
						o2ValueTextView.setText("" + _o2RandValue);
					}
				});
				checkAlarm();
			}
		}
	}
	
	/**
     * Generates a gaussian variation on the CO2 and the respiration value. The value is kept constant 
     * until the number of heart-beats since the last change reaches a certain value and a random 
     * number is greater than a threshold.
     */
	public void co2RespVariation() {
		// CO2:
		if (_co2Active) { // Only do something if CO2 is active.
			_co2PeakCounter++;
			// Check if new variation should be done and adjust the curve.
			if (Math.random() > _randThreshold && _co2PeakCounter > (_respValue/_respDivFactor)) {
				double rand = _randomGenerator.nextGaussian();
				_co2RandValue = _co2Value + (int) (rand * _co2RandMult);
				_signalServer.changeCO2Value(_co2RandValue);
				_co2PeakCounter = 0;
			}
			// One heart-beat after a change also update the parameter value and trigger
			// a alarm if necessary.
			if (_co2PeakCounter == 1) {
				runOnUiThread(new Runnable() {
					public void run() {
						TextView co2ValueTextView = (TextView) findViewById(R.id.co2ValueTextView);
						co2ValueTextView.setText("" + _co2RandValue);
					}
				});
				checkAlarm();
			}
		}
		// Respiration:
		// Only do something if there is a active breathing.
		if (_respValue > 0 && _respActive) {
			_respPeakCounter++;
			// Check if new variation should be done and adjust the curve.
			if (Math.random() > _randThreshold && _respPeakCounter > (_respValue/_respDivFactor)) {
				double rand = _randomGenerator.nextGaussian();
				_respRandValue = _respValue + (int) (rand * _respRandMult);
				// Catch negative values.
				if (_respRandValue < 0) {
					_respRandValue = 0;
				}
				_signalServer.changeRespirationRate(_respRandValue);
				_respPeakCounter = 0;
			}
			// One heart-beat after a change also update the parameter value and trigger
			// a alarm if necessary.
			if (_respPeakCounter == 1) {
				runOnUiThread(new Runnable() {
					public void run() {
						TextView afValueTextView = (TextView) findViewById(R.id.afValueTextView);
						afValueTextView.setText("" + _respRandValue);
					}
				});
				checkAlarm();
			}
		}
	}
	
	/**
     * Displays the EKG-settings by marking the EKG-selection-button and loading/displaying the
     * current EKG-settings in the settings-layout.
     * 
     * @param view which called the method.
     */
	public void showEKGSettings(View view) {
		_menuSelection = 0;
		// Change the button-text-color of the selected button to black.
		Button ekgSettingsButton = (Button) this.findViewById(R.id.ekgSettingsButton);
		ekgSettingsButton.setTextColor(Color.BLACK);
		// Move the arrow under the button.
		ImageView setPointArrowImageView = (ImageView) this.findViewById(R.id.settingsPointingArrowImageView);
		int arrowPos = ekgSettingsButton.getLeft() + ekgSettingsButton.getWidth() / 2 - setPointArrowImageView.getWidth() / 2;
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) setPointArrowImageView.getLayoutParams();
		params.setMargins(arrowPos, 0, 0, 0); //Parameters for left, top, right and bottom margin.
		setPointArrowImageView.setLayoutParams(params);
		// Change the button-text-colors of the remaining buttons back to white.
		Button ibpSettingsButton = (Button) this.findViewById(R.id.ibpSettingsButton);
		ibpSettingsButton.setTextColor(Color.WHITE);
		Button o2SettingsButton = (Button) this.findViewById(R.id.o2SettingsButton);
		o2SettingsButton.setTextColor(Color.WHITE);
		Button co2SettingsButton = (Button) this.findViewById(R.id.co2SettingsButton);
		co2SettingsButton.setTextColor(Color.WHITE);
		// Display the sound settings. Not needed at the moment.
		// showSoundSetting(true);
		// Display/Hide and setup the alarm settings.
		showAlarmSetting(true, false, false, false, "Frequency", "", "" + _ekgAlarmLowValue, "" + _ekgAlarmUpValue, "", "");
		// Update the alarm and sound on/off buttons. 
		setAlarmButton(_ekgAlarmOn);
		setSoundButton(_ekgSoundOn);
	}
	
	/**
     * Displays the blood pressure-settings by marking RR-selection-button and loading/displaying the
     * current blood pressure-settings in the settings-layout.
     * 
     * @param view which called the method.
     */
	public void showIBPSettings(View view) {
		_menuSelection = 1;
		// Change the button-text-color of the selected button to black.
		Button ibpSettingsButton = (Button) this.findViewById(R.id.ibpSettingsButton);
		ibpSettingsButton.setTextColor(Color.BLACK);
		// Move the arrow under the button.
		ImageView setPointArrowImageView = (ImageView) this.findViewById(R.id.settingsPointingArrowImageView);
		int arrowPos = ibpSettingsButton.getLeft() + ibpSettingsButton.getWidth() / 2 - setPointArrowImageView.getWidth() / 2;
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) setPointArrowImageView.getLayoutParams();
		params.setMargins(arrowPos, 0, 0, 0); //Parameters for left, top, right and bottom margin.
		setPointArrowImageView.setLayoutParams(params);
		// Change the button-text-colors of the remaining buttons back to white.
		Button ekgSettingsButton = (Button) this.findViewById(R.id.ekgSettingsButton);
		ekgSettingsButton.setTextColor(Color.WHITE);
		Button o2SettingsButton = (Button) this.findViewById(R.id.o2SettingsButton);
		o2SettingsButton.setTextColor(Color.WHITE);
		Button co2SettingsButton = (Button) this.findViewById(R.id.co2SettingsButton);
		co2SettingsButton.setTextColor(Color.WHITE);
		// Hide the sound settings. Not needed at the moment.
		// showSoundSetting(false);
		// Display/Hide and setup the alarm settings.
		showAlarmSetting(true, false, false, true, "Systolic BP", "Diastolic BP", "" + _rrSysAlarmLowValue, "" + _rrSysAlarmUpValue, "" + _rrDiaAlarmLowValue, "" + _rrDiaAlarmUpValue);
		// Update the alarm on/off button.
		setAlarmButton(_rrAlarmOn);
	}
	
	/**
     * Displays the O2-settings by marking O2-selection-button and loading/displaying the
     * current O2-settings in the settings-layout.
     * 
     * @param view which called the method.
     */
	public void showO2Settings(View view) {
		_menuSelection = 2;
		// Change the button-text-color of the selected button to black.
		Button o2SettingsButton = (Button) this.findViewById(R.id.o2SettingsButton);
		o2SettingsButton.setTextColor(Color.BLACK);
		// Move the arrow under the button.
		ImageView setPointArrowImageView = (ImageView) this.findViewById(R.id.settingsPointingArrowImageView);
		int arrowPos = o2SettingsButton.getLeft() + o2SettingsButton.getWidth() / 2 - setPointArrowImageView.getWidth() / 2;
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) setPointArrowImageView.getLayoutParams();
		params.setMargins(arrowPos, 0, 0, 0); //Parameters for left, top, right and bottom margin.
		setPointArrowImageView.setLayoutParams(params);
		// Change the button-text-colors of the remaining buttons back to white.
		Button ekgSettingsButton = (Button) this.findViewById(R.id.ekgSettingsButton);
		ekgSettingsButton.setTextColor(Color.WHITE);
		Button ibpSettingsButton = (Button) this.findViewById(R.id.ibpSettingsButton);
		ibpSettingsButton.setTextColor(Color.WHITE);
		Button co2SettingsButton = (Button) this.findViewById(R.id.co2SettingsButton);
		co2SettingsButton.setTextColor(Color.WHITE);
		// Hide the sound settings. Not needed at the moment.
		// showSoundSetting(false);
		showAlarmSetting(true, true, false, false, "O2 Saturation", "", "" + _o2AlarmLowValue, "", "", "");
		// Update the alarm on/off button.
		setAlarmButton(_o2AlarmOn);
	}
	
	/**
     * Displays the CO2-settings by marking CO2-selection-button and loading/displaying the
     * current CO2-settings in the settings-layout.
     * 
     * @param view which called the method.
     */
	public void showCO2Settings(View view) {
		_menuSelection = 3;
		// Change the button-text-color of the selected button to black.
		Button co2SettingsButton = (Button) this.findViewById(R.id.co2SettingsButton);
		co2SettingsButton.setTextColor(Color.BLACK);
		// Move the arrow under the button.
		ImageView setPointArrowImageView = (ImageView) this.findViewById(R.id.settingsPointingArrowImageView);
		int arrowPos = co2SettingsButton.getLeft() + co2SettingsButton.getWidth() / 2 - setPointArrowImageView.getWidth() / 2;
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) setPointArrowImageView.getLayoutParams();
		params.setMargins(arrowPos, 0, 0, 0); //Parameters for left, top, right and bottom margin.
		setPointArrowImageView.setLayoutParams(params);
		// Change the button-text-colors of the remaining buttons back to white.
		Button ekgSettingsButton = (Button) this.findViewById(R.id.ekgSettingsButton);
		ekgSettingsButton.setTextColor(Color.WHITE);
		Button ibpSettingsButton = (Button) this.findViewById(R.id.ibpSettingsButton);
		ibpSettingsButton.setTextColor(Color.WHITE);
		Button o2SettingsButton = (Button) this.findViewById(R.id.o2SettingsButton);
		o2SettingsButton.setTextColor(Color.WHITE);
		// Hide the sound settings. Not needed at the moment.
		// showSoundSetting(false);
		// Hide and setup the alarm settings.
		showAlarmSetting(true, false, false, false, "CO2-Level", "", "" + _co2AlarmLowValue, "" + _co2AlarmUpValue, "", "");
		// Update the alarm and sound on/off buttons. 
		setAlarmButton(_co2AlarmOn);
	}
	
	/**
     * Turns the alarm of the currently in the settings selected parameter on/off.
     * 
     * @param view which called the method.
     */
	public void triggerAlarm(View view) {
		if (_menuSelection == 0) {  // EKG-settings selected.
			// Trigger the alarm(-button).
			_ekgAlarmOn = !_ekgAlarmOn;
			setAlarmButton(_ekgAlarmOn);
			// Show/Hide the little bell in the EKG-parameter-section.
			ImageView ekgAlarmImageView = (ImageView) this.findViewById(R.id.ekgAlarmImageView);
			if (_ekgAlarmOn && _alarmActive) {
				ekgAlarmImageView.setVisibility(View.VISIBLE);
				checkAlarm();
			} else {
				ekgAlarmImageView.setVisibility(View.INVISIBLE);
				_soundHandler.setEKGAlarm(false);
			}
		} else if (_menuSelection == 1) {  // RR-settings selected.
			// Trigger the alarm(-button).
			_rrAlarmOn = !_rrAlarmOn;
			setAlarmButton(_rrAlarmOn);
			// Show/Hide the little bells in the IBP- and the NIBP-parameter-section.
			ImageView ibpAlarmImageView = (ImageView) this.findViewById(R.id.ibpAlarmImageView);
			ImageView nibpAlarmImageView = (ImageView) this.findViewById(R.id.nibpAlarmImageView);
			if (_rrAlarmOn && _alarmActive) {
				ibpAlarmImageView.setVisibility(View.VISIBLE);
				nibpAlarmImageView.setVisibility(View.VISIBLE);
				checkAlarm();
			} else {
				ibpAlarmImageView.setVisibility(View.INVISIBLE);
				nibpAlarmImageView.setVisibility(View.INVISIBLE);
				_soundHandler.setRRAlarm(false);
			}
		} else if (_menuSelection == 2) {  // O2-settings selected.
			// Trigger the alarm(-button).
			_o2AlarmOn = !_o2AlarmOn;
			setAlarmButton(_o2AlarmOn);
			// Show/Hide the little bell in the O2-parameter-section.
			ImageView o2AlarmImageView = (ImageView) this.findViewById(R.id.o2AlarmImageView);
			if (_o2AlarmOn && _alarmActive) {
				o2AlarmImageView.setVisibility(View.VISIBLE);
				checkAlarm();
			} else {
				o2AlarmImageView.setVisibility(View.INVISIBLE);
				_soundHandler.setO2Alarm(false);
			}
		} else if (_menuSelection == 3) {  // CO2-settings selected.
			// Trigger the alarm(-button).
			_co2AlarmOn = !_co2AlarmOn;
			setAlarmButton(_co2AlarmOn);
			// Show/Hide the little bell in the O2-parameter-section.
			ImageView co2AlarmImageView = (ImageView) this.findViewById(R.id.co2AlarmImageView);
			if (_co2AlarmOn && _alarmActive) {
				co2AlarmImageView.setVisibility(View.VISIBLE);
				checkAlarm();
			} else {
				co2AlarmImageView.setVisibility(View.INVISIBLE);
				_soundHandler.setCO2Alarm(false);
			}
		}
	}
	
	/**
     * Turns the QRS sound on/off.
     * 
     * @param view which called the method.
     */
	public void triggerSound(View view) {
		// Trigger the sound(-button).
		_ekgSoundOn = !_ekgSoundOn;
		setSoundButton(_ekgSoundOn);
		// Show/Hide the little speaker in the EKG-parameter-section.
		ImageView ekgSoundImageView = (ImageView) this.findViewById(R.id.ekgSoundImageView);
		if (_ekgSoundOn) {
			ekgSoundImageView.setVisibility(View.VISIBLE);
		} else {
			ekgSoundImageView.setVisibility(View.INVISIBLE);
		}
		// Show/Hide the little speaker on the main sound button in the status bar and change the text color.
		Button mainSoundOnOffButton = (Button) this.findViewById(R.id.mainSoundOnOffButton);
		if (_ekgSoundOn) {
			Drawable newIcon = getResources().getDrawable(R.drawable.speaker2icon);
			mainSoundOnOffButton.setCompoundDrawablesWithIntrinsicBounds(null, null, newIcon, null);
			mainSoundOnOffButton.setTextColor(Color.BLACK);
		} else {
			Drawable newIcon = getResources().getDrawable(R.drawable.nspeaker2icon);
			mainSoundOnOffButton.setCompoundDrawablesWithIntrinsicBounds(null, null, newIcon, null);
			mainSoundOnOffButton.setTextColor(Color.WHITE);
		}
	}
	
	/**
     * Activates/Deactivates the alarm in general.
     * 
     * @param view which called the method.
     */
	public void alarmOnOff(View view) {
		// If the alarm is paused, cancel this.
		if(_alarmPaused) {
			if(_alarmPauseTimer != null) {
				_alarmPauseTimer.cancel();
			}
			_alarmPaused = false;
			_alarmActive = true;
			Button alarmPauseButton = (Button) this.findViewById(R.id.alarmPauseButton);
			alarmPauseButton.setTextColor(Color.WHITE);
		}
		Button mainAlarmOnOffButton = (Button) this.findViewById(R.id.mainAlarmOnOffButton);
		ImageView ekgAlarmImageView = (ImageView) this.findViewById(R.id.ekgAlarmImageView);
		ImageView ibpAlarmImageView = (ImageView) this.findViewById(R.id.ibpAlarmImageView);
		ImageView nibpAlarmImageView = (ImageView) this.findViewById(R.id.nibpAlarmImageView);
		ImageView o2AlarmImageView = (ImageView) this.findViewById(R.id.o2AlarmImageView);
		ImageView co2AlarmImageView = (ImageView) this.findViewById(R.id.co2AlarmImageView);
		if (_alarmActive) {
			_alarmActive = false;
			// Change the button icon and text color.
			Drawable newIcon = getResources().getDrawable(R.drawable.nbell2icon);
			mainAlarmOnOffButton.setCompoundDrawablesWithIntrinsicBounds(null, null, newIcon, null);
			mainAlarmOnOffButton.setTextColor(Color.WHITE);
			// Hide the little bells at the parameters.
			ekgAlarmImageView.setVisibility(View.INVISIBLE);
			ibpAlarmImageView.setVisibility(View.INVISIBLE);
			nibpAlarmImageView.setVisibility(View.INVISIBLE);
			o2AlarmImageView.setVisibility(View.INVISIBLE);
			co2AlarmImageView.setVisibility(View.INVISIBLE);
		} else {
			_alarmActive = true;
			// Change the button icon and text color.
			Drawable newIcon = getResources().getDrawable(R.drawable.bell2icon);
			mainAlarmOnOffButton.setCompoundDrawablesWithIntrinsicBounds(null, null, newIcon, null);
			mainAlarmOnOffButton.setTextColor(Color.BLACK);
			// Show little bells at the parameters for which there is an alarm active now.
			if(_ekgAlarmOn) {
				ekgAlarmImageView.setVisibility(View.VISIBLE);
			}
			if(_rrAlarmOn) {
				ibpAlarmImageView.setVisibility(View.VISIBLE);
				nibpAlarmImageView.setVisibility(View.VISIBLE);
			}
			if(_o2AlarmOn) {
				o2AlarmImageView.setVisibility(View.VISIBLE);
			}
			if(_co2AlarmOn) {
				co2AlarmImageView.setVisibility(View.VISIBLE);
			}
		}
		checkAlarm();
	}
	
	/**
     * Pauses the alarm for one minute, if it is active.
     * 
     * @param view which called the method.
     */
	public void pauseAlarm(View view) {
		// If the alarm is already paused, cancel the pause and reactivate the alarm.
		if(_alarmPaused) {
			if(_alarmPauseTimer != null) {
				_alarmPauseTimer.cancel();
			}
			_alarmPaused = false;
			Button alarmPauseButton = (Button) this.findViewById(R.id.alarmPauseButton);
			alarmPauseButton.setTextColor(Color.WHITE);
			_alarmActive = true;
			checkAlarm();
		} else {
			// If the alarm is not already paused and alarm is active, stop it an schedule a
			// reactivation after _alarmPauseTime s.
			if(_alarmActive) {
				final Button alarmPauseButton = (Button) this.findViewById(R.id.alarmPauseButton);
				alarmPauseButton.setTextColor(Color.BLACK);
				_alarmActive = false;
				checkAlarm();
				_alarmPaused = true;
				_alarmPauseTimer = new Timer();
				_alarmPauseTimer.schedule(new TimerTask() {
		        	public void run(){
		        		runOnUiThread(new Runnable() {
		        			public void run() {
		        				_alarmActive = true;
		        				checkAlarm();
		        				_alarmPaused = false;
		        				alarmPauseButton.setTextColor(Color.WHITE);
		        			}
		        		});
		    		}
	        	}, (_alarmPauseTime * 1000));
			}
		}
	}
	
	/**
     * Changes the color of the parameters/curve of a selected vital parameter according to a
     * pressed button.
     * 
     * @param view which called the method.
     */
	public void changeColor(View view) {
		// Distinguish which color was selected and save it.
		int color = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		if (view.getId() == R.id.colorSelectionRedButton) {
			color = Color.RED;
			red = 255;
		} else if (view.getId() == R.id.colorSelectionBlueButton) {
			color = Color.BLUE;
			blue = 255;
		} else if (view.getId() == R.id.colorSelectionGreenButton) {
			color = Color.GREEN;
			green = 255;
		} else if (view.getId() == R.id.colorSelectionYellowButton) {
			color = Color.YELLOW;
			red = 255;
			green = 255;
		} else if (view.getId() == R.id.colorSelectionGrayButton) {
			color = Color.GRAY;
			red = 128;
			green = 128;
			blue = 128;
		}
		// Change the color of the parameters/curve of the currently selected vital-parameter.
		if (_menuSelection == 0) {
			// Change the color of the EKG-parameters/curve.
			TextView ekgValueTextView = (TextView) this.findViewById(R.id.ekgValueTextView);
			ekgValueTextView.setTextColor(color);
			_glActivivty.SetColor(GLRenderer.LineType.Heart, red, green, blue);
		} else if (_menuSelection == 1) {
			// Change the color of the blood pressure-parameters/curve.
			TextView ibpValueTextView = (TextView) this.findViewById(R.id.ibpValueTextView);
			TextView nibpValueTextView = (TextView) this.findViewById(R.id.nibpValueTextView);
			ibpValueTextView.setTextColor(color);
			nibpValueTextView.setTextColor(color);
			_glActivivty.SetColor(GLRenderer.LineType.Blood, red, green, blue);
		} else if (_menuSelection == 2) {
			// Change the color of the O2-parameters/curve.
			TextView o2ValueTextView = (TextView) this.findViewById(R.id.o2ValueTextView);
			o2ValueTextView.setTextColor(color);
			_glActivivty.SetColor(GLRenderer.LineType.O2, red, green, blue);
		} else if (_menuSelection == 3) {
			// Change the color of the CO2-parameters/curve and respiration-parameter.
			TextView co2ValueTextView = (TextView) this.findViewById(R.id.co2ValueTextView);
			TextView afValueTextView = (TextView) this.findViewById(R.id.afValueTextView);
			co2ValueTextView.setTextColor(color);
			afValueTextView.setTextColor(color);
			_glActivivty.SetColor(GLRenderer.LineType.CO2, red, green, blue);
		}
	}
	
	/**
     * Changes the background color of the whole parameters/curve-view according to a
     * pressed button.
     * 
     * @param view which called the method.
     */
	public void changeBackColor(View view) {
		// Get all relevant layout-elements.
		RelativeLayout ekgParamLayout = (RelativeLayout) this.findViewById(R.id.ekgParamLayout);
		RelativeLayout ibpParamLayout = (RelativeLayout) this.findViewById(R.id.ibpParamLayout);
		RelativeLayout o2ParamLayout = (RelativeLayout) this.findViewById(R.id.o2ParamLayout);
		RelativeLayout co2ParamLayout = (RelativeLayout) this.findViewById(R.id.co2ParamLayout);
		RelativeLayout nibpParamLayout = (RelativeLayout) this.findViewById(R.id.nibpParamLayout);
		RelativeLayout nibpSettingsLayout = (RelativeLayout) this.findViewById(R.id.nibpSettingsLayout);
		RelativeLayout defiLayout = (RelativeLayout) this.findViewById(R.id.defiLayout);
		TextView ekgTitleTextView = (TextView) this.findViewById(R.id.ekgTitleTextView);
		TextView ibpTitleTextView = (TextView) this.findViewById(R.id.ibpTitleTextView);
		TextView ibpUnitTextView = (TextView) this.findViewById(R.id.ibpUnitTextView);
		TextView o2TitleTextView = (TextView) this.findViewById(R.id.o2TitleTextView);
		TextView o2UnitTextView = (TextView) this.findViewById(R.id.o2UnitTextView);
		TextView co2TitleTextView = (TextView) this.findViewById(R.id.co2TitleTextView);
		TextView co2UnitTextView = (TextView) this.findViewById(R.id.co2UnitTextView);
		TextView afTitleTextView = (TextView) this.findViewById(R.id.afTitleTextView);
		TextView nibpSettingsTitleTextView = (TextView) this.findViewById(R.id.nibpSettingsTitleTextView);
		TextView nibpAutoTimeTextView = (TextView) this.findViewById(R.id.nibpAutoTimeTextView);
		TextView nibpParamTitleTextView = (TextView) this.findViewById(R.id.nibpParamTitleTextView);
		TextView nibpUnitTextView = (TextView) this.findViewById(R.id.nibpUnitTextView);
		TextView defiTitletextView = (TextView) this.findViewById(R.id.defiTitletextView);
		TextView defiEnergy = (TextView) this.findViewById(R.id.defiEnergy);
		// Distinguish which color was selected and save it.
		int color = 0;
		int defiEngergyColor = 0;
		int drawable = 0;
		if (view.getId() == R.id.backColorSelectionWhiteButton) {
			color = Color.BLACK;
			defiEngergyColor = Color.WHITE;
			drawable = R.drawable.border_white_back;
			// Change background color of GL-part.
			_glActivivty.SetColor(GLRenderer.LineType.Background, 255, 255, 255);
		} else if (view.getId() == R.id.backColorSelectionBlackButton) {
			color = Color.WHITE;
			defiEngergyColor = Color.BLACK;
			drawable = R.drawable.border_black_back;
			// Change background color of GL-part.
			_glActivivty.SetColor(GLRenderer.LineType.Background, 0, 0, 0);
		}
		// Set the new color in all relevant layout-elements.
		ekgParamLayout.setBackgroundResource(drawable);
		ibpParamLayout.setBackgroundResource(drawable);
		o2ParamLayout.setBackgroundResource(drawable);
		co2ParamLayout.setBackgroundResource(drawable);
		nibpParamLayout.setBackgroundResource(drawable);
		nibpSettingsLayout.setBackgroundResource(drawable);
		defiLayout.setBackgroundResource(drawable);
		ekgTitleTextView.setTextColor(color);
		ibpTitleTextView.setTextColor(color);
		ibpUnitTextView.setTextColor(color);
		o2TitleTextView.setTextColor(color);
		o2UnitTextView.setTextColor(color);
		co2TitleTextView.setTextColor(color);
		co2UnitTextView.setTextColor(color);
		afTitleTextView.setTextColor(color);
		nibpSettingsTitleTextView.setTextColor(color);
		nibpAutoTimeTextView.setTextColor(color);
		nibpParamTitleTextView.setTextColor(color);
		nibpUnitTextView.setTextColor(color);
		defiTitletextView.setTextColor(color);
		defiEnergy.setBackgroundColor(color);
		defiEnergy.setTextColor(defiEngergyColor);
	}
	
	/**
     * Should be called if the highest point of one period of the heart-curve is reached to 
     * trigger the sound and blinking heart.
     */
	public void heartPeak() {
		// Schedule a blink of the little EKG-heart and, if activated, the heart-sound in the 
		// main GUI thread.
		runOnUiThread(new Runnable() {
			public void run() {
				blinkHeart();
				if (_ekgSoundOn) {
					_soundHandler.playHeartSound(_o2Active, _o2Value);
				}
			}
		});
	}
	
	/**
     * Should be called if the highest point of one period of the o2-curve is reached to 
     * trigger the sound and blinking heart, while EKG isn't available.
     */
	public void o2Peak() {
		// If there is no EKG-signal, schedule a blink of the little EKG-heart and, if
		// activated, the heart-sound in the main GUI thread.
		if(!_ekgActive) {
			runOnUiThread(new Runnable() {
				public void run() {
					blinkHeart();
					if (_ekgSoundOn) {
						_soundHandler.playHeartSound(true, _o2Value);
					}
				}
			});
		}
	}
	
	/**
	 * Starts/Stops the timer in the status bar.
	 * 
	 * @param run - states if the timer should run or not.
	 */
	public void startStopTimer(boolean run) {
		// If the timer should run, create a scheduled task, that every increases a value
		// every second and displays it.
		if (run) {
			if(!_timerActive) {
				_stopWatchTimer = new Timer();
				_stopWatchTimer.schedule(new TimerTask() {
					public void run(){
						runOnUiThread(new Runnable() {
							public void run() {
								_timeInSec++;  // Increase the counter every second.
								updateTime();  // Update the GUI.
							}
						});
					}
				}, 1000, 1000);
				_timerActive = true;
			}
		} else {
			// If the timer should be stopped, cancel the scheduled task
			if(_stopWatchTimer != null) {
				_stopWatchTimer.cancel();
			}
			_timerActive = false;
		}
	}
	
	/**
	 * Resets the timer in status bar.
	 */
	public void resetTimer() {
		_timeInSec = 0;
		runOnUiThread(new Runnable() {
			public void run() {
				TextView statusBarTitle = (TextView) findViewById(R.id.statusBarTitle);
				statusBarTitle.setText("Timer:   00:00");
			}
		});
	}
	
	/**
     * Increases the first upper alarm threshold by one and schedules an automatic increase if the
     * button is pressed continuously.
     * 
     * @param view which called the method.
     */
	public void startIncFirstAlarmUpTH(View view) {
		// Increase the first upper threshold value by one.
		incFirstAlarmUpTH();
		// Schedule an automatic increase after _incDecAutoStartDelay ms every _incDecStepTime
		// until the button is released.
		_firstAlarmUpperTHUpButtonPressed= true;
    	if (_incDecTimer != null) _incDecTimer.cancel();
    	_incDecTimer = new Timer();
    	_incDecTimer.scheduleAtFixedRate(new TimerTask() {
	        	public void run(){
	        		if(!_firstAlarmUpperTHUpButtonPressed) {
	        			this.cancel();
	        		} else {
	        			incFirstAlarmUpTH();
	        		}
	        	}
        	}, _incDecAutoStartDelay, _incDecStepTime);
	}
	
	/**
	 * Performs an increase of the currently selected first upper alarm threshold,
	 * displays the new value and activates the alarm if necessary. 
	 */
	public void incFirstAlarmUpTH() {
		// Schedule the increase in the main GUI-thread.
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView firstAlarmUpperTHValueTextView = (TextView) findViewById(R.id.firstAlarmUpperTHValueTextView);
				if (_menuSelection == 0) {  // EKG-settings selected.
					if ((_ekgAlarmUpValue + _ekgAlarmIncDecStep) <= _ekgAlarmUpValueMax) {
						// When the new value doesn't conflict with the borders, save and display it.
						_ekgAlarmUpValue = _ekgAlarmUpValue + _ekgAlarmIncDecStep;
						firstAlarmUpperTHValueTextView.setText("" + _ekgAlarmUpValue);
					}
				} else if (_menuSelection == 1) {  // RR-settings selected.
					if ((_rrSysAlarmUpValue + _rrAlarmIncDecStep) <= _rrSysAlarmUpValueMax) {
						// When the new value doesn't conflict with the borders, save and display it.
						_rrSysAlarmUpValue = _rrSysAlarmUpValue + _rrAlarmIncDecStep;
						firstAlarmUpperTHValueTextView.setText("" + _rrSysAlarmUpValue);
					}
				} else if (_menuSelection == 3) {  // CO2-settings selected.
					if ((_co2AlarmUpValue + _co2AlarmIncDecStep) <= _co2AlarmUpValueMax) {
						// When the new value doesn't conflict with the borders, save and display it.
						_co2AlarmUpValue = _co2AlarmUpValue + _co2AlarmIncDecStep;
						firstAlarmUpperTHValueTextView.setText("" + _co2AlarmUpValue);
					}
				}
				// Activate the alarm if necessary.
				checkAlarm();
			}
		});
	}
	
	/**
     * Decreases the first upper alarm threshold by one and schedules an automatic decrease if the
     * button is pressed continuously.
     * 
     * @param view which called the method.
     */
	public void startDecFirstAlarmUpTH(View view) {
		// Decrease the first upper threshold value by one.
		decFirstAlarmUpTH();
		// Schedule an automatic decrease after _incDecAutoStartDelay ms every _incDecStepTime
		// until the button is released.
		_firstAlarmUpperTHDownButtonPressed= true;
    	if (_incDecTimer != null) _incDecTimer.cancel();
    	_incDecTimer = new Timer();
    	_incDecTimer.scheduleAtFixedRate(new TimerTask() {
	        	public void run(){
	        		if(!_firstAlarmUpperTHDownButtonPressed) {
	        			this.cancel();
	        		} else {
	        			decFirstAlarmUpTH();
	        		}
	        	}
        	}, _incDecAutoStartDelay, _incDecStepTime);
	}
	
	/**
	 * Performs an decrease of the currently selected first upper alarm threshold,
	 * displays the new value and activates the alarm if necessary. 
	 */
	public void decFirstAlarmUpTH() {
		// Schedule the decrease in the main GUI-thread.
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView firstAlarmUpperTHValueTextView = (TextView) findViewById(R.id.firstAlarmUpperTHValueTextView);
				if (_menuSelection == 0) {  // EKG-settings selected.
					if ((_ekgAlarmUpValue - _ekgAlarmIncDecStep) >= _ekgAlarmUpValueMin) {
						// When the new value doesn't conflict with the borders, save and display it.
						_ekgAlarmUpValue = _ekgAlarmUpValue - _ekgAlarmIncDecStep;
						firstAlarmUpperTHValueTextView.setText("" + _ekgAlarmUpValue);
					}		
				} else if (_menuSelection == 1) {  // RR-settings selected.
					if ((_rrSysAlarmUpValue - _rrAlarmIncDecStep) >= _rrSysAlarmUpValueMin) {
						// When the new value doesn't conflict with the borders, save and display it.
						_rrSysAlarmUpValue = _rrSysAlarmUpValue - _rrAlarmIncDecStep;
						firstAlarmUpperTHValueTextView.setText("" + _rrSysAlarmUpValue);
					}
				} else if (_menuSelection == 3) {  // CO2-settings selected.
					if ((_co2AlarmUpValue - _co2AlarmIncDecStep) >= _co2AlarmUpValueMin) {
						// When the new value doesn't conflict with the borders, save and display it.
						_co2AlarmUpValue = _co2AlarmUpValue - _co2AlarmIncDecStep;
						firstAlarmUpperTHValueTextView.setText("" + _co2AlarmUpValue);
					}
				}
				// Activate the alarm if necessary.
				checkAlarm();
			}
		});
	}
	
	/**
     * Increases the first lower alarm threshold by one and schedules an automatic increase if the
     * button is pressed continuously.
     * 
     * @param view which called the method.
     */
	public void startIncFirstAlarmLowTH(View view) {
		// Increase the first lower threshold value by one.
		incFirstAlarmLowTH();
		// Schedule an automatic increase after _incDecAutoStartDelay ms every _incDecStepTime
		// until the button is released.
		_firstAlarmLowerTHUpButtonPressed= true;
    	if (_incDecTimer != null) _incDecTimer.cancel();
    	_incDecTimer = new Timer();
    	_incDecTimer.scheduleAtFixedRate(new TimerTask() {
	        	public void run(){
	        		if(!_firstAlarmLowerTHUpButtonPressed) {
	        			this.cancel();
	        		} else {
	        			incFirstAlarmLowTH();
	        		}
	        	}
        	}, _incDecAutoStartDelay, _incDecStepTime);
	}
	
	/**
	 * Performs an increase of the currently selected first lower alarm threshold,
	 * displays the new value and activates the alarm if necessary. 
	 */
	public void incFirstAlarmLowTH() {
		// Schedule the increase in the main GUI-thread.
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView firstAlarmLowerTHValueTextView = (TextView) findViewById(R.id.firstAlarmLowerTHValueTextView);
				if (_menuSelection == 0) {  // EKG-settings selected.
					if ((_ekgAlarmLowValue + _ekgAlarmIncDecStep) <= _ekgAlarmLowValueMax) {
						// When the new value doesn't conflict with the borders, save and display it.
						_ekgAlarmLowValue = _ekgAlarmLowValue + _ekgAlarmIncDecStep;
						firstAlarmLowerTHValueTextView.setText("" + _ekgAlarmLowValue);
					}
				} else if (_menuSelection == 1) {  // RR-settings selected.
					if ((_rrSysAlarmLowValue + _rrAlarmIncDecStep) <= _rrSysAlarmLowValueMax) {
						// When the new value doesn't conflict with the borders, save and display it.
						_rrSysAlarmLowValue = _rrSysAlarmLowValue + _rrAlarmIncDecStep;
						firstAlarmLowerTHValueTextView.setText("" + _rrSysAlarmLowValue);
					}
				} else if (_menuSelection == 2) {  // O2-settings selected.
					if ((_o2AlarmLowValue + _o2AlarmIncDecStep) <= _o2AlarmLowValueMax) {
						// When the new value doesn't conflict with the borders, save and display it.
						_o2AlarmLowValue = _o2AlarmLowValue + _o2AlarmIncDecStep;
						firstAlarmLowerTHValueTextView.setText("" + _o2AlarmLowValue);
					}
				} else if (_menuSelection == 3) {  // CO2-settings selected.
					if ((_co2AlarmLowValue + _co2AlarmIncDecStep) <= _co2AlarmLowValueMax) {
						// When the new value doesn't conflict with the borders, save and display it.
						_co2AlarmLowValue = _co2AlarmLowValue + _co2AlarmIncDecStep;
						firstAlarmLowerTHValueTextView.setText("" + _co2AlarmLowValue);
					}
				}
				// Activate the alarm if necessary.
				checkAlarm();
			}
		});
	}
	
	/**
     * Decreases the first lower alarm threshold by one and schedules an automatic decrease if the
     * button is pressed continuously.
     * 
     * @param view which called the method.
     */
	public void startDecFirstAlarmLowTH(View view) {
		// Decrease the first lower threshold value by one.
		decFirstAlarmLowTH();
		// Schedule an automatic decrease after _incDecAutoStartDelay ms every _incDecStepTime
		// until the button is released.
		_firstAlarmLowerTHDownButtonPressed= true;
    	if (_incDecTimer != null) _incDecTimer.cancel();
    	_incDecTimer = new Timer();
    	_incDecTimer.scheduleAtFixedRate(new TimerTask() {
	        	public void run(){
	        		if(!_firstAlarmLowerTHDownButtonPressed) {
	        			this.cancel();
	        		} else {
	        			decFirstAlarmLowTH();
	        		}
	        	}
        	}, _incDecAutoStartDelay, _incDecStepTime);
	}
	
	/**
	 * Performs an decrease of the currently selected first lower alarm threshold,
	 * displays the new value and activates the alarm if necessary. 
	 */
	public void decFirstAlarmLowTH() {
		// Schedule the decrease in the main GUI-thread.
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView firstAlarmLowerTHValueTextView = (TextView) findViewById(R.id.firstAlarmLowerTHValueTextView);
				if (_menuSelection == 0) {  // EKG-settings selected.
					if ((_ekgAlarmLowValue - _ekgAlarmIncDecStep) >= _ekgAlarmLowValueMin) {
						// When the new value doesn't conflict with the borders, save and display it.
						_ekgAlarmLowValue = _ekgAlarmLowValue - _ekgAlarmIncDecStep;
						firstAlarmLowerTHValueTextView.setText("" + _ekgAlarmLowValue);
					}
				} else if (_menuSelection == 1) {  // RR-settings selected.
					if ((_rrSysAlarmLowValue - _rrAlarmIncDecStep) >= _rrSysAlarmLowValueMin) {
						// When the new value doesn't conflict with the borders, save and display it.
						_rrSysAlarmLowValue = _rrSysAlarmLowValue - _rrAlarmIncDecStep;
						firstAlarmLowerTHValueTextView.setText("" + _rrSysAlarmLowValue);
					}
				} else if (_menuSelection == 2) {  // O2-settings selected.
					if ((_o2AlarmLowValue - _o2AlarmIncDecStep) >= _o2AlarmLowValueMin) {
						// When the new value doesn't conflict with the borders, save and display it.
						_o2AlarmLowValue = _o2AlarmLowValue - _o2AlarmIncDecStep;
						firstAlarmLowerTHValueTextView.setText("" + _o2AlarmLowValue);
					}
				} else if (_menuSelection == 3) {  // CO2-settings selected.
					if ((_co2AlarmLowValue - _co2AlarmIncDecStep) >= _co2AlarmLowValueMin) {
						// When the new value doesn't conflict with the borders, save and display it.
						_co2AlarmLowValue = _co2AlarmLowValue - _co2AlarmIncDecStep;
						firstAlarmLowerTHValueTextView.setText("" + _co2AlarmLowValue);
					}
				}
				// Activate the alarm if necessary.
				checkAlarm();
			}
		});
	}
	
	/**
     * Increases the second upper alarm threshold by one and schedules an automatic increase if the
     * button is pressed continuously.
     * 
     * @param view which called the method.
     */
	public void startIncSecondAlarmUpTH(View view) {
		// Increase the second upper threshold value by one.
		incSecondAlarmUpTH();
		_secondAlarmUpperTHUpButtonPressed= true;
		// Schedule an automatic increase after _incDecAutoStartDelay ms every _incDecStepTime
		// until the button is released.
    	if (_incDecTimer != null) _incDecTimer.cancel();
    	_incDecTimer = new Timer();
    	_incDecTimer.scheduleAtFixedRate(new TimerTask() {
	        	public void run(){
	        		if(!_secondAlarmUpperTHUpButtonPressed) {
	        			this.cancel();
	        		} else {
	        			incSecondAlarmUpTH();
	        		}
	        	}
        	}, _incDecAutoStartDelay, _incDecStepTime);
	}
	
	/**
	 * Performs an increase of the currently selected second upper alarm threshold,
	 * displays the new value and activates the alarm if necessary. 
	 */
	public void incSecondAlarmUpTH() {
		// Schedule the increase in the main GUI-thread.
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView secondAlarmUpperTHValueTextView = (TextView) findViewById(R.id.secondAlarmUpperTHValueTextView);
				if (_menuSelection == 1) {  // RR-settings selected.
					if ((_rrDiaAlarmUpValue + _rrAlarmIncDecStep) <= _rrDiaAlarmUpValueMax) {
						// When the new value doesn't conflict with the borders, save and display it.
						_rrDiaAlarmUpValue = _rrDiaAlarmUpValue + _rrAlarmIncDecStep;
						secondAlarmUpperTHValueTextView.setText("" + _rrDiaAlarmUpValue);
					}
				}
				// Activate the alarm if necessary.
				checkAlarm();
			}
		});
	}
	
	/**
     * Decreases the second upper alarm threshold by one and schedules an automatic decrease if the
     * button is pressed continuously.
     * 
     * @param view which called the method.
     */
	public void startDecSecondAlarmUpTH(View view) {
		// Decrease the second upper threshold value by one.
		decSecondAlarmUpTH();
		// Schedule an automatic decrease after _incDecAutoStartDelay ms every _incDecStepTime
		// until the button is released.
		_secondAlarmUpperTHDownButtonPressed= true;
    	if (_incDecTimer != null) _incDecTimer.cancel();
    	_incDecTimer = new Timer();
    	_incDecTimer.scheduleAtFixedRate(new TimerTask() {
	        	public void run(){
	        		if(!_secondAlarmUpperTHDownButtonPressed) {
	        			this.cancel();
	        		} else {
	        			decSecondAlarmUpTH();
	        		}
	        	}
        	}, _incDecAutoStartDelay, _incDecStepTime);
	}
	
	/**
	 * Performs an decrease of the currently selected second upper alarm threshold,
	 * displays the new value and activates the alarm if necessary. 
	 */
	public void decSecondAlarmUpTH() {
		// Schedule the decrease in the main GUI-thread.
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView secondAlarmUpperTHValueTextView = (TextView) findViewById(R.id.secondAlarmUpperTHValueTextView);
				if (_menuSelection == 1) {  // RR-settings selected.
					if ((_rrDiaAlarmUpValue - _rrAlarmIncDecStep) >= _rrDiaAlarmUpValueMin) {
						// When the new value doesn't conflict with the borders, save and display it.
						_rrDiaAlarmUpValue = _rrDiaAlarmUpValue - _rrAlarmIncDecStep;
						secondAlarmUpperTHValueTextView.setText("" + _rrDiaAlarmUpValue);
					}
				}
				// Activate the alarm if necessary.
				checkAlarm();
			}
		});
	}
	
	/**
     * Increases the second lower alarm threshold by one and schedules an automatic increase if the
     * button is pressed continuously.
     * 
     * @param view which called the method.
     */
	public void startIncSecondAlarmLowTH(View view) {
		// Increase the second lower threshold value by one.
		incSecondAlarmLowTH();
		// Schedule an automatic increase after _incDecAutoStartDelay ms every _incDecStepTime
		// until the button is released.
		_secondAlarmLowerTHUpButtonPressed= true;
    	if (_incDecTimer != null) _incDecTimer.cancel();
    	_incDecTimer = new Timer();
    	_incDecTimer.scheduleAtFixedRate(new TimerTask() {
	        	public void run(){
	        		if(!_secondAlarmLowerTHUpButtonPressed) {
	        			this.cancel();
	        		} else {
	        			incSecondAlarmLowTH();
	        		}
	        	}
        	}, _incDecAutoStartDelay, _incDecStepTime);
	}
	
	/**
	 * Performs an increase of the currently selected second lower alarm threshold,
	 * displays the new value and activates the alarm if necessary. 
	 */
	public void incSecondAlarmLowTH() {
		// Schedule the increase in the main GUI-thread.
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView secondAlarmLowerTHValueTextView = (TextView) findViewById(R.id.secondAlarmLowerTHValueTextView);
				if (_menuSelection == 1) {  // RR-settings selected.
					if ((_rrDiaAlarmLowValue + _rrAlarmIncDecStep) <= _rrDiaAlarmLowValueMax) {
						// When the new value doesn't conflict with the borders, save and display it.
						_rrDiaAlarmLowValue = _rrDiaAlarmLowValue + _rrAlarmIncDecStep;
						secondAlarmLowerTHValueTextView.setText("" + _rrDiaAlarmLowValue);
					}
				}
				// Activate the alarm if necessary.
				checkAlarm();
			}
		});
	}
	
	/**
     * Decreases the second lower alarm threshold by one and schedules an automatic decrease if the
     * button is pressed continuously.
     * 
     * @param view which called the method.
     */
	public void startDecSecondAlarmLowTH(View view) {
		// Decrease the second lower threshold value by one.
		decSecondAlarmLowTH();
		// Schedule an automatic decrease after _incDecAutoStartDelay ms every _incDecStepTime
		// until the button is released.
		_secondAlarmLowerTHDownButtonPressed= true;
    	if (_incDecTimer != null) _incDecTimer.cancel();
    	_incDecTimer = new Timer();
    	_incDecTimer.scheduleAtFixedRate(new TimerTask() {
	        	public void run(){
	        		if(!_secondAlarmLowerTHDownButtonPressed) {
	        			this.cancel();
	        		} else {
	        			decSecondAlarmLowTH();
	        		}
	        	}
        	}, _incDecAutoStartDelay, _incDecStepTime);
	}
	
	/**
	 * Performs an decrease of the currently selected second lower alarm threshold,
	 * displays the new value and activates the alarm if necessary. 
	 */
	public void decSecondAlarmLowTH() {
		// Schedule the decrease in the main GUI-thread.
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView secondAlarmLowerTHValueTextView = (TextView) findViewById(R.id.secondAlarmLowerTHValueTextView);
				if (_menuSelection == 1) {  // RR-settings selected.
					if ((_rrDiaAlarmLowValue - _rrAlarmIncDecStep) >= _rrDiaAlarmLowValueMin) {
						// When the new value doesn't conflict with the borders, save and display it.
						_rrDiaAlarmLowValue = _rrDiaAlarmLowValue - _rrAlarmIncDecStep;
						secondAlarmLowerTHValueTextView.setText("" + _rrDiaAlarmLowValue);
					}
				}
				// Activate the alarm if necessary.
				checkAlarm();
			}
		});
	}
	
	/**
	 * Returns the current EKG-value.
	 * 
	 * @return current EKG value.
	 */
	public int getEKGValue() {
		return _ekgValue;
	}
	
	/**
	 * Returns the current diastolic blood pressure-value.
	 * 
	 * @return current diastolic blood pressure value.
	 */
	public int getDiaBPValue() {
		return _diaBloodPressure;
	}
	
	/**
	 * Returns the current systolic blood pressure-value.
	 * 
	 * @return current systolic blood pressure value.
	 */
	public int getSysBPValue() {
		return _sysBloodPressure;
	}
	
	/**
	 * Returns the current O2-value.
	 * 
	 * @return current O2 value.
	 */
	public int getO2Value() {
		return _o2Value;
	}
	
	/**
	 * Returns the current CO2-value.
	 * 
	 * @return current CO2 value.
	 */
	public int getCO2Value() {
		return _co2Value;
	}
	
	/**
	 * Returns the current respiration-value.
	 * 
	 * @return current respiration value.
	 */
	public int getRespValue() {
		return _respValue;
	}
	
	/**
	 * Triggers a update of the GUI with a new event.
	 * 
	 * @param jsonEvent - The new event as JSON string.
	 */
	public void newEvent(String jsonEvent) {
		_updateHandler.updateGui(jsonEvent);
	}
	
	/**
	 * Triggers a change of the general EKG-curve-pattern and saves it. Start the
	 * asystole alarm and stops the normal alarm, if necessary.
	 * 
	 * @param p - New curve pattern.
	 */
	public void changeEKGPattern(Event.HeartPattern p) {
		_signalServer.changeHeartRhythm(p);
		_heartPattern = p;
		if (p == Event.HeartPattern.ASYSTOLE) {
			_soundHandler.playAsystoleAlarm(true);
			_soundHandler.setEKGAlarm(false);
			_soundHandler.setRRAlarm(false);
			_soundHandler.setO2Alarm(false);
		} else {
			_soundHandler.playAsystoleAlarm(false);
			checkAlarm();
			// Stop the parameter blinking if necessary.
			if (_paramBlinkTimer != null) {
				_paramBlinkTimer.cancel();
				_paramBlinkActive = false;
			}
		}
	}
	
	/**
	 * Triggers a change of the general O2-curve-pattern.
	 * 
	 * @param p - New curve pattern.
	 */
	public void changeO2Pattern(Event.O2Pattern p) {
		_signalServer.changeO2pattern(p);
	}
	
	/**
	 * Sets the time of the timer.
	 * 
	 * @param timeStamp - New time of the timer.
	 */
	public void setTimerValue(int timeStamp) {
		_timeInSec = timeStamp;
	}
	
	/**
	 * Sets if EKG-curve/parameter is active.
	 * 
	 * @param active - Indicates if EKG is active.
	 */
	public void setEKGActive (final boolean active) {
		_ekgActive = active;
		// Schedule show/hide  of the curve and values.
		runOnUiThread(new Runnable() {
			public void run() {
				TextView ekgValueTextView = (TextView) findViewById(R.id.ekgValueTextView);
				if(active) {
					_glActivivty.ToogleLine(GLRenderer.LineType.Heart, true);
					ekgValueTextView.setText("" + _ekgValue);
				} else {
					_glActivivty.ToogleLine(GLRenderer.LineType.Heart, false);
					ekgValueTextView.setText("- - -");
				}
			}
		});
	}
	
	/**
	 * Sets if RR-curve/parameter is active.
	 * 
	 * @param active - Indicates if RR is active.
	 */
	public void setRRActive (final boolean active) {
		_rrActive = active;
		// Schedule show/hide  of the curve and values.
		runOnUiThread(new Runnable() {
			public void run() {
				TextView ibpValueTextView = (TextView) findViewById(R.id.ibpValueTextView);
				if(active) {
					_glActivivty.ToogleLine(GLRenderer.LineType.Blood, true);
					ibpValueTextView.setText("" + _sysBloodPressure + "/" + _diaBloodPressure);
				} else {
					_glActivivty.ToogleLine(GLRenderer.LineType.Blood, false);
					ibpValueTextView.setText("- - / - -");
				}
			}
		});
				
	}
	
	/**
	 * Sets if O2-curve/parameter is active.
	 * 
	 * @param active - Indicates if O2 is active.
	 */
	public void setO2Active (final boolean active) {
		_o2Active = active;
		// Schedule show/hide  of the curve and values.
		runOnUiThread(new Runnable() {
			public void run() {
				TextView o2ValueTextView = (TextView) findViewById(R.id.o2ValueTextView);
				if(active) {
					_glActivivty.ToogleLine(GLRenderer.LineType.O2, true);
					o2ValueTextView.setText("" + _o2Value);
				} else {
					_glActivivty.ToogleLine(GLRenderer.LineType.O2, false);
					o2ValueTextView.setText("- -");
				}
			}
		});
	}
	
	/**
	 * Sets if CO2-curve/parameter is active.
	 * 
	 * @param active - Indicates if CO2 is active.
	 */
	public void setCO2Active (final boolean active) {
		_co2Active = active;
		// Schedule show/hide  of the curve and values.
		runOnUiThread(new Runnable() {
			public void run() {
				TextView co2ValueTextView = (TextView) findViewById(R.id.co2ValueTextView);
				if(active) {
					_glActivivty.ToogleLine(GLRenderer.LineType.CO2, true);
					co2ValueTextView.setText("" + _co2Value);
				} else {
					_glActivivty.ToogleLine(GLRenderer.LineType.CO2, false);
					co2ValueTextView.setText("- -");
				}
			}
		});
	}
	
	/**
	 * Sets if Respiration-parameter is active.
	 * 
	 * @param active - Indicates if CO2 is active.
	 */
	public void setRespActive (final boolean active) {
		_respActive = active;
		// Schedule show/hide  of the curve and values.
		runOnUiThread(new Runnable() {
			public void run() {
				TextView afValueTextView = (TextView) findViewById(R.id.afValueTextView);
				if(active) {
					afValueTextView.setText("" + _respValue);
				} else {
					afValueTextView.setText("- -");
				}
			}
		});
	}
	
	/**
	 * Sets if NIBP is active.
	 * 
	 * @param active - Indicates if NIBP is active.
	 */
	public void setNIBPActive (boolean active) {
		_nibpActive = active;
	}
	
	/**
	 * Increase the energy of the defibrilator.
	 */
	public void defiEnergyUp(View view) {
		if (_defiEnergy + 10 <= _defiEnergyThr)
			_defiEnergy = ((_defiEnergy + 10 == _defiEnergyUpperBound)
				? _defiEnergyUpperBound : _defiEnergy + 10);
		else 
			_defiEnergy = ((_defiEnergy + 50 >= _defiEnergyUpperBound)
					? _defiEnergyUpperBound : _defiEnergy + 50);
		TextView energy = (TextView) this.findViewById(R.id.defiEnergy);
		energy.setText(String.valueOf(_defiEnergy)+ " J");
	}
	
	/**
	 * Decrease the energy of the defibrilator.
	 */
	public void defiEnergyDown(View view) {
		if (_defiEnergy - 10 <= _defiEnergyThr)
			_defiEnergy = ((_defiEnergy - 10 <= _defiEnergyLowerBound)
				? _defiEnergyLowerBound : _defiEnergy - 10);
		else 
			_defiEnergy = ((_defiEnergy - 50 <= _defiEnergyLowerBound)
					? _defiEnergyLowerBound : _defiEnergy - 50);
		TextView energy = (TextView) this.findViewById(R.id.defiEnergy);
		energy.setText(String.valueOf(_defiEnergy)+ " J");
	}
	
	/**
	 * Charge the defibrilator
	 */
	public void defiCharge(View view) {
		if (!_defiCharged && !_defiCharging) {
			_soundHandler.playDefiCharge(this.getBaseContext());
			_handler = new Handler();
			_defiCharging = true;
			_handler.postDelayed(new Runnable() {

				@Override
				public void run() {					
					_handler.removeCallbacks(this);					
					if (_defiCharging) {
						_defiCharged = true;
						changeShockButton();
						_soundHandler.playDefiReady(getBaseContext());
					}					
					_defiCharging = false;
				}
			}, 4000);
		}
		
		/*
	    _defiTimerInterval = (int) (((double) _defi1kTime * ((double)_defiEnergy / 1000)) / (double)_defiFrequencySteps);
	    _defiSoundIncrement = (int) (((double)_defi1kFrequency * ((double)(_defiEnergy) / 1000)) / (double)_defiFrequencySteps);
		_defiTimer = new Timer();
		_defiTimer.scheduleAtFixedRate(new TimerTask() {
	        	public void run(){
	        		if (_defiSteps >= _defiFrequencySteps) {     			
	        			_defiCharged = true;
	        			_defiSteps = 0;
	        			_defiSoundFrequency = 100;
	        			MonitorMainScreen.this.runOnUiThread(new Runnable() {
	        				public void run() {
	        					changeShockButton(true);
	        				}
	        			});
	        			this.cancel();
	        			
	        		} else {
	        			_soundHandler.playFreqSound(_defiSoundFrequency,
	        					_defiTimerInterval);
	        			_defiSoundFrequency += _defiSoundIncrement;
	        			_defiSteps++;
	        		}        		
	    		}
        	}, 0, _defiTimerInterval);
        	*/
	}
	
	/**
	 * Discharge the defibrilator
	 */
	public void defiDischarge(View view) {
		_defiCharged = false;
		_defiCharging = false;
		changeShockButton();
		_soundHandler.stopDefiReady(getBaseContext());
	}
	
	/**
	 * Change the background of the shock button.
	 */
	private void changeShockButton() {
		runOnUiThread(new Runnable() {			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				ImageButton shockButton = (ImageButton)
						findViewById(R.id.defiShockButton);
				if (_defiCharged)
					shockButton.setBackgroundResource(R.drawable.roundedbutton_green);
				else
					shockButton.setBackgroundResource(R.drawable.roundedbutton);	
			}
		});
	}
	
	/**
	 * Make a shock.
	 */
	public void shock(View view) {
		if (_defiCharged) {
			_signalServer.getShocked();
			_defiCharged = false;
			changeShockButton();
			_soundHandler.stopDefiReady(getBaseContext());
		}
	}
}
