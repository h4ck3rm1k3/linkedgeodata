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

import java.sql.SQLException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.util.SQLUtil;
import org.hibernate.Query;

/**
 * FIXME This class is closely related to the TagMappingDB class - merge?
 * 
 * @author raven
 *
 */
public class HibernateSessionProvider
	implements ISessionProvider
{
	private Logger logger = Logger.getLogger(HibernateSessionProvider.class);
	
	@Override
	public Session createSession()
		throws Exception
	{
		// FIXME This is a hack - the session-factory should probably
		// not always be reset.
		TagMappingDB.reset();
		
		return TagMappingDB.getSession();
	}

	private long counter = 1000;
	/**
	 * Validates a hibernate session object.
	 * Cannot be used when there is already an open transaction on the session.
	 * 
	 * @param session
	 */
	void sendTestQuery(Session session)
	{
		
		Transaction tx = session.beginTransaction();
		//logger.debug("IsSessionConnection: " + session.isConnected());
		//session.clear();
		//Query q = session.createQuery("SELECT o From AbstractSimpleTagMapperState o WHERE o.id = -1");
		//SQLQuery q = session.createSQLQuery(/"SELECT * FROM information_sche)
		
		//SQLQuery q = session.createSQLQuery("SELECT table_name FROM information_schema.tables LIMIT 1");
		//SQLQuery q = session.createSQLQuery("SELECT table_name FROM information_schema.tables WHERE table_name = '' || RANDOM()");
		//SQLQuery q  = session.createSQLQuery("SELECT table_name FROM nodes WHERE table_name = '" + (counter++) + "'");

		SQLQuery q = session.createSQLQuery("SELECT 1");
		
		//SQLQuery q  = session.createSQLQuery("SELECT user_id FROM nodes WHERE id = '" + (counter++) + "'");
		Object o = q.uniqueResult();
		logger.debug("Result of test query: " + o);
		
		tx.commit();
	}

	
	/**
	 * This method first retrieves the current session, sends a test query
	 * and returns a session if that was successful.
	 * 
	 * Otherwise it will reset the session factory and attempt to obtain
	 * a new session.
	 * @throws SQLException 
	 * 
	 */
	@Override
	public Session getSession()
	{
		while(true) {
			Session candidate = TagMappingDB.getSession();
			try {
				sendTestQuery(candidate);
				break;
			} catch(Throwable t) {
				logger.error(t);
				
				
				// Wait a while, then aquire a fresh connection
				try {
					Thread.sleep(5000);
				} catch(Throwable u) {
					logger.error(ExceptionUtils.getFullStackTrace(u));
				}
				TagMappingDB.reset();
			}
			finally {
				if(candidate.isOpen())
					candidate.close();
			}
		}

		return TagMappingDB.getSession();	
	}
}
