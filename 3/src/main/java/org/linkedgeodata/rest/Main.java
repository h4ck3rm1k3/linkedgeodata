package org.linkedgeodata.rest;

import java.io.FileInputStream;
import java.util.logging.LogManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.PropertyConfigurator;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.util.MultiPartWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.servlet.ServletContainer;



public class Main {
	/**
	 * @param exitCode
	 */
	public static void printHelpAndExit(int exitCode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(Main.class.getName(), cliOptions);
		System.exit(exitCode);
	}

	private static final Logger logger = LoggerFactory
			.getLogger(Main.class);
	private static final Options cliOptions = new Options();

	/**
	 * @param args
	 *            the command line arguments
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("log4j.properties");
		LogManager.getLogManager().readConfiguration(
				new FileInputStream("jdklog.properties"));

		CommandLineParser cliParser = new GnuParser();

		cliOptions.addOption("P", "port", true, "Server port");
		cliOptions.addOption("C", "context", true, "Context e.g. /sparqlify");
		cliOptions.addOption("B", "backlog", true,
				"Maximum number of connections");

		cliOptions.addOption("t", "type", true,
				"Database type (posgres, mysql,...)");
		cliOptions.addOption("d", "database", true, "Database name");
		cliOptions.addOption("u", "username", true, "");
		cliOptions.addOption("p", "password", true, "");
		cliOptions.addOption("h", "hostname", true, "");

		cliOptions.addOption("c", "config", true, "Sparqlify config file");

		cliOptions.addOption("t", "timeout", true, "Maximum query execution timeout");
		cliOptions.addOption("n", "resultsetsize", true, "Maximum result set size");
		
		CommandLine commandLine = cliParser.parse(cliOptions, args);

		// Parsing of command line args
		/*
		String portStr = commandLine.getOptionValue("P", "7000");
		String backLogStr = commandLine.getOptionValue("B", "100");
		String contextStr = commandLine.getOptionValue("C", "/sparqlify");
		int port = Integer.parseInt(portStr);
		int backLog = Integer.parseInt(backLogStr);

		String hostName = commandLine.getOptionValue("h", "localhost");
		String dbName = commandLine.getOptionValue("d", "");
		String userName = commandLine.getOptionValue("u", "");
		String passWord = commandLine.getOptionValue("p", "");

		String maxQueryExecutionTimeStr = commandLine.getOptionValue("t", null);
		Integer maxQueryExecutionTime = maxQueryExecutionTimeStr == null
				? null
				: Integer.parseInt(maxQueryExecutionTimeStr);
		
		String maxResultSetSizeStr = commandLine.getOptionValue("n", null);
		Long maxResultSetSize = maxResultSetSizeStr == null
				? null
				: Long.parseLong(maxResultSetSizeStr);
		
		
		String configFileStr = commandLine.getOptionValue("c");

		if (configFileStr == null) {
			logger.error("No config file given");

			printHelpAndExit(-1);
		}

		File configFile = new File(configFileStr);
		if (!configFile.exists()) {
			logger.error("File does not exist: " + configFileStr);

			printHelpAndExit(-1);
		}
		*/
		
		
		ServletHolder sh = new ServletHolder(ServletContainer.class);

		
		
		/*
		 * For 0.8 and later the "com.sun.ws.rest" namespace has been renamed to
		 * "com.sun.jersey". For 0.7 or early use the commented out code instead
		 */
		// sh.setInitParameter("com.sun.ws.rest.config.property.resourceConfigClass",
		// "com.sun.ws.rest.api.core.PackagesResourceConfig");
		// sh.setInitParameter("com.sun.ws.rest.config.property.packages",
		// "jetty");
		sh.setInitParameter(
				"com.sun.jersey.config.property.resourceConfigClass",
				"com.sun.jersey.api.core.PackagesResourceConfig");
		sh.setInitParameter("com.sun.jersey.config.property.packages",
				"org.linkedgeodata.rest");

		Server server = new Server(9998);
		Context context = new Context(server, "/", Context.SESSIONS);
		context.addServlet(sh, "/*");
		

		server.start();

	}

}
