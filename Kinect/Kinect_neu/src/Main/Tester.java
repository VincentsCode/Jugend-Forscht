package Main;

import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import KinectPV2.KinectPV2;
import Util.ImageUtils;
import processing.core.PApplet;
import processing.core.PImage;

public class Tester extends PApplet {

	private static KinectPV2 kinect;

	private static PImage depthToColorImg;

	private static JFrame imageFrame = new JFrame();
	private static JLabel imageLabel = new JLabel();

	private static Tester tester = new Tester();
	static PrintWriter writer;

	public static void main(String[] args) {
		
		// Setup
		kinect = new KinectPV2(tester);
		kinect.enableBodyTrackImg(true);
		kinect.enableColorChannel(true);
		kinect.enableColorImg(true);
		kinect.enableCoordinateMapperRGBDepth(true);
		kinect.enableDepthImg(true);
		kinect.enableSkeletonDepthMap(true);
		kinect.enableDepthMaskImg(true);
		kinect.enableInfraredImg(true);
		kinect.enablePointCloud(true);
		kinect.enableSkeleton(true);
		kinect.enableSkeleton3dMap(true);
		kinect.activateRawDepth(true);
		kinect.activateRawColor(true);
		kinect.enableLongExposureInfraredImg(true);
		kinect.activateRawInfrared(true);
		kinect.activateRawLongExposure(true);
		kinect.init();

		imageFrame.setLayout(new FlowLayout());
		imageFrame.setTitle("Image");
		imageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		imageFrame.setVisible(true);
		imageFrame.add(imageLabel);
		imageFrame.setBounds(0, 10, 1100, 1000);
		
		// Code
		System.out.println(getDepthForColorPixel(736, 657));
		mapColorToDepth();
		System.exit(0);
		
	}
	
	public static int getDepthForColorPixel(int x, int y) { // Es wird nur jeder 3. oder 4. X-ColorPixel verwendet
		float[] mapDCT = kinect.getMapDepthToColor(); // 0 -> returns X-Coord; 1 -> returns Y-Coord of Color Pixel in 1920x1080-Image for given DepthPixel
		int[] rawDepth = kinect.getRawDepth();
		int depthIndex = -1;
		
		while (true) {
			mapDCT = kinect.getMapDepthToColor();
			int fails = 0;
			for (float f : mapDCT) {
				if (f == 0) {
					fails++;
				}
			}
			if (fails == 0) {
				break;
			}
		}
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			writer = new PrintWriter("C://Users//vince//Desktop//text.txt");
			
			for (int i = 0; i < kinect.getRawLongExposure().length; i++) {
				System.out.println(kinect.getRawLongExposure()[i]);
			}
			
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		Point[] pointArray = new Point[217088];
		for (int i = 0; i < 217088; i++) {
			pointArray[i] = new Point( (int) mapDCT[i*2+0], (int) mapDCT[i*2+1]);
		}
		
		List<Point> pointList = Arrays.asList(pointArray);
		
		for (int i = -2; i <=2; i++) {
			Point p = new Point((x + i), y);
			if (pointList.contains(p)) {
				depthIndex = pointList.indexOf(p);
			}
		}
		
		if (depthIndex < 0) {
			return 0;
		}
		
		if(rawDepth[depthIndex] > 0) {
			return rawDepth[depthIndex];
		} else {
			System.out.println("No valid Depth @" + depthIndex);
			return 0;
		}
	}

	public static BufferedImage mapColorToDepth() {
		try {
			float[] mapDCT = kinect.getMapDepthToColor(); // 0 -> returns X-Coord; 1 -> returns Y-Coord of Color Pixel in 1920x1080-Image for given DepthPixel (poition in Array -> DepthPixel)
			int[] colorRaw = kinect.getRawColor(); // 1920 * 1080

			
			depthToColorImg = tester.createImage(512, 424, PImage.RGB); // Creates dtc-Image
			
			// Waits for the Sensor
			while (true) {
				mapDCT = kinect.getMapDepthToColor();
				int fails = 0;
				for (float f : mapDCT) {
					if (f == 0) {
						fails++;
					}
				}
				if (fails == 0) {
					break;
				}
			}

			
			int count = 0; // from 0 to 217088 ( 512 * 424 )
			for (int i = 0; i < 512; i++) { // Iterates through Depth-X
				for (int j = 0; j < 424; j++) { // Iterates through Depth-Y

					float valX = mapDCT[count * 2 + 0]; // Each first value = X
					float valY = mapDCT[count * 2 + 1]; // Each second value = Y
					
					int valXDepth = (int) ((valX / 1920.0) * 512.0); // Gets valX for 512*424 Images ( like DepthImages )
					int valYDepth = (int) ((valY / 1080.0) * 424.0); // Gets valY for 512*424 Images ( like DepthImages )

					int valXColor = (int) (valX); // Gets valX for 1920*1080 Images ( like ColorImages )
					int valYColor = (int) (valY); // Gets valX for 1920*1080 Images ( like ColorImages )
					
					if (valXDepth >= 0 && valXDepth < 512 && valYDepth >= 0 && valYDepth < 424 && valXColor >= 0 && valXColor < 1920 && valYColor >= 0 && valYColor < 1080) { 	// Filters "valX = -Infinity"-Errors
						
						int colorPixel = colorRaw[valYColor * 1920 + valXColor];  	// gets colorValues for row (valYColor * 1920) and column (valXColor) in color Image
						depthToColorImg.pixels[valYDepth * 512 + valXDepth] = colorPixel;	// sets colorValue for row (valYDepth * 512) and column (valYDepth) in depth Image
						
					}
					count++;

				}
			}
			
			depthToColorImg.updatePixels();
			
			BufferedImage dtc = ImageUtils.resize((BufferedImage) depthToColorImg.getImage(), 1920, 1080);
			BufferedImage color = (BufferedImage) kinect.getColorImage().getImage();
			
			ImageUtils.saveImage(dtc, "C://Users//vince//Desktop//dtc.png");
			ImageUtils.saveImage(color, "C://Users//vince//Desktop//color.png");

			Thread.sleep(2000);
			return dtc;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void updateImage(BufferedImage i) {
		ImageIcon icon = new ImageIcon(i);
		imageLabel.setIcon(icon);
	}
}
