package com.github.spring_batch_smell_detector.calculators;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKMethodResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.CouplingUtils;
import com.github.spring_batch_smell_detector.metrics.util.MethodCouplingComposite;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.github.spring_batch_smell_detector.model.SmellType;
import com.github.spring_batch_smell_detector.report.SmellReportBuilder;
import com.github.spring_batch_smell_detector.smells.AmateurWriter;
import com.github.spring_batch_smell_detector.smells.BrainProcessor;
import com.github.spring_batch_smell_detector.smells.BrainReader;
import com.github.spring_batch_smell_detector.smells.BrainService;
import com.github.spring_batch_smell_detector.smells.BrainWriter;
import com.github.spring_batch_smell_detector.smells.GlobalProcessor;
import com.github.spring_batch_smell_detector.smells.ImproperCommunication;
import com.github.spring_batch_smell_detector.smells.ReadaholicComponent;
import com.google.common.base.Strings;

@Component
public class SmellCalculator {

	private final String[] FILE_EXECUTION_HEADER = { "file", "class", "smell" };
	
	private Map<UUID, CKClassResultSpringBatch> ckResults = new HashMap<>();
	
	@Autowired
	private CKSpringBatch ck;

	@Autowired
	private GlobalProcessor globalProcessor;

	@Autowired
	private BrainReader brainReader;

	@Autowired
	private BrainProcessor brainProcessor;

	@Autowired
	private BrainService brainService;

	@Autowired
	private BrainWriter brainWriter;

	@Autowired
	private AmateurWriter amateurWriter;

	@Autowired
	private ImproperCommunication improperCommunication;

	@Autowired
	private ReadaholicComponent readaholicComponent;
	
	@Autowired
	private SmellReportBuilder reportBuilder;
	
	@SuppressWarnings("rawtypes")
	public void calculate(String path, String jobPath, String alternativeSqlPath, boolean verbose, boolean batch)
			throws ParserConfigurationException, SAXException, IOException {

		if (Strings.isNullOrEmpty(path) || Strings.isNullOrEmpty(jobPath) || Strings.isNullOrEmpty(alternativeSqlPath))
			throw new RuntimeException(
					"Um ou mais parâmetros de localização para os arquivos do projeto não foram informados.");
		
		ck.calculate(path, jobPath, alternativeSqlPath, result -> {
			ckResults.put(result.getId(), result);
		});

		Map<UUID, Set<UUID>> classCouplingTree = CouplingUtils.initialize(ckResults).getCouplingClassMap();

		if (verbose) {
			printProcessInfo(classCouplingTree);
		}

		Set<UUID> awResult = amateurWriter.analyse(ckResults);
		Set<UUID> bpResult = brainProcessor.analyse(ckResults);
		Set<UUID> bsResult = brainService.analyse(ckResults);
		Set<UUID> brResult = brainReader.analyse(ckResults);
		Set<UUID> bwResult = brainWriter.analyse(ckResults);
		Set<UUID> gpResult = globalProcessor.analyse(ckResults);
		Set<UUID> rcResult = readaholicComponent.analyse(ckResults);
		Set<UUID> icResult = improperCommunication.analyse(ckResults);

		System.out.println("Global Processor: " + gpResult);
		System.out.println("Brain Reader: " + brResult);
		System.out.println("Brain Processor" + bpResult);
		System.out.println("Brain Processor" + bsResult);
		System.out.println("Brain Writer" + bwResult);
		System.out.println("Readaholic Component" + rcResult);
		System.out.println("Amateur Writer" + awResult);
		System.out.println("Improper Communication" + icResult);

		reportBuilder.printStatistics(true).addReportSection(SmellType.BRAIN_READER, brResult)
				.addReportSection(SmellType.BRAIN_PROCESSOR, bpResult)
				.addReportSection(SmellType.BRAIN_SERVICE, bsResult).addReportSection(SmellType.BRAIN_WRITER, bwResult)
				.addReportSection(SmellType.GLOBAL_PROCESSOR, gpResult)
				.addReportSection(SmellType.AMATEUR_WRITER, awResult)
				.addReportSection(SmellType.IMPROPER_COMMUNICATION, icResult)
				.addReportSection(SmellType.READAHOLIC_COMPONENT, rcResult).print();

		if (batch) {
			Map<Class, Set<UUID>> results = new HashMap<Class, Set<UUID>>();

			results.put(AmateurWriter.class, awResult);
			results.put(BrainReader.class, brResult);
			results.put(BrainProcessor.class, bpResult);
			results.put(BrainService.class, bsResult);
			results.put(BrainWriter.class, bwResult);
			results.put(GlobalProcessor.class, gpResult);
			results.put(ReadaholicComponent.class, rcResult);
			results.put(ImproperCommunication.class, icResult);

			registerExecution(results);
		}

	}
	
