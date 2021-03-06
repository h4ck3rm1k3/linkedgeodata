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
 * along with this program.  If not, see <http://www.gnu.h sllorg/licenses/>.
 *
 */ 
package org.linkedgeodata.scripts;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.commons.sparql.core.impl.ModelSparqlEndpoint;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.core.vocab.GeoRSS;
import org.linkedgeodata.core.vocab.WGS84Pos;
import org.linkedgeodata.dao.LGDDAO;
import org.linkedgeodata.dao.LGDRDFDAO;
import org.linkedgeodata.dao.TagMapperDAO;
import org.linkedgeodata.dump.NodeIdIterator;
import org.linkedgeodata.dump.WayIdIterator;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.osmosis.plugins.TagFilter;
import org.linkedgeodata.util.ExceptionUtil;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.stats.SimpleStatsTracker;
import org.springframework.util.StopWatch;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Deprecated: Example command line args
 * -hlocalhost -dunittest_lgd -upostgres -n500 -wpostgres -xnt -ooutput/NodeTags.n3 -tf "k NOT IN ('created_by','ele','time','layer','source','tiger:tlid','tiger:county','tiger:upload_uuid','attribution','source_ref','KSJ2:coordinate','KSJ2:lat','KSJ2:long','KSJ2:curve_id','AND_nodes','converted_by','TMC:cid_58:tabcd_1:LocationCode','TMC:cid_58:tabcd_1:LCLversion','TMC:cid_58:tabcd_1:NextLocationCode','TMC:cid_58:tabcd_1:PrevLocationCode','TMC:cid_58:tabcd_1:LocationCode')" -ef "(filter.k IN ('highway', 'barrier', 'power') OR (filter.k = 'railway' AND filter.v NOT IN ('station')))"
 * 
 * Now:
 * -hlocalhost -dunittest_lgd -upostgres -n500 -wpostgres -xnt -ooutput/NodeTags.n3 -tff /data/live/LiveTagFilter.txt -eff /data/live/LiveEntityFilter.txt
 */

abstract class ResolveTask
	implements Runnable
{
	private static Logger logger = Logger.getLogger(ResolveTask.class);
	
	protected LGDRDFDAO dao;
	protected Collection<Long> ids;
	protected String tagFilter;
	protected boolean skipUntagged;
	protected Model model;
	protected OutputStream out;
	
	// use a common tracker instead I'd say
	private SimpleStatsTracker tracker = new SimpleStatsTracker();
	
	public ResolveTask(Collection<Long> ids, LGDRDFDAO dao, boolean skipUntagged, String tagFilter, OutputStream out)
	{
		this.ids = ids;
		this.dao = dao;
		this.skipUntagged = skipUntagged;
		this.tagFilter = tagFilter;
		this.out = out;
		this.model = ModelFactory.createDefaultModel();
	}
	
	public void setIds(Collection<Long> ids)
	{
		this.ids = ids;
	}
	
	
	@Override
	public void run()
	{
		try {
			myRun();
		} catch (Exception e) {
			logger.error(ExceptionUtil.toString(e));
		}
	}
	
	public void myRun()
		throws Exception
	{
		int count = doWork();// 

		tracker.update(count);			
		//if(model.size() > 10000) {
			//LGDDumper.writeModel(model, out, false);
			throw new NotImplementedException();
		//}
	}
	
	protected abstract int doWork()
		throws Exception;	
}


class NodeResolveTask
	extends ResolveTask
{
	public NodeResolveTask(Collection<Long> ids, LGDRDFDAO dao, boolean skipUntagged,
			String tagFilter, OutputStream out)
	{
		super(ids, dao, skipUntagged, tagFilter, out);
	}

	@Override
	protected int doWork()
		throws Exception
	{
		return dao.resolveNodes(model, ids, skipUntagged, tagFilter);
	}
}


