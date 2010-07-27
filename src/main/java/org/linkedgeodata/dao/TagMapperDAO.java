package org.linkedgeodata.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.dom4j.tree.AbstractEntity;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.osm.mapping.TagMapperInstantiator;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.RegexTagPattern;
import org.linkedgeodata.tagmapping.client.entity.AbstractSimpleTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.AbstractTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.RegexTextTagMapperState;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TagMapperDAO
	implements ITagMapper
{
	private static final Logger logger = Logger.getLogger(TagMapperDAO.class);
	
	
	/*
	public Collection<String> getClassKeys()
	{
	}
	
	public Collection<String> getClassTags()
	{
	}
	*/
	
	
	public List<AbstractTagMapperState> getTagMappings(String k, String v)
	{
		//int limit = 100;
		//limit = constrain(limit, 0, 100);
		
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();

		//k = k.replace("%", "\\%") + "%";
		//v = v.replace("%", "\\%") + "%";
		
		List<AbstractTagMapperState> result = new ArrayList<AbstractTagMapperState>();
		try {
			Query query = session.createQuery("SELECT o FROM RegexTextTagMapperState o");

			for(Object o : query.list()) {
				if(o instanceof RegexTextTagMapperState) {
					RegexTextTagMapperState item = (RegexTextTagMapperState)o;
					
					IOneOneTagMapper mapper = TagMapperInstantiator.getInstance().instantiate(item);
					if(mapper.matches(new Tag(k, v))) {
					
						result.add(item);
					}
				}
			}
			
			if(result.isEmpty()) {
				query = session.createQuery("SELECT o FROM " + AbstractSimpleTagMapperState.class.getName() + " o WHERE o.tagPattern.key = :k AND o.tagPattern.value = :v");
				
				query.setParameter("k", k);
				query.setParameter("v", v);
				
				//query.setMaxResults(limit);
				
				for(Object o: query.list()) {
					result.add((AbstractTagMapperState)o);
				}
			}
			
			if(result.isEmpty()) {
				query = session.createQuery("SELECT o FROM " + AbstractSimpleTagMapperState.class.getName() + " o WHERE o.tagPattern.key = :k AND o.tagPattern.value IS NULL");
				
				query.setParameter("k", k);				
				//query.setMaxResults(limit);
				
				for(Object o: query.list()) {
					result.add((AbstractTagMapperState)o);
				}
			}
			
			
			tx.commit();
		}
		catch(Exception e) {
			logger.error(ExceptionUtils.getFullStackTrace(e));
			tx.rollback();
		}
		
		return result;
	}



	@Override
	public Model map(String subject, Tag tag, Model model)
	{
		if(model == null)
			model = ModelFactory.createDefaultModel();
		
		Set<IOneOneTagMapper> mappers = lookup(tag.getKey(), tag.getValue());
		
		for(IOneOneTagMapper item : mappers) {
			 item.map(subject, tag, model);
		}

		return model;
	}



	@Override
	public Set<IOneOneTagMapper> lookup(String k, String v)
	{
		Set<IOneOneTagMapper> result = new HashSet<IOneOneTagMapper>();

		List<AbstractTagMapperState> mapperStates = getTagMappings(k, v);
		for(AbstractTagMapperState item : mapperStates) {
			IOneOneTagMapper mapper = TagMapperInstantiator.getInstance().instantiate(item);
			result.add(mapper);
		}
		
		return result;
	}
	

	/**
	 * Retrieve a list of all TagMappers in the database.
	 * 
	 * @return All TagMappers in the database
	 */
	@Override
	public List<IOneOneTagMapper> getAllMappers()
	{
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();

		List<IOneOneTagMapper> result = new ArrayList<IOneOneTagMapper>();

		Criteria criteria = session.createCriteria(AbstractTagMapperState.class);
		
		for(Object o : criteria.list()) {			
			AbstractTagMapperState item = (AbstractTagMapperState)o;
			
			IOneOneTagMapper mapper = TagMapperInstantiator.getInstance().instantiate(item);
			result.add(mapper);
		}
		
		tx.commit();
		
		return result;
	}
}
