package Main;

import java.awt.image.BufferedImage;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import Util.ImageUtils;

@SuppressWarnings("unused")
public class Tester {
	
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	public static void main(String[] args) {
		
		System.out.println(BufferedImage.TYPE_INT_RGB);
		
		System.out.println(BufferedImage.TYPE_INT_ARGB);
		
		/*
		Size s = new Size(640, 360);
		
		ObjectRecognizer.init(0.7f); // -> 3ms
		
		Mat objectA = Highgui.imread("C://Users//vince//Desktop//3.png", Highgui.CV_LOAD_IMAGE_COLOR); //-> 24ms
		Mat scene = Highgui.imread("C://Users//vince//Desktop//1.jpg", Highgui.CV_LOAD_IMAGE_COLOR); //-> 24ms
		
		Imgproc.resize(objectA, objectA, s); //-> 3.5ms
		Imgproc.resize(scene, scene, s); //-> 3.5ms
		
		ObjectRecognizer.learn(new Mat[]{objectA}); //-> 27ms
		
		Mat m = ObjectRecognizer.process(scene); //-> 80ms
		
		if (m != null)
			ImageUtils.showImage(ImageUtils.matToImage(m)); */
	} 
}
