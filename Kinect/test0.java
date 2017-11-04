package tests;

import java.awt.geom.Line2D;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import edu.ufl.digitalworlds.j4k.J4KSDK;
import edu.ufl.digitalworlds.j4k.Skeleton;

public class test0 {

	// Bodyparts
	static int head = Skeleton.HEAD;
	static int neck = Skeleton.NECK;
	static int vortex_top = Skeleton.SPINE_SHOULDER;
	static int vortex_mid = Skeleton.SPINE_MID;
	static int vortex_bot = Skeleton.SPINE_BASE;
	static int elbow_left = Skeleton.ELBOW_LEFT;
	static int elbow_right = Skeleton.ELBOW_RIGHT;
	static int hand_left = Skeleton.HAND_LEFT;
	static int hand_right = Skeleton.HAND_RIGHT;
	static int knee_left = Skeleton.KNEE_LEFT;
	static int knee_right = Skeleton.KNEE_RIGHT;
	static int foot_left = Skeleton.FOOT_LEFT;
	static int foot_right = Skeleton.FOOT_RIGHT;
	
	// 3D-Koordinate-Arrays
	static float[] head_position = new float[3];
	static float[] neck_position = new float[3];
	static float[] vortex_top_position = new float[3];
	static float[] vortex_mid_position = new float[3];
	static float[] vortex_bot_position = new float[3];
	static float[] elbow_left_position = new float[3];
	static float[] elbow_right_position = new float[3];
	static float[] hand_left_position = new float[3];
	static float[] hand_right_position = new float[3];
	static float[] knee_left_position = new float[3];
	static float[] knee_right_position = new float[3];
	static float[] foot_left_position = new float[3];
	static float[] foot_right_position = new float[3];

	// Values
	static float[] position = new float[3];
	static double angle;
	static double distance;
	
	static int id = -1;
	static boolean scan = false;
	static boolean found = false;
	static boolean newScan = false;
	
	static Skeleton s = null;
	
	//Indentificators
	static float headHeight = -1;
	
	static Socket client;
	static DataOutputStream os;
	
	//Settings
	static boolean showImage = true;

