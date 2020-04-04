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
import com.github.spring_batch_smell_detector.model.BatchRole;

@Component
public class ImproperCommunication implements SmellDetector {

	private Map<UUID, CKClassResultSpringBatch> ckResults;

	@Override
	public Set<UUID> analyse(Map<UUID, CKClassResultSpringBatch> results) {
		if(results == null || results.isEmpty())
			throw new RuntimeException("O resultado da análise das métricas não foi informado.");
		
		ckResults = results;

		final Set<UUID> affectedClasses = new HashSet<>();
		final Set<UUID> components = extractComponents();

		components.forEach(component -> {
			Set<UUID> componentClassRef = CouplingUtils.getLoadedInstance().getClassCoupling(component);
			MethodCouplingComposite componentMethodRef = CouplingUtils.getLoadedInstance().getMethodCoupling(component);
			
			if (verifyClassImproperCommunication(componentClassRef) > 0
					|| verifyMethodImproperCommunication(componentMethodRef) > 0) {
				affectedClasses.add(component);
			}
		});

		return affectedClasses;
	}

	private int verifyMethodImproperCommunication(MethodCouplingComposite componentMethodRef) {
		List<Integer> numberOfCommunications = new ArrayList<>();

		componentMethodRef.preOrder(id -> {	
			if (id != componentMethodRef.getClassId() && hasArchtecturalRole(id)) {
				numberOfCommunications.add(1);
			}
		});

		return numberOfCommunications.size();
	}

	private int verifyClassImproperCommunication(Set<UUID> componentClassRef) {
		int numberOfCommunications = 0;

		for (UUID id : componentClassRef) {
			
			if (hasArchtecturalRole(id)) {
				numberOfCommunications++;
			}
		}

		return numberOfCommunications;
	}

	private Set<UUID> extractComponents() {
		Set<UUID> components = new HashSet<>();

		ckResults.values().forEach(r -> {
			if (hasArchtecturalRole(r.getId())) {
				components.add(r.getId());
			}
		});
		
		return components;
	}

	private boolean hasArchtecturalRole(UUID classId) {
		CKClassResultSpringBatch classRef = ckResults.get(classId);

		if (classRef == null || classRef.getBatchRole().isEmpty()) {
			return false;
		}
				
		return 	classRef.getBatchRole().contains(BatchRole.READER) ||
				classRef.getBatchRole().contains(BatchRole.PROCESSOR) ||
				classRef.getBatchRole().contains(BatchRole.WRITER);
	}
}
