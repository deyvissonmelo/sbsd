package com.github.spring_batch_smell_detector.report;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.CouplingUtils;

@Component
public class BrainProcessorSmellReport implements SmellReport {

	@Override
	public void print(Set<UUID> components, String filePath) {
		try (FileWriter fw = new FileWriter(filePath, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter writer = new PrintWriter(bw)) {
			writer.println("BRAIN PROCESSOR");
			writer.println("------------------------------------");
			writer.println("AFFECTED CLASSES:");

			printAffectedClasses(components, writer);

			writer.println();

		} catch (IOException e) {
			System.out.println("Ocorreu um erro ao tentar gerar o relatório do smell Brain Processor");
			e.printStackTrace();
		}

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
				writer.println(String.format("\tLCOM: %d", result.getLcom()));
				writer.println();
			}
		});
	}

}