	@SuppressWarnings("rawtypes")
	private void registerExecution(Map<Class, Set<UUID>> results) throws IOException {		

		try (
				final FileWriter writer = new FileWriter("execution.csv");
				final CSVPrinter printer = new CSVPrinter(writer,
						CSVFormat.DEFAULT.withHeader(FILE_EXECUTION_HEADER));
		) {
			results.forEach((smellClass, smellResults) ->

				smellResults.forEach(id -> {
					CKClassResultSpringBatch ckClassResult = CouplingUtils.getLoadedInstance().getCKClassResult(id);

					if (ckClassResult != null) {
						try {
							printer.printRecord(ckClassResult.getFile(), ckClassResult.getClassName(),
									smellClass.getSimpleName());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				})
			);

			printer.flush();
		}
	}
	
	private void printProcessInfo(Map<UUID, Set<UUID>> classCouplingTree) {
		printSQLQueries(ckResults);
		printDataBaseReaders(ckResults);
		printCoupling(ckResults, classCouplingTree);
		printCouplingMethods(CouplingUtils.getLoadedInstance().getCouplingMethodMap().values(), 0);
	}

	private void printSQLQueries(Map<UUID, CKClassResultSpringBatch> classResults) {
		System.out.println("SQL Queries:");
		System.out.println("----------------------------------------------");

		classResults.forEach((id, result) -> {
			System.out.println("Class: " + result.getClassName());
			System.out.println("----------------------------------------------");
			System.out.println("Queries:");
			result.getSqlQueries().forEach(key -> {
				System.out.println("\tkey: " + key);
				System.out.println(
						"\tFile Key: " + SQLQueriesFinder.getLoadedInstance().getQueries().get(key).getFileKey());
				System.out.println("\tType: " + SQLQueriesFinder.getLoadedInstance().getQueries().get(key).getType());
				System.out.println("\t" + SQLQueriesFinder.getLoadedInstance().getQueries().get(key).getQuery());
			});

			result.getMethods().forEach(method -> {
				System.out.println("\tMethod: " + method.getMethodName());
				System.out.println("\tAccess Database: " + ((CKMethodResultSpringBatch) method).getAccessDatabase());
				System.out.println("\t----------------------------------------------");
				System.out.println("\tQueries:");
				((CKMethodResultSpringBatch) method).getSqlQueries().forEach(key -> {
					System.out.println("\tkey: " + key);
					System.out.println(
							"\tFile Key: " + SQLQueriesFinder.getLoadedInstance().getQueries().get(key).getFileKey());
					System.out
							.println("\tType: " + SQLQueriesFinder.getLoadedInstance().getQueries().get(key).getType());
					System.out.println("\t" + SQLQueriesFinder.getLoadedInstance().getQueries().get(key).getQuery());
				});
			});
		});
	}
	
	private void printDataBaseReaders(Map<UUID, CKClassResultSpringBatch> ckResults2) {
		ckResults.forEach((key, classResult) -> {

			if (!classResult.getSqlQueries().isEmpty()) {
				System.out.println(classResult.getClassName());
			}

			classResult.getMethods().forEach(methodResult -> {
				if (!((CKMethodResultSpringBatch) methodResult).getSqlQueries().isEmpty()) {
					System.out.println(methodResult.getMethodName());
				}
			});

		});
	}
	
	private void printCoupling(Map<UUID, CKClassResultSpringBatch> ckResults, Map<UUID, Set<UUID>> classCouplingTree) {
		System.out.println("*************************CLASSCOUPLING******************************");
		
		for (Map.Entry<UUID, Set<UUID>> node : classCouplingTree.entrySet()) {
			System.out.println(node.getKey() + ":" + ckResults.get(node.getKey()).getClassName());

			if (!node.getValue().isEmpty()) {
				System.out.println("Childs --------------------------------------");

				for (UUID childId : node.getValue()) {
					System.out.println(childId + ":" + ckResults.get(childId).getClassName());
				}

				System.out.println("End Childs --------------------------------------");
			}
		}
		
		System.out.println();
	}
	
	private void printCouplingMethods(Collection<MethodCouplingComposite> couplings, int tabLevel) {
		System.out.println("*************************CLASS METHOD COUPLING******************************");
		
		final String tab = "\t".repeat(tabLevel);

		if (couplings == null)
			return;

		couplings.forEach(coupling -> {
			UUID classId = coupling.getClassId();

			System.out.println(tab + "Id: " + classId);
			System.out.println(tab + "Class: " + ckResults.get(classId).getClassName());
			System.out.println(tab + "Batch Role: " + ckResults.get(classId).getBatchRole());
			System.out.println(tab + "Number of Methods: " + coupling.getMethods().size());
			System.out.println(tab + "----------------------------------------------");
			coupling.getMethods().forEach((methodId, methodCouplings) -> {
				System.out.println(
						tab + "\tMethod: " + ckResults.get(classId).getMethodById(methodId).get().getMethodName());

				if (methodCouplings.size() > 0) {
					System.out.println(tab + "\tCouplings (" + methodCouplings.size() + " classes):");
					System.out.println(tab + "\t----------------------------------------------");

					printCouplingMethods(methodCouplings, tabLevel + 2);
				}
			});
			System.out.println();
		});
	}
	
}
