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
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.jtriplify.LGDOSMEntityBuilder;
import org.linkedgeodata.jtriplify.TagMapper;
import org.linkedgeodata.jtriplify.mapping.SimpleNodeToRDFTransformer;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.PrefetchIterator;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.stats.SimpleStatsTracker;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


class WayTagIteratorSchemaDenorm1
	extends PrefetchIterator<Way>
{
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;
	
	public WayTagIteratorSchemaDenorm1(Connection conn)
	{
		this.conn = conn;
	}
	
	public WayTagIteratorSchemaDenorm1(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	@Override
	protected Iterator<Way> prefetch()
		throws Exception
	{
		String sql = "SELECT DISTINCT way_id FROM way_tags ";
		if(offset != null)
			sql += "WHERE way_id > " + offset + " ";
		
		sql += "ORDER BY way_id ASC ";
		
		if(batchSize != null)
			sql += "LIMIT " + batchSize + " ";

		String s = "SELECT way_id, k, v, linestring::geometry FROM way_tags WHERE way_id IN (" + sql + ")";

		//System.out.println(s);
		ResultSet rs = conn.createStatement().executeQuery(s);

		Map<Long, Way> idToWay = new HashMap<Long, Way>();
		
		int counter = 0;
		while(rs.next()) {
			++counter;

			long wayId = rs.getLong("way_id");		
			String k = rs.getString("k");
			String v = rs.getString("v");
			PGgeometry g = (PGgeometry)rs.getObject("linestring");
			
			offset = offset == null ? wayId : Math.max(offset, wayId);
			
			Way way = idToWay.get(wayId);
			if(way == null) {
				way = new Way(wayId, 0, (Date)null, null, -1);
				idToWay.put(wayId, way);
			}

			Tag tag = new Tag(k, v);
			way.getTags().add(tag);

			if(g != null) {
				LineString ls = new LineString(g.getValue());
				String value = "";
				for(Point point : ls.getPoints()) {
					if(!value.isEmpty())
						value += " ";

					value += point.getY() + " " + point.getX();
				}

				String key =
					(ls.getFirstPoint().equals(ls.getLastPoint()) && ls.getPoints().length > 2)
					? "@@geoRSSLine"
					: "@@geoRSSPolygon";
				
				tag = new Tag(key, value);
				//System.out.println(tag);
				
				if(!way.getTags().contains(tag)) {
					way.getTags().add(tag);
				}
			}
		}
		
		if(counter == 0)
			return null;
		else
			return idToWay.values().iterator();
	}
}






class WayTagIterator
	extends PrefetchIterator<Way>
{
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;
	
	public WayTagIterator(Connection conn)
	{
		this.conn = conn;
	}
	
	public WayTagIterator(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	@Override
	protected Iterator<Way> prefetch()
		throws Exception
	{
		Map<Long, Way> idToWay = new HashMap<Long, Way>();

		String sql = "SELECT id, linestring::geometry FROM ways ";
		if(offset != null)
			sql += "WHERE id > " + offset + " ";
		
		sql += "ORDER BY id ASC ";
		
		if(batchSize != null)
			sql += "LIMIT " + batchSize + " ";
	

		ResultSet rs = conn.createStatement().executeQuery(sql);
		
		while(rs.next()) {
			long id = rs.getLong("id");
			PGgeometry g = (PGgeometry)rs.getObject("linestring");

			offset = offset == null ? id : Math.max(offset, id);

			Way way = new Way(id, 0, (Date)null, null, -1);
			idToWay.put(id, way);
	
			if(g != null) {
				LineString ls = new LineString(g.getValue());
				String value = "";
				for(Point point : ls.getPoints()) {
					if(!value.isEmpty())
						value += " ";

					value += point.getY() + " " + point.getX();
				}
	
				String key =
					(ls.getFirstPoint().equals(ls.getLastPoint()) && ls.getPoints().length > 2)
					? "@@geoRSSLine"
					: "@@geoRSSPolygon";
				
				Tag tag = new Tag(key, value);

				way.getTags().add(tag);
			}
			
		}
		
		if(idToWay.isEmpty())
			return null;
	
		String s = "SELECT way_id, k, v FROM way_tags WHERE way_id IN (" + 
		StringUtil.implode(", ", idToWay.keySet()) + ")";
	
		//System.out.println(s);
		rs = conn.createStatement().executeQuery(s);
	
		
		int counter = 0;
		while(rs.next()) {
			++counter;
	
			long wayId = rs.getLong("way_id");
			String k = rs.getString("k");
			String v = rs.getString("v");
			
			Way way = idToWay.get(wayId);	
			Tag tag = new Tag(k, v);
			way.getTags().add(tag);
		}
		
		return idToWay.values().iterator();
	}
}





/**
 * An iterator for the modified node_tags table.
 * Modified means, that the geom column was added to this table.
 * 
 * Assumes the following columns:
 * id   : long
 * k    : String
 * v    : String
 * geom : geometry
 * 
 * @author Claus Stadler
 *
 */
class NodeTagIteratorDenorm1
	extends PrefetchIterator<Node>
{
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;

	public NodeTagIteratorDenorm1(Connection conn)
	{
		this.conn = conn;
	}
	
	public NodeTagIteratorDenorm1(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	@Override
	protected Iterator<Node> prefetch()
		throws Exception
	{
		String sql = "SELECT DISTINCT node_id FROM node_tags ";
		if(offset != null)
			sql += "WHERE node_id > " + offset + " ";
		
		sql += "ORDER BY node_id ASC ";
		
		if(batchSize != null)
			sql += "LIMIT " + batchSize + " ";
	
		String s = "SELECT node_id, k, v, geom::geometry FROM node_tags WHERE node_id IN (" + sql + ")";
	
		//System.out.println(s);
		ResultSet rs = conn.createStatement().executeQuery(s);
		
		Collection<Node> coll = LGDOSMEntityBuilder.processResultSet(rs, null).values();

		for(Node node : coll) {
			offset = offset == null ? node.getId() : Math.max(offset, node.getId());
		}

		return coll.iterator();
	}	
}





class SimpleWayToRDFTransformer
	implements ITransformer<Way, Model>
{
	private TagMapper tagMapper;

	public SimpleWayToRDFTransformer(TagMapper tagMapper)
	{
		this.tagMapper = tagMapper;
	}

	@Override
	public Model transform(Model model, Way way)
	{
		
		String subject = getSubject(way);
		//Resource subjectRes = model.getResource(subject + "#id");
		
		//generateWGS84(model, subjectRes, node);
		//generateGeoRSS(model, subjectRes, node);
		SimpleNodeToRDFTransformer.generateTags(tagMapper, model, subject, way.getTags());

		return model;
	}

	@Override
	public Model transform(Way way)
	{
		Model model = ModelFactory.createDefaultModel();
		
		return transform(model, way);
	}
	
	private String getSubject(Way way)
	{
		String prefix = "http://linkedgeodata.org/";
		String result = prefix + "way/" + way.getId();
		
		return result;
	}

	//public static void generateGeoRSS(Model model, Resource subjectRes, node);

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
		String outFileName = commandLine.getOptionValue("o", "out.n3");
	
		String batchSizeStr = commandLine.getOptionValue("n", "1000");
	
		int batchSize = Integer.parseInt(batchSizeStr);
		if(batchSize <= 0)
			throw new RuntimeException("Invalid argument for batchsize");
	
		
		boolean exportNodeTags = commandLine.hasOption("xnt");
		boolean exportWayTags = commandLine.hasOption("xwt");
		
		logger.info("ExportNodeTags: " + exportNodeTags);
		logger.info("ExportWayTags: " + exportWayTags);
	
		
		
		logger.info("Loading namespace prefixes");
		// Create a model containing the namespace prefixes
		Model baseModel = ModelFactory.createDefaultModel();
		ModelUtil.read(baseModel, new File("Namespaces.ttl"), "TTL");
		Map<String, String> prefixMap = baseModel.getNsPrefixMap();
		//System.exit(0);
		
		logger.info("Loading mapping rules");
		TagMapper tagMapper = new TagMapper();
		tagMapper.load(new File("LGDMappingRules.xml"));

		
		
		Connection conn = LineStringUpdater.connectPostGIS(hostName, dbName, userName, passWord);
		logger.info("Connected to db");

		
		
		logger.info("Opening output stream: " + outFileName);
		OutputStream out = new FileOutputStream(outFileName);

		baseModel.write(out, "N3");


		if(exportNodeTags) {
			SimpleNodeToRDFTransformer nodeTransformer =
				new SimpleNodeToRDFTransformer(tagMapper);
			
			runNodeTags(conn, batchSize, nodeTransformer, prefixMap, out);		
		}
		
		if(exportWayTags) {
			SimpleWayToRDFTransformer wayTransformer =
				new SimpleWayToRDFTransformer(tagMapper);

			runWayTags(conn, batchSize, wayTransformer, prefixMap, out);
		}
		
		out.flush();
		out.close();
	}

	public static void runNodeTags(Connection conn, int batchSize, ITransformer<Node, Model> nodeTransformer, Map<String, String> prefixMap, OutputStream out)
		throws Exception
	{	
		NodeTagIteratorDenorm1 it = new NodeTagIteratorDenorm1(conn, batchSize);
	
		SimpleStatsTracker tracker = new SimpleStatsTracker();
	
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefixes(prefixMap);
		
		while(it.hasNext()) {
			Node way = it.next();
			
			nodeTransformer.transform(model, way);

			if(model.size() > 10000) {
				writeModel(model, out);
			}

			tracker.update(1);
		}
		writeModel(model, out);
		
	}
	
	public static void runWayTags(Connection conn, int batchSize, ITransformer<Way, Model> wayTransformer, Map<String, String> prefixMap, OutputStream out)
		throws Exception
	{	
		WayTagIterator it = new WayTagIterator(conn, batchSize);
	
		SimpleStatsTracker tracker = new SimpleStatsTracker();

		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefixes(prefixMap);
		
		while(it.hasNext()) {
			Way way = it.next();
			
			wayTransformer.transform(model, way);

			if(model.size() > 10000) {
				writeModel(model, out);
			}

			tracker.update(1);
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
	}
}
