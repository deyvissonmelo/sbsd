package com.github.spring_batch_smell_detector;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.xml.sax.SAXException;

import com.github.spring_batch_smell_detector.calculators.SmellCalculator;
import com.github.spring_batch_smell_detector.calculators.StatisticCalculator;

@SpringBootApplication
public class Runner implements CommandLineRunner {

	@Autowired
	private StatisticCalculator statisticCalculator;

	@Autowired
	private SmellCalculator smellCalculator;

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

		if (args == null || args.length < 3) {
			System.out.println("Informe os parâmetros de execução");
			System.out.println(
					"java -jar spring_batch_smell_detector -s <project_path> <job_file_path> <query_fle_path>");
			System.exit(1);
		}
		
		smellCalculator.calculate(args[0], args[1], args[2], verbose, batch);
	}

	private void calculateMetrics(String[] args, boolean verbose)
			throws ParserConfigurationException, SAXException, IOException {
		if (args == null || args.length < 3) {
			System.out.println("Informe os parâmetros de execução");
			System.out.println(
					"java -jar spring_batch_smell_detector -s <project_path> <job_file_path> <query_fle_path>");
			System.exit(1);
		}

		statisticCalculator.calculate(args[0], args[1], args[2], verbose);
	}
}
