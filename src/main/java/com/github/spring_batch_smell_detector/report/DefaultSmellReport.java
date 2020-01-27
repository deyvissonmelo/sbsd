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
import com.github.spring_batch_smell_detector.model.SmellType;

@Component
public class DefaultSmellReport implements SmellReport {
	
	private SmellType type;
	
	public void setType(SmellType type) {
		this.type = type;
	}
	
	@Override
	public void print(Set<UUID> components, String filePath) {
		try (
				FileWriter fw = new FileWriter(filePath, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter writer = new PrintWriter(bw)
			) 
		{

			writer.println(type);
			writer.println("------------------------------------");
			writer.println("AFFECTED CLASSES:");
			
			components.forEach(c -> {
				CKClassResultSpringBatch result = CouplingUtils.getLoadedInstance().getCKClassResult(c);			
				writer.println(result.getClassName());						
			});
			
			writer.println();

		} catch (IOException e) {
			System.out.println("Ocorreu um erro ao tentar gerar o relatório do smell Amateur Writer");
			e.printStackTrace();
		}
	}

}
