package com.github.spring_batch_smell_detector.smells;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKMethodResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.CouplingUtils;
import com.github.spring_batch_smell_detector.metrics.util.MethodCouplingComposite;
import com.github.spring_batch_smell_detector.model.BatchRole;

@Component
public class GlobalProcessor implements SmellDetector {
	
	private class GlobalProcessorStatistics {
		UUID serviceId;
		List<UUID> externalClassReferences;
		Map<UUID, Set<UUID>> externalMethodReferences;
		boolean isAffected;

		public GlobalProcessorStatistics(UUID serviceId, List<UUID> numberOfClassesReferences,
				Map<UUID, Set<UUID>> numberOfMethodReferences) {
			this.serviceId = serviceId;
			this.externalClassReferences = numberOfClassesReferences;
			this.externalMethodReferences = numberOfMethodReferences;
			this.isAffected = false;
		}
	}

	private Set<CKClassResultSpringBatch> services;
	
	private Map<UUID, CKClassResultSpringBatch> ckResults;

	@Override
	public Set<UUID> analyse(Map<UUID, CKClassResultSpringBatch> results) {
		final List<GlobalProcessorStatistics> statistics = new ArrayList<GlobalProcessor.GlobalProcessorStatistics>();

		if(results == null || results.isEmpty())
			throw new RuntimeException("O resultado da análise das métricas não foi informado.");
		
		ckResults = results;
		
		// Extrair os serviços
		services = extractServices(ckResults.values());

		// Contabilizar referencias externas
		services.stream().forEach(service -> {			
			// Total de referencias de classes
			List<UUID> classReferences = extractClassReference(service.getId());

			// Total de referências aos métodos do serviço
			Map<UUID, Set<UUID>> methodReferences = new HashMap<>();

			service.getMethods().forEach(method -> {
				
				if(method.isConstructor() || ((CKMethodResultSpringBatch) method).isPrivate()) {
					return;
				}
				
				UUID methodId = ((CKMethodResultSpringBatch) method).getId();
				
				Set<UUID> references = extractMethodReferences(service.getId(), methodId);
				
				if(references != null && references.size() > 0) {
					methodReferences.put(methodId, references);
				}
			});
			
			statistics.add(new GlobalProcessorStatistics(service.getId(), classReferences, methodReferences));
		});

		return evaluateResults(statistics);

	}

	private Set<UUID> evaluateResults(List<GlobalProcessorStatistics> statistics) {
		if (statistics == null) {
			throw new RuntimeException("Erro inesperado ao tentar verificar o Design Smell Global Processor");
		}

		Set<UUID> references = new HashSet<>();

		statistics.stream().forEach(gps -> {

			// Classe referenciada por mais de 1 ItemProcessor
			if (gps.externalClassReferences != null && gps.externalClassReferences.size() > 1) {

				int numberOfMethods = gps.externalMethodReferences.size();
				int methodsWithSingleRef = 0;

				for (Set<UUID> refs : gps.externalMethodReferences.values()) {
					methodsWithSingleRef += refs.size() == 1 ? 1 : 0;
				}

				BigDecimal percentMethods = BigDecimal
						.valueOf((Double.valueOf(methodsWithSingleRef) / Double.valueOf(numberOfMethods)) * 100.00).setScale(2, RoundingMode.HALF_DOWN);

				gps.isAffected = (percentMethods.compareTo(BigDecimal.valueOf(33.33)) == 1);

				if (gps.isAffected) {
					references.add(gps.serviceId);
				}

			} else {
				gps.isAffected = false;
			}

		});

		return references;
	}

	private Set<UUID> extractMethodReferences(UUID classId, UUID methodId) {

		Set<UUID> references = new HashSet<>();

		Map<UUID, MethodCouplingComposite> couplingMethodMap = CouplingUtils.getLoadedInstance().getCouplingMethodMap();

		couplingMethodMap.forEach((key, value) -> {
			if (key == classId) {
				return;
			}
			
			if (!ckResults.get(key).getBatchRole().contains(BatchRole.PROCESSOR)) {
				return;
			}			
			
			if(value.methodIsDirectInvoked(classId, methodId)) {
				references.add(key);
			}
		});

		return references;
	}

	private List<UUID> extractClassReference(UUID uuid) {
		List<UUID> references = new ArrayList<>();

		CouplingUtils.getLoadedInstance().getCouplingClassMap().forEach((key, value) -> {
			if (key != uuid && value != null && value.contains(uuid)) {
				Set<BatchRole> roles = ckResults.get(key).getBatchRole();

				if (roles.contains(BatchRole.PROCESSOR)) {
					references.add(key);
				}
			}
		});

		return references;
	}

	private HashSet<CKClassResultSpringBatch> extractServices(Collection<CKClassResultSpringBatch> results) {
		return new HashSet<>(results.stream().filter(r -> r.getBatchRole().contains(BatchRole.SERVICE))
				.collect(Collectors.toList()));
	}

}
