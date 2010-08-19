package org.linkedgeodata.scripts;


import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.tagmapping.client.entity.SimpleObjectPropertyTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleTagPattern;

/**
 * Since the TagMappingDB is managed by hibernate, adding new mapping rules
 * shouldn't be done on the DB directly.
 * Currently this file is not a configurable script 
 * 
 * 
 * @author raven
 *
 */
public class MappingRuleAdder
{
	public static void main(String[] args)
		throws Exception
	{
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();
		
		session.save(new SimpleObjectPropertyTagMapperState("http://linkedgeodata.org/property/", "http://linkedgeodata.org/triplify/", true, new SimpleTagPattern(null, null), false));
		
		tx.commit();
	}
}
