package com.github.spring_batch_smell_detector.report;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.spring_batch_smell_detector.metrics.MetricsThresholds;
import com.github.spring_batch_smell_detector.model.BatchRole;
import com.github.spring_batch_smell_detector.model.Metrics;
import com.github.spring_batch_smell_detector.model.SmellType;
import com.github.spring_batch_smell_detector.statistics.MetricStatistics;
import com.google.common.base.Strings;

@Component
public class ThresholdsSmellReport implements SmellReport {

	@Autowired
	private MetricsThresholds thresholds;
	
	@Override
	public void print(Set<UUID> components, String filePath) {
		try (PrintWriter writer = new PrintWriter(filePath)) {

			writer.println("STATISTICAL THRESHOLDS");
			writer.println("------------------------------------");
			writer.println();

			printRoleThresholds(BatchRole.READER, writer);
			printRoleThresholds(BatchRole.PROCESSOR, writer);
			printRoleThresholds(BatchRole.SERVICE, writer);
			printRoleThresholds(BatchRole.WRITER, writer);

		} catch (IOException e) {
			System.out.println("Ocorreu um erro ao tentar gerar o relatório de threshoulds");
			e.printStackTrace();
		}

	}

	private void printRoleThresholds(BatchRole role, PrintWriter writer) {
		MetricStatistics loc = thresholds.getThreshoulds(role, Metrics.LOC);
		MetricStatistics lcom = thresholds.getThreshoulds(role, Metrics.LCOM);
		MetricStatistics wmc = thresholds.getThreshoulds(role, Metrics.WMC);
		MetricStatistics maxNeasting = thresholds.getThreshoulds(role, Metrics.MAXNESTING);
		MetricStatistics ficp = thresholds.getThreshoulds(role, Metrics.FICP);
		MetricStatistics sqlComplexity = thresholds.getThreshoulds(role, Metrics.SQL_COMPLEXITY);
		
		writer.println("ROLE: " + role);
		writer.println("--------------------------------");
		
		writer.println("LOC");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("MIN", 10, ' '));
		writer.print(Strings.padEnd("AVG", 10, ' '));
		writer.print(Strings.padEnd("MAX", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", loc.getMin()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", loc.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", loc.getMax()), 10, ' '));
		writer.println();
		writer.println();
		
		writer.println("LCOM");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("MIN", 10, ' '));
		writer.print(Strings.padEnd("AVG", 10, ' '));
		writer.print(Strings.padEnd("MAX", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", lcom.getMin()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", lcom.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", lcom.getMax()), 10, ' '));
		writer.println();
		writer.println();
		
		writer.println("WMC");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("MIN", 10, ' '));
		writer.print(Strings.padEnd("AVG", 10, ' '));
		writer.print(Strings.padEnd("MAX", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", wmc.getMin()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", wmc.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", wmc.getMax()), 10, ' '));
		writer.println();
		writer.println();
		
		writer.println("MAX_NEASTING");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("MIN", 10, ' '));
		writer.print(Strings.padEnd("AVG", 10, ' '));
		writer.print(Strings.padEnd("MAX", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", maxNeasting.getMin()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", maxNeasting.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", maxNeasting.getMax()), 10, ' '));
		writer.println();
		writer.println();
		
		writer.println("FICP");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("MIN", 10, ' '));
		writer.print(Strings.padEnd("AVG", 10, ' '));
		writer.print(Strings.padEnd("MAX", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", ficp.getMin()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", ficp.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", ficp.getMax()), 10, ' '));
		writer.println();
		writer.println();
		
		writer.println("SQL_COMPLEXITY");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("MIN", 10, ' '));
		writer.print(Strings.padEnd("AVG", 10, ' '));
		writer.print(Strings.padEnd("MAX", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", sqlComplexity.getMin()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", sqlComplexity.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", sqlComplexity.getMax()), 10, ' '));
		writer.println();
		writer.println("------------------------------------");
		writer.println();
		writer.println();
	}

}
