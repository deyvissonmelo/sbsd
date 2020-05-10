package com.github.spring_batch_smell_detector.smells;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKMethodResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.MetricsThresholds;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQuery;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueryFileType;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueryType;
import com.github.spring_batch_smell_detector.model.BatchRole;
import com.github.spring_batch_smell_detector.model.Metrics;
import com.github.spring_batch_smell_detector.statistics.MetricStatistics;

@Component
public class BrainWriter implements SmellDetector {

	@Autowired
	private MetricsThresholds threshoulds;
	
	@Override
	public Set<UUID> analyse(Map<UUID, CKClassResultSpringBatch> results) {
		if(results == null || results.isEmpty())
			throw new RuntimeException("O resultado da análise das métricas não foi informado.");
		
		MetricStatistics loc = threshoulds.getThreshoulds(BatchRole.WRITER, Metrics.LOC);
		MetricStatistics wmc = threshoulds.getThreshoulds(BatchRole.WRITER, Metrics.WMC);
		MetricStatistics maxNeasting = threshoulds.getThreshoulds(BatchRole.WRITER, Metrics.MAXNESTING);
		MetricStatistics sqlComplexity_writer = threshoulds.getThreshoulds(BatchRole.WRITER, Metrics.SQL_COMPLEXITY_WRITE);
		
		final Set<UUID> affectedClasses = new HashSet<>();

		results.values().stream().forEach(classResult -> {
			if (classResult.getBatchRole().contains(BatchRole.WRITER)) {
				
				boolean isMethodLong = classResult.getLoc() >= loc.getHigher();
				boolean isWMCHigh = classResult.getWmc() > wmc.getAverage();
				boolean isMaxNestingHigh = classResult.getMaxNestedBlocks() > maxNeasting.getAverage();

				int maxSqlComplexity = classResult.getMaxSqlComplexity();

				for (CKMethodResult method : classResult.getMethods()) {
					int methodSQLComplexity = ((CKMethodResultSpringBatch) method).getMaxSqlComplexity();
					maxSqlComplexity = maxSqlComplexity < methodSQLComplexity ? methodSQLComplexity : maxSqlComplexity;
				}

				boolean isSQLHigh = maxSqlComplexity > sqlComplexity_writer.getAverage();

				boolean isAffected = (isMethodLong && (isWMCHigh || isMaxNestingHigh)) || isSQLHigh;
				
				if(isAffected) {
					affectedClasses.add(classResult.getId());
				}
			}
		});
		
		affectedClasses.addAll(analyseJobQueries(sqlComplexity_writer.getAverage()));
		
		return affectedClasses;
	}
	
	private Collection<? extends UUID> analyseJobQueries(double averageComplexity) {
		Set<UUID> affectedQueries = new HashSet<>();
		List<SQLQuery> jobQueries = SQLQueriesFinder.getLoadedInstance().getByFileType(SQLQueryFileType.JOB_FILE);
		
		jobQueries.forEach(q -> {
			if(q.getType().equals(SQLQueryType.WRITE_SQL) && q.getComplexity() > averageComplexity) {
				affectedQueries.add(q.getId());
			}
		});
		
		return affectedQueries;
	}

}
