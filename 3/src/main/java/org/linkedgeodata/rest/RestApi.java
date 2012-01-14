package org.linkedgeodata.rest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.aksw.commons.sparql.api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.commons.sparql.api.cache.extra.CacheCoreEx;
import org.aksw.commons.sparql.api.cache.extra.CacheCoreH2;
import org.aksw.commons.sparql.api.cache.extra.CacheEx;
import org.aksw.commons.sparql.api.cache.extra.CacheExImpl;
import org.aksw.commons.sparql.api.core.QueryExecutionFactory;
import org.aksw.commons.sparql.api.delay.extra.Delayer;
import org.aksw.commons.sparql.api.delay.extra.DelayerDefault;
import org.aksw.commons.sparql.api.http.QueryExecutionFactoryHttp;
import org.aksw.commons.sparql.api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.commons.util.strings.StringUtils;
import org.hibernate.engine.jdbc.StreamUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;






@Path("/api/")
public class RestApi {

	private static LGDVocab vocab = new LGDVocabDefault();
	
	private QueryExecutionFactory<?> qeFactory;
	
	private Map<String, String>	prefixMap = new HashMap<String, String>();
	
	
	public RestApi() throws ClassNotFoundException, SQLException {
		String service = "http://test.linkedgeodata.org/sparql";
		//service = "http://localhost:9999/sparql";
		//service = "http://linkedgeodata.org/sparql";
		
		QueryExecutionFactory<?> tmp = new QueryExecutionFactoryHttp(service);
		
		CacheCoreEx cacheBackend = CacheCoreH2.create("sparql", 24l * 60l * 60l * 1000l, true);
		CacheEx cacheFrontend = new CacheExImpl(cacheBackend); 
		
		//QueryExecutionFactory<?> 
		tmp = new QueryExecutionFactoryCacheEx(tmp, cacheFrontend);

		
		tmp = new QueryExecutionFactoryPaginated(tmp, 1000);
		
		qeFactory = tmp;
		
		//prefixMap.put(arg0, arg1);

	}
	
	
	private Model createModel()
	{
		Model result = ModelFactory.createDefaultModel();
		result.setNsPrefixes(prefixMap);

		return result;
	}

	@GET
	@Path("/ontology/")
	public Model getOntology() throws Exception
	{
		String query = "Construct { ?s ?p ?o } { ?s a <" + OWL.Class.toString() + "> . ?s ?p ?o . }";
		Model result = qeFactory.createQueryExecution(query).execConstruct();
		
		//result.write(System.out, "TURTLE");
		
		return result;
	}

	
	
	class JsonResponseItem {
		String osm_type;
		long osm_id;
		
		public JsonResponseItem() {
		}

		public String getOsm_type() {
			return osm_type;
		}

		public void setOsm_type(String osm_type) {
			this.osm_type = osm_type;
		}

		public long getOsm_id() {
			return osm_id;
		}

		public void setOsm_id(long osm_id) {
			this.osm_id = osm_id;
		}
	}
	
	private Delayer delayer = new DelayerDefault(1000);
	
