package org.linkedgeodata.osm.mapping;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.tagmapping.client.entity.AbstractEntity;
import org.linkedgeodata.tagmapping.client.entity.AbstractSimpleOneOneTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.RegexTextTagMapperState;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;


public class DBTagMapper
	implements ITagMapper
{
	private static final Logger logger = Logger.getLogger(DBTagMapper.class);
	
	
	public List<AbstractEntity> getTagMappings(String k, String v)
	{
		int limit = 100;
		//limit = constrain(limit, 0, 100);
		
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();

		//k = k.replace("%", "\\%") + "%";
		//v = v.replace("%", "\\%") + "%";
		
		List<AbstractEntity> result = new ArrayList<AbstractEntity>();
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
				query = session.createQuery("SELECT o FROM " + AbstractSimpleOneOneTagMapperState.class.getName() + " o WHERE o.tagPattern.key = :k AND o.tagPattern.value = :v");
				
				query.setParameter("k", k);
				query.setParameter("v", v);
				
				query.setMaxResults(limit);
				
				for(Object o: query.list()) {
					result.add((AbstractEntity)o);
				}
			}
			
			if(result.isEmpty()) {
				query = session.createQuery("SELECT o FROM " + AbstractSimpleOneOneTagMapperState.class.getName() + " o WHERE o.tagPattern.key = :k AND o.tagPattern.value IS NULL");
				
				query.setParameter("k", k);				
				query.setMaxResults(limit);
				
				for(Object o: query.list()) {
					result.add((AbstractEntity)o);
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
		List<AbstractEntity> mapperStates = getTagMappings(tag.getKey(), tag.getValue());
		
		for(AbstractEntity item : mapperStates) {
			IOneOneTagMapper mapper = TagMapperInstantiator.getInstance().instantiate(item);
			
			//Model model = ModelFactory.
			mapper.map(subject, tag, model);
			
			//List<CTriple> triples = JenaModelConverter.convert(model);
			
			//result.put(item, triples);
		}

		return model;
	}
	
}
