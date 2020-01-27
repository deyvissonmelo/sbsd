package com.github.spring_batch_smell_detector.metrics;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import com.github.mauricioaniche.ck.metric.ClassLevelMetric;
import com.github.mauricioaniche.ck.metric.MethodLevelMetric;

public class MetricsExecutorSpringBatch extends FileASTRequestor {

	private Callable<List<ClassLevelMetric>> classLevelMetrics;
	private Callable<List<MethodLevelMetric>> methodLevelMetrics;
	private CKNotifierSpringBatch notifier;

	private static Logger log = Logger.getLogger(MetricsExecutorSpringBatch.class);

	public MetricsExecutorSpringBatch(Callable<List<ClassLevelMetric>> classLevelMetrics,
			Callable<List<MethodLevelMetric>> methodLevelMetrics, CKNotifierSpringBatch notifier) {
		this.classLevelMetrics = classLevelMetrics;
		this.methodLevelMetrics = methodLevelMetrics;
		this.notifier = notifier;
	}

	@Override
	public void acceptAST(String sourceFilePath, CompilationUnit cu) {

		try {
			CKVisitorSpringBatch visitor = new CKVisitorSpringBatch(sourceFilePath, cu, classLevelMetrics,
					methodLevelMetrics);

			cu.accept(visitor);
			Set<CKClassResultSpringBatch> collectedClasses = visitor.getCollectedClassesSpringBatch();

			for (CKClassResultSpringBatch collectedClass : collectedClasses) {
				log.info(collectedClass);
				notifier.notify(collectedClass);
			}
		} catch (Exception e) {
			log.error("error in " + sourceFilePath, e);
		}
	}

}
