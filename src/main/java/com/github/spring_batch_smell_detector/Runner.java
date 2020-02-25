package com.github.spring_batch_smell_detector;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.xml.sax.SAXException;

import com.github.mauricioaniche.ck.CKMethodResult;
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
import com.github.spring_batch_smell_detector.statistics.BatchRoleStatistics;

@SpringBootApplication
public class Runner implements CommandLineRunner {

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
	
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(Runner.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		Options opt = new Options();
		opt.addOption(new Option("s", false, "Calculate statistics"));

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(opt, args);

		if (cmd.hasOption("s")) {
			calculateMetrics(cmd.getArgs());
		} else {
			analyseProject(cmd.getArgs());
		}
	}

	private void analyseProject(String[] args) throws ParserConfigurationException, SAXException, IOException {		
		
		ck.calculate(args[0], args[1], args[2], result -> {
			ckResults.put(result.getId(), result);
		});

		// printSQLQueries(ckResults);
		// printDataBaseReaders(ckResults);

		Map<UUID, Set<UUID>> classCouplingTree = CouplingUtils.initialize(ckResults).getCouplingClassMap();
		System.out.println("*************************CLASS COUPLING******************************");
		printCoupling(ckResults, classCouplingTree);
		System.out.println();
		System.out.println("*************************CLASS METHOD COUPLING******************************");
		printCouplingMethods(CouplingUtils.getLoadedInstance().getCouplingMethodMap(), 0);

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

		reportBuilder
			.printStatistics(true)
			.addReportSection(SmellType.BRAIN_READER, brResult)
			.addReportSection(SmellType.BRAIN_PROCESSOR, bpResult)
			.addReportSection(SmellType.BRAIN_SERVICE, bsResult)
			.addReportSection(SmellType.BRAIN_WRITER, bwResult)
			.addReportSection(SmellType.GLOBAL_PROCESSOR, gpResult)
			.addReportSection(SmellType.AMATEUR_WRITER, awResult)
			.addReportSection(SmellType.IMPROPER_COMMUNICATION, icResult)
			.addReportSection(SmellType.READAHOLIC_COMPONENT, rcResult)
			.print();
		
	}

	private void calculateMetrics(String[] args) throws ParserConfigurationException, SAXException, IOException {
		if (args == null || args.length < 3) {
			System.out.println("Informe os parâmetros de execução");
			System.out.println("java -jar spring_batch_smell_detector <project_path> <job_file_path> <query_fle_path>");
			System.exit(1);
		}

		final String[] FILE_METRIC_HEADER = { "file", "class", "role", "loc", "lcom", "wmc", "maxNeasting", "ficp",
				"sqlComplexity" };

		try (final FileWriter writer = new FileWriter("metrics.csv");
				final CSVPrinter classPrinter = new CSVPrinter(writer,
						CSVFormat.DEFAULT.withHeader(FILE_METRIC_HEADER));) {
			ck.calculate(args[0], args[1], args[2], result -> {
				try {
					classPrinter.printRecord(result.getFile(), result.getClassName(), result.getBatchRole(),
							result.getLoc(), result.getLcom(), result.getWmc(), result.getMaxNestedBlocks(),
							result.getCoupling().size(), result.getMaxSqlComplexity());
				} catch (IOException e) {
					System.out.println("Ocorreu um erro ao criar os arquivo de métricas");
					e.printStackTrace();
				}
			});

			classPrinter.flush();
		}

		final String[] FILE_QUERY_METRIC_HEADER = { "file", "fileType", "key", "queryType", "sqlComplexity" };

		try (final FileWriter queryWriter = new FileWriter("query_metrics.csv");
				final CSVPrinter queryPrinter = new CSVPrinter(queryWriter,
						CSVFormat.DEFAULT.withHeader(FILE_QUERY_METRIC_HEADER));) {
			SQLQueriesFinder.getLoadedInstance().getQueries().forEach((id, query) -> {
				try {
					queryPrinter.printRecord(query.getFilePath(), query.getFileType(), query.getFileKey(), query.getFileType(), query.getComplexity());
				} catch (IOException e) {
					System.out.println("Ocorreu um erro ao criar os arquivo de métricas");
					e.printStackTrace();
				}
			});

			queryPrinter.flush();
		}
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

	private void printCouplingMethods(Map<UUID, MethodCouplingComposite> couplingMethodMap, int tabLevel) {
		final String tab = "\t".repeat(tabLevel);

		if (couplingMethodMap == null)
			return;

		couplingMethodMap.forEach((classId, coupling) -> {
			System.out.println(tab + "Id: " + classId);
			System.out.println(tab + "Class: " + ckResults.get(classId).getClassName());
			System.out.println(tab + "Batch Role: " + ckResults.get(classId).getBatchRole());
			System.out.println(tab + "----------------------------------------------");
			coupling.getMethods().forEach((methodId, methodCouplings) -> {
				System.out.println(
						tab + "\tMethod: " + ckResults.get(classId).getMethodById(methodId).get().getMethodName());
				printCouplingMethods(methodCouplings, tabLevel + 1);
			});
			System.out.println();
		});
	}

	private void printCoupling(Map<UUID, CKClassResultSpringBatch> ckResults, Map<UUID, Set<UUID>> classCouplingTree) {
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
	}

	private void printStatistics(BatchRoleStatistics batchRoleStatistics) {
		System.out.println(batchRoleStatistics.getRole().name() + " Statistics:");

		batchRoleStatistics.getMetricStatistics().forEach((metric, statistics) -> {
			System.out.println(metric.name());
			System.out.println("---------------------------");
			System.out.println("MIN: " + batchRoleStatistics.getMetricStatistic(metric).getMin());
			System.out.println("AVERAGE: " + batchRoleStatistics.getMetricStatistic(metric).getAverage());
			System.out.println("MAX: " + batchRoleStatistics.getMetricStatistic(metric).getMax());
			System.out
					.println("STANDARD DEVIATION: " + batchRoleStatistics.getMetricStatistic(metric).getStdDeviation());
			System.out.println("HIGHER MARGIN: " + batchRoleStatistics.getMetricStatistic(metric).getHigherMargin());
			System.out.println(
					"VERY HIGHER MARGIN: " + batchRoleStatistics.getMetricStatistic(metric).getVeryHigherMargin());
			System.out.println();
		});
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

	private static void printResults(CKClassResultSpringBatch result) {
		System.out.println("Class:");
		System.out.println(result.getClassName());
		System.out.println();
		System.out.println("Papeis arquiteturais:");
		System.out.println(result.getBatchRole());
		System.out.println("SQL Class");
		System.out.println(result.getSqlQueries());
		System.out.println(result.getMaxSqlComplexity());
		System.out.println("SQL Method");
		result.getMethods().forEach(m -> {
			System.out.println(((CKMethodResultSpringBatch) m).getSqlQueries());
			System.out.println(((CKMethodResultSpringBatch) m).getMaxSqlComplexity());
		});

		System.out.println("--------------------------------------");
		System.out.println();
		System.out.println(String.format("Coupling: %d", result.getCbo()));
		result.getCoupling().stream().forEach(c -> System.out.println(c));
		System.out.println();
		System.out.println("Referências a métodos:");
		for (CKMethodResult m : result.getMethods()) {
			System.out.println("Método: " + m.getMethodName());
			System.out.println("--------------------------------------");
			((CKMethodResultSpringBatch) m).getMethodInvokeCoupling().stream().forEach(c -> System.out.println(c));
			System.out.println();
		}
		System.out.println("--------------------------------------");
		System.out.println();
		System.out.println();
	}
}
