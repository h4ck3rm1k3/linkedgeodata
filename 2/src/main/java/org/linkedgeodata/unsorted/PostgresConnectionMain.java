package org.linkedgeodata.unsorted;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.util.MetaBZip2InputStream;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StreamUtil;
import org.openstreetmap.osmosis.core.Osmosis;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class PostgresConnectionMain
{
	private static final Logger logger = Logger.getLogger(PostgresConnectionMain.class);

	
	public static Connection connectPostGIS(String hostName, String dbName, String userName, String passWord)
		throws ClassNotFoundException, SQLException
	{
		Class.forName("org.postgis.DriverWrapper");
		String url = "jdbc:postgresql_postGIS://" + hostName + "/" + dbName;
		Connection conn = DriverManager.getConnection(url, userName, passWord);
		
		return conn;
	}

	public static List<String> getUnitDBSQLStatements()
		throws FileNotFoundException, IOException
	{
			// Load the osmosis schema
			String lgdPath = "data/lgd/sql/";
			String osmPath = "lib/osmosis/0.34/script/";
			
			String[] fileNames = {
					lgdPath + "pgsql_simple_schema_0.6_geography.sql",
					osmPath + "pgsql_simple_schema_0.6_action.sql",

					"data/lgd/sql/Core.sql",
					//"data/lgd/sql/Denormalization.sql"
			};
			
		return loadSQLFiles(fileNames);
	}
	
	private static List<String> loadSQLFiles(String[] fileNames)
		throws FileNotFoundException, IOException
	{
		List<String> result = new ArrayList<String>();
		for(String fileName : fileNames) {
			File file = new File(fileName);
			
			String content = StreamUtil.toString(new FileInputStream(file));
			List<String> sqls = SQLUtil.parseSQLScript(content);
			
			result.addAll(sqls);
		}
		
		return result;
	}
	
	
	public static List<String> getPostLoadStatements()
		throws FileNotFoundException, IOException
	{
		// Load the osmosis schema
		String lgdPath = "data/lgd/sql/";
		String osmPath = "lib/osmosis/0.34/script/";
		
		String[] fileNames = {
				lgdPath + "pgsql_simple_schema_0.6_linestring_geography.sql",

				"data/lgd/sql/Denormalization.sql"
		};
		
		return loadSQLFiles(fileNames);		
	}

	
	public static void execute(Connection conn, String sql)
		throws SQLException
	{
		execute(conn, Collections.singleton(sql).iterator());
	}
	
	public static void execute(Connection conn, Iterable<String> it)
		throws SQLException
	{
		execute(conn, it.iterator());
	}
	
	public static void execute(Connection conn, Iterator<String> it)
		throws SQLException
	{
		Statement stmt = conn.createStatement();

		while(it.hasNext()) {
			String sql = it.next();
			
			logger.trace("Executing statement: " + sql);
			stmt.execute(sql);
		}

		stmt.close();
	}
	
	public static void createUnitTestDB(String hostName, String dbName, String userName, String passWord)
		throws Exception
	{
		logger.info("Attempting to drop database '" + dbName + "'");
		Connection conn = connectPostGIS(hostName, "", userName, passWord);

		execute(conn, "CREATE DATABASE " + dbName + " WITH TEMPLATE \"template_postgis_1.5\"");

		conn.close();		
	}
	
	public static void dropUnitTestDB(String hostName, String dbName, String userName, String passWord)
		throws Exception
	{
		logger.info("Attempting to create database '" + dbName + "'");
		Connection conn = connectPostGIS(hostName, "", userName, passWord);

		execute(conn, "DROP DATABASE " + dbName);

		conn.close();		
	}

	public static void loadUnitTestDB(String hostName, String dbName, String userName, String passWord)
		throws Exception
	{
		logger.info("Loading database '" + dbName + "'");

		Connection conn = connectPostGIS(hostName, dbName, userName, passWord);
		Statement stmt = conn.createStatement();
		
		List<String> sqls = getUnitDBSQLStatements();		
		for(String sql : sqls) {
			logger.trace("Executing statement: " + sql);
			stmt.execute(sql);
		}

		stmt.close();
		conn.close();
	}
	

	public static void postLoadUnitTestDB(String hostName, String dbName, String userName, String passWord)
		throws Exception
	{
		logger.info("Post Loading database '" + dbName + "'");
	
		Connection conn = connectPostGIS(hostName, dbName, userName, passWord);
		Statement stmt = conn.createStatement();
		
		List<String> sqls = getPostLoadStatements();		
		for(String sql : sqls) {
			logger.trace("Executing statement: " + sql);
			stmt.execute(sql);
		}
	
		stmt.close();
		conn.close();
	}

	
	
	public static File extract(File in)
		throws IOException
	{
		String inFileName = in.getCanonicalPath();

		int splitIndex = inFileName.lastIndexOf('.');

		String outFileName = inFileName.substring(0, splitIndex);
		String suffix = inFileName.substring(splitIndex + 1);
		
		if(!suffix.equalsIgnoreCase("bz2")) {
			throw new IOException("Unknown file extension: " + suffix);
		}
		
		
		InputStream is = new MetaBZip2InputStream(new FileInputStream(in));
		
		File result = new File(outFileName);
		FileOutputStream os = new FileOutputStream(result);
		
		copy(is, os, 4096);	
		
		return result;
	}
	
	public static void copy(InputStream in, OutputStream out, int bufferSize)
		throws IOException
	{
		byte[] buffer = new byte[bufferSize];
		int n;
		while((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
	}
	
	
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");

		/*
		File file = new File("data/test/test.sql");
		String str = StreamUtil.toString(new FileInputStream(file));
		List<String> strs = SQLUtil.parseSQLScript(str);
		System.out.println(strs);
		*/
		

		String hostName = "localhost";
		//String hostName = "139.18.251.5";
		//String hostName = "hobbit.local";
		String dbName = "unittest_lgd";
		String userName = "postgres";
		String passWord = "postgres";

		String osmosisPath = "/opt/osmosis/0.35.1/bin/osmosis";
		
		String zippedFile = "data/test/osm/bremen.osm.bz2";
		
		File unzipped = extract(new File(zippedFile));
		
		String dataFile = unzipped.getCanonicalPath();
		
		
		
		dropUnitTestDB(hostName, dbName, "postgres", "postgres");
		createUnitTestDB(hostName, dbName, userName, passWord);
		loadUnitTestDB(hostName, dbName, userName, passWord);


		logger.info("Loading dataset using osmosis");
		String[] options = {
				"--read-xml",
				"file=" + dataFile, // FIXME Escape whitespaces in filename
				"--write-pgsql",
				"host=" + hostName,
				"database=" + dbName,
				"user=" + userName,
				"password=" + passWord,
		};
		
		Runtime runtime = Runtime.getRuntime();
		String command =
			//"java -classpath lib/osmosis/0.34/lib -jar lib/osmosis/0.34/osmosis.jar  " +
			osmosisPath + " " +
			org.linkedgeodata.util.StringUtil.implode(" ", options);
			
		logger.debug("Executing system call: " + command);
		Process p = runtime.exec(command);

		logger.debug("Waiting for process to terminate...");
		p.waitFor();
		InputStream in = p.getInputStream();
		logger.trace("Console output of process:\n" + StreamUtil.toString(in));
		in.close();
		
		logger.debug("Process terminated with exit code '" + p.exitValue() + "'");

		logger.info("Performing post-load actions");
		postLoadUnitTestDB(hostName, dbName, userName, passWord);
		
		//Connection conn = connectPostGIS("dwarf.local", "unittest_lgd", "lgd", "lgd");
	}
}
