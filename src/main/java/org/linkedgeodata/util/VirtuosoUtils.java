package org.linkedgeodata.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class VirtuosoUtils
{
	public static Connection connect(ConnectionConfig config)
		throws Exception
	{
		return connect(
				config.getHostName(), config.getUserName(), config.getPassWord());
	}
	
	public static Connection connect(String hostName, String userName, String passWord)
		throws Exception
	{
		Class.forName("virtuoso.jdbc3.Driver");
		String url = "jdbc:virtuoso://" + hostName + "/charset=UTF-8";
		//Connection connection = DriverManager.getConnection(url);
		Connection conn = DriverManager.getConnection(url, userName, passWord);
		
		return conn;
	}
}
