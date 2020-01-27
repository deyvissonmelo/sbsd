package com.github.spring_batch_smell_detector.metrics.metric;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.metric.ClassLevelMetric;
import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.model.BatchRole;

public class ArchtectureRole extends ASTVisitor implements ClassLevelMetric {

	private Set<BatchRole> roles = new HashSet<>();

	@Override
	public boolean visit(TypeDeclaration node) {
		ITypeBinding resolvedType = node.resolveBinding();

		if (resolvedType != null) {
			ITypeBinding binding = resolvedType.getSuperclass();

			if (binding != null) {
				Stream.of(BatchRole.values()).forEach(br -> {
					try {
						if (br.getSpringClasses().contains(binding.getName())) {
							this.roles.add(br);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}

			for (ITypeBinding i : resolvedType.getInterfaces()) {				
				int index = i.getName().indexOf("<");				
				String interfaceName = index == -1 ? i.getName() : i.getName().substring(0, index);
				
				Stream.of(BatchRole.values()).forEach(br -> {
					try {
						if (br.getSpringClasses().contains(interfaceName)) {
							this.roles.add(br);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
		} else {
			List<Type> typeList = node.superInterfaceTypes();

			for (Type type : typeList) {
				int index = type.getClass().getName().indexOf("<");				
				String interfaceName = index == -1 ? 
						type.getClass().getName() : type.getClass().getName().substring(0, index);
				
				Stream.of(BatchRole.values()).forEach(br -> {
					try {
						if (br.getSpringClasses().contains(interfaceName)) {
							this.roles.add(br);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
		}

		return super.visit(node);
	}

	@Override
	public void setResult(CKClassResult result) {
		if (result instanceof CKClassResultSpringBatch) {
			((CKClassResultSpringBatch) result).setBatchRole(roles);
		}

	}

}
