package Main;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import KinectPV2.KinectPV2;
import KinectPV2.Skeleton;
import Util.ImageUtils;
import Util.MathUtils;
import processing.core.PApplet;
import processing.core.PImage;

public class Main extends PApplet {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	static JFrame imageFrame = new JFrame();
	static JLabel imageLabel = new JLabel();
	static JLabel fpsLabel = new JLabel();
	static JLabel foundLabel = new JLabel();
	
	static KinectPV2 kinect;
	static VideoCapture camera;
	
	static boolean tracked = false;
	static boolean retrack = false;
	static boolean useDefaultTracker = true;
	
	static ArrayList<listSkeleton> trackedSkeletons = new ArrayList<>();
	static int id;
	
	static int minTrackTime = 10000; // minimal track time in millis
	
	static long retrackStartTime;
	static long fps;
	
	static ArrayList<Mat> savedImages = new ArrayList<>();
	
	// Settings
	static String remoteAddress = "192.168.2.100";
	static int port = 1337;
	
	
	static int width = 640;
	static int height = 360;
	static Size imageSize = new Size(width, height);
	
	static Mat frame = new Mat();

	public static void main(String[] args) {

		Main t = new Main();
		t.setup();
		
		System.out.println("Bitte gerade vor dem Roboter platzieren.");
		long startTime = System.currentTimeMillis();

		while (true) {
			long a = System.currentTimeMillis();
			
			Skeleton[] skeletons = kinect.getSkeleton3d();
			
			if (useDefaultTracker) {
				if (!tracked) {
					if (!retrack) {
						for (int i = 0; i < skeletons.length; i++) {
							if (skeletons[i].isTracked()) {
								if (skeletons[i].getJoints()[0].getX() < 10 && skeletons[i].getJoints()[0].getX() > -10) {
									trackedSkeletons.add(new listSkeleton(skeletons[i], i));
								}	
							}
						}
					} else {
						for (int i = 0; i < skeletons.length; i++) {
							if (skeletons[i].isTracked()) {
								
								if (true) {//TODO: Replace true by Characteristics
									trackedSkeletons.add(new listSkeleton(skeletons[i], i));
								}
							}
						}
					}
					
					if (trackedSkeletons.size() == 0) {
						
					} else if (trackedSkeletons.size() == 1 ) {
						// TODO: Define characteristics to refind person (x = trackedSkeletons.get(0).skeleton.getJoints(); ...) (-> Körpergröße errechnen)
						id = trackedSkeletons.get(0).id;
						trackedSkeletons = new ArrayList<>();
						tracked = true;
						System.out.println("Tracked: ID=" + id);
						if (retrack) {
							retrack = false;
							startTime = startTime + System.currentTimeMillis() - retrackStartTime;
							System.out.println("Retracked: ID=" + id);
						}
					} else {
						System.out.println("Es stehen zu viele Personen vor dem Roboter!");
						trackedSkeletons = new ArrayList<>();
					}
					
				} else if (skeletons[id].isTracked()){
					double xPosition = (skeletons[id].getJoints()[0].getX() + skeletons[id].getJoints()[1].getX() + skeletons[id].getJoints()[20].getX()) / 3  * 100;
					double zPosition = (skeletons[id].getJoints()[0].getZ() + skeletons[id].getJoints()[1].getZ() + skeletons[id].getJoints()[20].getZ()) / 3  * 100;
					
					System.out.println("X-Position: " + xPosition + "; " + "Distanz: " + zPosition);
					ConnectionManager.send(MathUtils.getAngle(new Line2D.Double(0.0f, 0.0f, xPosition, zPosition), new Line2D.Double(0.0f, 0.0f, 0.0f, Float.MAX_VALUE)), zPosition);
					if (savedImages.size() < 20) {
						savedImages.add(ImageUtils.imageToMat(ImageUtils.resize((BufferedImage) kinect.getCoordinateRGBDepthImage().getImage(), width, height)));
					}
				} else {
					if (System.currentTimeMillis() - startTime < minTrackTime) {
						if (tracked && !retrack) {
							retrackStartTime = System.currentTimeMillis();
							tracked = false;
							retrack = true;
							System.out.println("Retracking...");
						}

					} else {
						useDefaultTracker = false;
						ObjectRecognizer.learn(savedImages.toArray(new Mat[20]));
						System.out.println("Switched to OpenCV-Tracker");
					}
				}
				BufferedImage colorImage = (BufferedImage) kinect.getCoordinateRGBDepthImage().getImage();
				updateImage(colorImage);
				
			} else {
				// TODO: Optimize || Add on fail -> Use other learned Images & Optimize Time
				BufferedImage colorImage = (BufferedImage) kinect.getColorImage().getImage();
				frame = ImageUtils.imageToMat(colorImage);
				Imgproc.resize(frame, frame, imageSize);
				frame = ObjectRecognizer.process(frame, 0);
				if (frame != null) {
					BufferedImage img = ImageUtils.matToImage(frame);
					updateImage(img);
					foundLabel.setText("Object found!");
					foundLabel.setForeground(new Color(0, 255, 0));
				} else {
					foundLabel.setText("No Object found!");
					foundLabel.setForeground(new Color(255, 0, 0));
				}
				

			}
			
			fps = Math.round((1000.0 / (System.currentTimeMillis() - a)));
			fpsLabel.setText(String.valueOf(fps));
			
			if (fps < 30) {
				fpsLabel.setForeground(new Color(255, 0, 0));
			} else {
				fpsLabel.setForeground(new Color(0, 255, 0));
			}
		}
	}

	public static void updateImage(BufferedImage i) {
		ImageIcon icon = new ImageIcon(i);
		imageLabel.setIcon(icon);
	}
	
	@Override
	public void setup() {
		kinect = new KinectPV2(this);

		kinect.enableBodyTrackImg(true);
		kinect.enableColorChannel(true);
		kinect.enableColorImg(true);
		kinect.enableCoordinateMapperRGBDepth(true);
		kinect.enableDepthImg(true);
		kinect.enableDepthMaskImg(true);
		kinect.enableInfraredImg(true);
		kinect.enablePointCloud(true);
		kinect.enableSkeleton(true);
		kinect.enableSkeleton3dMap(true);

		kinect.setHighThresholdPC(2000);
		kinect.setLowThresholdPC(0);

		kinect.init();

		sketchPath(System.getProperty("user.dir"));
		PImage img = loadImage(System.getProperty("user.dir") + "\\res\\background.png"); //TODO: Generate Image based on opposite of Background
		img.loadPixels();
		kinect.setCoordBkgImg(img.pixels);
		
		camera = new VideoCapture(0);
		camera.set(5, 20);

		imageFrame.setLayout(new FlowLayout());
		imageFrame.setTitle("Image");
		imageFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		imageFrame.setVisible(true);
		imageFrame.add(imageLabel);
		imageFrame.add(fpsLabel);
		imageFrame.add(foundLabel);
		imageFrame.setBounds(20, 20, 720, 960);
		
		ConnectionManager.connect(remoteAddress, port);
		ObjectRecognizer.init(0.6f, 4); // TODO: Perfekte Werte finden
		
		System.out.println("Set up.");
	}
	
	public static class listSkeleton {
		Skeleton skeleton;
		int id;
		
		listSkeleton(Skeleton s, int i) {
			this.id = i;
			this.skeleton = s;
		}
	}
	
}
