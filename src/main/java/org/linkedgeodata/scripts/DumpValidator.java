package org.linkedgeodata.scripts;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.util.ExceptionUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class DumpValidator
{
	private static Logger logger = Logger.getLogger(DumpValidator.class);
    protected static Options cliOptions;
	
    
	private static final Pattern linePattern = Pattern.compile(".*at line\\s+([0-9]+).*", Pattern.DOTALL);
	
    public static Long getLineNumber(String text)
    {
    	System.out.println("Text = " + text);
    	
		Matcher matcher = linePattern.matcher(text);
		Long result = null;
		if(matcher.matches()) {
			result = Long.parseLong(matcher.group(1));
		}

		return result;
    }
	
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
		
		initCLIOptions();
	

		//test();
		
		
		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);
		
		String inFileName = commandLine.getOptionValue("f");
		String outFilePrefix = commandLine.getOptionValue("p", "/tmp/output/xx");
		int batchSize = Integer.parseInt(commandLine.getOptionValue("n", "10000"));
		
		File inFile = new File(inFileName);
		
		
		BufferedReader reader = new BufferedReader(new FileReader(inFile));
		
		Model model = ModelFactory.createDefaultModel();
		
		String line = null;
		String part = "";
		int batchCount = 0;
		long lineCount = 0;
		
		int fileId = 0;
		
		String header = null;
		
		while((line = reader.readLine()) != null) {
			++lineCount;
			
			if(line.trim().isEmpty()) {

				if(header == null) {
					header = part + "\n\n";
				}
				else {
				
					try {
						model.read(new ByteArrayInputStream(part.getBytes()), "", "N3");
					}
					catch(Exception e) {
						logger.error("at line " + lineCount + ": " +e.getMessage());
					}
				
					if(++batchCount > batchSize) {
						//logger.info(batchCount + " objects processed, part size = " + part.length());

						String outFileName = outFilePrefix + fileId;
						OutputStream out = new FileOutputStream(new File(outFileName));
						model.write(out, "N-TRIPLE", "");
					
						++fileId;
						out.close();
						batchCount = 0;
						model.removeAll();
					}
				}
				
				part = header;
			}

			part += line + "\n";
		}
	}


	/*************************************************************************/
	/* Init                                                                  */
	/*************************************************************************/	
	private static void initCLIOptions()
	{
		cliOptions = new Options();
		
		cliOptions.addOption("f", "file", true, "File");
		cliOptions.addOption("p", "prefix", true, "Prefix");
		cliOptions.addOption("n", "batchsize", true, "Batch szie");

		/*
		cliOptions.addOption("d", "database", true, "Database name");
		cliOptions.addOption("u", "user", true, "");
		cliOptions.addOption("w", "password", true, "");
		cliOptions.addOption("h", "host", true, "");
		cliOptions.addOption("n", "batchSize", true, "Batch size");
		cliOptions.addOption("o", "outfile", true, "Output filename");		

		cliOptions.addOption("xnt", "tagsn", false, "eXport node tags");
		cliOptions.addOption("xwt", "tagsw", false, "eXport way tags");
		cliOptions.addOption("xrt", "tagsr", false, "eXport relation tags");
		
		cliOptions.addOption("ef", "entityfilter", true, "");
		cliOptions.addOption("tf", "tagfilter", true, "");
		*/
	}

}
