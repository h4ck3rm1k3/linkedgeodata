package org.linkedgeodata.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class PostGISUtil
{

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
