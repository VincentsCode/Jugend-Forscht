package Main;

import org.opencv.core.Mat;

public class Result {
	
	Mat image;
	int x;
	
	public Result(Mat image, int x) {
		this.image = image;
		this.x = x;
	}
}
