package Main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import Util.ImageUtils;

public class CustomRecognizer {
	
	private static FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SURF);;
	private static DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);;
	private static DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

	private static Scalar newKeypointColor = new Scalar(255, 0, 0);
	private static Scalar matchestColor = new Scalar(0, 255, 0);

	private static float Ratio;
	private static int minMatches;
	private static int imageCount;

	private static MatOfKeyPoint[] processedObjectKeyPoints;
	private static MatOfKeyPoint[] processedObjectDescriptors;
	private static Mat[] processedOutputImages;
	private static Mat[] processedObjects;

	private static processingThread[] threads;

	private static CustomRecognizer customRecognizer = new CustomRecognizer();

	private static Map<Integer, Integer> plausibilities = new HashMap<>();
	private static Map<Integer, Mat> resultImages = new HashMap<>();
	private static Map<Integer, Integer> resultValues = new HashMap<>();

	public static void init(float ratio, int matches, int count) {
		CustomRecognizer.Ratio = ratio;
		CustomRecognizer.minMatches = matches;
		CustomRecognizer.imageCount = count;
		
		processedObjectKeyPoints = new MatOfKeyPoint[imageCount];
		processedObjectDescriptors = new MatOfKeyPoint[imageCount];
		processedOutputImages = new Mat[imageCount];
		processedObjects = new Mat[imageCount];
		threads = new processingThread[imageCount];
	}

	public static void learn(Mat[] objects) {
		for (int i = 0; i < imageCount; i++) {
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
		
		if (processedOutputImages.length > 4) {
			for (int i = 0; i < processedOutputImages.length; i++) {
				Imgproc.resize(processedOutputImages[i], processedOutputImages[i], new Size(640 / (processedOutputImages.length / 4), 360 / (processedOutputImages.length / 4)));
			}
		}
		
		ImageUtils.showImages(ImageUtils.matsToImages(processedOutputImages));
	}

	public static Result process(Mat sceneImage) {

		for (int s = 0; s < imageCount; s++) {
			threads[s] = customRecognizer.new processingThread(s, sceneImage, s);
			threads[s].start();
		}

		while (resultImages.size() != imageCount || plausibilities.size() != imageCount || resultValues.size() != imageCount) {
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
			System.out.println("Plausabilty: " + plausibilities.get(maxKey));
			return new Result(resultImages.get(maxKey), resultValues.get(maxKey));
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
			// gets preprocessed object-keypoints
			Mat objectImage = processedObjects[side];
			MatOfKeyPoint objectKeyPoints = processedObjectKeyPoints[side];
			MatOfKeyPoint objectDescriptors = processedObjectDescriptors[side];
			
			// gets scene-keypoints
			MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
			MatOfKeyPoint sceneDescriptors = new MatOfKeyPoint();
			featureDetector.detect(input, sceneKeyPoints);
			descriptorExtractor.compute(input, sceneKeyPoints, sceneDescriptors);
			KeyPoint[] sceneKeyPointsArray = sceneKeyPoints.toArray();
			
			// gets matches of scene and object
			Mat matchoutput = new Mat(input.rows() * 2, input.cols() * 2, Highgui.CV_LOAD_IMAGE_COLOR);
			List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
			descriptorMatcher.knnMatch(objectDescriptors, sceneDescriptors, matches, 2);

			// filters matches
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
			
			double finalX = 0;
			if (goodMatchesList.size() >= minMatches) {
				// draws matches
				MatOfDMatch goodMatches = new MatOfDMatch();
				goodMatches.fromList(goodMatchesList);
				Features2d.drawMatches(objectImage, objectKeyPoints, input, sceneKeyPoints, goodMatches, matchoutput, matchestColor, newKeypointColor, new MatOfByte(), 2);
				
				// draws and gets x-position of user
				List<Double> xValues = new ArrayList<>(); 
				for (int i = 0; i < goodMatchesList.size(); i++) {
					xValues.add(sceneKeyPointsArray[goodMatchesList.get(i).trainIdx].pt.x);
				}
				
				xValues.sort(Double::compareTo);
				
				double q1 = xValues.get(xValues.size() / 4);
				double q3 = xValues.get(xValues.size() / 2 + xValues.size() / 4);
				double iqd = (q3 - q1) * 2;
				
				double rangeUp = q3 + iqd;
				double rangeDown = q1 - iqd;
				
				for (int i = 0; i < xValues.size(); i++) {
					if (xValues.get(i) > rangeUp || xValues.get(i) < rangeDown) {
						xValues.remove(i);
					}
				}
				
				xValues = xValues.subList(1, xValues.size() -1);
				
				finalX = 0;
				for (double d : xValues) {
					finalX += d;
				}
				finalX = finalX / xValues.size();
				
				Core.line(matchoutput, new Point(finalX + 640, 0), new Point(finalX + 640, 400), newKeypointColor, 10);
				Core.putText(matchoutput, String.valueOf((int)(finalX - 320)), new Point(640, 20), Core.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 0, 255));
				
			}

			// returns plausability and results
			plausibilities.put(side, goodMatchesList.size());
			resultImages.put(side, matchoutput);
			resultValues.put(side, (int) finalX - 320);
		}
	}

}