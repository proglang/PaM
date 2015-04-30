/*
 * Copyright: Universität Freiburg, 2015
 * Authors: Marc Pfeifer <pfeiferm@tf.uni-freiburg.de> Everything except Defibrillator-Sounds
 * 			Johannes Scherle <johannes.scherle@googlemail.com> Defibrillator-Sounds
 */

package monitor.pack;

import java.util.Timer;
import java.util.TimerTask;
import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;


/**
 * A class which handles the complete sound-output of a patient-monitor. This consists of the
 * playback of given sound-files and the generation and playback of sounds with a given frequency. 
 */
public class SoundHandler {
	
	
  // PUBLIC:
	
	
	/**
	 * Constructor which initializes a player, the sound-files and some members. 
	 * 
	 * @param m - The main GUI-thread.
	 */
	public SoundHandler(MonitorMainScreen m) {
		// Create a sound-player according to the API-level.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			buildSoundHandler();
		} else {
			createSoundHandler();
		}
		// Load the pump-sound for non invasive blood pressure measurement.
		_bpSound = _sp.load(m, R.raw.bpmeasuremntsound, 1);
		
		// Initialize some members.
		_ekgAlarm = false;
		_rrAlarm = false;
		_o2Alarm = false;
		_alarmOn = false;
		_asysNormalSound = true;
		_asysAlarmOn = false;
	}
	
	/**
	 * Plays the pump-sound for non invasive blood pressure measurement.
	 */
	public void playBPSound() {
		if(_bpSound != 0)
    		_sp.play(_bpSound, 1, 1, 0, 0, 1);
	}
	
	/**
	 * A function which plays a sine sound with a given frequency for a given time.
	 * (Based on code form Singhak (http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android).)
	 *
	 *@param freq - Frequency of the sound.
	 *@param length - Length of the sound in ms.
	 */
	public void playFreqSound(float freq, int length) {
		// Calculate the needed number of samples to get the given play-length.
		int bufSize = length * (_sampleRate / 1000);
		// Get the minimum number of samples to get a proper sound.
	    int minBufSize = AudioTrack.getMinBufferSize(_sampleRate, 
	    		AudioFormat.CHANNEL_OUT_MONO, 
	    		AudioFormat.ENCODING_PCM_16BIT);
	    // Correct the number samples if necessary.
	    if (bufSize < minBufSize) {
	    	bufSize = minBufSize;
	    }
	    // Create a buffer with the calculated length and fill it with samples of sine wave with the given frequency.
	    short[] buffer = new short[bufSize];
	    float angle = 0;
	    float angular_frequency = (float) (2*Math.PI) * freq / _sampleRate;
	    for (int i = 0; i < buffer.length; i++) {
	    	buffer[i] = (short)(Short.MAX_VALUE * ((float) Math.sin(angle)) * _soundVolume );
	    	angle += angular_frequency;
	    }
	    // Close the (old) AudioTrack if necessary.
	    if (_soundAudioTrack != null) {
	    	_soundAudioTrack.release();
	    }
	    // Create a new audio-stream and start it. 
	    _soundAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
	    		_sampleRate, 
	    		AudioFormat.CHANNEL_OUT_MONO, 
	    		AudioFormat.ENCODING_PCM_16BIT, 
	    		bufSize, 
	    		AudioTrack.MODE_STREAM);
	    _soundAudioTrack.play();
	    // Output the buffer via the stream and stop it.
	    int b = _soundAudioTrack.write(buffer, 0, buffer.length);
	    _soundAudioTrack.stop();
	    if (DEBUG) System.out.println("Number of bytes written to sound audioTrack: " + b);
	}
	
	/**
	 * A function which plays a sine alarm-sound with a given frequency for a given time. The sound fades
	 * in at the begin and fades out at the end.
	 * (Based on code form Singhak (http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android).)
	 *
	 *@param freq - Frequency of the sound.
	 *@param length - Length of the sound in ms.
	 */
	public void playFreqAlarm(float freq, int length) {
		// Calculate the needed number of samples to get the given play-length.
		int bufSize = length * (_sampleRate / 1000);
		// Get the minimum number of samples to get a proper sound.
	    int minBufSize = AudioTrack.getMinBufferSize(_sampleRate, 
	    		AudioFormat.CHANNEL_OUT_MONO, 
	    		AudioFormat.ENCODING_PCM_16BIT);
	    // Correct the number samples if necessary.
	    if (bufSize < minBufSize) {
	    	bufSize = minBufSize;
	    }
	    // Calculate the number of periods in the whole sound.
	    float periodNumber = freq * ((float) length / 1000.0f);
	    // Calculate the number of samples in one period.
	    float samplesPerPeriod = ((float) bufSize) / periodNumber;
	    // Calculate how much the volume of each period must be increased/decreased to fade in/out the sound.
	    float incDecFactor = _maxAlarmVol / (periodNumber / _fadeFraction);
	    // Some running variables.
	    int periodCount = 1;
	    float multFactor = 0;
	    if (DEBUG) {
	    	System.out.println("periodNumber: " + periodNumber 
	    			+ ", samplesPerPeriod: " + samplesPerPeriod 
	    			+ ", incDecFactor: " + incDecFactor);
	    }
	    // Create a buffer with the calculated length and fill it with samples of sine wave with the given frequency.
	    short[] buffer = new short[bufSize];
	    float angle = 0;
	    float angular_frequency = (float) (2*Math.PI) * freq / _sampleRate;
	    for (int i = 0; i < buffer.length; i++) {
	    	buffer[i] = (short)(Short.MAX_VALUE * ((float) Math.sin(angle)) * multFactor);
	    	angle += angular_frequency;
	    	// Check if one hole period is completed.
	    	if (i >= (samplesPerPeriod * periodCount)) {
	    		periodCount++;
	    		// When we are in last fraction of the sound, decrease the multiplication factor
	    		// which represents the volume of a period.
	    		if (periodCount > ((_fadeFraction - 1.0f) * (periodNumber / _fadeFraction))) {
	    			multFactor = _maxAlarmVol - incDecFactor * ((float) (periodCount - (int) ((_fadeFraction - 1.0f) *(periodNumber / _fadeFraction))));
	    			if (DEBUG) System.out.println("2. Half - multfactor: " + multFactor);
	    		} 
	    		// When we are in first fraction of the sound, increase the multiplication factor
	    		// which represents the volume of a period.
	    		if (periodCount < (periodNumber / _fadeFraction)) {
	    			multFactor = incDecFactor * (float) periodCount;
	    			if (DEBUG) System.out.println("1. Half - multfactor: " + multFactor);
	    		}
	    	}
	    }
	    // Close the (old) AudioTrack if necessary.
	    if (_alarmAudioTrack != null) {
	    	_alarmAudioTrack.release();
	    }
	    // Create a new audio-stream and start it. 
	    _alarmAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
	    		_sampleRate, 
	    		AudioFormat.CHANNEL_OUT_MONO, 
	    		AudioFormat.ENCODING_PCM_16BIT, 
	    		buffer.length, 
	    		AudioTrack.MODE_STREAM);
	    _alarmAudioTrack.play();
	    // Output the buffer via the stream and stop it.
	    int b = _alarmAudioTrack.write(buffer, 0, buffer.length);
	    _alarmAudioTrack.stop();
	    if (DEBUG) System.out.println("Number of bytes written to alarm audioTrack: " + b);
	}
	
	/**
	 * Plays the heart-beat-sound once corresponding to current o2 saturation.
	 *
	 *@param o2SatAvailable - Indicates if a o2 saturation value is available.
	 *@param o2Sat - The current o2 saturation.
	 */
	public void playHeartSound(boolean o2SatAvailable, int o2Sat) {
		if (o2SatAvailable) {
			// If the o2 saturation vary the frequency of the sound according to it.
			switch (o2Sat) {
				case 100:
					playFreqSound(2200, _soundLength);  // CIS
				break;
				case 99:
					playFreqSound(2093, _soundLength);  // C
				break;
				case 98:
					playFreqSound(1975, _soundLength);  // H
				break;
				case 97:
					playFreqSound(1864, _soundLength);  // AIS
					break;
				case 96:
					playFreqSound(1760, _soundLength);  // A
					break;
				case 95:
					playFreqSound(1661, _soundLength);  // GIS
					break;
				case 94:
					playFreqSound(1567, _soundLength);  // G
					break;
				case 93:
					playFreqSound(1479, _soundLength);  // FIS
					break;
				case 92:
					playFreqSound(1369, _soundLength);  // F
					break;
				case 91:
					playFreqSound(1318, _soundLength);  // E
					break;
				case 90:
					playFreqSound(1244, _soundLength);  // DIS
					break;
				case 89:
					playFreqSound(1174, _soundLength);  // D
					break;
				case 88:
					playFreqSound(1108, _soundLength);  // CIS
					break;
				case 87:
					playFreqSound(1046, _soundLength);  // C
					break;
				case 86:
					playFreqSound(987, _soundLength);  // H
					break;
				case 85:
					playFreqSound(932, _soundLength);  // AIS
					break;
				default:
					playFreqSound(932, _soundLength);  // AIS
					break;
			}
		} else {
			// If no o2 saturation is available play the standard 932Hz-Sound.
			playFreqSound(932, _soundLength);  // AIS
		}
	}
	
	/**
	 * Activates/Deactivates the EKG alarm.
	 *
	 *@param on - Indicates if the alarm should be on.
	 */
	public void setEKGAlarm(boolean on) {
		_ekgAlarm = on;
		startStopAlarm();
	}
	
	/**
	 * Activates/Deactivates the blood pressure alarm.
	 *
	 *@param on - Indicates if the alarm should be on.
	 */
	public void setRRAlarm(boolean on) {
		_rrAlarm = on;
		startStopAlarm();
	}
	
	/**
	 * Activates/Deactivates the O2 alarm.
	 *
	 *@param on - Indicates if the alarm should be on.
	 */
	public void setO2Alarm(boolean on) {
		_o2Alarm = on;
		startStopAlarm();
	}
	
	/**
	 * Activates/Deactivates the CO2 alarm.
	 *
	 *@param on - Indicates if the alarm should be on.
	 */
	public void setCO2Alarm(boolean on) {
		_co2Alarm = on;
		startStopAlarm();
	}
	
	/**
	 * Activates/Deactivates the asystole alarm.
	 *
	 *@param on - Indicates if the alarm should be on.
	 */
	public void playAsystoleAlarm(boolean on) {
		if (on) {
			// If the alarm isn't already active, start it.
			if(!_asysAlarmOn) {
			// Start a sound which plays the normal alarm sound followed by lower sound and
			// repeat it until it's stopped. 
			_asysAlarmOn = true;
			_asysTimer = new Timer();
			_asysTimer.scheduleAtFixedRate(new TimerTask() {
		        	public void run(){
		        		if (_asysNormalSound) {
		        			playFreqAlarm(_alarmFreq, _asysAlarmLength);
		        			_asysNormalSound = false;
		        		} else {
		        			playFreqAlarm(_asysAlarmFreq, _asysAlarmLength);
		        			_asysNormalSound = true;
		        		}
		    		}
	        	}, _asysAlarmInterval, _asysAlarmInterval);
			}
		} else {
			// Stop the sound.
			if (_asysTimer != null) {
				_asysTimer.cancel();
			}
			_asysAlarmOn = false;
		}
	}
	
	/**
	 * Destroys all used instances if necessary.
	 */
	public void destroy() {
		if (_soundAudioTrack != null) _soundAudioTrack.release();
		if (_alarmAudioTrack != null) _alarmAudioTrack.release();
		if (_timer != null) _timer.cancel();
		if (_asysTimer != null) _asysTimer.cancel();
		if (_sp != null) _sp.release();
	}
	
	/**
	 * Plays the defi charge sound. (Created by Johannes)
	 * 
	 * @param c - main context
	 */
	public void playDefiCharge(Context c) {
		_defiMp = MediaPlayer.create(c, R.raw.defiwavcharge);
		_defiMp.start();
	}
	
	/**
	 * Plays the defi ready sound in loop. (Created by Johannes)
	 * 
	 * @param c - main context
	 */
	public void playDefiReady(Context c) {
		_defiMp = MediaPlayer.create(c, R.raw.defiwavready);
		_defiMp.setLooping(true);
		_defiMp.start();		
	}
	
	/**
	 * Stops the defi ready sound. (Created by Johannes)
	 * @param c - main context
	 */
	public void stopDefiReady(Context c) {
		_defiMp.setLooping(false);
		_defiMp.stop();
	}
	
  // PRIVATE:
	
	/**
	 * Create the soundPool acording to API-levels lower 21.
	 */
	@SuppressWarnings("deprecation")
	private void createSoundHandler() {
		_sp = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
	}
	
	/**
	 * Create the soundPool acording to API-level 21.
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void buildSoundHandler() {
		AudioAttributes audioAttributes = new AudioAttributes.Builder()
		.setUsage(AudioAttributes.USAGE_MEDIA)
		.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
		.build();
		_sp = new SoundPool.Builder()
		.setAudioAttributes(audioAttributes)
		.build();
	}
	
	/**
	 * Starts the alarm, if at least one parameter is active and stops it if no one is actove any more.
	 */
	private void startStopAlarm() {
		if ((_ekgAlarm || _rrAlarm || _o2Alarm || _co2Alarm) && !_asysAlarmOn) {
			if (!_alarmOn) {
				// If at least one of the three alarms should be active and the
				// alarm-sound isn't already activated start the alarm by scheduling a
				// sound every _alarmInterval.
				if (DEBUG) System.out.println("Start Alarm");
				_alarmOn = true;
				_timer = new Timer();
		    	_timer.scheduleAtFixedRate(new TimerTask() {
			        	public void run(){
			        		playFreqAlarm(_alarmFreq, _alarmLength);
			    		}
		        	}, _alarmInterval, _alarmInterval);
			}
		} else if (_alarmOn) {
			// If no of the three alarms should be active, deactivate the alarm-sound.
			if (DEBUG) System.out.println("Stop Alarm");
			if (_timer != null) {
				_timer.cancel();
			}
			_alarmOn = false;
		}
	}
	
	// FINAL MEMBERS:
	// De(Activate) the debug-messages.
	private final boolean DEBUG = false;
	// The sampling-rate of the generated sounds.
	private final int _sampleRate = 16000;
	// The standard-sound-length.
	private final int _soundLength = 150;
	// The volume of the sound.
	private final float _soundVolume = 0.1f;
	// The interval of the alarm-sounds.
	private final int _alarmInterval = 1000;
	// The length of one alarm sound.
	private final int _alarmLength = 500;
	// The frequency of the alarm sound.
	private final int _alarmFreq = 660;
	// Maximum volume of the alarm sound.
	private final float _maxAlarmVol = 0.5f;
	// The number by which the length of the alarm sound is divided. 
	// With this one can control how long the fade in, the hold, and the 
	// fade out time are. The first fraction is the fade in time, the
	// last the fade out time. Must be >= 2.0f.
	private final float _fadeFraction = 3.0f;
	// The length of one asystole alarm sound.
	private final int _asysAlarmLength = 500;
	// The frequency of the second asystole alarm sound.
	private final int _asysAlarmFreq = 587;
	// The interval of the asystole alarm-sounds.
	private final int _asysAlarmInterval = 1000;
	
	// MEMBERS:
	private SoundPool _sp;  // The soundPool/sound-player.
	private Timer _timer;  // The alarm-sound-timer.
	private Timer _asysTimer;  // The asystole alarm-sound-timer.
	private int _bpSound;  // The pump-sound for non invasive blood pressure measurement
	// Flags which indicates if the alarm for a parameter should be active.
	private boolean _ekgAlarm;
	private boolean _rrAlarm;
	private boolean _o2Alarm;
	private boolean _co2Alarm;
	// A flags which indicates if the alarm-sound is active.
	private boolean _alarmOn;
	// A flags which indicates if the alarm-sound is active.
	private boolean _asysAlarmOn;
	// A flags which indicates which asystole alarm sound should be played next.
	private boolean _asysNormalSound;
	// The AudioTracks.
	AudioTrack _soundAudioTrack;
	AudioTrack _alarmAudioTrack;
	
	// Members for the defi-mode.
	MediaPlayer _defiMp;
}
