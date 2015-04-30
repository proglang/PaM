/*
 * Copyright: Universität Freiburg, 2015
 * Author: Marc Pfeifer <pfeiferm@tf.uni-freiburg.de>
 */

package monitor.pack;

import java.util.Timer;
import java.util.TimerTask;


/**
 * A class which handles the updating of the parameters on the patient monitor
 * when a new event from the controller arrives.
 */
public class UpdateHandler {
	
	/**
	 * Constructor which sets the MonitorMainInstance. 
	 */
	public UpdateHandler(MonitorMainScreen m) {
		this._mms = m;
	}
	
	/**
	 * Updates the GUI with the new values from the event either immediately or
	 * step by step if a schedule time is set. 
	 * 
	 * @param jsonEvent - The new event as JSON string.
	 */
	public void updateGui(String jsonEvent) {
		// Cancel an old scheduled task if necessary.
		if (_timer != null) _timer.cancel();
		// Try to parse the given JSON string into an Event-object.
		Event e;
		try {
			e = Event.fromJsonEvent(jsonEvent);
		} catch (Exception ex) {
			System.err.println(ex);
			return;
		}
		// Synchronize the timer.
		if(e._timerState == Event.TimerState.START) {
			_mms.startStopTimer(true);
		} else if (e._timerState == Event.TimerState.STOP || e._timerState == Event.TimerState.PAUSE) {
			_mms.startStopTimer(false);
		} else if (e._timerState == Event.TimerState.RESET) {
			_mms.resetTimer();
		}
		if(e._syncTimer) {
			_mms.setTimerValue(e._timeStamp);
		}
		// Update the EKG- and the O2-curve-pattern.
		_mms.changeEKGPattern(e._heartPattern);
		_mms.changeO2Pattern(e._oxyPattern);
		// Update the active-states.
		_mms.setEKGActive(e._heartOn);
		_mms.setRRActive(e._bpOn);
		_mms.setO2Active(e._oxyOn);
		_mms.setCO2Active(e._carbOn);
		_mms.setNIBPActive(e._cuffOn);
		_mms.setRespActive(e._respOn);
		// When there is a schedule-time:
		if (e._time > 0) {
			// Get the current values for EKG, blood pressure, O2 and CO2.
			updateValues();
			// Calculate the division-factor.
			_divFactor = Math.round(((float) e._time) / _updateInterval);
			// Calculate the increment/decrement steps-size for each parameter.
			final float heartRateInc = (float) (e._heartRateTo - _startEKGValue) / _divFactor;
			final float diaBloodPressureInc = (float) (e._bloodPressureDias - _startDiaBPValue) / _divFactor;
			final float sysBloodPressureInc = (float) (e._bloodPressureSys - _startSysBPValue) / _divFactor;
			final float o2Inc = (float) (e._oxygenTo - _starto2Value) / _divFactor;
			final float co2Inc = (float) (e._carbTo - _startco2Value) / _divFactor;
			final float respInc = (float) (e._respRate - _startRespValue) / _divFactor;
			if (DEBUG) {
				System.out.println("Start values:");
				System.out.println("EKG: " + _startEKGValue
						+ ", DiaBP: " + _startDiaBPValue
						+ ", SysBP: " + _startSysBPValue
						+ ", O2: " + _starto2Value
						+ ", CO2: " + _startco2Value
						+ ", Resp: " + _startRespValue);
				System.out.println("Number of increment steps: " + _divFactor);
				System.out.println("Increment-Step-Sizes:");
				System.out.println("EKGInc: " + heartRateInc
						+ ", DiaBPInc: " + diaBloodPressureInc
						+ ", SysBPInc: " + sysBloodPressureInc
						+ ", O2Inc: " + o2Inc
						+ ", CO2Inc: " + co2Inc
						+ ", RespInc: " + respInc);
			}
			_updateCount = 0;
			// Create a scheduled task which increases/decreases the parameters
			// each time step until the given final value is reached.
			_timer = new Timer();
	    	_timer.scheduleAtFixedRate(new TimerTask() {
		        	public void run(){
		        		_updateCount++;  // Count the steps.
		        		_mms.setEKG(_startEKGValue + (int) (heartRateInc * (float) _updateCount));
		    			_mms.setIBP(_startDiaBPValue + (int) (diaBloodPressureInc * (float) _updateCount),
		    					_startSysBPValue + (int) (sysBloodPressureInc * (float) _updateCount));
		    			_mms.setO2(_starto2Value + (int) (o2Inc * (float) _updateCount));
		    			_mms.setCO2(_startco2Value + (int) (co2Inc * (float) _updateCount));
		    			_mms.setResp(_startRespValue + (int) (respInc * (float) _updateCount));
		    			if (_updateCount >= _divFactor) {
		    				this.cancel();
		    			}
		        	}
	        	}, (int) (_updateInterval * 1000), (int) (_updateInterval * 1000));
	    // When there is no schedule-time, just set the values directly.
		} else {
			_mms.setEKG(e._heartRateTo);
			_mms.setIBP(e._bloodPressureDias, e._bloodPressureSys);
			_mms.setO2(e._oxygenTo);
			_mms.setCO2(e._carbTo);
			_mms.setResp(e._respRate);
		}
	}
	
	
	/**
	 * Updates the GUI with the new values from the event either immediately or
	 * step by step if a schedule time is set. 
	 * 
	 * @param e - The new event.
	 */
	public void updateGui(final Event e) {
		// Cancel an old scheduled task if necessary.
		if (_timer != null) _timer.cancel();
		// Synchronize the timer.
		if(e._timerState == Event.TimerState.START) {
			_mms.startStopTimer(true);
		} else if (e._timerState == Event.TimerState.STOP || e._timerState == Event.TimerState.PAUSE) {
			_mms.startStopTimer(false);
		} else if (e._timerState == Event.TimerState.RESET) {
			_mms.resetTimer();
		}
		if(e._syncTimer) {
			_mms.setTimerValue(e._timeStamp);
		}
		// Update the EKG- and the O2-curve-pattern.
		_mms.changeEKGPattern(e._heartPattern);
		_mms.changeO2Pattern(e._oxyPattern);
		// Update the active-states.
		_mms.setEKGActive(e._heartOn);
		_mms.setRRActive(e._bpOn);
		_mms.setO2Active(e._oxyOn);
		_mms.setCO2Active(e._carbOn);
		_mms.setNIBPActive(e._cuffOn);
		_mms.setRespActive(e._respOn);
		// When there is a schedule-time:
		if (e._time > 0) {
			// Get the current values for EKG, blood pressure, O2 and CO2.
			updateValues();
			// Calculate the division-factor.
			_divFactor = Math.round(((float) e._time) / _updateInterval);
			// Calculate the increment/decrement steps-size for each parameter.
			final float heartRateInc = (float) (e._heartRateTo - _startEKGValue) / _divFactor;
			final float diaBloodPressureInc = (float) (e._bloodPressureDias - _startDiaBPValue) / _divFactor;
			final float sysBloodPressureInc = (float) (e._bloodPressureSys - _startSysBPValue) / _divFactor;
			final float o2Inc = (float) (e._oxygenTo - _starto2Value) / _divFactor;
			final float co2Inc = (float) (e._carbTo - _startco2Value) / _divFactor;
			final float respInc = (float) (e._respRate - _startRespValue) / _divFactor;
			if (DEBUG) {
				System.out.println("Start values:");
				System.out.println("EKG: " + _startEKGValue
						+ ", DiaBP: " + _startDiaBPValue
						+ ", SysBP: " + _startSysBPValue
						+ ", O2: " + _starto2Value
						+ ", CO2: " + _startco2Value
						+ ", Resp: " + _startRespValue);
				System.out.println("Number of increment steps: " + _divFactor);
				System.out.println("Increment-Step-Sizes:");
				System.out.println("EKGInc: " + heartRateInc
						+ ", DiaBPInc: " + diaBloodPressureInc
						+ ", SysBPInc: " + sysBloodPressureInc
						+ ", O2Inc: " + o2Inc
						+ ", CO2Inc: " + co2Inc
						+ ", RespInc: " + respInc);
			}
			_updateCount = 0;
			// Create a scheduled task which increases/decreases the parameters
			// each time step until the given final value is reached.
			_timer = new Timer();
		   	_timer.scheduleAtFixedRate(new TimerTask() {
		        	public void run(){
		        		_updateCount++;  // Count the steps.
		        		_mms.setEKG(_startEKGValue + (int) (heartRateInc * (float) _updateCount));
		    			_mms.setIBP(_startDiaBPValue + (int) (diaBloodPressureInc * (float) _updateCount),
		    					_startSysBPValue + (int) (sysBloodPressureInc * (float) _updateCount));
		    			_mms.setO2(_starto2Value + (int) (o2Inc * (float) _updateCount));
		    			_mms.setCO2(_startco2Value + (int) (co2Inc * (float) _updateCount));
		    			_mms.setResp(_startRespValue + (int) (respInc * (float) _updateCount));
		    			if (_updateCount >= _divFactor) {
		    				this.cancel();
		    			}
		        	}
		       	}, (int) (_updateInterval * 1000), (int) (_updateInterval * 1000));
		// When there is no schedule-time, just set the values directly.
		} else {
			_mms.setEKG(e._heartRateTo);
			_mms.setIBP(e._bloodPressureDias, e._bloodPressureSys);
			_mms.setO2(e._oxygenTo);
			_mms.setCO2(e._carbTo);
			_mms.setResp(e._respRate);
		}
	}
	
