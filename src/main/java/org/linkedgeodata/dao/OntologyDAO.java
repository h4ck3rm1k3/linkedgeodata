package org.linkedgeodata.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.access.TagFilterUtils;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.osm.mapping.TagMapperInstantiator;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.RegexTextTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.tagmapping.client.entity.RegexTextTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleObjectPropertyTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleTagPattern;
import org.linkedgeodata.util.ConnectionConfig;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.PostGISUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OntologyDAO
	implements ISQLDAO
{
	private static final Logger logger = Logger.getLogger(OntologyDAO.class);
	
	private ITagMapper tagMapper;
	private TagLabelDAO tagLabelDAO = new TagLabelDAO();
	private ITagDAO tagDAO = new TagDAO();
	
	public static void createTestProperty()
	{
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();
		
		/*
		SimpleObjectPropertyTagMapperState mapping = new SimpleObjectPropertyTagMapperState(
				"http://linkedgeodata.org/ontology/createdBy",
				"http://linkedgeodata.org/ontology/",
				true,
				new SimpleTagPattern("created_by", null),
				false);
		*/
		
		/*
		RegexTextTagMapperState mapping = new RegexTextTagMapperState(
				"http://www.w3.org/2000/01/rdf-schema#label",
				"name:([^:]+)",
				false);
		*/
		
		//session.persist(mapping);
		tx.commit();		
	}
	
	
	private static void testSQLConstraints(OntologyDAO ontologyDAO)
		throws Exception
	{
		TagFilterUtils tagFilterDAO = new TagFilterUtils(ontologyDAO);

		String filter;
		/*
		filter =
			tagFilterDAO.restrictByObject(
					"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
					"http://linkedgeodata.org/ontology/railway_station",
					"alias");

		System.out.println(filter);
		*/
		filter =
			tagFilterDAO.restrictByText(
					"http://www.w3.org/2000/01/rdf-schema#label",
					"%Haup't%",
					"en",
					TagFilterUtils.MatchMode.LIKE,
					"alias");

		System.out.println(filter);
		
		
	}
	
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
		
		//createTestProperty();
		//if(true) return;
		
		Connection conn = PostGISUtil.connectPostGIS(new ConnectionConfig("localhost", "unittest_lgd", "postgres", "postgres"));
		
		TagMapperDAO tagMapper = new TagMapperDAO();
		
		//Map<Long, Long> map = nodeStatsDAO.doesNodeTagExist(tag)(Collections.singleton(0l), 0, "amenity", "parking");
		//System.out.println("counts: " + map);
		
		OntologyDAO dao = new OntologyDAO(tagMapper, conn);


/*
		Model model = ModelFactory.createDefaultModel();
		//model = dao.describe("http://linkedgeodata.org/resource/JOSM", model);
		model = dao.describe("http://linkedgeodata.org/ontology/amenity_parking", model);
		System.out.println(ModelUtil.toString(model));
*/
		testSQLConstraints(dao);
				
		//System.out.println(tagFilterDAO.restrictByClass("http://linkedgeodata.org/ontology/railway_station", "alias"));
		
		//System.out.println(tagFilterDAO.restrictByLabel("Dresden", null, TagFilterUtils.MatchMode.EQUALS, "alias"));

		
		
		//dao.describeObjectPropertyObject("http://linkedgeodata.org/resource/JOSM", model);
	}

	
	public OntologyDAO(ITagMapper tagMapper)
	{
		this.tagMapper = tagMapper;
	}

	public OntologyDAO(ITagMapper tagMapper, Connection conn)
		throws SQLException
	{
		this.tagMapper = tagMapper;
		
		setConnection(conn);
	}
	
	@Override
	public void setConnection(Connection conn)
		throws SQLException
	{
		tagLabelDAO.setConnection(conn);
		tagDAO.setConnection(conn);
	}
	
	
	public ITagDAO getTagDAO()
	{
		return tagDAO;
	}
	
	/**
	 * Attempts to find information about a resource which denotes an 
	 * ObjectProperty.
	 * 
	 * @param uri
	 * @param model
	 * @return
	 * @throws SQLException
	 * /
	public Model describeObjectProperty(String uri, Model model)
		throws SQLException
	{
		// Catch the property namespace
		//ILGDVocab vocab;
		//if(uri.startsWith(vocab.getPropertyNs());		
		if(model == null)
			model = ModelFactory.createDefaultModel();
		
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();
		
		MultiMap<Tag, IOneOneTagMapper> matches = new MultiHashMap<Tag, IOneOneTagMapper>();
		
		// Check if there are exact matches
		String query = "Select r From SimpleObjectPropertyTagMapperState r Where r.property = :uri";
		for(Object o : session.createQuery(query).setString("uri", uri).list()) {
			AbstractSimpleTagMapperState state = (AbstractSimpleTagMapperState)o;
			
			IOneOneTagMapper mapper = TagMapperInstantiator.getInstance().instantiate(state);
			
			matches.put(new Tag(state.getTagPattern().getKey(), state.getTagPattern().getValue()),
					mapper);					
		}
		
		
		logger.debug("Found matches: " + matches);
		tx.commit();

		
		OntologyGeneratorVisitor visitor = new OntologyGeneratorVisitor(model, tagMapper);
		for(Map.Entry<Tag, Collection<IOneOneTagMapper>> entry : matches.entrySet()) {
			Tag tag = entry.getKey();
			
			
			for(IOneOneTagMapper item : entry.getValue()) {
				if(item instanceof ISimpleOneOneTagMapper) {
					ISimpleOneOneTagMapper x = (ISimpleOneOneTagMapper)item;
					x.accept(visitor);
					
					String resource = x.getResource(entry.getKey());
					
					MultiMap<String, String> langToLabels = tagLabelDAO.getLabels(tag);
					
					processLabels(model, resource, langToLabels);
				}
			}
		}
		
		
		// Filter the model to the desired subject as 
		// some definitions for referenced resources may have been generated.
		model = ModelUtil.filterBySubject(model, model.createResource(uri));

		
		return model;
	}
	*/
	

	/**
	 * 
	 * @param uri
	 * @param model
	 * @return
	 * @throws Exception 
	 */
	public Model describe(String uri, Model model)
		throws Exception
	{
		if(model == null)
			model = ModelFactory.createDefaultModel();

		MultiMap<Tag, IOneOneTagMapper> matches = reverseMapResource(null, uri);

		OntologyGeneratorVisitor visitor = new OntologyGeneratorVisitor(model, tagMapper);
		for(Map.Entry<Tag, Collection<IOneOneTagMapper>> entry : matches.entrySet()) {
			Tag tag = entry.getKey();
			
			
			for(IOneOneTagMapper item : entry.getValue()) {
				if(item instanceof ISimpleOneOneTagMapper) {
					ISimpleOneOneTagMapper x = (ISimpleOneOneTagMapper)item;
					x.accept(visitor);
					
					String resource = x.getObject(entry.getKey());
					
					MultiMap<String, String> langToLabels = tagLabelDAO.getLabels(tag);
					
					processLabels(model, resource, langToLabels);
				}
			}
		}
		
		
		// Filter the model to the desired subject as 
		// some definitions for referenced resources may have been generated.
		model = ModelUtil.filterBySubject(model, model.createResource(uri));

		
		return model;		
	}
	
	
	/**
	 * Maps a given URI back to a set of tags an the corresponding Mapping
	 * rules that could produce that URI.
	 * 
	 * 
	 * @param uri
	 * @return
	 * @throws Exception 
	 */
	public MultiMap<Tag, IOneOneTagMapper> reverseMapResource(String propertyURI, String objectURI)
		throws Exception
	{
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();

		MultiMap<Tag, IOneOneTagMapper> matches = new MultiHashMap<Tag, IOneOneTagMapper>();
		String sql = "Select r From SimpleObjectPropertyTagMapperState r Where ((r.tagPattern.value Is Not Null AND r.object = :objectURI) OR (r.tagPattern.value Is Null AND :objectURI Like r.object || '%'))";

		if(propertyURI != null)
			sql += " And r.property = :propertyURI";
		
		Query query = session.createQuery(sql);
		query.setString("objectURI", objectURI);
		if(propertyURI != null)
			query.setString("propertyURI", propertyURI);
		
		
		for(Object o : query.list()) {
			SimpleObjectPropertyTagMapperState state = (SimpleObjectPropertyTagMapperState)o;
			
			SimpleObjectPropertyTagMapper mapper = TagMapperInstantiator.getInstance().visit(state);
			
			Tag tag = mapper.getTag(objectURI);

			// Checking if the tag even exists
			boolean tagExists = tagDAO.doesTagExist(tag);
			logger.debug("Reverse-map: " + tag + " (exists: " + tagExists + ") <- " + objectURI);
			
			if(tag != null && tagExists) {	
				matches.put(new Tag(state.getTagPattern().getKey(), state.getTagPattern().getValue()),
						mapper);					
			}
		}

		//logger.debug("Found matches: " + matches);
		tx.commit();

		return matches;
	}

		/*
		if(matches.isEmpty()) {
			// Check if there is a match where the given uri starts with the resource of a rule
			query = "Select r From SimpleObjectPropertyTagMapperState r Where :uri Like r.resource || '%'";
			
			for(Object o : session.createQuery(query).setString("uri", uri).list()) {
				SimpleObjectPropertyTagMapperState state = (SimpleObjectPropertyTagMapperState)o;
				
				SimpleObjectPropertyTagMapper mapper = TagMapperInstantiator.getInstance().visit(state);
				
				Tag tag = mapper.getTag(uri);
				logger.debug("Reverse-map: " + tag + " <- " + uri);
				
				// Checking if the tag even exists
				boolean tagExists = tagDAO.doesTagExist(tag);
				logger.debug("Reverse-map: " + tag + " (exists: " + tagExists + ") <- " + uri);
				

				
				if(tag != null && tagExists) {	
					matches.put(new Tag(state.getTagPattern().getKey(), state.getTagPattern().getValue()),
							mapper);					
				}
			}
			logger.debug("Found fuzzy matches: " + matches);
		}		
		*/
	
	private static void processLabels(Model model, String resource, MultiMap<String, String> langToLabels)
	{
		Resource subject = model.createResource(resource);
		
		for(Map.Entry<String, Collection<String>> entry : langToLabels.entrySet()) {
			String lang = entry.getKey();

			for(String label : entry.getValue()) {
				
				model.addLiteral(
						subject,
						RDFS.label,
						model.createLiteral(label, lang));						
			}
		}
	}	
}