	public Model publicGeocode(String queryString)
		throws Exception
	{
		delayer.doDelay();

		//queryString = StringUtils.urlDecode(queryString);
		
		String service = "http://open.mapquestapi.com/nominatim/v1/search";
//http://nominatim.openstreetmap.org/search
			
		String uri = service + "?format=json&q=" + queryString;
		
		URL url = new URL(uri);
		URLConnection c = url.openConnection();
		c.setRequestProperty("User-Agent", "http://linkedgeodata.org, mailto:cstadler@informatik.uni-leipzig.de");
		
		InputStream ins = c.getInputStream();
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StreamUtils.copy(ins, out);
		
		String json = out.toString();
		
		Gson gson = new Gson();
		
		Type collectionType = new TypeToken<Collection<JsonResponseItem>>(){}.getType();
		Collection<JsonResponseItem> items = gson.fromJson(json, collectionType);
		
		//gson.fromJson(json, JsonResponseItem.class);
		
		List<Resource> resources = new ArrayList<Resource>();
		for(JsonResponseItem item : items) {
			Resource resource = null;
			
			
			
			
			if(item.getOsm_type().equals("node")) {
				resource = vocab.createNIRNodeURI(item.getOsm_id());
			} else if(item.getOsm_type().equals("way")) {
				resource = vocab.createNIRWayURI(item.getOsm_id());
			} else {
				continue;
			}
			
			resources.add(resource);
		}
		
		Model result = createModel();

		
		
		QueryExecutionFactoryHttp qef = new QueryExecutionFactoryHttp("http://live.linkedgeodata.org/sparql", Collections.singleton("http://linkedgeodata.org"));

		for(Resource resource : resources) {
			String serviceUri = "http://test.linkedgeodata.org/sparql?format=text%2Fplain&default-graph-uri=http%3A%2F%2Flinkedgeodata.org&query=DESCRIBE+<" + StringUtils.urlEncode(resource.toString()) + ">";
			URL serviceUrl = new URL(serviceUri);
			URLConnection conn = serviceUrl.openConnection();
			conn.addRequestProperty("Accept", "text/plain");
			
			/*
			ByteArrayOutputStream out1 = new ByteArrayOutputStream();
			StreamUtils.copy(serviceUrl.openStream(), out1);
			String nt = out1.toString();
			*/
			
			InputStream in = null;
			try {
				in = conn.getInputStream();
				result.read(in, null, "N-TRIPLE");
			} finally {
				if(in != null) {
					in.close();
				}
			}
			

			/*
			String tmp = "DESCRIBE <" + resource + ">";
			System.out.println(tmp);
			QueryExecution qe = qef.createQueryExecution(tmp);
			qe.execDescribe(result);
			*/
			
		}
		
		return result;
	}
	
	public Model describe(String uri)
	{
		String query = "Describe <" + uri + ">";
		
		return qeFactory.createQueryExecution(query).execConstruct();
	}

	/*************************************************************************/
	/* Helper methods for processing class and label restrictions */
	/*************************************************************************/
	/*
	private Pair<String, TagFilterUtils.MatchMode> getMatchConfig(String label,
			String matchMode)
	{
		if (label == null || matchMode == null)
			return null;

		TagFilterUtils.MatchMode mm = TagFilterUtils.MatchMode.EQUALS;
		if (matchMode.equalsIgnoreCase("contains")) {
			mm = TagFilterUtils.MatchMode.ILIKE;
			label = "%" + label.replace("%", "\\%") + "%";
		} else if (matchMode.equalsIgnoreCase("startsWith")) {
			mm = TagFilterUtils.MatchMode.ILIKE;
			label = label.replace("%", "\\%") + "%";
		}
		if (matchMode.equalsIgnoreCase("ccontains")) {
			mm = TagFilterUtils.MatchMode.LIKE;
			label = "%" + label.replace("%", "\\%") + "%";
		} else if (matchMode.equalsIgnoreCase("cstartsWith")) {
			mm = TagFilterUtils.MatchMode.LIKE;
			label = label.replace("%", "\\%") + "%";
		}

		return new Pair<String, TagFilterUtils.MatchMode>(label, mm);
	}

	private List<String> getEntityTagCondititions(String className,
			String label, String language, String matchMode) throws Exception
	{
		if (language != null && language.equalsIgnoreCase("any"))
			language = null;

		Pair<String, TagFilterUtils.MatchMode> lmm = getMatchConfig(label,
				matchMode);

		// FIXME Add this to some kind of facade
		TagFilterUtils filterUtil = new TagFilterUtils(
				lgdRDFDAO.getOntologyDAO());
		filterUtil.setSession(lgdRDFDAO.getOntologyDAO().getSession());

		List<String> entityTagConditions = new ArrayList<String>();

		if (className != null)
			entityTagConditions.add(filterUtil.restrictByObject(
					RDF.type.toString(), "http://linkedgeodata.org/ontology/"
							+ className, "$$"));

		if (label != null)
			entityTagConditions.add(filterUtil.restrictByText(
					RDFS.label.toString(), lmm.getKey(), language,
					lmm.getValue(), "$$"));

		return entityTagConditions;
	}
	*/


}
