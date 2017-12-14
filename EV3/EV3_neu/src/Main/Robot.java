package Main;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import lejos.hardware.motor.Motor;
import lejos.hardware.motor.NXTRegulatedMotor;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.remote.nxt.BTConnector;
import lejos.remote.nxt.NXTConnection;

public class Robot {

	private boolean stop = false; //!
	private boolean streaming = false;

	private String BTMessage = "Nix";
	private String WlanMessage = "Nix";

	private boolean BTConnected = true; //!
	private boolean WlanConnected = false;

	private long driveSpeed = 0;
	private long controlSpeed = 600;

	private ServerSocket server;
	private Socket client;
	
	private Double xPosition = 0.0;
	private int zPosition;

	public Robot() {

		try {

			final NXTRegulatedMotor mA = Motor.A;
			final NXTRegulatedMotor mB = Motor.B;
			final NXTRegulatedMotor mC = Motor.C;
			final NXTRegulatedMotor mD = Motor.D;
			
			final EV3UltrasonicSensor sensor = new EV3UltrasonicSensor(SensorPort.S1);
			sensor.enable();
			UltrasonicSensor ultrasonicSensor = new UltrasonicSensor(sensor.getDistanceMode());
			
			mA.setSpeed(driveSpeed);
			mB.setSpeed(controlSpeed);
			mC.setSpeed(controlSpeed);
			mD.setSpeed(driveSpeed);
			
			bluetoothThread bT = new bluetoothThread("BT");
			wlanThread wT = new wlanThread("WLAN", 1337);
			bT.start();
			wT.start();
			
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				
				@Override
				public void run() {
					
					mB.rotateTo(0);
					mC.rotateTo(0);
					
					mA.close();
					mB.close();
					mC.close();
					mD.close();
					sensor.close();
				}
			}));

			while (true) {

				if (WlanConnected && BTConnected) {

					// gets messages
					WlanMessage = wT.Command;
					BTMessage = bT.Command;

					// checks if BT tells the robot to stop or to drive
					if (BTMessage.toLowerCase().equals("go")) {
						stop = false;
					} else if (BTMessage.toLowerCase().equals("stop")) {
						stop = true;
					}

					// gets data out of the Wlan-Message
					if (!WlanMessage.equals("Nix")) {
						xPosition = Double.valueOf(WlanMessage);
						streaming = true;
					}
					
					// gets distance to user by an ultrasonic-sensor
					zPosition = ultrasonicSensor.getDistance();
					
					if (zPosition > 0 /* TODO Change val */ && !stop && streaming) {
						
						if (xPosition > 10 || xPosition < -10) {
							int angle = (int) (((xPosition / 320) * (84.1 / 2)) * -1); // 84.1 => FOV; FOV / 2 -> FOV / Seite; angle *-1 => mirror-inverted
							mB.rotateTo(angle  * 8, true); // angle * 8 => gearbox
							mC.rotateTo(-angle * 8, false);
							System.out.println("Rotated to: " + angle);
						} else {
							mB.rotateTo(0, true);
							mC.rotateTo(0, false);
						}

						mA.backward();
						mD.backward();
					} else {
						System.out.println(zPosition);
						System.out.println(streaming);
						System.err.println(stop);
						mA.stop();
						mD.stop();
					}

				} else {
					System.out.println("BT-State: " + BTConnected + ", Wlan-State; " + WlanConnected);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class bluetoothThread extends Thread {

		String name;
		String Command = "Nix";

		bluetoothThread(String name) {
			this.name = name;
		}

		@Override
		public void run() {

			System.out.println("Thread " + name + " gestartet");

			BTConnector connector = new BTConnector();

			System.out.println("Auf B-Signal warten");

			NXTConnection conn = connector.waitForConnection(0, NXTConnection.RAW);
			InputStream is = conn.openInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is), 1);

			String message = "";

			BTConnected = true;

			System.out.println("B verbunden");

			while (true) {

				message = "";
				try {
					message = br.readLine();
					if (message != null && !message.equals("Nix")) {
						this.Command = message;
					}
				} catch (IOException e) {
					e.printStackTrace(System.out);
				}
			}
		}
	}

	public class wlanThread extends Thread {

		String name;
		String Command = "Nix";
		int port;

		wlanThread(String name, int port) {
			this.name = name;
			this.port = port;
		}

		@Override
		public void run() {

			try {
				server = new ServerSocket(port);

				System.out.println("Auf W-Signal warten");

				client = server.accept();

				DataInputStream stream = new DataInputStream(client.getInputStream());

				String message = "";

				WlanConnected = true;

				System.out.println("W verbunden");

				while (true) {

					message = "";
					message = stream.readUTF();
					if (message != null && !message.equals("Nix")) {
						this.Command = message;
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		new Robot();
	}

}