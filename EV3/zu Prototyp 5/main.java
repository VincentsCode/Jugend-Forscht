package EV3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import ch.aplu.ev3.IRDistanceSensor;
import lejos.hardware.Audio;
import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.motor.Motor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.remote.ev3.RMIRegulatedMotor;
import lejos.remote.ev3.RemoteEV3;
import lejos.remote.nxt.BTConnector;
import lejos.remote.nxt.NXTConnection;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;

public class main {
	
	static Brick localEV3;
	static RemoteEV3 remoteEV3;
	
	
	static Wheel frontWheelRight;
	static Wheel frontWheelLeft;
	static Wheel backWheelRight;
	static Wheel backWheelLeft;
	
	static Chassis chassis;
	
	static MovePilot pilot;
	
	static RMIRegulatedMotor rMotorA;
	static RMIRegulatedMotor rMotorB;
	static RMIRegulatedMotor rMotorC;
	static RMIRegulatedMotor rMotorD;
	
	static Port lPort1;
	static Port lPort2;
	static Port lPort3;
	static Port lPort4;
	
	static Port rPort1;
	static Port rPort2;
	static Port rPort3;
	static Port rPort4;
	
	static UltrasonicSensor ultrasonicSensorDown;
	static EV3UltrasonicSensor USSDown;
	
	static UltrasonicSensor ultrasonicSensorLeft;
	static EV3UltrasonicSensor USSLeft;
	
	static UltrasonicSensor ultrasonicSensorRight;
	static EV3UltrasonicSensor USSRight;
	
	static IRSeekSensor IRSLeft;
	static IRSeekSensor IRSRight;
	
	Waypoint[] usedPoints = {};
	Waypoint[] ownerPositions = {};
	Waypoint[] wände = {};
	Waypoint[] treppen = {};
	
	long drehung;
	
	Waypoint currentPosition = new Waypoint(0, 0, 0);

	String message = "Nix";
	boolean Stop = false;
	
	int leftDistance;
	int rightDistance;
	int downDistance;
	
	int IRDistanceLeft;
	int IRDistanceRight;

	int Speed = 100;
	
	static double xA = -18; //Abstand IRSensor A zur Mitte
	static double xB = 18;	//Abstand IRSensor B zur Mitte
	
