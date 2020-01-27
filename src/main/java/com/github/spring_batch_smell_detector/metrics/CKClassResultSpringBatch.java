package com.github.spring_batch_smell_detector.metrics;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.spring_batch_smell_detector.model.BatchRole;

public class CKClassResultSpringBatch extends CKClassResult {
	
	private UUID id;
	
	private Set<String> coupling;
	
	private Set<BatchRole> batchRole;
	
	private Set<UUID> sqlQueries;
	
	private Integer maxSqlComplexity;
	
	public CKClassResultSpringBatch(String file, String className, String type, int modifiers) {
		super(file, className, type, modifiers);
		this.id = UUID.randomUUID();
		this.coupling = new HashSet<>();	
		this.sqlQueries = new HashSet<>();
		this.maxSqlComplexity = 0;
	}		
	
	public UUID getId() {
		return id;
	}
	
	public Set<String> getCoupling() {
		return coupling;
	}
	
	public void setCoupling(Set<String> coupling) {
		this.coupling = coupling;
	}
	
	public Set<BatchRole> getBatchRole() {
		return batchRole;
	}
	
	public void setBatchRole(Set<BatchRole> roles) {
		this.batchRole = roles;
	}
	
	public Set<UUID> getSqlQueries() {
		return sqlQueries;
	}
	
	public void setSqlQueries(Set<UUID> sqlQueries) {
		this.sqlQueries = sqlQueries;
	}
	
	public Integer getMaxSqlComplexity() {
		return maxSqlComplexity;
	}
	
	public void setMaxSqlComplexity(Integer maxSqlComplexity) {
		this.maxSqlComplexity = maxSqlComplexity;
	}
	
	public Optional<CKMethodResult> getMethodById(UUID methodId) {
		return getMethods().stream().filter(m -> ((CKMethodResultSpringBatch) m).getId().equals(methodId)).findFirst();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		
		if(!(o instanceof CKClassResultSpringBatch))
			return false;
		
		CKClassResultSpringBatch clazz = (CKClassResultSpringBatch) o;
						
		return Objects.equals(clazz.getId(), this.getId());		
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
