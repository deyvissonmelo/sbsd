package com.github.spring_batch_smell_detector.smells;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKMethodResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.CouplingUtils;
import com.github.spring_batch_smell_detector.metrics.util.MethodCouplingComposite;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQuery;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueryType;
import com.github.spring_batch_smell_detector.model.BatchRole;

@Component
public class ReadaholicComponent implements SmellDetector {

	Map<UUID, CKClassResultSpringBatch> ckResults;
	
	@Override
	public Set<UUID> analyse(Map<UUID, CKClassResultSpringBatch> results) {
		final Set<UUID> affectedClasses = new HashSet<>();
		final Set<UUID> components = extractComponents(results.values());		
		
		if(results == null || results.isEmpty())
			throw new RuntimeException("O resultado da análise das métricas não foi informado.");
		
		ckResults = results;
		
		components.forEach(component -> {			
			
			Set<UUID> componentClassRef = CouplingUtils.getLoadedInstance().getClassCoupling(component);
			MethodCouplingComposite componentMethodRef = CouplingUtils.getLoadedInstance().getMethodCoupling(component);		
			
			int totalOfAccess = calculateComponentDatabaseAccess(component);
			
			totalOfAccess += calculateClassDatabaseAccess(componentClassRef);
			totalOfAccess += calculateMethodDatabaseAccess(componentMethodRef);

			if (totalOfAccess > 0) {
				affectedClasses.add(component);
			}
		});

		return affectedClasses;
	}

	private int calculateMethodDatabaseAccess(MethodCouplingComposite componentMethodRef) {
		List<Integer> totalOfAccess = new ArrayList<>();
		
		componentMethodRef.preOrder(id -> {
			CKClassResultSpringBatch classRef = ckResults.get(id);
			
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

	private int calculateComponentDatabaseAccess(UUID component) {
		int totalOfAccess = getTotalReaderQueries(ckResults.get(component).getSqlQueries());
		
		for(CKMethodResult method : ckResults.get(component).getMethods()) {
			totalOfAccess += getTotalReaderQueries(((CKMethodResultSpringBatch) method).getSqlQueries());
		}
		
		return totalOfAccess;
	}



	private int calculateClassDatabaseAccess(Set<UUID> componentRef) {
		int numberDatabaseAccess = 0;

		if (componentRef.isEmpty()) {
			return 0;
		}

		for (UUID id : componentRef) {
			Set<UUID> childsRef = CouplingUtils.getLoadedInstance().getClassCoupling(id);
			numberDatabaseAccess += calculateClassDatabaseAccess(childsRef);

			if (ckResults.get(id) != null) {
				numberDatabaseAccess += getTotalReaderQueries(ckResults.get(id).getSqlQueries());
			}
		}

		return numberDatabaseAccess;
	}

	private Set<UUID> extractComponents(Collection<CKClassResultSpringBatch> results) {
		Set<UUID> components = new HashSet<>();

		results.forEach(r -> {
			if (r.getBatchRole().contains(BatchRole.PROCESSOR) || r.getBatchRole().contains(BatchRole.WRITER)) {
				components.add(r.getId());
			}
		});

		return components;
	}

}
