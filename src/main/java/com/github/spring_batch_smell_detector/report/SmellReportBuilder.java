package com.github.spring_batch_smell_detector.report;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.spring_batch_smell_detector.model.SmellType;
import com.google.common.base.Strings;

@Component
public class SmellReportBuilder {
	
	@Autowired
	private ThresholdsSmellReport thresholdReport;
	
	@Autowired
	private DefaultSmellReport defaultReport;
	
	@Autowired
	private BrainProcessorSmellReport brainProcessorReport;
	
	@Autowired
	private BrainReaderSmellReport brainReaderReport;
	
	@Autowired
	private BrainWriterSmellReport brainWriterReport;
	
	private String filePath;
	
	private boolean printStatistics;
	
	private Map<SmellType, Set<UUID>> reportSections;
	
	public SmellReportBuilder() {		
		this.printStatistics = true;
		this.reportSections = new EnumMap<>(SmellType.class);
	}
	
	public SmellReportBuilder filePath(String path) {
		this.filePath = path;
		return this;
	}
	
	public SmellReportBuilder printStatistics(boolean value) {
		this.printStatistics = value;
		return this;
	}
	
	public SmellReportBuilder addReportSection(SmellType type, Set<UUID> results) {
		reportSections.put(type, results);
		return this;
	}
	
	public void print() {
		if(Strings.isNullOrEmpty(filePath)) {
			filePath = "app_smell_report.txt";
		}
		
		if(printStatistics) {
			thresholdReport.print(null, filePath);
		}
		
		if(reportSections.containsKey(SmellType.BRAIN_READER)) {
			brainReaderReport.print(reportSections.get(SmellType.BRAIN_READER), filePath);
		}
		
		if(reportSections.containsKey(SmellType.BRAIN_PROCESSOR)) {
			brainProcessorReport.print(reportSections.get(SmellType.BRAIN_PROCESSOR), filePath);
		}
		
		if(reportSections.containsKey(SmellType.BRAIN_SERVICE)) {
			brainProcessorReport.print(reportSections.get(SmellType.BRAIN_SERVICE), filePath);
		}
		
		if(reportSections.containsKey(SmellType.BRAIN_WRITER)) {
			brainWriterReport.print(reportSections.get(SmellType.BRAIN_WRITER), filePath);
		}
		
		if(reportSections.containsKey(SmellType.AMATEUR_WRITER)) {
			defaultReport.setType(SmellType.AMATEUR_WRITER);
			defaultReport.print(reportSections.get(SmellType.AMATEUR_WRITER), filePath);
		}
		
		if(reportSections.containsKey(SmellType.READAHOLIC_COMPONENT)) {
			defaultReport.setType(SmellType.READAHOLIC_COMPONENT);
			defaultReport.print(reportSections.get(SmellType.READAHOLIC_COMPONENT), filePath);
		}
		
		if(reportSections.containsKey(SmellType.GLOBAL_PROCESSOR)) {
			defaultReport.setType(SmellType.GLOBAL_PROCESSOR);
			defaultReport.print(reportSections.get(SmellType.GLOBAL_PROCESSOR), filePath);
		}
		
		if(reportSections.containsKey(SmellType.IMPROPER_COMMUNICATION)) {
			defaultReport.setType(SmellType.IMPROPER_COMMUNICATION);
			defaultReport.print(reportSections.get(SmellType.IMPROPER_COMMUNICATION), filePath);
		}
	}
}
