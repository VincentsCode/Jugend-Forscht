import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import KinectPV2.KinectPV2;
import processing.core.PApplet;

public class test extends PApplet {
	
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	static JFrame imageFrame = new JFrame();
	static JLabel imageLabel = new JLabel();
	static JLabel imageLabel2 = new JLabel();

	static KinectPV2 kinect;
	static VideoCapture camera;

	public static void main(String[] args) {

		test t = new test();
		t.init();

		while (true) {
			long a = System.currentTimeMillis();
			BufferedImage dephtImage = (BufferedImage) kinect.getPointCloudDepthImage().getImage();
			BufferedImage colorImage = (BufferedImage) kinect.getColorImage().getImage();
			// kinect.getBodyTrackImage().getImage();
			// BufferedImage infraredImage = (BufferedImage)
			// kinect.getInfraredLongExposureImage().getImage();

			ArrayList<?> trackedUsers = kinect.getSkeletonDepthMap();
			// int[] depthData = kinect.getRawDepthData();

//			Mat videoFrame = new Mat();
			// camera.read(videoFrame);
			// BufferedImage videoFrameImage = t.matToImage(videoFrame);

			if (trackedUsers.size() > 0) {
				System.out.println("Found User");
//				updateImage(colorImage, trackerImage);
				// System found User
				// TODO: (Check User ->) Process Data and send it!
			} else {
				System.out.println("Found no User");
				// Find User by myself
				// TODO: Write Algo
			}

			updateImage(dephtImage, colorImage);

			long delta = System.currentTimeMillis() - a;
			if (delta > 30) {
				if (delta > 100) {
					System.err.println("It's taking too long: " + delta);
				} else {
					System.out.println("It's taking long: " + delta);
				}
			}

		}

	}

	public void init() {
		kinect = new KinectPV2(this);

		kinect.enableBodyTrackImg(true);
		kinect.enableColorImg(true);
		kinect.enableColorPointCloud(true);
		kinect.enableCoordinateMapperRGBDepth(true);
		kinect.enableDepthImg(true);
		kinect.enableDepthMaskImg(true);
		kinect.enableFaceDetection(true);
		kinect.enableHDFaceDetection(true);
		kinect.enableInfraredImg(true);
		kinect.enableInfraredLongExposureImg(true);
		kinect.enablePointCloud(true);
		kinect.enableSkeleton3DMap(true);
		kinect.enableSkeletonColorMap(true);
		kinect.enableSkeletonDepthMap(true);

		kinect.setHighThresholdPC(1700);
		kinect.setLowThresholdPC(0);

		kinect.init();

		camera = new VideoCapture(0);

		imageFrame.setLayout(new FlowLayout());
		imageFrame.setTitle("Image");
		imageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		imageFrame.setVisible(true);
		imageFrame.add(imageLabel);
		imageFrame.add(imageLabel2);
		imageFrame.setBounds(20, 20, 720, 960);

		System.out.println("Initialized");
	}

	public static void updateImage(BufferedImage i1, BufferedImage i2) {

		ImageIcon icon1 = new ImageIcon(i1);
		ImageIcon icon2 = new ImageIcon(i2);

		imageLabel.setIcon(icon1);
		imageLabel2.setIcon(icon2);
	}

	public BufferedImage matToImage(Mat frame) {
		int type = 0;
		if (frame.channels() == 1) {
			type = BufferedImage.TYPE_BYTE_GRAY;
		} else if (frame.channels() == 3) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
		WritableRaster raster = image.getRaster();
		DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
		byte[] data = dataBuffer.getData();
		frame.get(0, 0, data);

		return image;
	}

}
