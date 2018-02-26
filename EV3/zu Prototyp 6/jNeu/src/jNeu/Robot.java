package jNeu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import lejos.hardware.motor.Motor;
import lejos.hardware.motor.NXTRegulatedMotor;
import lejos.remote.nxt.BTConnector;
import lejos.remote.nxt.NXTConnection;

public class Robot {
	
	// varaibles
	public static boolean bluetoothConnected = false;
	public static boolean wlanConnected = false;
	
	public static int zPosition = 0;
	public static int xPosition = 0;
	public static boolean stop = true;
	
	private int controlSpeed = 600;
	
	// constants
	private final NXTRegulatedMotor motorA = Motor.A;
	private final NXTRegulatedMotor motorB = Motor.B;
	private final NXTRegulatedMotor motorC = Motor.C;
	private final NXTRegulatedMotor motorD = Motor.D;
	
	Robot() {
		// starts the motors
		motorB.setSpeed(controlSpeed);
		motorC.setSpeed(controlSpeed);
		
		// starts wlan- and bluetooth-thread
		new wlanThread(1337).start();
		new bluetoothThread().start();
		
		// loop
		while (true) {
			// checks if laptop and smartphone are connected
			if (wlanConnected && bluetoothConnected) {
				// checks if the person is far enough away and the robot is not told to stop
				if (zPosition > 300 && !stop) {
					// apply new speed
					motorA.setSpeed(zPosition);
					motorD.setSpeed(zPosition);
						
					// if person is not in front of robot: calculate angle and rotate to
					double angle = ((double) xPosition / 960d) * (84.1 / 2); // 84.1 => FOV; FOV / 2 -> FOV / Seite; angle *-1 => mirror-inverted
					motorB.rotateTo((int) -angle  * 8, true); // angle * 8 => gearbox
					motorC.rotateTo((int) angle * 8, false);
					System.out.println("Rotated to: " + angle + " (xPosition = " + xPosition + ")");

					if (!motorA.isMoving() || !motorD.isMoving()) {
						motorA.backward();
						motorD.backward();
					}
				} else {
					// robot is told to stop or person is to close
					System.out.println(zPosition);
					System.err.println(stop);
					motorA.setSpeed(0);
					motorD.setSpeed(0);
					motorA.stop();
					motorD.stop();
				}
			} else {
				// smartphone or laptop are disconnected
				System.out.println("Waiting for connection: wlan-state: " + wlanConnected + ", bluetooth-state: " + bluetoothConnected);
			}
		}
	}
	
	// continuously checks for new data over wlan
	class wlanThread extends Thread {
		
		private int port;
		private ServerSocket serverSocket;
		private Socket client;
		private BufferedReader reader;
		
		wlanThread(int port) {
			this.port = port;
		}
		
		@Override
		public void run() {
			try {
				System.out.println("wlanThread started.");
	            serverSocket = new ServerSocket(port);
	            client = serverSocket.accept();
	            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
				wlanConnected = true;
				System.out.println("Laptop connected.");
	            while (true) {
	                char[] buffer = new char[200];
	                int charCount = reader.read(buffer, 0, 200);
	                String message = new String(buffer, 0, charCount);
	                if (message.length() < 10) {
		                int seperatorIndex = message.indexOf("#");
		                xPosition = Integer.valueOf(message.substring(0, seperatorIndex));
		                zPosition = Integer.valueOf(message.substring(seperatorIndex + 1));
	                }
	            }
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
	}
	
	// continuously checks for new data over bluetooth
	class bluetoothThread extends Thread {
		
		private BTConnector connector;
		private NXTConnection connection;
		private InputStream stream;
		private BufferedReader reader;

		@Override
		public void run() {
			try {
				System.out.println("bluetoothThread started.");
				connector = new BTConnector();
				connection = connector.waitForConnection(0, NXTConnection.RAW);
				stream = connection.openInputStream();
				reader = new BufferedReader(new InputStreamReader(stream), 1);
				bluetoothConnected = true;
				System.out.println("Smartphone connected.");
		
				while (true) {
					String message = reader.readLine();
					if (message != null && !message.equals("")) {
						if (message.toLowerCase().equals("go")) {
							stop = false;
						} else if ((message.toLowerCase().equals("stop"))) {
							stop = true;
						} else {
							System.out.println("Received uncommon message: " + message.toLowerCase());
						}
					}
		
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
}