/**
 * @author  Christian Schönweiß, University Freiburg
 * @since   09.02.2015
 * 
 */

package monitor.pack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.Log;


/**
 * 
 * <h1>Client (Socket)</h1>
 * Every Monitor holds a Client and its Socket
 * Connection to a Controller (Server Socket) Every Client holds two Thread:
 * Connection Thread: Connects Client (Socket) to Controller (Server Socket)
 * Discovery Thread: Discovers registered service of Controller in the Network
 * 
 */

class Client {

	private EnterScreen es;
	private MonitorMainScreen mms;

	// Socket connection containing IP and Port of Controller
	private Socket mSocket;
	private InetAddress controllerIP;
	private final int controllerPort = 3000;

	// Service Name set by User
	private String mServiceName;

	// Debug variable
	private String tagClient = ("Client");

	// Mutex Variables for Thread Concurrency
	// Mutex for Connection and Discovery Thread
	private final Object lockConnect = new Object();
	private final Object lockDiscovery = new Object();

	// Separated Threads for Connection
	private Thread connectionThread = null;
	private Thread discoverThread = null;

	// -- GETTER / SETTER --
	
	void setmServiceName(String serviceName) {
		mServiceName = serviceName;
	}
	
	public void setHost(InetAddress host) {
		this.controllerIP = host;
	}

	public InetAddress getHost() {
		return controllerIP;
	}

	public int getControllerPort() {
		return controllerPort;
	}

