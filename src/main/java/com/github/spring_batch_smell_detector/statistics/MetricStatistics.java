package com.github.spring_batch_smell_detector.statistics;

public class MetricStatistics {
	
	private double average;
	
	private double higher;
	
	private double lower;
		
	public MetricStatistics(double average, double higher, double lower) {
		super();
		this.average = average;
		this.higher = higher;
		this.lower = lower;
	}

	public double getAverage() {
		return average;
	}

	public void setAverage(double average) {
		this.average = average;
	}

	public double getHigher() {
		return higher;
	}
	
	public void setHigher(double higher) {
		this.higher = higher;
	}
	
	public double getLower() {
		return lower;
	}
	
	public void setLower(double lower) {
		this.lower = lower;
	}
}
