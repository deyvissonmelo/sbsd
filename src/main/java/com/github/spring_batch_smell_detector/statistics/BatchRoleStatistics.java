package com.github.spring_batch_smell_detector.statistics;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.github.spring_batch_smell_detector.model.BatchRole;
import com.github.spring_batch_smell_detector.model.Metrics;

public class BatchRoleStatistics {

	private final BatchRole role;

	private final List<CKClassResultSpringBatch> ckClassResults;

	private final Map<Metrics, MetricStatistics> metricStatistics;

	public BatchRoleStatistics(BatchRole role, List<CKClassResultSpringBatch> results) {
		this.role = role;
		this.ckClassResults = results;
		this.metricStatistics = new EnumMap<>(Metrics.class);

		calculateStatistics();
	}

	private void calculateStatistics() {
		final Map<Metrics, double[]> mapMetrics = extractMetrics();

		Stream.of(Metrics.values()).forEach(m -> {
			DescriptiveStatistics ds = new DescriptiveStatistics(mapMetrics.get(m));
			metricStatistics.put(m,
					new MetricStatistics(ds.getMean(), ds.getStandardDeviation(), ds.getMax(), ds.getMin()));
		});

		metricStatistics.put(Metrics.SQL_COMPLEXITY, extractSQLMetrics());

	}

	private MetricStatistics extractSQLMetrics() {
		SQLQueriesFinder sqlFinder = SQLQueriesFinder.getLoadedInstance();
		List<Integer> sqlComplexities = new ArrayList<>(sqlFinder.getSqlQueriesComplexity().values());
		double[] sqlMetrics = new double[sqlComplexities.size()];

		for (int i = 0; i < sqlComplexities.size(); i++) {
			sqlMetrics[i] = sqlComplexities.get(i);
		}

		DescriptiveStatistics ds = new DescriptiveStatistics(sqlMetrics);

		return new MetricStatistics(ds.getMean(), ds.getStandardDeviation(), ds.getMax(), ds.getMin());
	}

	private Map<Metrics, double[]> extractMetrics() {
		Map<Metrics, double[]> mapMetrics = new EnumMap<>(Metrics.class);

		Stream.of(Metrics.values()).forEach(m -> mapMetrics.put(m, new double[ckClassResults.size()]));

		for (int i = 0; i < ckClassResults.size(); i++) {
			mapMetrics.get(Metrics.LOC)[i] = ckClassResults.get(i).getLoc();
			mapMetrics.get(Metrics.LCOM)[i] = ckClassResults.get(i).getLcom();
			mapMetrics.get(Metrics.MAXNESTING)[i] = ckClassResults.get(i).getMaxNestedBlocks();
			mapMetrics.get(Metrics.WMC)[i] = ckClassResults.get(i).getWmc();
			mapMetrics.get(Metrics.FICP)[i] = ckClassResults.get(i).getCoupling().size();
		}

		return mapMetrics;
	}

	public BatchRole getRole() {
		return role;
	}

	public Map<Metrics, MetricStatistics> getMetricStatistics() {
		return metricStatistics;
	}
	
	public MetricStatistics getMetricStatistic(Metrics metric) {
		return metricStatistics.get(metric);
	}
}
