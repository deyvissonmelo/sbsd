package com.github.spring_batch_smell_detector.metrics;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.github.mauricioaniche.ck.CKMethodResult;

public class CKMethodResultSpringBatch extends CKMethodResult {

	private UUID id;
	
	private Set<String> methodClassCoupling;
	
	private Set<String> methodInvokeCoupling;	
	
	private Set<UUID> sqlQueries;
	
	private Integer maxSqlComplexity;
	
	private boolean isPrivate;
	
	public CKMethodResultSpringBatch(String methodName, boolean isConstructor, boolean isPrivate, int modifiers) {
		super(methodName, isConstructor, modifiers);
		this.id = UUID.randomUUID();
		this.methodClassCoupling = new HashSet<>();
		this.methodInvokeCoupling = new HashSet<>();
		this.sqlQueries = new HashSet<>();
		this.maxSqlComplexity = 0;
		this.isPrivate = isPrivate;
	}

	public UUID getId() {
		return id;
	}
	
	public Set<String> getMethodClassCoupling() {
		return methodClassCoupling;
	}
	
	public void setMethodClassCoupling(Set<String> methodClassCoupling) {
		this.methodClassCoupling = methodClassCoupling;
	}
	
	public Set<String> getMethodInvokeCoupling() {
		return methodInvokeCoupling;
	}

	public void setMethodInvokeCoupling(Set<String> methodCoupling) {
		this.methodInvokeCoupling = methodCoupling;
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
	
	public Boolean getAccessDatabase() {
		return maxSqlComplexity > 0;
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public void setPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		
		if(!(o instanceof CKMethodResultSpringBatch))
			return false;
		
		CKMethodResultSpringBatch method = (CKMethodResultSpringBatch) o;
						
		return Objects.equals(method.getId(), this.getId());		
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
