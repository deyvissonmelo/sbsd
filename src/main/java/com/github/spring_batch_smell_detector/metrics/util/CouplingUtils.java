package com.github.spring_batch_smell_detector.metrics.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKMethodResultSpringBatch;

public class CouplingUtils {

	private Map<UUID, Set<UUID>> couplingClassMap;

	private Map<UUID, MethodCouplingComposite> couplingMethodMap;

	private Map<UUID, CKClassResultSpringBatch> ckResults;

	private static CouplingUtils instance;

	private CouplingUtils(Map<UUID, CKClassResultSpringBatch> ckResults) {
		this.ckResults = ckResults;
		this.couplingClassMap = buildCouplingClassMap();
		this.couplingMethodMap = buildCouplingMethodMap();
	}

	public static CouplingUtils initialize(Map<UUID, CKClassResultSpringBatch> ckResults) {
		if (instance == null) {
			instance = new CouplingUtils(ckResults);
		}

		return instance;
	}

	public static CouplingUtils getLoadedInstance() {
		if (instance == null) {
			throw new RuntimeException("CouplingUtils não foi inicializado.");
		}

		return instance;
	}

	public CKClassResultSpringBatch getCKClassResult(UUID id) {
		return ckResults.get(id);
	}

	public Map<UUID, Set<UUID>> getCouplingClassMap() {
		return couplingClassMap;
	}

	public Set<UUID> getClassCoupling(UUID classId) {
		return couplingClassMap.get(classId);
	}

	public void setCouplingClassMap(Map<UUID, Set<UUID>> couplingClassMap) {
		this.couplingClassMap = couplingClassMap;
	}

	public Map<UUID, MethodCouplingComposite> getCouplingMethodMap() {
		return couplingMethodMap;
	}

	public MethodCouplingComposite getMethodCoupling(UUID classId) {
		return couplingMethodMap.get(classId);
	}

	private Map<UUID, Set<UUID>> buildCouplingClassMap() {
		Map<UUID, Set<UUID>> couplingTree = new HashMap<>();

		ckResults.forEach((id, classResult) -> {
			couplingTree.put(id, extractClassCouplings(classResult));
		});

		return couplingTree;
	}

	private Set<UUID> extractClassCouplings(CKClassResultSpringBatch index) {
		Set<UUID> couplings = new HashSet<>();
		int couplingCount = index.getCoupling().size();

		if (couplingCount > 0) {
			for (Map.Entry<UUID, CKClassResultSpringBatch> entry : ckResults.entrySet()) {
				if (couplingCount == 0) {
					break;
				}

				if (entry.getKey().equals(index.getId())) {
					continue;
				}

				boolean isCoupling = index.getCoupling().stream()
						.anyMatch(c -> c.contains(entry.getValue().getClassName()));

				if (isCoupling) {
					--couplingCount;
					couplings.add(entry.getKey());
				}
			}
		}

		index.getMethods().forEach(method -> {
			CKMethodResultSpringBatch m = (CKMethodResultSpringBatch) method;

			int methodCouplingCount = m.getMethodClassCoupling().size();

			if (methodCouplingCount > 0) {
				for (Map.Entry<UUID, CKClassResultSpringBatch> entry : ckResults.entrySet()) {
					if (methodCouplingCount == 0) {
						break;
					}

					if (entry.getKey().equals(index.getId())) {
						continue;
					}

					boolean isCoupling = m.getMethodClassCoupling().stream().anyMatch(c -> {
						String className = extractClassName(entry.getValue().getClassName());
						return c.contains(className);
					});

					if (isCoupling) {
						--methodCouplingCount;
						couplings.add(entry.getKey());
					}
				}
			}

		});

		return couplings;
	}

	private Map<UUID, MethodCouplingComposite> buildCouplingMethodMap() {
		Map<UUID, MethodCouplingComposite> invocationMap = new HashMap<>();

		ckResults.forEach((invokerId, invokerClass) -> {
			invocationMap.put(invokerId, new MethodCouplingComposite(invokerId));

			invokerClass.getMethods().forEach(
				invokerMethod -> {
					Set<MethodCouplingComposite> invokedClassMethods = 
							extractMethodCouplings((CKMethodResultSpringBatch) invokerMethod);
					
					invocationMap.get(invokerId).getMethods().put(
							((CKMethodResultSpringBatch) invokerMethod).getId(),
							invokedClassMethods
					);
				}
			);
		});

		return invocationMap;
	}

	private Set<MethodCouplingComposite> extractMethodCouplings(CKMethodResultSpringBatch invokerMethod) {
		return extractMethodCouplings(invokerMethod, null);
	}
	
	private Set<MethodCouplingComposite> extractMethodCouplings(CKMethodResultSpringBatch invokedMethod,
			List<UUID> chainCalledMethods) {
		Set<MethodCouplingComposite> calledMethods = new HashSet<>();
		int couplingCount = invokedMethod.getMethodInvokeCoupling().size();

		if(couplingCount == 0) {
			return calledMethods;
		}
		
		if(chainCalledMethods == null) {
			chainCalledMethods = new ArrayList<UUID>();
		}
		
		if(chainCalledMethods.contains(invokedMethod.getId())) {
			return calledMethods;
		}
		else {
			chainCalledMethods.add(invokedMethod.getId());
		}
		
		for (Map.Entry<UUID, CKClassResultSpringBatch> entry : ckResults.entrySet()) {
			if (couplingCount == 0) {
				break;
			}

			for (CKMethodResult entryMethod : entry.getValue().getMethods()) {
						
				if(chainCalledMethods.contains(((CKMethodResultSpringBatch)entryMethod).getId())) {
					continue;
				}
				
				String className = entry.getValue().getClassName();
				String methodName = extractMethodName(entryMethod.getMethodName());
				String fullMethodName = className + ":" + methodName;
				
				boolean isCoupling = invokedMethod.getMethodInvokeCoupling().stream()
						.anyMatch(c -> c.equals(fullMethodName));

				if (isCoupling) {
					
					UUID methodKey = ((CKMethodResultSpringBatch) entryMethod).getId();

					Optional<MethodCouplingComposite> targetCoupling = Optional.ofNullable(null);
					
					targetCoupling = calledMethods.stream().filter(c -> c.getClassId().equals(entry.getKey())).findFirst();
					
					if(targetCoupling.isEmpty()) {
						targetCoupling = Optional.of(new MethodCouplingComposite(entry.getKey()));						
						calledMethods.add(targetCoupling.get());						
					}
									
					targetCoupling.get().getMethods().put(
							methodKey, 
							extractMethodCouplings(
									(CKMethodResultSpringBatch) entryMethod,
									chainCalledMethods
							)
					);					

					--couplingCount;
					
					int index = chainCalledMethods.indexOf(invokedMethod.getId());
					chainCalledMethods = chainCalledMethods.subList(0, index + 1);
				}
			}
		}

		return calledMethods;
	}

	private String extractClassName(String className) {
		int delimiterIndex = className.indexOf("$");

		if (delimiterIndex == -1) {
			return className;
		} else {
			return className.substring(0, delimiterIndex);
		}
	}

	private String extractMethodName(String methodName) {
		int delimiterIndex = methodName.indexOf("/");

		if (delimiterIndex == -1) {
			return methodName;
		} else {
			return methodName.substring(0, delimiterIndex);
		}
	}
}
