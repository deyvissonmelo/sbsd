package com.github.spring_batch_smell_detector.metrics.metric;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.mauricioaniche.ck.metric.MethodLevelMetric;
import com.github.spring_batch_smell_detector.metrics.CKMethodResultSpringBatch;

public class ReferencedMethods extends ASTVisitor implements MethodLevelMetric {

	private Set<String> coupling = new HashSet<>();

	@Override
	public boolean visit(MethodInvocation node) {
		String methodClassName = "";
		String methodName = node.getName().getFullyQualifiedName();
		
		IMethodBinding binding = node.resolveMethodBinding();
		if(binding!=null) {
			methodClassName = binding.getDeclaringClass().getQualifiedName();
			
			if(!isFromJava(methodClassName) && !binding.getDeclaringClass().isPrimitive())
				coupling.add(methodClassName + ":" + methodName);
		}
			
		return true;
	}
	
	private boolean isFromJava(String type) {
		return type.startsWith("java.") || type.startsWith("javax.");
	}
	
	@Override
	public void setResult(CKMethodResult result) {
		if(result instanceof CKMethodResultSpringBatch) {
			((CKMethodResultSpringBatch) result).setMethodInvokeCoupling(coupling);
		}
	}

}