  // PRIVATE:
	
	/**
	 * Gets the current EKG, blood pressure, o2, co2 and respiration values from the monitor
	 * as start-values.
	 */
	private void updateValues() {
		_startEKGValue = _mms.getEKGValue();
		_startDiaBPValue = _mms.getDiaBPValue();
		_startSysBPValue = _mms.getSysBPValue();
		_starto2Value = _mms.getO2Value();
		_startco2Value = _mms.getCO2Value();
		_startRespValue = _mms.getRespValue();
	}
	
	
	// FINAL MEMBERS:
	// De(Activate) the debug-messages.
	private final boolean DEBUG = false;
	// The update-interval-time in seconds. For REALLY exact timing use values 0.1, 0.2, 0.5, 1.
	// Otherwise the number of increment-steps will maybe rounded.
	private final float _updateInterval = 0.5f;
	
	// MEMBERS:
	private MonitorMainScreen _mms;  // The main GUI-Thread.
	private Timer _timer;  // The schedule-timer.
	// The scheduled time divided by the updateInterval -> Number of increment-steps.
	private float _divFactor;
	// The parameter values before the begin of an scheduled update.
	private int _startEKGValue;
	private int _startDiaBPValue;
	private int _startSysBPValue;
	private int _starto2Value;
	private int _startco2Value;
	private int _startRespValue;
	// Counter for the update-steps.
	private int _updateCount;

}
