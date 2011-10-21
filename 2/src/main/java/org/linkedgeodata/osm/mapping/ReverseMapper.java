package org.linkedgeodata.osm.mapping;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.apache.commons.collections15.MultiMap;
import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.dao.ITagDAO;
import org.linkedgeodata.dao.OntologyDAO;
import org.linkedgeodata.dao.TagDAO;
import org.linkedgeodata.i18n.gettext.EntityResolver2;
import org.linkedgeodata.i18n.gettext.IEntityResolver;
import org.linkedgeodata.scripts.LiveSync;
import org.linkedgeodata.util.PostGISUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;


/**
 * Given a model of RDF statements, construct a set of OSM entities
 * 
 * @author raven
 *
 */
public class ReverseMapper
{
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");

		
		Options cliOptions = new Options();
		//cliOptions.addOption("c", "config", true, "Config filename");
		
		
		//CommandLineParser cliParser = new GnuParser();
		//CommandLine commandLine = cliParser.parse(cliOptions, args);

		
		//String configFileName = commandLine.getOptionValue("c", "config.ini");

		String configFileName = "src/main/java/org/linkedgeodata/imports/icons/brion_quinion/hackconfig.ini";
		File configFile = new File(configFileName);

		Map<String, String> config = LiveSync.loadIniFile(configFile);

	
		Connection conn = PostGISUtil.connectPostGIS(
				config.get("osmDb_hostName"), config.get("osmDb_dataBaseName"),
				config.get("osmDb_userName"), config.get("osmDb_passWord"));
		
		ITagDAO tagDao = new TagDAO();
		tagDao.setConnection(conn);

		
		InMemoryTagMapper tagMapper = new InMemoryTagMapper();
		tagMapper.load(new File("config/LiveSync/TagMappings.xml"));
		
		IEntityResolver resolver = new EntityResolver2(tagMapper);

		
		Model model = ModelFactory.createDefaultModel();
		
		Session session = TagMappingDB.getSession();

		OntologyDAO dao = new OntologyDAO(tagMapper);
		dao.setSession(session);
		dao.setConnection(conn);
		
		Transaction tx = session.beginTransaction();
		//MultiMap<Tag, IOneOneTagMapper> rev = dao.reverseMapResourceObject(RDF.type.getURI(), "http://linkedgeodata.org/ontology/School");
		//MultiMap<Tag, IOneOneTagMapper> rev = dao.reverseMapResourceObject(RDF.type.getURI(), "http://linkedgeodata.org/ontology/TourismHotel");
		MultiMap<Tag, IOneOneTagMapper> rev = dao.reverseMapResourceObject(RDF.type.getURI(), "http://linkedgeodata.org/ontology/Amenity");
		System.out.println(rev.keySet());
		tx.commit();
		//tagDao.doesTagExist(tag)
	}

	
	public static boolean isMoreSpecific(Tag a, Tag b) {
		//if(a.getKey() == null &&
		return false;
	}
	
	/**
	 * Takes a model and attempts to generate OSM entities from it. 
	 * 
	 * @param source
	 * @return
	 */
	public static Set<Entity> reverseMap(Model model, OntologyDAO dao, ILGDVocab vocab) {
		Map<Resource, Entity> resourceToEntity = new HashMap<Resource, Entity>();
		
		//dao.
		
		for(Statement stmt : model.listStatements().toList()) {

			Entity entity = null;
			if(resourceToEntity.containsKey(stmt.getSubject())) {
				entity = resourceToEntity.get(stmt.getSubject());
			} else {
				entity = vocab.createEntity(stmt.getSubject());
				resourceToEntity.put(stmt.getSubject(), entity);
			}
			
			
		}
		
		
		return null;
	}
}
