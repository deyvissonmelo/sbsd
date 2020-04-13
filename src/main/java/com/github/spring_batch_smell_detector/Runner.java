package com.github.spring_batch_smell_detector;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
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

import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKMethodResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.CouplingUtils;
import com.github.spring_batch_smell_detector.metrics.util.MethodCouplingComposite;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQuery;
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
		opt.addOption(new Option("v", false, "Verbose"));
		opt.addOption(new Option("b", false, "Batch"));

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(opt, args);

		if (cmd.hasOption("s")) {
			calculateMetrics(cmd.getArgs(), cmd.hasOption('v'));
		} else {
			analyseProject(cmd.getArgs(), cmd.hasOption('v'), cmd.hasOption('b'));
		}
	}

	private void analyseProject(String[] args, boolean verbose, boolean batch)
			throws ParserConfigurationException, SAXException, IOException {

		ck.calculate(args[0], args[1], args[2], result -> {
			ckResults.put(result.getId(), result);
		});

		Map<UUID, Set<UUID>> classCouplingTree = CouplingUtils.initialize(ckResults).getCouplingClassMap();

		if (verbose) {
			printSQLQueries(ckResults);
			printDataBaseReaders(ckResults);
			System.out.println("*************************CLASSCOUPLING******************************");
			printCoupling(ckResults, classCouplingTree);
			System.out.println();
			System.out.println("*************************CLASS METHOD COUPLING******************************");
			printCouplingMethods(CouplingUtils.getLoadedInstance().getCouplingMethodMap().values(), 0);
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

	private void registerExecution(Map<Class, Set<UUID>> results) throws IOException {
		final String[] FILE_EXECUTION_HEADER = { "file", "class", "smell" };

		try (final FileWriter writer = new FileWriter("execution.csv");
				final CSVPrinter printer = new CSVPrinter(writer,
						CSVFormat.DEFAULT.withHeader(FILE_EXECUTION_HEADER));) {
			results.forEach((smellClass, smellResults) -> {

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
				});

			});

			printer.flush();
		}
	}

	private void calculateMetrics(String[] args, boolean verbose)
			throws ParserConfigurationException, SAXException, IOException {
		if (args == null || args.length < 3) {
			System.out.println("Informe os parâmetros de execução");
			System.out.println(
					"java -jar spring_batch_smell_detector -s <project_path> <job_file_path> <query_fle_path>");
			System.exit(1);
		}

		final String[] FILE_METRIC_HEADER = { "file", "class", "role", "methods", "loc", "lcom", "wmc", "maxNeasting",
				"ficp", "sqlComplexity" };

		try (final FileWriter writer = new FileWriter("metrics.csv");
				final CSVPrinter classPrinter = new CSVPrinter(writer,
						CSVFormat.DEFAULT.withHeader(FILE_METRIC_HEADER));) {
			ck.calculate(args[0], args[1], args[2], result -> {
				try {
					if (verbose) {
						printStatistics(result);
					}

					classPrinter.printRecord(result.getFile(), result.getClassName(), result.getBatchRole(),
							result.getNumberOfMethods(), result.getLoc(), result.getLcom(), result.getWmc(),
							result.getMaxNestedBlocks(), result.getCoupling().size(), result.getMaxSqlComplexity());
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
					if (verbose) {
						printQueryStatistics(query);
					}

					queryPrinter.printRecord(query.getFilePath(), query.getFileType(), query.getFileKey(),
							query.getType(), query.getComplexity());
				} catch (IOException e) {
					System.out.println("Ocorreu um erro ao criar os arquivo de métricas");
					e.printStackTrace();
				}
			});

			queryPrinter.flush();
		}
	}

	private void printQueryStatistics(SQLQuery result) {
		System.out.println(result.getFileKey());
		System.out.println("------------------------------------");
		System.out.println(String.format("File: %s", result.getFilePath()));
		System.out.println(String.format("File Key: %s", result.getFileKey()));
		System.out.println(String.format("File Type: %s", result.getFileType()));
		System.out.println(String.format("Query Tyoe: %s", result.getType()));
		System.out.println(String.format("SQL Complexity: %s", result.getComplexity()));
		System.out.println("------------------------------------");
		System.out.println();
	}

	private void printStatistics(CKClassResultSpringBatch result) {
		System.out.println(result.getClassName());
		System.out.println("------------------------------------");
		System.out.println(String.format("File: %s", result.getFile()));
		System.out.println(String.format("Class: %s", result.getClassName()));
		System.out.println(String.format("Role: %s", result.getBatchRole()));
		System.out.println(String.format("LOC: %s", result.getLoc()));
		System.out.println(String.format("LCOM: %s", result.getLcom()));
		System.out.println(String.format("WMC: %s", result.getWmc()));
		System.out.println(String.format("Max Neasting: %s", result.getMaxNestedBlocks()));
		System.out.println(String.format("FICP: %s", result.getCoupling().size()));
		System.out.println(String.format("Max SQL Complexity: %s", result.getMaxSqlComplexity()));
		System.out.println("------------------------------------");
		System.out.println();
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

	private void printCouplingMethods(Collection<MethodCouplingComposite> couplings, int tabLevel) {
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
}