	public Socket getSocket() {
		return mSocket;
	}

	
	/**
	 * Sets socket after a socket connection was established
	 * closes socket if a socket exists before and creates a new one
	 * @param socket Established socket connection to Controller
	 */
	private synchronized void setSocket(Socket socket) {
		Log.d(tagClient, "setSocket being called.");
		if (socket == null) {
			Log.d(tagClient, "Setting a null socket.");
		}
		if (mSocket != null) {
			if (mSocket.isConnected()) {
				try {
					mSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		mSocket = socket;
	}

		public Thread getConnectionThread() {
		return connectionThread;
	}

	
	public void setConnectionThread(Thread connThread) {
		this.connectionThread = connThread;
	}

	
	public Thread getDiscoverThread() {
		return discoverThread;
	}

	
	public void setDiscoverThread(Thread discoverThread) {
		this.discoverThread = discoverThread;
	}

	
	public void setMonitorMainScreen(MonitorMainScreen m) {
		this.mms = m;
	}

	// -END- GETTER / SETTER -END-

	
	/**
	 * Creates a new instance of a client. Depending on used API the Connection-
	 * and Discovery-Thread starts (API >= 16 (= Android JellyBean)) with the
	 * default Session name: "PaM_Session" or only the Connection Thread starts
	 * and waits for user input (IP Adress)
	 * 
	 * @param e EnterScreen handover from Main Activity
	 */
	Client(EnterScreen e) {
		this.es = e;
		// API 16 (Jelly Bean) required for NSDManager
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			this.setmServiceName("PaM_Session");
			this.discoverThread = new Thread(new DiscoveryThread());
			this.discoverThread.start();
			Log.d(tagClient, "DiscoveryThread started");
			this.connectionThread = new Thread(new ConnectionThread());
			this.connectionThread.start();
			Log.d(tagClient, "ConnectionThread started");
			this.doNotifyOnDiscovery();
			es.showHideConNameSettings(true);

		} else {
			this.connectionThread = new Thread(new ConnectionThread());
			this.connectionThread.start();
			Log.d(tagClient, "ConnectionThread started");
			es.showHideIPSettings(true);
			es.showHideConNameSettings(false);
		}
	}

	/**
	 * Notify Discovery Thread to continue
	 */
	void doNotifyOnDiscovery() {
		synchronized (lockDiscovery) {
			es.setConnectionAvailable(false);
			lockDiscovery.notify();
			String TAG = "MUTEX Discovery";
			Log.d(TAG, "Wakeup ConnectionThread");
		}
	}

	
	/**
	 * 
	 * <h1>Connection Thread</h1>
	 * Establishes Thread for connection between Monitor and Controller (Socket
	 * and Server Socket). Holds incoming message, handover to Update Handler
	 * to create Event in Event-class (fromJSON();)
	 * 
	 */
	class ConnectionThread extends Thread {
		// Different debugging tags for LogCat
		private final String TAG = "ConnectionThread";

		// Message send from Controller
		private String incomeMessage = "";
		private boolean newMessage = false;

		// GETTER - SETTER

		/**
		 * @param incomeMessage
		 *            Set income message to a new message after read
		 */
		public void setIncomeMessage(String incomeMessage) {
			this.incomeMessage = incomeMessage;
		}

		/**
		 * @return incomeMessage if new message arrived
		 */
		public String getIncomeMessage() {
			return incomeMessage;
		}

		/**
		 * @return If new message arrives true else false
		 */
		public boolean isNewMessage() {
			return newMessage;
		}

		/**
		 * @param newMessage If a new message arrives set message
		 */
		public void setNewMessage(boolean newMessage) {
			this.newMessage = newMessage;
		}

		/**
		 * Locks Mutex "lockConnect" for Connection Thread waiting for Input of
		 * user or Discovery Thread
		 */
		private void doWaitOnConnect() {
			synchronized (lockConnect) {
				try {
					lockConnect.wait();
					Log.d(TAG,
							"ConnectionThread waiting for Input or DiscoveryThread");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		/*
		 * Creates socket connection to controller, reads incoming messages from
		 * controller
		 */
		public void run() {
			// Wait until Discovery Thread finds ControllerService on Network or
			// IP Adress was entered
			while (!Thread.currentThread().isInterrupted()) {
				doWaitOnConnect();

				try {
					while (!Thread.currentThread().isInterrupted()) {
						if (getSocket() == null) {
							setSocket(new Socket(controllerIP, controllerPort));
							Log.d(TAG, "Client connected");
							Log.d(TAG,
									"Connected to "
											+ controllerIP.getHostAddress()
											+ " on port " + controllerPort);
							es.setConnectionAvailable(true);
						} else {
							Log.d(TAG, "Socket already initialized. SKIPPED");
						}

						incomeMessage = in();
						Log.d(TAG, incomeMessage);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
					Log.e(TAG, "Unknown Host");
				}
			}
		}

		/**
		 * @return Message from Controller as JSONString
		 */
		private String in() {
			String inStr = "";
			StringBuilder completeString = new StringBuilder();
			try {
				BufferedReader inMessage = new BufferedReader(
						new InputStreamReader(mSocket.getInputStream()));

				while ((inStr = inMessage.readLine()) != null) {
					System.out.println(inStr);
					completeString.append(inStr);
					if (inStr.contains("}"))
						break;
				}
				if (completeString.length() != 0) {
					if (mms != null) {
						mms.newEvent(completeString.toString());
					}
				}
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return completeString.toString();
		}
	}

	
	
	/**
	 * 
	 * <h1>Discovery Thread</h1>
	 * Discovers service using implemented NSD Manager Uses: Service Type = HTTP
	 * over TCP, DNS Multicast and listener for Monitoring discovery
	 * 
	 */
	private @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	class DiscoveryThread extends Thread {

		// Server Client Connection using HTTP over TCP for JSON transmission
		private static final String SERVICE_TYPE = "_http._tcp.";

		// NSD Manager contains a Listener for discovery of Monitoring Service
		// in the Network
		private DiscoveryListener mDiscoveryListener;

		// NSD Manager contains a Listener for resolvement of Monitoring Service
		// in the Network
		private ResolveListener mResolveListener;
		private final String TAG = "DiscoveryThread";

		// Manager for Android Network Discovery on Monitor
		private NsdManager mNsdManager;

		// NSD Service Info for Monitoring service discovery and resolvement
		private NsdServiceInfo mService;

		/**
		 * Service discovery starts based on initialized Listener and service
		 * type http.tcp
		 */
		private void discoverServices() {
			mNsdManager.discoverServices(SERVICE_TYPE,
					NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
		}

		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {

					doWaitOnDiscovery();

					// Initialize NSD Manager using MainActivity Context
					mNsdManager = (NsdManager) EnterScreen.getAppContext()
							.getSystemService(Context.NSD_SERVICE);

					// Initialize Listener for Resolve Monitoring Service
					initializeResolveListener();
					Log.d(TAG, "Initializing of ResolveListener DONE");

					// Initialize Listener for Discovery Monitoring Service
					initializeDiscoveryListener();
					Log.d(TAG, "Initializing of DiscoveryListener DONE");

					// After initialization discover Monitoring service in the
					// network
					discoverServices();
				} catch (Exception e1) {
					e1.printStackTrace();
					Log.e(TAG, "No Host found");
				}
			}
		}

		/**
		 * Initialize Listener for Discovery of Monitoring Service
		 */
		private void initializeDiscoveryListener() {

			// Instantiate a new DiscoveryListener
			mDiscoveryListener = new NsdManager.DiscoveryListener() {

				// Called as soon as service discovery begins.
				@Override
				public void onDiscoveryStarted(String regType) {
					Log.d(TAG, "Service discovery started");
				}

				@Override
				public void onServiceFound(NsdServiceInfo service) {
					// Service was found
					Log.d(TAG, "Service discovery success" + service);
					if (!service.getServiceType().equals(SERVICE_TYPE)) {
						// Service type is the string containing the protocol
						// and
						// transport layer for this service.
						Log.d(TAG,
								"Unknown Service Type: "
										+ service.getServiceType());

					} else if (service.getServiceName().equals(mServiceName)) {
						// The name of the service was found in the network
						mNsdManager.resolveService(service, mResolveListener);
						Log.d(TAG, "Service found: " + mServiceName);
					}
				}

				@Override
				public void onServiceLost(NsdServiceInfo service) {
					// When the network service is no longer available.
					Log.e(TAG, "service lost" + service);
				}

				@Override
				public void onDiscoveryStopped(String serviceType) {
					Log.i(TAG, "Discovery stopped: " + serviceType);
				}

				// Stops Service Discovery if discovery fails at the start
				@Override
				public void onStartDiscoveryFailed(String serviceType,
						int errorCode) {
					Log.e(TAG, "Discovery failed: Error code:" + errorCode);
					mNsdManager.stopServiceDiscovery(this);
				}

				// Stops Service Discovery if discovery fails at the end
				@Override
				public void onStopDiscoveryFailed(String serviceType,
						int errorCode) {
					Log.e(TAG, "Discovery failed: Error code:" + errorCode);
					mNsdManager.stopServiceDiscovery(this);
				}
			};
		}

		/**
		 * Initialize Listener for Resolvement of Monitoring Service
		 */
		private void initializeResolveListener() {
			mResolveListener = new NsdManager.ResolveListener() {

				@Override
				public void onResolveFailed(NsdServiceInfo serviceInfo,
						int errorCode) {
					// Called when the resolve fails
					Log.e(TAG, "Resolve failed" + errorCode);
				}

				@Override
				public void onServiceResolved(NsdServiceInfo serviceInfo) {
					Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
					mService = serviceInfo;
					setHost(mService.getHost());
					Log.d(TAG, "Set Host");
					doNotifyOnConnect();
				}
			};
		}
	}

	/**
	 * Notify ConnectionThread to continue
	 */
	void doNotifyOnConnect() {
		synchronized (lockConnect) {
			es.setConnectionAvailable(false);
			try {
				getSocket().shutdownInput();
				getSocket().close();
			} catch (Exception e) {
				System.err.println(e);
			}
			mSocket = null;
			lockConnect.notify();
			String TAG = "Lock Connection";
			Log.d(TAG, "Wakeup ConnectionThread");
		}
	}

	/**
	 * DiscoveryThread waits for User Input
	 */
	private void doWaitOnDiscovery() {
		synchronized (lockDiscovery) {
			try {
				lockDiscovery.wait();
				String TAG = "Lock Discovery";
				Log.d(TAG, "DiscoveryThread waiting for Input of User");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
