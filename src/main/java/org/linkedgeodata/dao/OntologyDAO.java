package org.linkedgeodata.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.osm.mapping.TagMapperInstantiator;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.tagmapping.client.entity.AbstractSimpleTagMapperState;
import org.linkedgeodata.util.ConnectionConfig;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.PostGISUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OntologyDAO
{
	private ITagMapper tagMapper;
	private TagLabelDAO tagLabelDAO;
	
	
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
		

		Connection conn = PostGISUtil.connectPostGIS(new ConnectionConfig("localhost", "unittest_lgd", "postgres", "postgres"));
		
		TagMapperDAO tagMapper = new TagMapperDAO();
		TagLabelDAO tagLabelDAO = new TagLabelDAO(conn);
		OntologyDAO dao = new OntologyDAO(tagMapper, tagLabelDAO);
		
		Model model = dao.describe("http://linkedgeodata.org/ontology/amenity_parking", null);
		System.out.println(ModelUtil.toString(model));
	}
	
	public OntologyDAO(ITagMapper tagMapper, TagLabelDAO tagLabelDAO)
	{
		this.tagMapper = tagMapper;
		this.tagLabelDAO = tagLabelDAO;
	}
	
	public Model describe(String uri, Model model)
		throws SQLException
	{
		// Catch the property namespace
		//ILGDVocab vocab;
		//if(uri.startsWith(vocab.getPropertyNs());
		
		if(model == null)
			model = ModelFactory.createDefaultModel();


		
		// Reverse-map the uri to the corresponding tags
		
		// For the reverse mapping we need to consider resources, the given uri stats with
		// e.g. if uri = lgd:AmenityPark
		// then we also need to retrieve lgd:Amenity
		//
		
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();
		
		// FIXME This query causes a sequential scan, although actually the
		// btree index on the resource column would be capable of efficiently
		// answering it. However, I yet have to find out how to rephrase the
		// query.

		//List<Pair<IOneOneTagMapper, Tag>> matches = new ArrayList<Pair<IOneOneTagMapper, Tag>>();
		MultiMap<Tag, IOneOneTagMapper> matches = new MultiHashMap<Tag, IOneOneTagMapper>();
		
		// Check if there are exact matches
		String query = "Select r From AbstractSimpleTagMapperState r Where r.resource = :uri";
		for(Object o : session.createQuery(query).setString("uri", uri).list()) {
			AbstractSimpleTagMapperState state = (AbstractSimpleTagMapperState)o;
			
			IOneOneTagMapper mapper = TagMapperInstantiator.getInstance().instantiate(state);
			
			matches.put(new Tag(state.getTagPattern().getKey(), state.getTagPattern().getValue()),
					mapper);					
		}
		
		
		System.out.println("After exact " + matches);
		
		if(matches.isEmpty()) {
			// Check if there is a match where the given uri starts with the resource of a rule
			query = "Select r From AbstractSimpleTagMapperState r Where :uri Like r.resource || '%'";
			
			for(Object o : session.createQuery(query).setString("uri", uri).list()) {
				AbstractSimpleTagMapperState state = (AbstractSimpleTagMapperState)o;
	
				Tag tag = getTag(uri, state.getResource(), state.getTagPattern().getKey(), state.getTagPattern().getValue());

				if(tag != null) {
					IOneOneTagMapper mapper = TagMapperInstantiator.getInstance().instantiate(state);
					
					matches.put(new Tag(state.getTagPattern().getKey(), state.getTagPattern().getValue()),
							mapper);					
				}
			}
		}		
		
		System.out.println("After fuzzy " + matches);
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

		
		return model;
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
	
	public static Tag getTag(String uri, String resource, String k, String v)
	{
		if(k == null)
			return null;
		
		if(uri.equals(resource))
			return new Tag(k, v);
		
		if(!uri.startsWith(resource))
			return null;
		
		v = uri.substring(resource.length());
		
		return new Tag(k, v);
	}
}
