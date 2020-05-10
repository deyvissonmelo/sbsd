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
		MetricStatistics sqlComplexity_read = thresholds.getThreshoulds(role, Metrics.SQL_COMPLEXITY_READ);
		MetricStatistics sqlComplexity_write = thresholds.getThreshoulds(role, Metrics.SQL_COMPLEXITY_WRITE);
		
		writer.println("ROLE: " + role);
		writer.println("--------------------------------");
		
		writer.println("LOC");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("LOWER", 10, ' '));
		writer.print(Strings.padEnd("AVERAGE", 10, ' '));
		writer.print(Strings.padEnd("HIGHER", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", loc.getLower()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", loc.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", loc.getHigher()), 10, ' '));
		writer.println();
		writer.println();
		
		writer.println("LCOM");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("LOWER", 10, ' '));
		writer.print(Strings.padEnd("AVERAGE", 10, ' '));
		writer.print(Strings.padEnd("HIGHER", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", lcom.getLower()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", lcom.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", lcom.getHigher()), 10, ' '));
		writer.println();
		writer.println();
		
		writer.println("WMC");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("LOWER", 10, ' '));
		writer.print(Strings.padEnd("AVERAGE", 10, ' '));
		writer.print(Strings.padEnd("HIGHER", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", wmc.getLower()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", wmc.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", wmc.getHigher()), 10, ' '));
		writer.println();
		writer.println();
		
		writer.println("MAX_NEASTING");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("LOWER", 10, ' '));
		writer.print(Strings.padEnd("AVERAGE", 10, ' '));
		writer.print(Strings.padEnd("HIGHER", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", maxNeasting.getLower()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", maxNeasting.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", maxNeasting.getHigher()), 10, ' '));
		writer.println();
		writer.println();
		
		writer.println("FICP");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("LOWER", 10, ' '));
		writer.print(Strings.padEnd("AVERAGE", 10, ' '));
		writer.print(Strings.padEnd("HIGHER", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", ficp.getLower()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", ficp.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", ficp.getHigher()), 10, ' '));
		writer.println();
		writer.println();
		
		writer.println("SQL_COMPLEXITY_READ");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("LOWER", 10, ' '));
		writer.print(Strings.padEnd("AVERAGE", 10, ' '));
		writer.print(Strings.padEnd("HIGHER", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", sqlComplexity_read.getLower()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", sqlComplexity_read.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", sqlComplexity_read.getHigher()), 10, ' '));
		writer.println();
		writer.println("------------------------------------");
		writer.println();
		writer.println();
		
		writer.println("SQL_COMPLEXITY_WRITE");
		writer.println("-------------------------");
		writer.print(Strings.padEnd("LOWER", 10, ' '));
		writer.print(Strings.padEnd("AVERAGE", 10, ' '));
		writer.print(Strings.padEnd("HIGHER", 10, ' '));
		writer.println();
		writer.print(Strings.padEnd(String.format("%.2f", sqlComplexity_write.getLower()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", sqlComplexity_write.getAverage()), 10, ' '));
		writer.print(Strings.padEnd(String.format("%.2f", sqlComplexity_write.getHigher()), 10, ' '));
		writer.println();
		writer.println("------------------------------------");
		writer.println();
		writer.println();
	}

}