	public static void main(String[] args) {

		try {

			J4KSDK j = new J4KSDK() {

				@Override
				public void onSkeletonFrameEvent (boolean[] skeleton_tracked, float[] joint_positions, float[] joint_orientations, byte[] joint_tracked) {
					
					//finding the right Skeleton
					if (scan && !found) {
						for (int i = 0; i < skeleton_tracked.length; i++) {
							if (skeleton_tracked[i]) {
								Skeleton s1 = Skeleton.getSkeleton(i, skeleton_tracked, joint_positions, joint_orientations, joint_tracked, this);
								if (!newScan) {
									if (s1.get2DJoint(head, 1920, 1080)[0] < 1200 && s1.get2DJoint(head, 1920, 1080)[0] > 800) {
										System.out.println(i);
										id = i;
										found = true;
										s = s1;
										headHeight = s1.get3DJointY(head) * 100;
									} else {
										System.out.println(s1.get2DJoint(head, 1920, 1080)[0]);
									}
								} else if(s1.get3DJointY(head) * 100 > headHeight - 15 && s1.get3DJointY(head) * 100 < headHeight + 15) {
									System.out.println(i + "Head: " + s1.get3DJointY(head) * 100);
									id = i;
									found = true;
									s = s1;
								} else {
									System.out.println(s1.get3DJointY(head) * 100 +  " != " + headHeight);
								}

							}
						}
					} else {
					
						s = Skeleton.getSkeleton(id, skeleton_tracked, joint_positions, joint_orientations, joint_tracked, this);
						
						if (s.isTracked()) {
							
							head_position[0] = s.get3DJointX(head);
							head_position[1] = s.get3DJointY(head);
							head_position[2] = s.get3DJointZ(head);
	
							neck_position[0] = s.get3DJointX(neck);
							neck_position[1] = s.get3DJointY(neck);
							neck_position[2] = s.get3DJointZ(neck);
	
							vortex_top_position[0] = s.get3DJointX(vortex_top);
							vortex_top_position[1] = s.get3DJointY(vortex_top);
							vortex_top_position[2] = s.get3DJointZ(vortex_top);
	
							vortex_mid_position[0] = s.get3DJointX(vortex_mid);
							vortex_mid_position[1] = s.get3DJointY(vortex_mid);
							vortex_mid_position[2] = s.get3DJointZ(vortex_mid);
	
							vortex_bot_position[0] = s.get3DJointX(vortex_bot);
							vortex_bot_position[1] = s.get3DJointY(vortex_bot);
							vortex_bot_position[2] = s.get3DJointZ(vortex_bot);
	
							elbow_left_position[0] = s.get3DJointX(elbow_left);
							elbow_left_position[1] = s.get3DJointY(elbow_left);
							elbow_left_position[2] = s.get3DJointZ(elbow_left);
	
							elbow_right_position[0] = s.get3DJointX(elbow_right);
							elbow_right_position[1] = s.get3DJointY(elbow_right);
							elbow_right_position[2] = s.get3DJointZ(elbow_right);
	
							hand_left_position[0] = s.get3DJointX(hand_left);
							hand_left_position[1] = s.get3DJointY(hand_left);
							hand_left_position[2] = s.get3DJointZ(hand_left);
	
							hand_right_position[0] = s.get3DJointX(hand_right);
							hand_right_position[1] = s.get3DJointY(hand_right);
							hand_right_position[2] = s.get3DJointZ(hand_right);
	
							knee_left_position[0] = s.get3DJointX(knee_left);
							knee_left_position[1] = s.get3DJointY(knee_left);
							knee_left_position[2] = s.get3DJointZ(knee_left);
	
							knee_right_position[0] = s.get3DJointX(knee_right);
							knee_right_position[1] = s.get3DJointY(knee_right);
							knee_right_position[2] = s.get3DJointZ(knee_right);
	
							foot_left_position[0] = s.get3DJointX(foot_left);
							foot_left_position[1] = s.get3DJointY(foot_left);
							foot_left_position[2] = s.get3DJointZ(foot_left);
	
							foot_right_position[0] = s.get3DJointX(foot_right);
							foot_right_position[1] = s.get3DJointY(foot_right);
							foot_right_position[2] = s.get3DJointZ(foot_right);
							
							position[0] = vortex_top_position[0] + vortex_mid_position[0] + vortex_bot_position[0] / 3;
							position[1] = vortex_top_position[1] + vortex_mid_position[1] + vortex_bot_position[1] / 3;
							position[2] = vortex_top_position[2] + vortex_mid_position[2] + vortex_bot_position[2] / 3;

							Line2D line = new Line2D.Float(0.0f, 0.0f, position[0] * 100, position[2] * 100);
							Line2D xAchse = new Line2D.Float(0.0f, 0.0f, 0.0f, Float.MAX_VALUE);

							angle = getAngle(line, xAchse) * 100;
							distance = position[2] * (100 / 3);

							angle = (int) Math.round(angle);
							distance = (int) Math.round(distance);

							System.out.println("Winkel: " + angle + "° " + "Distanz: " + distance + "cm");
							
							sendToWlan(angle, distance);
							
							
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
						} else {
							found = false;
							newScan = true;
							scan = true;
						}

					}
				}

				@Override
				public void onDepthFrameEvent(short[] arg0, byte[] arg1, float[] arg2, float[] arg3) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onColorFrameEvent(byte[] arg0) {
					// TODO Auto-generated method stub

				}
			};
			
			connect("127.0.0.1", 1337);
//			connect("192.168.2.108", 1337);

			j.start(J4KSDK.COLOR | J4KSDK.DEPTH | J4KSDK.SKELETON);
			
			System.out.println("Bitte gerade vor dem Roboter platzieren.");

			Thread.sleep(2000);
			
			scan = true;
			
			while (!found) {
				Thread.sleep(0);
			}
			
			System.out.println("Found @" + id);
			
			if (showImage) {
				j.showViewerDialog();
			}

			while (true) {
				Thread.sleep(Integer.MAX_VALUE);
			}
			
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static double getAngle(Line2D line1, Line2D line2) {
		double angle1 = Math.atan2(line1.getY1() - line1.getY2(), line1.getX1() - line1.getX2());
		double angle2 = Math.atan2(line2.getY1() - line2.getY2(), line2.getX1() - line2.getX2());
		return angle1 - angle2;
	}
	
	public static void connect(String address, int port) {
		try {
			client = new Socket(address, port);
			os = new DataOutputStream(client.getOutputStream());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendToWlan(double angl, double dist) {
		int i;
		try {
			String s = "#" + String.valueOf(Math.round(angl)) + "," + String.valueOf(Math.round(dist));

			for (i = s.length(); i < 20; i++) {
				s = s + "y";
			}
			os.writeChars(s);
			os.flush();
			System.out.println(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
