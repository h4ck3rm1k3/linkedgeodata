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
package org.linkedgeodata.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class PostGISUtil
{
	public static Connection connectPostGIS(ConnectionConfig config)
		throws Exception
	{
		return connectPostGIS(
				config.getHostName(), config.getDataBaseName(), config.getUserName(), config.getPassWord());
	}
	
	public static Connection connectPostGIS(String hostName, String dbName, String userName, String passWord)
		throws Exception
	{
		Class.forName("org.postgis.DriverWrapper");
		String url = "jdbc:postgresql_postGIS://" + hostName + "/" + dbName;
		//Connection connection = DriverManager.getConnection(url);
		Connection conn = DriverManager.getConnection(url, userName, passWord);
		
		return conn;
	}

}
