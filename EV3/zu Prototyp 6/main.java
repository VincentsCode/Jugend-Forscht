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
	
	public static boolean bluetoothConnected = false;
	public static boolean wlanConnected = false;
	
	public static int zPosition = 0;
	public static int xPosition = 0;
	public static boolean stop = true;
	
	private int driveSpeed = 0;
	private int controlSpeed = 600;
	
	private final NXTRegulatedMotor motorA = Motor.A;
	private final NXTRegulatedMotor motorB = Motor.B;
	private final NXTRegulatedMotor motorC = Motor.C;
	private final NXTRegulatedMotor motorD = Motor.D;
	
	Robot() {
		motorA.setSpeed(driveSpeed);
		motorB.setSpeed(controlSpeed);
		motorC.setSpeed(controlSpeed);
		motorD.setSpeed(driveSpeed);
		
		new wlanThread(1337).start();
		new bluetoothThread().start();
		
		while (true) {
			if (wlanConnected && bluetoothConnected) {
				if (zPosition > 300 && !stop) {
					
					if (xPosition > 30 || xPosition < -30) {
						double d1 = (double) xPosition / 960d;
						System.out.println("D1: " + d1);
						double d2 = 84.1 / 2; // 84.1 => FOV of the Sensor; FOV / 2 -> FOV / Seite;
						System.out.println("D2: " + d2);
						double angle = d1 * d2;
						motorB.rotateTo((int) -angle  * 8, true); // angle * 8 => gearbox
						motorC.rotateTo((int) angle * 8, false);
						System.out.println("Rotated to: " + angle + " (xPosition = " + xPosition + ")");
					} else {
						motorB.rotateTo(0, true);
						motorC.rotateTo(0, false);
					}

					motorA.backward();
					motorD.backward();
				} else {
					System.out.println(zPosition);
					System.err.println(stop);
					motorA.stop();
					motorD.stop();
				}
			}
		}
	}
	
	class wlanThread extends Thread {
		
		private int port;
		
		wlanThread(int port) {
			this.port = port;
		}
		
		@Override
		public void run() {
			try {
				System.out.println("wlanThread started.");
	            ServerSocket serverSocket = new ServerSocket(port);
	            Socket client = serverSocket.accept();
	            BufferedReader reader =  new BufferedReader(new InputStreamReader(client.getInputStream()));
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

public static void main(String[] args) {
	new Robot();
}