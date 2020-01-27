package com.github.spring_batch_smell_detector.metrics;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.github.mauricioaniche.ck.metric.ClassLevelMetric;
import com.github.mauricioaniche.ck.metric.MethodLevelMetric;
import com.github.mauricioaniche.ck.util.FileUtils;
import com.github.spring_batch_smell_detector.metrics.util.MetricsFinderSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.google.common.collect.Lists;

@Service
public class CKSpringBatch {

	private static final int MAX_AT_ONCE;

	static {
		String jdtMax = System.getProperty("jdt.max");
		if (jdtMax != null) {
			MAX_AT_ONCE = Integer.parseInt(jdtMax);
		} else {
			long maxMemory = Runtime.getRuntime().maxMemory() / (1 << 20); // in MiB

			if (maxMemory >= 2000)
				MAX_AT_ONCE = 400;
			else if (maxMemory >= 1500)
				MAX_AT_ONCE = 300;
			else if (maxMemory >= 1000)
				MAX_AT_ONCE = 200;
			else if (maxMemory >= 500)
				MAX_AT_ONCE = 100;
			else
				MAX_AT_ONCE = 25;
		}
	}

	Callable<List<ClassLevelMetric>> classLevelMetrics;
	Callable<List<MethodLevelMetric>> methodLevelMetrics;

	public CKSpringBatch() {
		MetricsFinderSpringBatch finder = new MetricsFinderSpringBatch();

		this.classLevelMetrics = () -> finder.allClassLevelMetrics();
		this.methodLevelMetrics = () -> finder.allMethodLevelMetrics();
	}

	public void calculate(String path, String jobPath, String alternativeSqlPath, CKNotifierSpringBatch notifier)
			throws ParserConfigurationException, SAXException, IOException {
		String[] srcDirs = FileUtils.getAllDirs(path);
		String[] javaFiles = FileUtils.getAllJavaFiles(path);

		// Carregando sql do projeto
		SQLQueriesFinder.initialize(jobPath, alternativeSqlPath);

		MetricsExecutorSpringBatch storage = new MetricsExecutorSpringBatch(classLevelMetrics, methodLevelMetrics,
				notifier);

		List<List<String>> partitions = Lists.partition(Arrays.asList(javaFiles), MAX_AT_ONCE);

		for (List<String> partition : partitions) {
			ASTParser parser = ASTParser.newParser(AST.JLS11);

			parser.setResolveBindings(true);
			parser.setBindingsRecovery(true);

			Map<String, String> options = JavaCore.getOptions();
			JavaCore.setComplianceOptions(JavaCore.VERSION_11, options);
			parser.setCompilerOptions(options);
			parser.setEnvironment(null, srcDirs, null, true);
			parser.createASTs(partition.toArray(new String[partition.size()]), null, new String[0], storage, null);
		}
	}
}
