package com.github.spring_batch_smell_detector.report;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.CouplingUtils;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQuery;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueryFileType;

@Component
public class BrainReaderSmellReport implements SmellReport {

	@Override
	public void print(Set<UUID> components, String filePath) {
		try (FileWriter fw = new FileWriter(filePath, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter writer = new PrintWriter(bw)) {
			writer.println("BRAIN READER");
			writer.println("------------------------------------");
			writer.println("AFFECTED COMPONENTS:");

			printAffectedClasses(components, writer);
			printAffectedQueries(components, writer);

			writer.println();

		} catch (IOException e) {
			System.out.println("Ocorreu um erro ao tentar gerar o relatório do smell Brain Reader");
			e.printStackTrace();
		}

	}

	private void printAffectedQueries(Set<UUID> components, PrintWriter writer) {
		List<SQLQuery> jobQueries = SQLQueriesFinder.getLoadedInstance().getByFileType(SQLQueryFileType.JOB_FILE);
		
		components.forEach(c -> {
			
			Optional<SQLQuery> query = jobQueries.stream().filter( q -> q.getId().equals(c)).findFirst();
			
			if(query.isPresent()) {
				writer.println("Query: " + query.get().getFileKey());

				writer.println("\tMETRICS:");
				writer.println("\t--------------------");
				writer.println(String.format("\tSQL COMPLEXITY: %d", query.get().getComplexity()));
				writer.println();
			}
						
		});
	}

	private void printAffectedClasses(Set<UUID> components, PrintWriter writer) {
		components.forEach(c -> {
			CKClassResultSpringBatch result = CouplingUtils.getLoadedInstance().getCKClassResult(c);

			if (result != null) {
				writer.println("Class: " + result.getClassName());

				writer.println("\tMETRICS:");
				writer.println("\t--------------------");
				writer.println(String.format("\tLOC: %d", result.getLoc()));
				writer.println(String.format("\tWMC: %d", result.getWmc()));
				writer.println(String.format("\tMAX NEASTING: %d", result.getMaxNestedBlocks()));
				writer.println(String.format("\tSQL COMPLEXITY: %d", result.getMaxSqlComplexity()));
				writer.println();
			}
			
		});
	}

}
