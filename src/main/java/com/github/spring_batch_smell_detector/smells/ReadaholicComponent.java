package com.github.spring_batch_smell_detector.smells;

import java.util.ArrayList;
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
import com.github.spring_batch_smell_detector.metrics.util.CouplingUtils;
import com.github.spring_batch_smell_detector.metrics.util.MethodCouplingComposite;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQuery;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueryType;
import com.github.spring_batch_smell_detector.model.BatchRole;

@Component
public class ReadaholicComponent implements SmellDetector {
	
	@Autowired
	private MetricsThresholds thresholds;
	
	@Override
	public Set<UUID> analyse(Map<UUID, CKClassResultSpringBatch> results) {
		final Set<UUID> affectedClasses = new HashSet<>();
				
		if(results == null || results.isEmpty())
			throw new RuntimeException("O resultado da análise das métricas não foi informado.");
		
		final Set<CKClassResultSpringBatch> components = extractComponents(results.values());
		
		int maxReading = thresholds.getMetricReadaholicMaxReading();
		
		components.forEach(component -> {			
						
			MethodCouplingComposite componentMethodRef = CouplingUtils.getLoadedInstance().getMethodCoupling(component.getId());		
			
			int totalOfAccess = calculateComponentDatabaseAccess(component);
			
			totalOfAccess += calculateMethodDatabaseAccess(componentMethodRef, results);

			if (totalOfAccess > maxReading) {
				affectedClasses.add(component.getId());
			}
		});

		return affectedClasses;
	}

	public int calculateMethodDatabaseAccess(MethodCouplingComposite componentMethodRef, 
			Map<UUID, CKClassResultSpringBatch> results) {
		List<Integer> totalOfAccess = new ArrayList<>();
		
		componentMethodRef.preOrder(id -> {
			CKClassResultSpringBatch classRef = results.get(id);
			
			if(getTotalReaderQueries(classRef.getSqlQueries()) > 0) {
				totalOfAccess.add(1);
			}
			
			classRef.getMethods().forEach(method -> {
				if(getTotalReaderQueries(((CKMethodResultSpringBatch) method).getSqlQueries()) > 0) {
					totalOfAccess.add(1);
				}
			});
			
		});
		
		return totalOfAccess.size();
	}
	
	private int getTotalReaderQueries(Set<UUID> queries) {
		SQLQueriesFinder sqlFinder = SQLQueriesFinder.getLoadedInstance();
		int totalReaderQueries = 0;
		
		for(UUID key : queries){
			SQLQuery query = sqlFinder.getQueries().get(key);
						
			if(query != null)
				totalReaderQueries += query.getType() == SQLQueryType.READ_SQL ? 1 : 0; 
		}
		
		return totalReaderQueries;
	}

	public int calculateComponentDatabaseAccess(CKClassResultSpringBatch component) {
		int totalOfAccess = getTotalReaderQueries(component.getSqlQueries());
		
		for(CKMethodResult method : component.getMethods()) {
			totalOfAccess += getTotalReaderQueries(((CKMethodResultSpringBatch) method).getSqlQueries());
		}
		
		return totalOfAccess;
	}

	private Set<CKClassResultSpringBatch> extractComponents(Collection<CKClassResultSpringBatch> results) {
		Set<CKClassResultSpringBatch> components = new HashSet<>();

		results.forEach(r -> {
			if (r.getBatchRole().contains(BatchRole.PROCESSOR) || r.getBatchRole().contains(BatchRole.WRITER)) {
				components.add(r);
			}
		});

		return components;
	}

}
