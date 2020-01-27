package com.github.spring_batch_smell_detector.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.github.spring_batch_smell_detector.model.BatchRole;
import com.github.spring_batch_smell_detector.model.Metrics;
import com.github.spring_batch_smell_detector.statistics.MetricStatistics;

@Component
@PropertySource("classpath:metric_statistics.properties")
public class MetricsThresholds {

	private final String ROLE_READER_KEY = "reader.statistic";
	
	private final String ROLE_SERVICE_KEY = "service.statistic";
	
	private final String ROLE_PROCESSOR_KEY = "processor.statistic";
	
	private final String ROLE_WRITER_KEY = "writer.statistic";
	
	private final String METRIC_LOC_KEY = "loc";
	
	private final String METRIC_LCOM_KEY = "lcom";
	
	private final String METRIC_MAX_NEASTING_KEY = "maxneasting";
	
	private final String METRIC_WMC_KEY = "wmc";
	
	private final String METRIC_FICP_KEY = "ficp";
	
	private final String METRIC_SQL_COMPLEXITY_KEY = "sqlcomplexity";
	
	private final String STATISTIC_MIN = "min";
	
	private final String STATISTIC_MAX = "max";
	
	private final String STATISTIC_AVERAGE = "average";
	
	@Autowired
	private Environment env;
	
	public MetricStatistics getThreshoulds(BatchRole role, Metrics metric) {
		switch (role) {
		case READER:
			return getMetricsStatistics(
					String.format("%s.%s", ROLE_READER_KEY, getMetricKey(metric))
			);			
		case SERVICE:
			return getMetricsStatistics(
					String.format("%s.%s", ROLE_SERVICE_KEY, getMetricKey(metric))
			);	
		case PROCESSOR:
			return getMetricsStatistics(
					String.format("%s.%s", ROLE_PROCESSOR_KEY, getMetricKey(metric))
			);
		case WRITER:
			return getMetricsStatistics(
					String.format("%s.%s", ROLE_WRITER_KEY, getMetricKey(metric))
			);
		default:
			throw new RuntimeException("Papel arquitetural informado não pertence ao arquivo de threshoulds");
		}
	}

	private String getMetricKey(Metrics metric) {
		switch (metric) {
		case LOC:
			return METRIC_LOC_KEY;
		case LCOM:
			return METRIC_LCOM_KEY;
		case MAXNESTING:
			return METRIC_MAX_NEASTING_KEY;
		case WMC:
			return METRIC_WMC_KEY;
		case FICP:
			return METRIC_FICP_KEY;
		case SQL_COMPLEXITY:
			return METRIC_SQL_COMPLEXITY_KEY;
		default:
			throw new RuntimeException("Métrica informada não pertence ao arquivo de threshoulds");
		}
	}
	
	private MetricStatistics getMetricsStatistics(String key) {
		try {
			double min = Double.valueOf(env.getProperty(String.format("%s.%s", key, STATISTIC_MIN)));
			double average = Double.valueOf(env.getProperty(String.format("%s.%s", key, STATISTIC_AVERAGE)));
			double max = Double.valueOf(env.getProperty(String.format("%s.%s", key, STATISTIC_MAX)));
			
			return new MetricStatistics(average, 0, max, min);
			
		}
		catch (NumberFormatException e) {
			throw new NumberFormatException("O arquivo de threshoulds possui valores inválidos.");
		}
	}
	
}