class WayResolveTask
	extends ResolveTask
{
	public WayResolveTask(Collection<Long> ids, LGDRDFDAO dao, boolean skipUntagged,
			String tagFilter, OutputStream out)
	{
		super(ids, dao, skipUntagged, tagFilter, out);
	}

	@Override
	protected int doWork()
		throws Exception
	{
		return dao.resolveWays(model, ids, skipUntagged, tagFilter);
	}
}




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
		String outputFormat = commandLine.getOptionValue("of", "N-TRIPLE");
		String outFileName = commandLine.getOptionValue("o", "out.nt");
	
		String batchSizeStr = commandLine.getOptionValue("n", "1000");
		
		int batchSize = Integer.parseInt(batchSizeStr);
		if(batchSize <= 0)
			throw new RuntimeException("Invalid argument for batchsize");
	
		
		boolean exportNodeTags = commandLine.hasOption("xnt");
		boolean exportWayTags = commandLine.hasOption("xwt");

		boolean enableVirtuosoPredicates = commandLine.hasOption("V");
		
		//String entityFilter =commandLine.getOptionValue("ef", null);
		//String tagFilter = commandLine.getOptionValue("tf", null);
		
		String entityFilterFileName =commandLine.getOptionValue("eff", null);
		String tagFilterFileName = commandLine.getOptionValue("tff", null);
		
		TagFilter entityFilterObj = new TagFilter(true);
		entityFilterObj.load(new File(entityFilterFileName));
		
		TagFilter tagFilterObj = new TagFilter();
		tagFilterObj.load(new File(tagFilterFileName));
		
		String entityFilter = TagFilter.createFilterSQL(entityFilterObj, "filter.k", "filter.v");
		String tagFilter = TagFilter.createFilterSQL(tagFilterObj, "k", "v");
		
		//entityFilter = null;
		//tagFilter = null;
		//String mappingRulesPath = "data/triplify/config/2.0/LGDMappingRules.2.0.xml";
		//String mappingRulesPath = "output/LGDMappingRules.2.0.xml";
		
		
		
		String rootModelPath    = "Namespaces.2.0.ttl";
		
		
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
		
		logger.info("Loading mapping rules from database");
		// TODO Write scripts for loading and exporting dumps
		TagMapperDAO dbTagMapper = new TagMapperDAO();
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();
		dbTagMapper.setSession(session);
		ITagMapper tagMapper = new InMemoryTagMapper(dbTagMapper);
		tx.commit();
		//InMemoryTagMapper tagMapper = new InMemoryTagMapper();
		//tagMapper.load(new File(mappingRulesPath));

		
		
		Connection conn = PostGISUtil.connectPostGIS(hostName, dbName, userName, passWord);
		logger.info("Connected to db");

		LGDDAO innerDao = new LGDDAO(conn);
		
		ILGDVocab vocab = new LGDVocab();
		LGDRDFDAO dao = new LGDRDFDAO(innerDao, tagMapper, vocab, new ModelSparqlEndpoint());
		
		logger.info("Opening output stream: " + outFileName);
		OutputStream out = new FileOutputStream(outFileName);

		RDFWriter rdfWriter = baseModel.getWriter(outputFormat); 
		rdfWriter.write(baseModel, out, "");
		//baseModel.write(out, outputFormat);

		
		StopWatch sw = new StopWatch();
		sw.start();

		if(exportNodeTags) {		
			//NodeTagIteratorDenorm1 it = new NodeTagIteratorDenorm1(conn, batchSize);
			Iterator<Collection<Long>> it = new NodeIdIterator(conn, batchSize, entityFilter);

			runNodeTags(it, tagFilter, prefixMap, dao, rdfWriter, out, enableVirtuosoPredicates);		
		}
		
		if(exportWayTags) {
			Iterator<Collection<Long>> it = new WayIdIterator(conn, batchSize, entityFilter);

			runWayTags(it, tagFilter, prefixMap, dao, rdfWriter, out, enableVirtuosoPredicates);
		}
		
		
		executor.shutdown();
		executor.awaitTermination(14, TimeUnit.DAYS);
		
		sw.stop();
		logger.info("Time taken: " + sw.getTotalTimeSeconds());
		
		//System.exit(0);
		//out.flush();
		//out.close();
	}

	
	private static ExecutorService executor = Executors.newFixedThreadPool(2);
	//private static ExecutorService executor = Executors.newSingleThreadExecutor();
	
	
	public static void runNodeTags2(Iterator<Collection<Long>> it, String tagFilter, Map<String, String> prefixMap, LGDRDFDAO dao, OutputStream out)
		throws Exception
	{	
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefixes(prefixMap);
	
		while(it.hasNext()) {
			Collection<Long> ids = it.next();

			
			ResolveTask task = new NodeResolveTask(ids, dao, true, tagFilter, out);
			executor.submit(task);
		}		
	}

	
	/**
	 * Creates a triple
	 * <x> geo:geometry "POINT(<long> <lat>)"^^virtrdf:Geometry
	 * for each triple of the form
	 * <x> georss:point "<lat> <long>"
	 * 
	 * @param model
	 * @return
	 */
	public static Model augmentGeoRSSWithVirtuoso(Model model)
	{
		TypeMapper tm = TypeMapper.getInstance();
		RDFDatatype virtrdfGeometry = tm.getSafeTypeByName("http://www.openlinksw.com/schemas/virtrdf#Geometry");

		Iterator<Statement> it = model.listStatements(null, GeoRSS.point, (RDFNode)null);
		while(it.hasNext()) {
			Statement stmt = it.next();

			String value = stmt.getObject().asNode().getLiteralValue().toString();
			
			String[] latlong = value.trim().split("\\s+");
			
			Literal virtLit = model.createTypedLiteral("POINT(" + latlong[1] + " " + latlong[0] + ")", virtrdfGeometry);
			
			model.add(stmt.getSubject(), WGS84Pos.geometry, virtLit);
		}
	
		
		return model;
	}
	
	
	public static void runNodeTags(Iterator<Collection<Long>> it, String tagFilter, Map<String, String> prefixMap, LGDRDFDAO dao, RDFWriter rdfWriter, OutputStream out, boolean enableVirtuosoPredicates)
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
				writeModel(model, rdfWriter, out, enableVirtuosoPredicates);
			}
		}
		writeModel(model, rdfWriter, out, enableVirtuosoPredicates);
	}
	
	public static void runWayTags(Iterator<Collection<Long>> it, String tagFilter, Map<String, String> prefixMap, LGDRDFDAO dao, RDFWriter rdfWriter, OutputStream out, boolean enableVirtuosoPredicates)
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
				writeModel(model, rdfWriter, out, enableVirtuosoPredicates);
			}
		}
		writeModel(model, rdfWriter, out, enableVirtuosoPredicates);
	}

	private static Pattern directivePattern = Pattern.compile("^@.*$.?\\n?", Pattern.MULTILINE);

	public static synchronized void writeModel(Model model, RDFWriter rdfWriter, OutputStream out, boolean enableVirtuosoPredicates)
		throws IOException
	{
		if(model.isEmpty())
			return;

		if(enableVirtuosoPredicates) {
			augmentGeoRSSWithVirtuoso(model);
		}

		String str = ModelUtil.toString(model, rdfWriter);
		
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
		
		//cliOptions.addOption("ef", "entityfilter", true, "");
		//cliOptions.addOption("tf", "tagfilter", true, "");

		cliOptions.addOption("eff", "entityfilterFile", true, "");
		cliOptions.addOption("tff", "tagfilterFile", true, "");
		
		cliOptions.addOption("V", "virtuoso predicates", false, "");
	}
}
