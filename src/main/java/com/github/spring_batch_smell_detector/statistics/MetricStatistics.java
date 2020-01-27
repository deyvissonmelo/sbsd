package com.github.spring_batch_smell_detector.statistics;

public class MetricStatistics {
	
	private double average;
	
	private double stdDeviation;
	
	private double max;
	
	private double min;
		
	public MetricStatistics(double average, double stdDeviation, double max, double min) {
		super();
		this.average = average;
		this.stdDeviation = stdDeviation;
		this.max = max;
		this.min = min;
	}

	public double getAverage() {
		return average;
	}

	public void setAverage(double average) {
		this.average = average;
	}

	public double getStdDeviation() {
		return stdDeviation;
	}

	public void setStdDeviation(double stdDeviation) {
		this.stdDeviation = stdDeviation;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}			
	
	public double getLowerMargin() {
		return average - stdDeviation;
	}		
	
	public double getHigherMargin() {
		return average + stdDeviation;
	}
	
	public double getVeryHigherMargin() {
		return (average + stdDeviation) * 1.5;
	}
}
