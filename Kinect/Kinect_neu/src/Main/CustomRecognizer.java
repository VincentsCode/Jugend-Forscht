package Main;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

import Util.ImageUtils;

public class CustomRecognizer {

	private static FeatureDetector featureDetector;
	private static DescriptorExtractor descriptorExtractor;
	private static DescriptorMatcher descriptorMatcher;

	private static Scalar newKeypointColor;
	private static Scalar matchestColor;

	private static float Ratio;
	private static int minMatches;

	// 1 = back
	// 2 = right
	// 3 = left
	// 4 = front
	private static MatOfKeyPoint[] processedObjectKeyPoints = new MatOfKeyPoint[4];
	private static MatOfKeyPoint[] processedObjectDescriptors = new MatOfKeyPoint[4];
	private static Mat[] processedOutputImages = new Mat[4];
	private static Mat[] processedObjects = new Mat[4];

	private static processingThread[] threads = new processingThread[4];

	private static CustomRecognizer customRecognizer = new CustomRecognizer();

	private static Map<Integer, Integer> plausibilities = new HashMap<>();
	private static Map<Integer, Mat> resultImages = new HashMap<>();

	public static void init(float ratio, int matches) {
		CustomRecognizer.Ratio = ratio;
		CustomRecognizer.minMatches = matches;

		featureDetector = FeatureDetector.create(FeatureDetector.SURF);
		descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
		descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

		newKeypointColor = new Scalar(255, 0, 0);
		matchestColor = new Scalar(0, 255, 0);

	}

	public static void learn(Mat[] objects) {
		for (int i = 0; i < 4; i++) {
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

		ImageUtils.showImages(new BufferedImage[] { ImageUtils.matToImage(processedOutputImages[0]),
				ImageUtils.matToImage(processedOutputImages[1]), ImageUtils.matToImage(processedOutputImages[2]),
				ImageUtils.matToImage(processedOutputImages[3]) });
	}

	public static Mat process(Mat sceneImage) {
		
		// TODO: Get Point with most matches and return the Position of it instead of the image

		for (int s = 0; s < 4; s++) {
			threads[s] = customRecognizer.new processingThread(s, sceneImage, s);
			threads[s].start();
		}

		while (resultImages.size() != 4 || plausibilities.size() != 4) {
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		int max = 0;
		int maxKey = -1;
		for (int i = 0; i < plausibilities.size(); i++) {
			if (plausibilities.get(i) > max && plausibilities.get(i) > minMatches) {
				max = plausibilities.get(i);
				maxKey = i;
			}
		}

		if (maxKey != -1) {
			System.out.println(plausibilities.get(maxKey));
			return resultImages.get(maxKey);
		}

		return null;
	}

	class processingThread extends Thread {
		public int id;
		private Mat input;
		private int side;

		public processingThread(int id, Mat input, int side) {
			this.id = id;
			this.input = input;
			this.side = side;

		}

		@Override
		public void run() {

			MatOfKeyPoint objectKeyPoints = processedObjectKeyPoints[side];
			MatOfKeyPoint objectDescriptors = processedObjectDescriptors[side];
			Mat objectImage = processedObjects[side];

			MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
			MatOfKeyPoint sceneDescriptors = new MatOfKeyPoint();
			featureDetector.detect(input, sceneKeyPoints);
			descriptorExtractor.compute(input, sceneKeyPoints, sceneDescriptors);

			Mat matchoutput = new Mat(input.rows() * 2, input.cols() * 2, Highgui.CV_LOAD_IMAGE_COLOR);
			List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();

			descriptorMatcher.knnMatch(objectDescriptors, sceneDescriptors, matches, 2);

			LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();

			for (int i = 0; i < matches.size(); i++) {
				MatOfDMatch matofDMatch = matches.get(i);
				DMatch[] dmatcharray = matofDMatch.toArray();
				DMatch m1 = dmatcharray[0];
				DMatch m2 = dmatcharray[1];

				if (m1.distance <= m2.distance * Ratio) {
					goodMatchesList.addLast(m1);

				}
			}

			if (goodMatchesList.size() >= minMatches) {

				List<KeyPoint> objKeypointlist = objectKeyPoints.toList();
				List<KeyPoint> scnKeypointlist = sceneKeyPoints.toList();

				// TODO: Get Rid of the Points on the pink background

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

				Core.perspectiveTransform(obj_corners, scene_corners, homography);

				Mat img = new Mat();
				input.copyTo(img);

				Core.line(img, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)),
						new Scalar(0, 255, 0), 4);
				Core.line(img, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)),
						new Scalar(0, 255, 0), 4);
				Core.line(img, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)),
						new Scalar(0, 255, 0), 4);
				Core.line(img, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)),
						new Scalar(0, 255, 0), 4);

				MatOfDMatch goodMatches = new MatOfDMatch();
				goodMatches.fromList(goodMatchesList);

				Features2d.drawMatches(objectImage, objectKeyPoints, input, sceneKeyPoints, goodMatches, matchoutput,
						matchestColor, newKeypointColor, new MatOfByte(), 2);
			}

			plausibilities.put(side, goodMatchesList.size());
			resultImages.put(side, matchoutput);
		}
	}

}