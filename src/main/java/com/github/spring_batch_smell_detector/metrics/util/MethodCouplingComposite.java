package com.github.spring_batch_smell_detector.metrics.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class MethodCouplingComposite {

	private UUID classId;
	
	private Map<UUID, Set<MethodCouplingComposite>> methods;

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

	public Map<UUID, Set<MethodCouplingComposite>> getMethods() {
		return methods;
	}

	public void setMethods(Map<UUID, Set<MethodCouplingComposite>> methods) {
		this.methods = methods;
	}
	
	public boolean methodIsDirectInvoked(UUID invokedClassId, UUID invokedMethodId) {
		boolean isInvoked = false;
		
		//Não considerar métodos da própria classe
		if(methods.keySet().contains(invokedMethodId)) {
			return false;
		}
		
		for(Entry<UUID, Set<MethodCouplingComposite>> method : methods.entrySet()) {
			
			//Verificar se no método existe referência para a classe passada
			Optional<MethodCouplingComposite> methodCoupling = method.getValue().stream().filter(
					coupling -> coupling.getClassId().equals(invokedClassId)).findFirst();
			
			if(methodCoupling.isEmpty()) {
				continue;
			}
			
			isInvoked = methodCoupling.get().getMethods().keySet().contains(invokedMethodId);
					
			if(isInvoked) {
				break;
			}
		}
		
		return isInvoked;
	}

	public void preOrder(Consumer<UUID> callback) {				
		for(Map.Entry<UUID, Set<MethodCouplingComposite>> method : methods.entrySet()) {
			for(MethodCouplingComposite methodCoupling : method.getValue()) {
				methodCoupling.preOrder(callback);
			}			
		}
		
		callback.accept(classId);
	}
	
	@Override
	public int hashCode() {
		return this.getClassId().hashCode();
	}

}
