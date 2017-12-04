package Main;

import lejos.robotics.SampleProvider;
import lejos.robotics.filter.AbstractFilter;

public class UltrasonicSensor extends AbstractFilter {
	
	float[] sample;

	public UltrasonicSensor(SampleProvider source) {
		super(source);
		sample = new float[sampleSize];
		
	}
	
	public int getDistance() {
		super.fetchSample(sample, 0);
		return Math.round(sample[0] * 100);
	}

}