    public main() {
		
    	try {    		
    		//EV3s verbinden
	        localEV3 = BrickFinder.getLocal();
			remoteEV3 = new RemoteEV3(BrickFinder.find("jufo")[0].getIPAddress());
			
			Audio localAudio = localEV3.getAudio();
			
			//Alle Ports schließen
			
			//Motoren registrieren
			frontWheelRight = WheeledChassis.modelWheel(Motor.D, 62.4).offset(-11.5); // 62.4 = durchmesser der Reifen
			frontWheelLeft = WheeledChassis.modelWheel(Motor.A, 62.4).offset(11.5);   // 11.5 & -11.5 = Abstand zu Mittelpunkt des Roboters auf der Z-Achse
			backWheelRight = WheeledChassis.modelWheel(Motor.C, 62.4).offset(-11.5);
			backWheelLeft = WheeledChassis.modelWheel(Motor.B, 62.4).offset(11.5);
			
			chassis = new WheeledChassis(new Wheel[]{frontWheelRight, frontWheelLeft, backWheelRight, backWheelLeft}, WheeledChassis.TYPE_DIFFERENTIAL);
			pilot = new MovePilot(chassis);
			
//	        rMotorA = remoteEV3.createRegulatedMotor("A", 'L'); TODO Pneumatik Schalter
//	        rMotorB = remoteEV3.createRegulatedMotor("B", 'L'); TODO Pneumatik Pumpe
//	        rMotorC = remoteEV3.createRegulatedMotor("C", 'L');
//			rMotorD = remoteEV3.createRegulatedMotor("D", 'L');
			
			//Ports registrieren
	        lPort1 = localEV3.getPort("S1");
	        lPort2 = localEV3.getPort("S2");
	        lPort3 = localEV3.getPort("S3");
	        lPort4 = localEV3.getPort("S4");
			
			rPort1 = remoteEV3.getPort("S1");
			rPort2 = remoteEV3.getPort("S2");
			rPort3 = remoteEV3.getPort("S3");
			rPort4 = remoteEV3.getPort("S4");
			
			//Sensoren registrieren
			IRSLeft = new IRSeekSensor(1);
			
			//TODO Button lPort 2 (Klappe)
			
			IRSRight = new IRSeekSensor(4);
			
    		USSLeft = new EV3UltrasonicSensor(rPort1);
    		ultrasonicSensorLeft = new UltrasonicSensor(USSLeft.getMode("Distance"));
    		
            USSDown = new EV3UltrasonicSensor(rPort2);
    		ultrasonicSensorDown = new UltrasonicSensor(USSDown.getMode("Distance"));
    		
    		USSRight = new EV3UltrasonicSensor(rPort4);
    		ultrasonicSensorRight = new UltrasonicSensor(USSRight.getMode("Distance"));
    		
    		USSLeft.enable();
    		USSDown.enable();
    		USSRight.enable();
    		
			//Programm
    		messageThread mT = new messageThread("messageThread");
    		mT.start();
    		
			pilot.setAngularAcceleration(1000);
			pilot.setLinearAcceleration(1000);
			
			Navigator navigator = new Navigator(pilot);
    		
    		while(true) {
    			
    			message = mT.Command;
    			
    			if(message.equals("Weiter")) {
    				Stop = false;
    				localAudio.playTone(1000, 1);
    				System.out.println("Roboter fährt weiter!");
    			} else if(message.equals("Stop")) {
    				Stop = true;
    				localAudio.playTone(1000, 1);
    				System.out.println("Roboter stoppt!");
    			} else if(!message.equals("Nix")){
    				System.out.println(message);
    			}
    			
    			if(!Stop) {
    				downDistance = (int) ultrasonicSensorDown.getDistance();
    				rightDistance = (int) ultrasonicSensorRight.getDistance();
    				leftDistance = (int) ultrasonicSensorLeft.getDistance();
    				
    				IRDistanceLeft = IRSLeft.getDistance();
    				IRDistanceRight = IRSRight.getDistance();
    				
    				if (downDistance < 10) {
    					if (leftDistance > 30) {
    						if (rightDistance > 30) {
    							if (IRDistanceLeft != -2 && IRDistanceRight != -2) {
    								//Main Programm
    								double[] intersection = getIntersection(IRDistanceLeft, IRDistanceRight);
    								Waypoint currentOwnerPosition = new Waypoint(intersection[0], intersection[1]);
    								if (ownerPositions[ownerPositions.length -1] != currentOwnerPosition &&
    										ownerPositions[ownerPositions.length -1].getX() - currentOwnerPosition.getX() > 20 ||
    										ownerPositions[ownerPositions.length -1].getX() - currentOwnerPosition.getX() < -20 ||
    										ownerPositions[ownerPositions.length -1].getY() - currentOwnerPosition.getY() > 20 ||
    										ownerPositions[ownerPositions.length -1].getY() - currentOwnerPosition.getY() < -20) {
    									ownerPositions[ownerPositions.length] = currentOwnerPosition;
    								}
    								
    								if(ownerPositions.length + 1 < usedPoints.length) {
    									navigator.addWaypoint(ownerPositions[ownerPositions.length -2]);
    									navigator.followPath();
    								} else if (navigator.isMoving()) {
    									//Alles Ok
    								} else if (!navigator.isMoving()) {
    									Path currentPath = navigator.getPath();
    									if (currentPath.size() != 0) {
    										navigator.followPath();
    									}
    								}
    								
    								
    							} else {
    								//Keine Fernbedienung || Nur 1 Sensor erkennt Fernbedienung
    								if (IRDistanceLeft == -2 && IRDistanceRight == -2) {
    									//Keine Fernbedienung -> Stoppen
    									if (!navigator.isMoving()) {
        									Path currentPath = navigator.getPath();
        									if (currentPath.size() == 0) {
        										localAudio.playTone(12, 10);
        									}
    									}
    								} else if (IRDistanceLeft == -2 && IRDistanceRight != -2) {
    									//Nur Rechts Fernbedienung -> In Richtung Bearing drehen
    									navigator.stop();
    									pilot.rotate(IRSRight.getBearing() * 7.5, false);
    								} else if (IRDistanceRight == -2 && IRDistanceLeft != -2) {
    									//Nur Links Fernbedienung -> In Richtung Bearing drehen
    									navigator.stop();
    									pilot.rotate(IRSLeft.getBearing() * 7.5, false);
    								}
    							}
    						} else {
    							//Wand rechts -> Nach Links drehen + In Liste eintragen
    						}
    					} else {
    						//Wand links -> Nach Rechts drehen + In Liste eintragen
    					}
    				} else {
						//Treppe || "Abgrund" -> Stoppen + In Liste eintragen
    					navigator.stop();
						Path currentPath = navigator.getPath();
						treppen[treppen.length] = navigator.getWaypoint();
						if (currentPath.size() > 1) {
							Waypoint lastElement = currentPath.get(currentPath.size());
							currentPath.clear();
							currentPath.add(lastElement);
							navigator.followPath(currentPath);
						} else {
							localAudio.playTone(12, 10);
						}
					}
    				
    			}
    			
    			else {
    				if(pilot.isMoving()) {
    					pilot.stop();
    				}
    			}
    			
    			
    			
    		}

		} catch (RemoteException | MalformedURLException | NotBoundException e) {
			e.printStackTrace();
		} 
    }
    
