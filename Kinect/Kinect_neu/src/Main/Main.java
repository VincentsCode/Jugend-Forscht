package Main;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import KinectPV2.KinectPV2;
import KinectPV2.Skeleton;
import Util.ImageUtils;
import processing.core.PApplet;
import processing.core.PImage;

public class Main extends PApplet {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	// Settings
	private static String remoteAddress = "192.168.2.100";
	private static int port = 1337;
	private static int imageWidth = 640;
	private static int imageHeight = 360;
	
	// Static variables
	private static KinectPV2 kinect;
	private static JFrame imageFrame = new JFrame();
	private static JLabel imageLabel = new JLabel();
	private static JLabel colorLabel = new JLabel();
	private static JLabel foundLabel = new JLabel();
	private static Mat frame = new Mat();
	private static Mat frontImage;
	private static Mat rightImage;
	private static Mat leftImage;
	private static Mat backImage;
	

	private static Mat getImage() throws InterruptedException {

		while (true) {

			Skeleton[] skeletons = kinect.getSkeleton3d();
			ArrayList<ListSkeleton> trackedSkeletons = new ArrayList<>();

			for (int i = 0; i < skeletons.length; i++) {
				if (skeletons[i].isTracked()) {
					if (skeletons[i].getJoints()[0].getX() < 10 && skeletons[i].getJoints()[0].getX() > -10) {
						trackedSkeletons.add(new ListSkeleton(skeletons[i], i));
					}
				}
			}

			if (trackedSkeletons.size() == 0) {
				System.err.println("Bitte platzieren sie sich vor dem Roboter!");
				Thread.sleep(1000);
			} else if (trackedSkeletons.size() == 1) {
				return ImageUtils.imageToMat(ImageUtils
						.resize((BufferedImage) kinect.getCoordinateRGBDepthImage().getImage(), imageWidth, imageHeight));
			} else {
				System.err.println("Es stehen zu viele Personen vor dem Roboter!");
				Thread.sleep(1000);
			}
		}

	}

	public static void main(String[] args) {

		Main main = new Main();
		main.setup();

		try {
			// TODO Replace "System.out.println()" by sending a message to the App ( -> TTS )
			System.out.println("Bitte stellen sie sich gerade vor den Roboter.");
			Thread.sleep(5000);
			frontImage = getImage();

			System.out.println("Bitte mit der Rechten Seite zum Roboter vor dem Roboter platzieren.");
			Thread.sleep(3000);
			rightImage = getImage();

			System.out.println("Bitte mit der Linken Seite zum Roboter vor dem Roboter platzieren.");
			Thread.sleep(3000);
			leftImage = getImage();

			System.out.println("Bitte stellen sie sich mit dem Rücken zum Roboter.");
			Thread.sleep(3000);
			backImage = getImage();

			System.out.println("Sie können nun loslaufen.");
			CustomRecognizer.learn(new Mat[] { backImage, rightImage, leftImage, frontImage });
			imageFrame.toFront();

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		while (true) {

			BufferedImage colorImage = (BufferedImage) kinect.getColorImage().getImage();
			frame = ImageUtils.imageToMat(colorImage);

			// TODO: Get rid of Background (a.e. Convert Image to 512x424 -> Map to Depth)

			Imgproc.resize(frame, frame, new Size(imageWidth, imageHeight));
			frame = CustomRecognizer.process(frame);
			if (frame != null) {
				BufferedImage img = ImageUtils.matToImage(frame);
				updateImage(img);

				// TODO: get X-Koord
				// TODO: Get Depth Data for Position of User -> Distance -> + X-Koord -> Angle

				foundLabel.setText("Object found!");
				foundLabel.setForeground(new Color(0, 255, 0));
			} else {
				foundLabel.setText("No Object found!");
				foundLabel.setForeground(new Color(255, 0, 0));
			}
			updateColor(ImageUtils.resize(colorImage, 640, 360));

			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	public static void updateImage(BufferedImage i) {
		ImageIcon icon = new ImageIcon(i);
		imageLabel.setIcon(icon);
	}

	public static void updateColor(BufferedImage i) {
		ImageIcon icon = new ImageIcon(i);
		colorLabel.setIcon(icon);
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
		kinect.activateRawDepth(true);
		kinect.activateRawColor(true);
		kinect.enableLongExposureInfraredImg(true);

		kinect.setHighThresholdPC(1000);
		kinect.setLowThresholdPC(0);

		kinect.init();

		sketchPath(System.getProperty("user.dir"));
		PImage img = loadImage(System.getProperty("user.dir") + "\\res\\pink.png");
		img.loadPixels();
		kinect.setCoordBkgImg(img.pixels);

		imageFrame.setLayout(new FlowLayout());
		imageFrame.setTitle("Image");
		imageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		imageFrame.setVisible(true);
		imageFrame.add(imageLabel);
		imageFrame.add(colorLabel);
		imageFrame.add(foundLabel);
		imageFrame.setBounds(0, 10, 1100, 1000);

		ConnectionManager.connect(remoteAddress, port);
		CustomRecognizer.init(0.7f, 10);

		System.out.println("Setup finished.");
	}

	public static class ListSkeleton {
		Skeleton skeleton;
		int id;

		ListSkeleton(Skeleton s, int i) {
			this.id = i;
			this.skeleton = s;
		}
	}
}
