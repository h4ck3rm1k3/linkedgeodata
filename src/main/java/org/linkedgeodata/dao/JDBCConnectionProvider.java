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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.util.ConnectionConfig;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.SQLUtil;

public class JDBCConnectionProvider
	implements IConnectionFactory
{
	private static final Logger logger = Logger.getLogger(JDBCConnectionProvider.class);
	
	private ConnectionConfig config;
	private volatile Connection conn;
	
	public JDBCConnectionProvider(ConnectionConfig config)
	{
		this.config = config;
	}
	
	@Override
	public Connection createConnection()
		throws Exception
	{
		return PostGISUtil.connectPostGIS(config);
	}
	
	void sendTestQuery()
		throws SQLException
	{
		//Object o = SQLUtil.execute(conn, "SELECT table_name FROM information_schema.tables LIMIT 1", Object.class);
		Object o = SQLUtil.execute(conn, "SELECT table_name FROM information_schema.tables WHERE table_name = '' || RANDOM()", Object.class);
		logger.debug("Result of test query: " + o);
		

		if(conn.getAutoCommit() == false) {
			conn.commit();
		}
	}
	
	private void resetConnection()
		throws Exception
	{
		if(conn != null)
			conn.close();
		
		conn = createConnection();
	}


	@Override
	public Connection getConnection()
		throws Exception
	{
		if(conn == null)
			resetConnection();
		
		while(true) {
			try {
				sendTestQuery();
				break;
			} catch(Throwable t) {
				logger.error(t);

				// Wait a while, then aquire a fresh connection
				try {
					Thread.sleep(5000);
				} catch(Throwable u) {
					logger.error(ExceptionUtils.getFullStackTrace(u));
				}
				resetConnection();
			}
		}

		return conn;
	}
}
