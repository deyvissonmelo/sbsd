package com.github.spring_batch_smell_detector.metrics.util;

import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

import com.github.mauricioaniche.ck.metric.ClassLevelMetric;
import com.github.mauricioaniche.ck.metric.MethodLevelMetric;
import com.github.mauricioaniche.ck.util.MetricsFinder;

public class MetricsFinderSpringBatch extends MetricsFinder {

	private static final String METRICS_PACKAGE = "com.github.spring_batch_smell_detector.metrics.metric";

	@Override
	public List<ClassLevelMetric> allClassLevelMetrics() {
		List<ClassLevelMetric> allClassLevelMetrics = super.allClassLevelMetrics();

		try {
			for (Class<? extends ClassLevelMetric> aClass : loadClassLevelClasses()) {
				allClassLevelMetrics.add(aClass.getDeclaredConstructor().newInstance());
			}

			return allClassLevelMetrics;
		} catch (Exception e) {
			throw new RuntimeException("Could not instantiate a method level metric. Something is really wrong", e);
		}
	}

	@Override
	public List<MethodLevelMetric> allMethodLevelMetrics() {

		List<MethodLevelMetric> allMethodLevelMetrics = super.allMethodLevelMetrics();

		try {
			for (Class<? extends MethodLevelMetric> aClass : loadMethodLevelClasses()) {
				allMethodLevelMetrics.add(aClass.getDeclaredConstructor().newInstance());
			}

			return allMethodLevelMetrics;
		} catch (Exception e) {
			throw new RuntimeException("Could not instantiate a method level metric. Something is really wrong", e);
		}
	}

	private Set<Class<? extends MethodLevelMetric>> loadMethodLevelClasses() {
		try {
			Reflections reflections = new Reflections(METRICS_PACKAGE);
			return reflections.getSubTypesOf(MethodLevelMetric.class);
		} catch (Exception e) {
			throw new RuntimeException("Could not find method level metrics. Something is really wrong", e);
		}
	}

	private Set<Class<? extends ClassLevelMetric>> loadClassLevelClasses() {
		try {
			Reflections reflections = new Reflections(METRICS_PACKAGE);
			return reflections.getSubTypesOf(ClassLevelMetric.class);
		} catch (Exception e) {
			throw new RuntimeException("Could not find method level metrics. Something is really wrong", e);
		}
	}	
}
