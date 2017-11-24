import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.*;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

public class ObjectRecognition {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	static JFrame imageFrame = new JFrame();
	static JLabel imageLabel = new JLabel();

	static VideoCapture camera;
	static Mat videoFrame = new Mat();
	
	static Mat object;

	static FeatureDetector detector;
	static DescriptorExtractor descriptor;
	
	public static void main(String[] args) {
		
		setup();
		
		object = Imgcodecs.imread("Some ImagePath here"); //TODO
		
		
		MatOfKeyPoint objectPoints = new MatOfKeyPoint();
		Mat objectDescriptors = new Mat();
		
		detector.detect(videoFrame, objectPoints);
		
		List<KeyPoint> list = objectPoints.toList();
		Collections.sort(list, new Comparator<KeyPoint>() {
		    @Override
		    public int compare(KeyPoint kp1, KeyPoint kp2) {
		        return (int) (kp2.response - kp1.response);
		    }
		});

		List<KeyPoint> objectPointList = list.subList(0, 500);
		
		while (true) {
			camera.read(videoFrame);
			
			MatOfKeyPoint scenePoints = new MatOfKeyPoint();
			Mat sceneDescriptors = new Mat();
			
			detector.detect(videoFrame, scenePoints);
			
			List<KeyPoint> listOfKeypoints = scenePoints.toList();
			Collections.sort(listOfKeypoints, new Comparator<KeyPoint>() {
			    @Override
			    public int compare(KeyPoint kp1, KeyPoint kp2) {
			        return (int) (kp2.response - kp1.response);
			    }
			});

			List<KeyPoint> scenePointList = listOfKeypoints.subList(0, 500);
			
			

			
		}
	}

	public static void setup() {
		
		detector = FeatureDetector.create(FeatureDetector.SURF);
		descriptor = DescriptorExtractor.create(DescriptorExtractor.SURF);
		
		camera = new VideoCapture(0);

		imageFrame.setLayout(new FlowLayout());
		imageFrame.setTitle("Image");
		imageFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		imageFrame.setVisible(true);
		imageFrame.add(imageLabel);
		imageFrame.setBounds(20, 20, 720, 960);
	}

	public static void updateImage(BufferedImage i) {

		ImageIcon icon = new ImageIcon(i);
		imageLabel.setIcon(icon);
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

}
