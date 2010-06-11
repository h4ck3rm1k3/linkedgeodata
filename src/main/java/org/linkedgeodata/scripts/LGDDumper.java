/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */ 
package org.linkedgeodata.scripts;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.collections15.MultiMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.dao.LGDDAO;
import org.linkedgeodata.dao.LGDRDFDAO;
import org.linkedgeodata.dump.NodeIdIterator;
import org.linkedgeodata.dump.WayIdIterator;
import org.linkedgeodata.jtriplify.TagMapper;
import org.linkedgeodata.jtriplify.mapping.SimpleNodeToRDFTransformer;
import org.linkedgeodata.jtriplify.mapping.SimpleWayToRDFTransformer;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.stats.SimpleStatsTracker;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;



public class LGDDumper
{
	private static final Logger logger = Logger.getLogger(LGDDumper.class);	
    protected static Options cliOptions;


	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
	
		initCLIOptions();
	
		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);
	
		String hostName = commandLine.getOptionValue("h", "localhost");
		String dbName   = commandLine.getOptionValue("d", "lgd");
		String userName = commandLine.getOptionValue("u", "lgd");
		String passWord = commandLine.getOptionValue("w", "lgd");
		String outFileName = commandLine.getOptionValue("o", "out.n3");
	
		String batchSizeStr = commandLine.getOptionValue("n", "1000");

		int batchSize = Integer.parseInt(batchSizeStr);
		if(batchSize <= 0)
			throw new RuntimeException("Invalid argument for batchsize");
	
		
		boolean exportNodeTags = commandLine.hasOption("xnt");
		boolean exportWayTags = commandLine.hasOption("xwt");

		
		String entityFilter =commandLine.getOptionValue("ef", null);
		String tagFilter = commandLine.getOptionValue("tf", null);
		
		
		//entityFilter = null;
		//tagFilter = null;

		
		String mappingRulesPath = "output/LGDMappingRules.xml";
		String rootModelPath    = "Namespaces.ttl";
		
		
		logger.info("ExportNodeTags: " + exportNodeTags);
		logger.info("ExportWayTags: " + exportWayTags);
		logger.info("EntityFilter: " + entityFilter);
		logger.info("TagFilter: " + tagFilter);
		
		
		logger.info("Loading namespace prefixes");
		// Create a model containing the namespace prefixes
		Model baseModel = ModelFactory.createDefaultModel();
		ModelUtil.read(baseModel, new File(rootModelPath), "TTL");
		Map<String, String> prefixMap = baseModel.getNsPrefixMap();
		//System.exit(0);
		
		logger.info("Loading mapping rules");
		TagMapper tagMapper = new TagMapper();
		tagMapper.load(new File(mappingRulesPath));

		
		
		Connection conn = LineStringUpdater.connectPostGIS(hostName, dbName, userName, passWord);
		logger.info("Connected to db");

		LGDDAO innerDao = new LGDDAO(conn);
		LGDRDFDAO dao = new LGDRDFDAO(innerDao, tagMapper);
		
		logger.info("Opening output stream: " + outFileName);
		OutputStream out = new FileOutputStream(outFileName);

		baseModel.write(out, "N3");


		if(exportNodeTags) {
			SimpleNodeToRDFTransformer nodeTransformer =
				new SimpleNodeToRDFTransformer(tagMapper);
		
			//NodeTagIteratorDenorm1 it = new NodeTagIteratorDenorm1(conn, batchSize);
			Iterator<Collection<Long>> it = new NodeIdIterator(conn, batchSize, entityFilter);

			runNodeTags(it, tagFilter, prefixMap, dao, out);		
		}
		
		if(exportWayTags) {
			SimpleWayToRDFTransformer wayTransformer =
				new SimpleWayToRDFTransformer(tagMapper);

			Iterator<Collection<Long>> it = new WayIdIterator(conn, batchSize, entityFilter);

			runWayTags(it, tagFilter, prefixMap, dao, out);
		}
		
		out.flush();
		out.close();
	}

	public static void runNodeTags(Iterator<Collection<Long>> it, String tagFilter, Map<String, String> prefixMap, LGDRDFDAO dao, OutputStream out)
		throws Exception
	{	
		SimpleStatsTracker tracker = new SimpleStatsTracker();
	
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefixes(prefixMap);
	
		while(it.hasNext()) {
			Collection<Long> ids = it.next();

			int count = dao.resolveNodes(model, ids, true, tagFilter);
			
			tracker.update(count);			
			if(model.size() > 10000) {
				writeModel(model, out);
			}
		}
		writeModel(model, out);
		
	}

	
	public static void runWayTags(Iterator<Collection<Long>> it, String tagFilter, Map<String, String> prefixMap, LGDRDFDAO dao, OutputStream out)
		throws Exception
	{	
		SimpleStatsTracker tracker = new SimpleStatsTracker();

		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefixes(prefixMap);

		while(it.hasNext()) {
			Collection<Long> ids = it.next();

			int count = dao.resolveWays(model, ids, true, tagFilter);			
			tracker.update(count);
			
			if(model.size() > 10000) {
				writeModel(model, out);
			}
		}
		writeModel(model, out);
	}

	private static Pattern directivePattern = Pattern.compile("^@.*$.?\\n?", Pattern.MULTILINE);

	private static void writeModel(Model model, OutputStream out)
		throws IOException
	{
		if(model.isEmpty())
			return;
		
		String str = ModelUtil.toString(model, "N3");
		
		Matcher matcher = directivePattern.matcher(str); 
		str = matcher.replaceAll("");
		str += "\n";
		
		out.write(str.getBytes());
		
		model.removeAll();
	}

	/*************************************************************************/
	/* Init                                                                  */
	/*************************************************************************/	
	private static void initCLIOptions()
	{
		cliOptions = new Options();
		
		cliOptions.addOption("t", "type", true, "Database type (posgres, mysql,...)");
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
	}
}
