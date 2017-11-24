import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class ObjectDetection {
	
	static { 
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME); 
	}

	static Net net;
	
	final static int IN_WIDTH = 300;
	final static int IN_HEIGHT = 300;
	final static float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
	final static double IN_SCALE_FACTOR = 0.007843;
	final static double MEAN_VAL = 127.5;
	final static double THRESHOLD = 0.2;
	
	static JFrame imageFrame = new JFrame();
	static JLabel imageLabel = new JLabel();

	public static void main(String[] args) {
		
		imageFrame.setLayout(new FlowLayout());
		imageFrame.setTitle("Image");
		imageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		imageFrame.setVisible(true);
		imageFrame.add(imageLabel);
		imageFrame.setBounds(20, 20, 720, 960);

		Mat frame = new Mat(); 

		net = Dnn.readNetFromCaffe(
				"C://Users//vince//workspaceE//Kinect_neu//src//MobileNetSSD_deploy.prototxt.txt",
				"C://Users//vince//workspaceE//Kinect_neu//src//MobileNetSSD_deploy.caffemodel"
				);
		
		String[] types = {
				"background", "aeroplane", "bicycle", "bird", "boat", "bottle", "bus", "car", "cat", "chair",
				"cow", "diningtable", "dog", "horse", "motorbike", "person", "pottedplant", "sheep", "sofa", "train",
				"tvmonitor" 
				};

		VideoCapture camera = new VideoCapture(0);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Started");

		while (true) {
			
			camera.read(frame);
			Mat blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR, new Size(IN_WIDTH, IN_HEIGHT), new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false, false);
			net.setInput(blob);
			Mat detections = net.forward();
			int cols = frame.cols();
			int rows = frame.rows();
			Size cropSize;
			if ((float)cols / rows > WH_RATIO) {
				cropSize = new Size(rows * WH_RATIO, rows);
			} else {
				cropSize = new Size(cols, cols / WH_RATIO);
			}	
			int y1 = (int)(rows - cropSize.height) / 2;
			int y2 = (int)(y1 + cropSize.height);
			int x1 = (int)(cols - cropSize.width) / 2;
			int x2 = (int)(x1 + cropSize.width);
			Mat subFrame = frame.submat(y1, y2, x1, x2);
			cols = subFrame.cols();
			rows = subFrame.rows();
			detections = detections.reshape(1, (int)detections.total() / 7);

			for (int i = 0; i < detections.rows(); ++i) {
				double confidence = detections.get(i, 2)[0];
				if (confidence > THRESHOLD) {
					int classId = (int)detections.get(i, 1)[0];
					int xLeftBottom = (int)(detections.get(i, 3)[0] * cols);
					int yLeftBottom = (int)(detections.get(i, 4)[0] * rows);
					int xRightTop   = (int)(detections.get(i, 5)[0] * cols);
					int yRightTop   = (int)(detections.get(i, 6)[0] * rows);
					
					Imgproc.rectangle(subFrame, new Point(xLeftBottom, yLeftBottom), new Point(xRightTop, yRightTop), new Scalar(0, 255, 0));
					String label = types[classId] + ": " + confidence;
					int[] baseLine = new int[1];
					Size labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);
					Imgproc.rectangle(subFrame, new Point(xLeftBottom, yLeftBottom - labelSize.height), new Point(xLeftBottom + labelSize.width, yLeftBottom + baseLine[0]), new Scalar(255, 255, 255), Core.FILLED);
					Imgproc.putText(subFrame, label, new Point(xLeftBottom, yLeftBottom), Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0));
					
					System.out.println(label + "@"+ (xLeftBottom + xRightTop) / 2);
					
				}
			}
			updateImage(matToImage(frame));
		}

	}

	public static BufferedImage matToImage(Mat frame) {
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

	public static void updateImage(BufferedImage i) {
		
		ImageIcon icon1 = new ImageIcon(i);
		
		imageLabel.setIcon(icon1);
	}
	
}
