package com.github.spring_batch_smell_detector.smells;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.CouplingUtils;
import com.github.spring_batch_smell_detector.metrics.util.MethodCouplingComposite;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueryType;
import com.github.spring_batch_smell_detector.model.BatchRole;

@Component
public class AmateurWriter implements SmellDetector {

	Map<UUID, CKClassResultSpringBatch> ckResults;
	
	@Override
	public Set<UUID> analyse(Map<UUID, CKClassResultSpringBatch> results) {
		if(results == null || results.isEmpty())
			throw new RuntimeException("O resultado da análise das métricas não foi informado.");
		
		ckResults = results;
		
		final Set<UUID> affectedClasses = new HashSet<>();
		final Set<UUID> components = extractComponents();
		
		components.forEach(component -> {
			MethodCouplingComposite componentMethodRef = CouplingUtils.getLoadedInstance().getMethodCoupling(component);
			
			if (verifyAmateurWriterMethod(componentMethodRef) > 0) {
				affectedClasses.add(component);
			}
		});
		
		return affectedClasses;
	}

	private int verifyAmateurWriterMethod(MethodCouplingComposite componentMethodRef) {
		List<Integer> numberOfAmateurWriters = new ArrayList<>();
		
		componentMethodRef.preOrder(id -> {
			if(verifyAmateurWriterMethod(id)) {
				numberOfAmateurWriters.add(1);
			}
		});
		
		return numberOfAmateurWriters.size();
	}

	private boolean verifyAmateurWriterMethod(UUID id) {
		SQLQueriesFinder finder = SQLQueriesFinder.getLoadedInstance();
		CKClassResultSpringBatch classRef = ckResults.get(id);
		
		if(classRef == null)
			throw new RuntimeException("Referẽncia para classe não encontrada.");
		
		boolean hasWriterQuery = false;
		
		for(UUID queryId : classRef.getSqlQueries()) {
			if(finder.getQueries().get(queryId).getType() == SQLQueryType.WRITE_SQL) {
				hasWriterQuery = true;
				break;
			}
		}
		
		boolean isItemWriter = classRef.getBatchRole().contains(BatchRole.WRITER);
		
		return hasWriterQuery || isItemWriter;
	}

	private Set<UUID> extractComponents() {
		Set<UUID> components = new HashSet<>();

		ckResults.values().forEach(r -> {
			if (!r.getBatchRole().contains(BatchRole.WRITER)) {
				components.add(r.getId());
			}
		});
		
		return components;
	}
	
}
