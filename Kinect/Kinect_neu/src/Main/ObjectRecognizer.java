package Main;

import java.util.LinkedList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;

public class ObjectRecognizer {

	private static FeatureDetector featureDetector;
	private static DescriptorExtractor descriptorExtractor;
	
	private static Scalar newKeypointColor;
	private static Scalar matchestColor;
	
	private static float nndrRatio;
	private static int minMatches;
	
	private static MatOfKeyPoint[] processedObjectKeyPoints = new MatOfKeyPoint[15];
	private static MatOfKeyPoint[] processedObjectDescriptors = new MatOfKeyPoint[15];
	private static Mat[] processedOutputImages = new Mat[15];
	private static Mat[] processedObjects = new Mat[15];

	public static void init(float ratio, int matches) {	
		ObjectRecognizer.nndrRatio = ratio;
		ObjectRecognizer.minMatches = matches;
		
		featureDetector = FeatureDetector.create(FeatureDetector.SURF);
		descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
		
		newKeypointColor = new Scalar(255, 0, 0);
		matchestColor = new Scalar(0, 255, 0);	
		
	}

	public static void learn(Mat[] objects) {
		if (objects.length >= 15) {
			for (int i = 0; i < 15; i++) {
				Mat objectImage = objects[i];
				
				MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();
				MatOfKeyPoint objectDescriptors = new MatOfKeyPoint();
				featureDetector.detect(objectImage, objectKeyPoints);
				descriptorExtractor.compute(objectImage, objectKeyPoints, objectDescriptors);
				
				Mat outputImage = new Mat(objectImage.rows(), objectImage.cols(), Highgui.CV_LOAD_IMAGE_COLOR);
				Features2d.drawKeypoints(objectImage, objectKeyPoints, outputImage, newKeypointColor, 0);
				
				processedObjectKeyPoints[i] = objectKeyPoints;
				processedObjectDescriptors[i] = objectDescriptors;
				processedOutputImages[i] = outputImage;
				processedObjects[i] = objects[i];
			}
		} else {
			for (int i = 0; i < objects.length; i++) {
				Mat objectImage = objects[i];
				
				MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();
				MatOfKeyPoint objectDescriptors = new MatOfKeyPoint();
				featureDetector.detect(objectImage, objectKeyPoints);
				descriptorExtractor.compute(objectImage, objectKeyPoints, objectDescriptors);
				
				Mat outputImage = new Mat(objectImage.rows(), objectImage.cols(), Highgui.CV_LOAD_IMAGE_COLOR);
				Features2d.drawKeypoints(objectImage, objectKeyPoints, outputImage, newKeypointColor, 0);
				
				processedObjectKeyPoints[i] = objectKeyPoints;
				processedObjectDescriptors[i] = objectDescriptors;
				processedOutputImages[i] = outputImage;
				processedObjects[i] = objects[i];
			}
		}

	}

	public static Mat process(Mat sceneImage, int t) {
		
		Mat objectImage = processedObjects[t];
		MatOfKeyPoint objectDescriptors = processedObjectDescriptors[t];
		MatOfKeyPoint objectKeyPoints = processedObjectKeyPoints[t];

		MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
		MatOfKeyPoint sceneDescriptors = new MatOfKeyPoint();
		featureDetector.detect(sceneImage, sceneKeyPoints);
		descriptorExtractor.compute(sceneImage, sceneKeyPoints, sceneDescriptors);
		

		Mat matchoutput = new Mat(sceneImage.rows() * 2, sceneImage.cols() * 2, Highgui.CV_LOAD_IMAGE_COLOR);
		

		List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
		DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
		System.out.println("Matching object and scene images...");
		descriptorMatcher.knnMatch(objectDescriptors, sceneDescriptors, matches, 2);

		System.out.println("Calculating good match list...");
		LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();

		for (int i = 0; i < matches.size(); i++) {
			MatOfDMatch matofDMatch = matches.get(i);
			DMatch[] dmatcharray = matofDMatch.toArray();
			DMatch m1 = dmatcharray[0];
			DMatch m2 = dmatcharray[1];

			if (m1.distance <= m2.distance * nndrRatio) {
				goodMatchesList.addLast(m1);

			}
		}

		if (goodMatchesList.size() >= minMatches) {
			System.out.println("Object Found!!!");

			List<KeyPoint> objKeypointlist = objectKeyPoints.toList();
			List<KeyPoint> scnKeypointlist = sceneKeyPoints.toList();

			LinkedList<Point> objectPoints = new LinkedList<>();
			LinkedList<Point> scenePoints = new LinkedList<>();

			for (int i = 0; i < goodMatchesList.size(); i++) {
				objectPoints.addLast(objKeypointlist.get(goodMatchesList.get(i).queryIdx).pt);
				scenePoints.addLast(scnKeypointlist.get(goodMatchesList.get(i).trainIdx).pt);
			}

			MatOfPoint2f objMatOfPoint2f = new MatOfPoint2f();
			objMatOfPoint2f.fromList(objectPoints);
			MatOfPoint2f scnMatOfPoint2f = new MatOfPoint2f();
			scnMatOfPoint2f.fromList(scenePoints);

			Mat homography = Calib3d.findHomography(objMatOfPoint2f, scnMatOfPoint2f, Calib3d.RANSAC, 3);

			Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
			Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

			obj_corners.put(0, 0, new double[] { 0, 0 });
			obj_corners.put(1, 0, new double[] { objectImage.cols(), 0 });
			obj_corners.put(2, 0, new double[] { objectImage.cols(), objectImage.rows() });
			obj_corners.put(3, 0, new double[] { 0, objectImage.rows() });

			System.out.println("Transforming object corners to scene corners...");
			Core.perspectiveTransform(obj_corners, scene_corners, homography);

			
			Mat img = new Mat();
			sceneImage.copyTo(img);

			Core.line(img, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)),
					new Scalar(0, 255, 0), 4);
			Core.line(img, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)),
					new Scalar(0, 255, 0), 4);
			Core.line(img, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)),
					new Scalar(0, 255, 0), 4);
			Core.line(img, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)),
					new Scalar(0, 255, 0), 4);

			System.out.println("Drawing matches image...");
			MatOfDMatch goodMatches = new MatOfDMatch();
			goodMatches.fromList(goodMatchesList);

			Features2d.drawMatches(objectImage, objectKeyPoints, sceneImage, sceneKeyPoints, goodMatches, matchoutput,
					matchestColor, newKeypointColor, new MatOfByte(), 2);

			return matchoutput;

		}  else {
			System.out.println("Not enough maches: " + goodMatchesList.size());
			return null;
		}
	}
}