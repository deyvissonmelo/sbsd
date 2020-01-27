package com.github.spring_batch_smell_detector.smells;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.MetricsThresholds;
import com.github.spring_batch_smell_detector.model.BatchRole;
import com.github.spring_batch_smell_detector.model.Metrics;
import com.github.spring_batch_smell_detector.statistics.MetricStatistics;

@Component
public class BrainProcessor implements SmellDetector {
	
	@Autowired
	private MetricsThresholds threshoulds;
	
	@Override
	public Set<UUID> analyse(Map<UUID, CKClassResultSpringBatch> results) {
		if(results == null || results.isEmpty())
			throw new RuntimeException("O resultado da análise das métricas não foi informado.");
		
		MetricStatistics loc = threshoulds.getThreshoulds(BatchRole.PROCESSOR, Metrics.LOC);
		MetricStatistics wmc = threshoulds.getThreshoulds(BatchRole.PROCESSOR, Metrics.WMC);
		MetricStatistics maxNeasting = threshoulds.getThreshoulds(BatchRole.PROCESSOR, Metrics.MAXNESTING);
		MetricStatistics lcom = threshoulds.getThreshoulds(BatchRole.PROCESSOR, Metrics.LCOM);
		
		final Set<UUID> affectedClasses = new HashSet<>();
		
		results.values().stream().forEach(classResult -> {
			if(classResult.getBatchRole().contains(BatchRole.PROCESSOR)) {
								
				boolean isMethodLong =  classResult.getLoc() >= loc.getHigherMargin();
				boolean isWMCHigh = classResult.getWmc() >= wmc.getHigherMargin();
				boolean isMaxNestingHigh = classResult.getMaxNestedBlocks() >= maxNeasting.getAverage();
				boolean isLowCoesion = classResult.getLcom() > lcom.getAverage();
				
				boolean isAffected = isMethodLong && (isWMCHigh || isMaxNestingHigh) && isLowCoesion;
				
				if(isAffected) {
					affectedClasses.add(classResult.getId());
				}
			}
		});
		
		return affectedClasses;
	}

}