    public void close() {
		//Motoren und Sensoren schließen		
		try {
			rMotorA.close();
			rMotorB.close();
			rMotorC.close();
			rMotorD.close();
			
			USSLeft.close();
			USSDown.close();
			USSRight.close();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    }
    
	public void backToInitiator() {
		//Gesamten Weg zurück fahren
		if(pilot.isMoving()) {
			pilot.stop();
		}
		Navigator navi = new Navigator(pilot);
		
		for(int i = 0; i < usedPoints.length; i++) {
			navi.addWaypoint(usedPoints[i]);
		}
		navi.followPath();
	}
	
	public class messageThread extends Thread {
		  
		  String name;
		  String Command = "Nix";
		  
		  messageThread(String name) {
			  this.name = name;
		  }
		  
		  @Override
		  public void run(){
			  
			  System.out.println("Thread " + name + " gestartet");
			  
			  BTConnector connector = new BTConnector();
			  
			  System.out.println("0. Auf Signal warten");
			  
			  NXTConnection conn = connector.waitForConnection(0, NXTConnection.RAW);
			  InputStream is = conn.openInputStream();
			  BufferedReader br = new BufferedReader(new InputStreamReader(is), 1);
			  
			  String message = "";
			  
			  while (true){
				  
				  message = "";	
				  try {
					  message = br.readLine();
					  if (message != null && !message.equals("Nix")) {
						  this.Command = message;
						  System.out.println("2. Message: " + message);
					  }
				  } catch (IOException e) {
					  e.printStackTrace(System.out);
				  }
				  
			  } 
		  }
	}
	
	
	public double[] getIntersection(double radiusA, double radiusB) {
		double[] aA = {xA, 0};
		double[] bB = {xB, 0};
		
		double[] results = Intersect2Circles(aA, radiusA, bB, radiusB);
		
		for(int i = 0; i < results.length; i ++) {
			System.out.println(results[i]);
		}
		
		return results;
		
	}
		
	public static double[] Intersect2Circles(double[] A, double a, double[] B, double b) {
		
		double[] res0 = {};
		
		double AB0 = B[0] - A[0];
		double AB1 = B[1] - A[1];
		
		double c = Math.sqrt(AB0 * AB0 + AB1 * AB1);
		
		if(c == 0) {
			return res0;
		}
		
		double x = (a*a + c*c - b*b) / (2*c);
		double y = a*a - x*x;
		
		if(y < 0) {
			return res0;
		}
		
		if(y > 0) {
			y = Math.sqrt(y);
		}
		
		double ex0 = AB0 / c;
		double ex1 = AB1 / c;
		double ey0 = -ex1;
		double ey1 = ex0;
		double Q1x = A[0] + x * ex0;
		double Q1y = A[1] + x * ex1;
		
		if(y == 0) {
			double[] res1 = {Q1x, Q1y};
			return res1;
		}
		
		Q1x += y * ey0;
		Q1y += y * ey1;
			
		double[] res2 = {Q1x, Q1y};
			
		return res2;
	}

	
    public static void main(String[] args) {
    	new main();
    }
    
}