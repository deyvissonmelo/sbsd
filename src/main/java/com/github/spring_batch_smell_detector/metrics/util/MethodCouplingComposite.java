package com.github.spring_batch_smell_detector.metrics.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Consumer;

public class MethodCouplingComposite {

	private UUID classId;
	
	private Map<UUID, Map<UUID, MethodCouplingComposite>> methods;

	public MethodCouplingComposite(UUID classId) {
		this.classId = classId;
		methods = new HashMap<>();
	}
	
	public UUID getClassId() {
		return classId;
	}

	public void setClassId(UUID classId) {
		this.classId = classId;
	}

	public Map<UUID, Map<UUID, MethodCouplingComposite>> getMethods() {
		return methods;
	}

	public void setMethods(Map<UUID, Map<UUID, MethodCouplingComposite>> methods) {
		this.methods = methods;
	}
	
	public boolean methodIsDirectInvoked(UUID invokedClassId, UUID invokedMethodId) {
		boolean isInvoked = false;
		
		if(methods.keySet().contains(invokedMethodId)) {
			return false;
		}
		
		for(Entry<UUID, Map<UUID, MethodCouplingComposite>> method : methods.entrySet()) {
			if(!method.getValue().keySet().contains(invokedClassId)) {
				continue;
			}
			
			MethodCouplingComposite invokedClass = method.getValue().get(invokedClassId);
			isInvoked = invokedClass.methods.keySet().contains(invokedMethodId);
			
			if(isInvoked) {
				return isInvoked;
			}
		}
		
		return isInvoked;
	}

	public void preOrder(Consumer<UUID> callback) {				
		for(Map.Entry<UUID, Map<UUID, MethodCouplingComposite>> method : methods.entrySet()) {
			for(Entry<UUID, MethodCouplingComposite> methodCoupling : method.getValue().entrySet()) {
				methodCoupling.getValue().preOrder(callback);
			}			
		}
		
		callback.accept(classId);
	}

}
