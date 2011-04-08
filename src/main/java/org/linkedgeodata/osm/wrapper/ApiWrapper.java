package org.linkedgeodata.osm.wrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;

import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.jtriplify.IRestApi;
import org.linkedgeodata.jtriplify.methods.Pair;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.impl.OSMEntityToRDFTransformer;
import org.linkedgeodata.osm.osmosis.plugins.INodeSerializer;
import org.linkedgeodata.osm.osmosis.plugins.VirtuosoOseNodeSerializer;
import org.linkedgeodata.scripts.LiveSync;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.StreamUtil;
import org.linkedgeodata.util.URIUtil;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.xml.v0_6.impl.OsmHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDFS;


class OsmEntityToRdfTransformer
	implements Sink
{
	private Logger logger = LoggerFactory.getLogger(OsmEntityToRdfTransformer.class);
	
	private Model model = ModelFactory.createDefaultModel();
	private ITransformer<Entity, Model> entityTransformer;
	
	public OsmEntityToRdfTransformer(ITransformer<Entity, Model> entityTransformer) {
		this.entityTransformer = entityTransformer;
	}
	
	@Override
	public void complete()
	{
	}

	@Override
	public void release()
	{
	}

	@Override
	public void process(EntityContainer ec)
	{
		//logger.info(ec.getEntity().toString());
		
		entityTransformer.transform(model, ec.getEntity());
	}
	
	public Model getModel()
	{
		return model;
	}
}

