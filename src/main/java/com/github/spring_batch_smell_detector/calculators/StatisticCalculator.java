package com.github.spring_batch_smell_detector.calculators;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.CouplingUtils;
import com.github.spring_batch_smell_detector.metrics.util.MethodCouplingComposite;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQuery;
import com.github.spring_batch_smell_detector.model.BatchRole;
import com.github.spring_batch_smell_detector.smells.ReadaholicComponent;
import com.google.common.base.Strings;

@Component
public class StatisticCalculator {

	private final String METRIC_FILE_NAME = "sbsd_metrics.csv";

	private final String QUERY_METRIC_FILE_NAME = "sbsd_query_metrics.csv";
	
	private Map<UUID, CKClassResultSpringBatch> ckResults = new HashMap<>();

	private final String[] FILE_METRIC_HEADER = { "file", "class", "role", "methods", "loc", "lcom", "wmc",
			"maxNeasting", "ficp", "sqlComplexity", "readOperations" };
	
	private final String[] FILE_QUERY_METRIC_HEADER = { "file", "fileType", "key", "queryType", "sqlComplexity" };

	@Autowired
	private CKSpringBatch ck;
	
	@Autowired
	private ReadaholicComponent rhComponent;

	boolean verbose = false;
	
	public void calculate(String path, String jobPath, String alternativeSqlPath, boolean verbose)
			throws ParserConfigurationException, SAXException, IOException {

		if (Strings.isNullOrEmpty(path) || Strings.isNullOrEmpty(jobPath) || Strings.isNullOrEmpty(alternativeSqlPath))
			throw new RuntimeException(
					"Um ou mais parâmetros de localização para os arquivos do projeto não foram informados.");

		this.verbose = verbose;
		
		ck.calculate(path, jobPath, alternativeSqlPath, result -> {
			ckResults.put(result.getId(), result);
		});

		CouplingUtils.initialize(ckResults);
		
		printMetricsFile();
		printQueryMetricsFile();

	}

	private void printQueryMetricsFile() throws IOException {
		try (
			final FileWriter queryWriter = new FileWriter(QUERY_METRIC_FILE_NAME);
			final CSVPrinter queryPrinter = new CSVPrinter(queryWriter,
					CSVFormat.DEFAULT.withHeader(FILE_QUERY_METRIC_HEADER));
		) {
			
			for(SQLQuery query : SQLQueriesFinder.getLoadedInstance().getQueries().values()) {
			
				if (verbose) {
					printQueryStatistics(query);
				}
	
				queryPrinter.printRecord(query.getFilePath(), query.getFileType(), query.getFileKey(),
						query.getType(), query.getComplexity());
			}
			
			queryPrinter.flush();
			
		} catch (IOException e) {
			System.out.println("Ocorreu um erro ao criar os arquivo de métricas");
			e.printStackTrace();
		}
	}

	private void printMetricsFile() {

		try(
			final FileWriter writer = new FileWriter(METRIC_FILE_NAME);
			final CSVPrinter classPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(FILE_METRIC_HEADER));
		){
			
			for(CKClassResultSpringBatch result : ckResults.values()) {
				
				if(verbose)
					printStatistics(result);
				
				int readOperations = calculateReadOperations(result);
				
				classPrinter.printRecord(result.getFile(), result.getClassName(), result.getBatchRole(),
						result.getNumberOfMethods(), result.getLoc(), result.getLcom(), result.getWmc(),
						result.getMaxNestedBlocks(), result.getCoupling().size(), result.getMaxSqlComplexity(), 
						readOperations);
			}
			
			classPrinter.flush();
			
		} catch (IOException e) {
			System.out.println("Ocorreu um erro ao criar os arquivo de métricas");
			e.printStackTrace();
		}
	}
	
	private int calculateReadOperations(CKClassResultSpringBatch result) {
		
		if(!result.getBatchRole().contains(BatchRole.PROCESSOR) && 
				!result.getBatchRole().contains(BatchRole.WRITER))
			return 0;
		
		MethodCouplingComposite componentMethodRef = 
				CouplingUtils.getLoadedInstance().getMethodCoupling(result.getId());
		
		
		int totalReadOprations = rhComponent.calculateComponentDatabaseAccess(result);
		totalReadOprations += rhComponent.calculateMethodDatabaseAccess(componentMethodRef, ckResults);
		
		return totalReadOprations;
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
}
