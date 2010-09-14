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
package org.linkedgeodata.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

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
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.TagMapperInstantiator;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.impl.AbstractSimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.tagmapping.client.entity.AbstractSimpleTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleObjectPropertyTagMapperState;
import org.linkedgeodata.util.ConnectionConfig;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.PostGISUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;


/**
 * Data Access Object for the following purposes:
 *     . retrieving the whole LinkedGeoData ontology
 *     . retrieve information about a specific URI (of the LGD-schema)
 *     . reverse-mapping URIs to mapping rules and tags(1)
 *     
 *     
 * (1) Known issue: Currently its not directly possible to tell whether a reverse
 * mapped URI corresponded to an object or a property or even both.
 *     
 * @author Claus Stadler
 *
 */
public class OntologyDAO
	implements ISQLDAO, IHibernateDAO
{
	private static final Logger logger = Logger.getLogger(OntologyDAO.class);
	
	private ITagMapper tagMapper;
	private TagLabelDAO tagLabelDAO = new TagLabelDAO();
	private ITagDAO tagDAO = new TagDAO();
	
	private Session session;
	
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
	
	public ITagMapper getTagMapper()
	{
		return tagMapper;
	}

	public TagLabelDAO getTagLabelDAO()
	{
		return tagLabelDAO;
	}
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

		MultiMap<Tag, IOneOneTagMapper> matches = reverseMapResource(uri);

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

	public MultiMap<Tag, IOneOneTagMapper> reverseMapResource(String uri)
		throws Exception
	{
		MultiMap<Tag, IOneOneTagMapper> result = reverseMapResourceObject(null, uri);

		result.putAll(reverseMapResourceProperty(uri));
		
		return result;
	}
	

	/**
	 * Retrieves the set of mapping rules that produce triples with the given
	 * propertyURI.
	 * 
	 * Note: If the key of a rule's tag pattern is non-null, we assume that
	 * properties are constants - i.e. they do not depend on the tag.
	 * 
	 * However, if a rule's tag pattern is (null, null) then the property
	 * is used as a prefix.
	 * 
	 * @param propertyURI
	 * @return
	 * @throws Exception
	 */
	public MultiMap<Tag, IOneOneTagMapper> reverseMapResourceProperty(String propertyURI)
		throws Exception
	{
		MultiMap<Tag, IOneOneTagMapper> result = new MultiHashMap<Tag, IOneOneTagMapper>();
		if(propertyURI == null)
			return result;
			

		String sql = "Select r From AbstractSimpleTagMapperState r Where r.property = :propertyURI";
		Query query = session.createQuery(sql);
		query.setParameter("propertyURI", propertyURI);
		
		for(Object o : query.list()) {
			AbstractSimpleTagMapperState state = (AbstractSimpleTagMapperState)o;
			
			AbstractSimpleOneOneTagMapper mapper = (AbstractSimpleOneOneTagMapper)state.accept(TagMapperInstantiator.getInstance());

			if(mapper.getTagPattern().getKey() == null)
				continue;
			
			Tag tag = new Tag(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue());

			result.put(tag, mapper);					
		}

		return result;
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
	public MultiMap<Tag, IOneOneTagMapper> reverseMapResourceObject(String propertyURI, String objectURI)
		throws Exception
	{
		MultiMap<Tag, IOneOneTagMapper> matches = new MultiHashMap<Tag, IOneOneTagMapper>();
		
		String sql = "Select r From SimpleObjectPropertyTagMapperState r Where ((r.tagPattern.value Is Not Null AND r.object = :objectURI) OR (r.tagPattern.value Is Null))";
		if(propertyURI != null)
			sql += " And r.property = :propertyURI";
		
		Query query = session.createQuery(sql);
		query.setString("objectURI", objectURI);

		if(propertyURI != null)
			query.setString("propertyURI", propertyURI);
		
		
		for(Object o : query.list()) {
			SimpleObjectPropertyTagMapperState state = (SimpleObjectPropertyTagMapperState)o;
			
			SimpleObjectPropertyTagMapper mapper = TagMapperInstantiator.getInstance().visit(state);
			
			// We have to do a filtering here which would ideally belong to the
			// SQL query
			if(!objectURI.startsWith(mapper.getObject()))
				continue;
			
			
			Tag tag = mapper.getTag(objectURI);

			// Checking if the reverse-mapped tag  exists in the database
			boolean tagExists = tagDAO.doesTagExist(tag);
			logger.debug("Reverse-map: " + tag + " (exists: " + tagExists + ") <- " + objectURI);
			
			if(tag != null && tagExists) {	
				matches.put(new Tag(state.getTagPattern().getKey(), state.getTagPattern().getValue()),
						mapper);					
			}
		}

		return matches;
	}
	

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
	
	
	
	/**
	 * Constructs a model for the whole LGD ontology.
	 * 
	 * @param model
	 * @return
	 * @throws SQLException
	 */
	public Model getOntology(Model model)
		throws SQLException
	{
		if(model == null)
			model = ModelFactory.createDefaultModel();
		
		
		OntologyGeneratorVisitor visitor = new OntologyGeneratorVisitor(model, tagMapper);
		
		
		for(IOneOneTagMapper item : tagMapper.getAllMappers()) {
			
			if(!(item instanceof ISimpleOneOneTagMapper))
				continue;
			
			
			// Optional: filter ontology only to things that actually exist in the DB
			//tagDAO.doesTagExist(item.)
			
			ISimpleOneOneTagMapper x = (ISimpleOneOneTagMapper)item;
			
			/*
			if(x.getProperty().contains("property")) {
				System.out.println("Property");
			}
			*/
			
			x.accept(visitor);
			
			if(x.getTagPattern().getKey() == null && x.getTagPattern().getValue() == null)
				continue;
			
			Tag tag = new Tag(x.getTagPattern().getKey(), x.getTagPattern().getValue());

			// Find out whether an (object*)-resource can be built from the tag
			// * object of an triple
			String resource = x.getObject(tag);
			if(resource != null) {
				// Check if there labels
				MultiMap<String, String> langToLabels = tagLabelDAO.getLabels(new Tag(x.getTagPattern().getKey(), x.getTagPattern().getValue()));
				processLabels(model, resource, langToLabels);
			}
			
		}
		
		return model;
	}


	/*************************************************************************/
	/* Getters for sub DAOs                                                  */
	/*************************************************************************/
	public ITagDAO getTagDAO()
	{
		return tagDAO;
	}

	
	/*************************************************************************/
	/* Getters and Setters for JDBC connections and hibernate sessions       */
	/*************************************************************************/
	@Override
	public Connection getConnection()
	{
		return this.tagDAO.getConnection();
	}
	
	@Override
	public void setConnection(Connection conn)
		throws SQLException
	{
		tagLabelDAO.setConnection(conn);
		tagDAO.setConnection(conn);
	}
	
	
	@Override
	public void setSession(Session session)
	{
		this.session = session;
	}


	@Override
	public Session getSession()
	{
		return this.session;
	}	
}



/* Following stuff is subject to removal
*/
// Determine the first object-value that is not a prefix to the given
// object-URI.
// Unfortunately we have to resort to a native query here.
// SELECT c.property FROM lgd_tag_mapping_simple_base c WHERE c.property > (select b.property from (select a.property from lgd_tag_mapping_simple_base a where a.property <= 'http://linkedgeodata.org/ontology/stair' ORDER BY a.property DESC) b WHERE  strpos('http://linkedgeodata.org/ontology/stair', b.property) != 1 LIMIT 1) AND c.property <= 'http://linkedgeodata.org/ontology/stair';
/*
String sqlLowerBound = "SELECT b.object FROM (SELECT a.object FROM lgd_tag_mapping_simple_object_property a WHERE a.object <= :objectURI ORDER BY a.object DESC) b WHERE  strpos(:objectURI, b.object) != 1 LIMIT 1";
SQLQuery nativeQuery = session.createSQLQuery(sqlLowerBound);
nativeQuery.setParameter("objectURI", objectURI);

String lowerBound = (String)nativeQuery.uniqueResult();
System.out.println("LOWER BOUND = " + lowerBound);
*/

//String sql = "Select r From SimpleObjectPropertyTagMapperState r Where ((r.tagPattern.value Is Not Null AND r.object = :objectURI) OR (r.tagPattern.value Is Null AND :objectURI Like r.object || '%'))";

/*
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
		* /
		
		//session.persist(mapping);
		tx.commit();		
	}
	
	
	/**
	 * TODO Make this a unit test
	 * 
	 * @param conn
	 * @param ontologyDAO
	 * @throws Exception
	 * /
	private static void testSQLConstraints(Connection conn, OntologyDAO ontologyDAO)
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
		* /
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

		Model model = ModelFactory.createDefaultModel();
		System.out.println(ModelUtil.toString(dao.getOntology(model)));
		
		if(true)
			return;

/*
		Model model = ModelFactory.createDefaultModel();
		//model = dao.describe("http://linkedgeodata.org/resource/JOSM", model);
		model = dao.describe("http://linkedgeodata.org/ontology/amenity_parking", model);
		System.out.println(ModelUtil.toString(model));
* /
		testSQLConstraints(conn, dao);
				
		//System.out.println(tagFilterDAO.restrictByClass("http://linkedgeodata.org/ontology/railway_station", "alias"));
		
		//System.out.println(tagFilterDAO.restrictByLabel("Dresden", null, TagFilterUtils.MatchMode.EQUALS, "alias"));

		
		
		//dao.describeObjectPropertyObject("http://linkedgeodata.org/resource/JOSM", model);
	}
*/
