package com.github.spring_batch_smell_detector.metrics.metric;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WildcardType;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.mauricioaniche.ck.metric.ClassLevelMetric;
import com.github.mauricioaniche.ck.metric.MethodLevelMetric;
import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKMethodResultSpringBatch;

public class ReferencedClasses extends ASTVisitor implements ClassLevelMetric, MethodLevelMetric {

	private Set<String> coupling = new HashSet<>();

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		coupleTo(node.getType());
		return super.visit(node);
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		coupleTo(node.getType());
		return super.visit(node);
	}

	@Override
	public boolean visit(ArrayCreation node) {
		coupleTo(node.getType());
		return super.visit(node);
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		coupleTo(node.getType());
		return super.visit(node);
	}

	public boolean visit(ReturnStatement node) {
		if (node.getExpression() != null) {
			coupleTo(node.getExpression().resolveTypeBinding());
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(TypeLiteral node) {
		coupleTo(node.getType());
		return super.visit(node);
	}

	@Override
	public boolean visit(ThrowStatement node) {
		if (node.getExpression() != null)
			coupleTo(node.getExpression().resolveTypeBinding());
		return super.visit(node);
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		ITypeBinding resolvedType = node.resolveBinding();

		if (resolvedType != null) {
			ITypeBinding binding = resolvedType.getSuperclass();
			if (binding != null)
				coupleTo(binding);

			for (ITypeBinding interfaces : resolvedType.getInterfaces()) {
				coupleTo(interfaces);
			}
		} else {
			coupleTo(node.getSuperclassType());
			List<Type> list = node.superInterfaceTypes();
			list.forEach(x -> coupleTo(x));
		}

		return super.visit(node);
	}

	@Override
	public boolean visit(MethodDeclaration node) {

		IMethodBinding resolvedMethod = node.resolveBinding();
		if (resolvedMethod != null) {

			coupleTo(resolvedMethod.getReturnType());

			for (ITypeBinding param : resolvedMethod.getParameterTypes()) {
				coupleTo(param);
			}
		} else {
			coupleTo(node.getReturnType2());
			List<TypeParameter> list = node.typeParameters();
			list.forEach(x -> coupleTo(x.getName()));
		}

		return super.visit(node);
	}

	@Override
	public boolean visit(CastExpression node) {
		coupleTo(node.getType());

		return super.visit(node);
	}

	@Override
	public boolean visit(InstanceofExpression node) {

		coupleTo(node.getRightOperand());
		coupleTo(node.getLeftOperand().resolveTypeBinding());

		return super.visit(node);
	}

	@Override
	public boolean visit(MethodInvocation node) {

		IMethodBinding binding = node.resolveMethodBinding();
		if (binding != null)
			coupleTo(binding.getDeclaringClass());

		return super.visit(node);
	}

	@Override
	public boolean visit(NormalAnnotation node) {
		coupleTo(node);
		return super.visit(node);
	}

	@Override
	public boolean visit(MarkerAnnotation node) {
		coupleTo(node);
		return super.visit(node);
	}

	@Override
	public boolean visit(SingleMemberAnnotation node) {
		coupleTo(node);
		return super.visit(node);
	}

	@Override
	public boolean visit(ParameterizedType node) {
		ITypeBinding binding = node.resolveBinding();
		if (binding != null) {

			coupleTo(binding);

			for (ITypeBinding types : binding.getTypeArguments()) {
				coupleTo(types);
			}
		} else {
			coupleTo(node.getType());
		}

		return super.visit(node);
	}

	private void coupleTo(Annotation type) {
		ITypeBinding resolvedType = type.resolveTypeBinding();
		if (resolvedType != null)
			coupleTo(resolvedType);
		else {
			addToSet(type.getTypeName().getFullyQualifiedName());
		}
	}

	private void coupleTo(Type type) {
		if (type == null)
			return;

		ITypeBinding resolvedBinding = type.resolveBinding();
		if (resolvedBinding != null)
			coupleTo(resolvedBinding);
		else {
			if (type instanceof SimpleType) {
				SimpleType castedType = (SimpleType) type;
				addToSet(castedType.getName().getFullyQualifiedName());
			} else if (type instanceof QualifiedType) {
				QualifiedType castedType = (QualifiedType) type;
				addToSet(castedType.getName().getFullyQualifiedName());
			} else if (type instanceof NameQualifiedType) {
				NameQualifiedType castedType = (NameQualifiedType) type;
				addToSet(castedType.getName().getFullyQualifiedName());
			} else if (type instanceof ParameterizedType) {
				ParameterizedType castedType = (ParameterizedType) type;
				coupleTo(castedType.getType());
			} else if (type instanceof WildcardType) {
				WildcardType castedType = (WildcardType) type;
				coupleTo(castedType.getBound());
			} else if (type instanceof ArrayType) {
				ArrayType castedType = (ArrayType) type;
				coupleTo(castedType.getElementType());
			} else if (type instanceof IntersectionType) {
				IntersectionType castedType = (IntersectionType) type;
				List<Type> types = castedType.types();
				types.stream().forEach(x -> coupleTo(x));
			} else if (type instanceof UnionType) {
				UnionType castedType = (UnionType) type;
				List<Type> types = castedType.types();
				types.stream().forEach(x -> coupleTo(x));
			}
		}
	}

	private void coupleTo(SimpleName name) {
		addToSet(name.getFullyQualifiedName());
	}

	private void coupleTo(ITypeBinding binding) {
		if (binding == null)
			return;
		if (binding.isWildcardType())
			return;

		if (!binding.isPrimitive() && binding.isFromSource()) {
			String type = binding.getTypeDeclaration().getPackage().getName() + "." + binding.getTypeDeclaration().getName();

			if (type.equals("null"))
				return;

			if (!isFromJava(type) && !binding.isPrimitive())
				addToSet(type.replace("[]", ""));
		}
	}

	private boolean isFromJava(String type) {
		return type.startsWith("java.") || type.startsWith("javax.");
	}

	private void addToSet(String name) {
		this.coupling.add(name);
	}

	// given that some resolvings might fail, we remove types that might
	// had appeared here twice.
	// e.g. if the set contains 'A.B.Class' and 'Class', it is likely that
	// 'Class' == 'A.B.Class'
	private void clean() {
		Set<String> singleQualifiedTypes = coupling.stream().filter(x -> !x.contains(".")).collect(Collectors.toSet());

		for (String singleQualifiedType : singleQualifiedTypes) {
			long count = coupling.stream().filter(x -> x.endsWith("." + singleQualifiedType)).count();

			boolean theSameFullyQualifiedTypeExists = count > 0;
			if (theSameFullyQualifiedTypeExists)
				coupling.remove(singleQualifiedType);
		}
	}

	@Override
	public void setResult(CKClassResult result) {
		if (result instanceof CKClassResultSpringBatch)
			((CKClassResultSpringBatch) result).setCoupling(coupling);
	}

	@Override
	public void setResult(CKMethodResult result) {
		if (result instanceof CKMethodResultSpringBatch)
			((CKMethodResultSpringBatch) result).setMethodClassCoupling(coupling);
	}

}