public class ApiWrapper
	implements IRestApi
{
	private String baseUri;
	private ITagMapper tagMapper;
	private Logger logger = LoggerFactory.getLogger(ApiWrapper.class);
	private ITransformer<Model, Model> postTransformer;
	private LGDVocab vocab = new LGDVocab();
	
	/*
	public ApiWrapper() {
		this.baseUri = "http://openstreetmap.org/api/0.6/";
	}
	*/

	public ApiWrapper(ITagMapper tagMapper, Map<String, String> config)
		throws Exception
	{
		this.baseUri = "http://www.openstreetmap.org/api/0.6/";
		this.tagMapper = tagMapper;

		
		postTransformer = LiveSync.getPostTransformer(config, vocab);
	}

	
	private Model fetchAndTransform(String uri)
		throws SAXException, IOException
	{
		logger.debug("Accessing <" + uri + ">");
		URL url = new URL(uri);

		SAXParser parser = LiveSync.createParser();
		
		
		INodeSerializer nodeSerializer = new VirtuosoOseNodeSerializer();
		ITransformer<Entity, Model> entityTransformer = new OSMEntityToRDFTransformer(
				tagMapper, vocab, nodeSerializer);
		
		OsmEntityToRdfTransformer workflow = new OsmEntityToRdfTransformer(entityTransformer);
		
		parser.parse(url.openStream(), new OsmHandler(workflow, true));
		
		Model tmp = workflow.getModel();

		return postTransformer.transform(tmp);
	}
	
	@Override
	public Model getNode(Long id) throws Exception
	{
		return fetchAndTransform(baseUri + "nodes?nodes=" + id);
	}

	@Override
	public Model getWayNode(Long id) throws Exception
	{
		return fetchAndTransform(baseUri + "ways?ways=" + id);
	}

	@Override
	public Model getWay(Long id) throws Exception
	{
		return fetchAndTransform(baseUri + "ways?ways=" + id);
	}

	@Override
	public Model publicGetEntitiesWithinRadius(Double lat, Double lon,
			Double radius, String className, String language, String matchMode,
			String label, Long offset, Long limit) throws Exception
	{
		//Ellipse2D circle = new Ellipse2D.Double();

		//&page=0

		//return fetchAndTransform(baseUri + "bbox?ways=" + id);

		
		
		// TODO Auto-generated method stub
		return null;
	}


	public String compileQuery(String className, String language,
			String matchMode, String label, Long offset, Long limit)
	{
		String p0 = "?s ?p ?o .\n";
		
		String p1 = language == null ? "" : "Filter(langMatches(?l, \"" + language + "\")) .\n";
		
		
		String p2 = "";
		Pair<String, String> pair = getMatchConfig(language, matchMode);
		if(pair != null) {
			p2 = "Filter(regex(?l, \"" + pair.getKey() + "\", \"" + pair.getValue() + "\")) .\n";
		}
		
		String p3 = "";
		if(!(p1.isEmpty() && p2.isEmpty())) {
			p3 = "?s <" + RDFS.label + "> ?l .\n";
		}
		
		String p4 = className == null ? "" : "?s a <http://linkedgeodata.org/ontology/" + className + "> . \n";

		String p5 = limit == null ? "" : "Limit " + limit + "\n"; 
		String p6 = offset == null ? "" : "Offset " + offset + "\n";
		
		String query = "Construct {?s ?p ?o .} {\n" + p0 + p1 + p2 + p3 + p4 + "}\n" + p5 + p6;

		return query;
	}
	
	@Override
	public Model publicGetEntitiesWithinRect(Double latMin, Double latMax,
			Double lonMin, Double lonMax, String className, String language,
			String matchMode, String label, Long offset, Long limit)
			throws Exception
	{
		Model model = fetchAndTransform(baseUri + "map?bbox=" + latMin + "," + lonMin + "," + latMax + "," + lonMax);
		//Model model = ModelFactory.createDefaultModel();
		
		//model.write(System.out, "N3");
		
		String query = compileQuery(className, language, matchMode, label, offset, limit);
		
		System.out.println(query);
		
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		Model result = qe.execConstruct();
		//QueryExecutionFactory.create(queryStr);
		
		// TODO Do filtering
		return result;
	}

	@Override
	public Model publicDescribe(String uri) throws Exception
	{
		return null;
	}
	
	
	/**
	 * Returns a pattern and mode arguments to be used for a sparql regex.
	 * 
	 * @param label
	 * @param matchMode
	 * @return
	 */
	private Pair<String, String> getMatchConfig(String label, String matchMode)
	{
		if(label == null || matchMode == null)
			return null;
		
		String mode = "";
		if(matchMode.equalsIgnoreCase("contains")) {
			mode = "i";
			label = ".*" + label.replace("*", "\\*") + ".*";
		}
		else if(matchMode.equalsIgnoreCase("startsWith")) {
			mode = "i";
			label = "^" + label.replace("%", "\\%") + "*";
		}
		if(matchMode.equalsIgnoreCase("ccontains")) {
			label = ".*" + label.replace("*", "\\*") + ".*";
		}
		else if(matchMode.equalsIgnoreCase("cstartsWith")) {
			label = "^" + label.replace("%", "\\%") + "*";
		}

		return new Pair<String, String>(label, mode);
	}	
	
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");

		InMemoryTagMapper tagMapper = new InMemoryTagMapper();
		tagMapper.load(new File("config/LiveSync/TagMappings.xml"));

		
		File ontologyFile = new File("/tmp/ontology.nt");
		if(!ontologyFile.exists()) {
			OutputStream os = new FileOutputStream(ontologyFile);
			
			URLConnection connection = new URL("http://linkedgeodata.org/ontology/").openConnection();
			connection.addRequestProperty("accept", "text/plain");
			StreamUtil.copyThenClose(connection.getInputStream(), os);
		}
		
		Map<String, String> config = new HashMap<String, String>();
		config.put("matInfSchemaFile", ontologyFile.getAbsolutePath());
		
		
		IRestApi restApi = new ApiWrapper(tagMapper, config);
				
		
		//restApi.getNode(259212302l).write(System.out, "N3");
		//restApi.getWay(3999478l).write(System.out, "N3");
		restApi.publicGetEntitiesWithinRect(16.03, 16.05, 47.98, 48.00, "Amenity", null, null, null, null, null).write(System.out, "N3");

		
		//restApi.publicGetEntitiesWithinRect(16.03, 16.05, 47.98, 48.00, "Pub", "en", null, null, null, null).write(System.out, "N3");
		//restApi.publicGetEntitiesWithinRect(16.03, 16.05, 47.98, 48.00, "Pub", "en", "contains", "a", null, null).write(System.out, "N3");
		//restApi.publicGetEntitiesWithinRect(16.03, 16.05, 47.98, 48.00, "Pub", "en", "startsWith", "b", null, null).write(System.out, "N3");
		//restApi.publicGetEntitiesWithinRect(16.03, 16.05, 47.98, 48.00, "Pub", "en", "ccontains", "c", null, null).write(System.out, "N3");
		//restApi.publicGetEntitiesWithinRect(16.03, 16.05, 47.98, 48.00, "Pub", "en", "cstartsWith", "d", null, null).write(System.out, "N3");
		
		
	}
}
