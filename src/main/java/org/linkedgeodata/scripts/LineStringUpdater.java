package org.linkedgeodata.scripts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.core.dao.AbstractDAO;
import org.linkedgeodata.util.SQLUtil;



class Updater
	extends AbstractDAO
{
	private static final Logger logger = Logger.getLogger(Updater.class);
	
	private int n;

	enum Queries {
		WAY_CANDIDATE_QUERY,
		WAY_UPDATE_QUERY,
		WAY_BUILD_QUERY
	}
	
	public Updater()
	{
		init(1000);
	}
	
	public Updater(int n)
	{
		init(n);
	}
	
	public void init(int n)
	{
		String wayCandidateQuery = "SELECT * FROM ways w WHERE w.linestring IS NULL LIMIT " + n;
		setPreparedStatement(Queries.WAY_CANDIDATE_QUERY, wayCandidateQuery);

		//select wn.way_id, wn.sequence_id, ST_AsEWKT(n.geom) from way_nodes wn JOIN nodes n ON (n.id = wn.node_id) where wn.way_id = 2598935;
		// For testing reasons do not update the database.
		String placeHolders = SQLUtil.placeHolder(n, 1);		
		String waySelectQuery =
				"SELECT\n" +
				"	c.way_id,\n" +
				"	MakeLine(c.geom) AS way_line\n" +
				"FROM\n" +
				"	(\n" +
				"		SELECT\n" +
				"			wn.way_id,\n" +
				"			n.geom\n" +
				"		FROM\n" +
				"			way_nodes wn\n" +
				"			INNER JOIN nodes n ON (n.id = wn.node_id)\n" +
				"		WHERE\n" +
				"			wn.way_id IN (" + placeHolders + ")\n" +
				"		ORDER BY\n" +
				"			wn.way_id,\n" +
				"			wn.sequence_id\n" +
				"	) AS c\n" +
				"GROUP BY\n" +
				"	c.way_id\n";
		
		String wayUpdateQuery =
			"UPDATE\n" +
			"	ways w\n" +
			"SET linestring = (\n" +
			"	SELECT\n" +
			"		MakeLine(c.geom) AS way_line\n" +
			"	FROM (\n" +
			"		SELECT\n" +
			"			n.geom\n" +
			"		FROM\n" +
			"			way_nodes wn\n" +
			"			INNER JOIN nodes n ON (n.id = wn.node_id)\n" +
			"		WHERE\n" +
			"			wn.way_id = w.id\n" +
			"		ORDER BY\n" +
			"			wn.sequence_id\n" +
			"	) AS c\n" +
			")\n" +
			"WHERE\n" +
			"	w.id IN (" + placeHolders + ")\n";
		System.out.println(wayUpdateQuery);
		setPreparedStatement(Queries.WAY_UPDATE_QUERY, wayUpdateQuery);

		/*
		String placeHolders = SQLUtil.placeHolder(n, 1);		
		String wayUpdateQuery = "UPDATE ways w SET linestring = (SELECT MakeLine(c.geom) AS way_line FROM (SELECT n.geom AS geom FROM nodes n INNER JOIN way_nodes wn ON n.id = wn.node_id WHERE (wn.way_id = w.id) ORDER BY wn.sequence_id) AS c) WHERE w.id IN (" + placeHolders + ")";
		setPreparedStatement(Queries.WAY_UPDATE_QUERY, wayUpdateQuery);		
*/
		
		this.n = n;
	}
	
	
	public List<Long> getCandidates()
		throws Exception
	{
		List<Long> result = executeList(Queries.WAY_CANDIDATE_QUERY, Long.class, new Object[]{});
		logger.trace("Retrieved way-ids" + result);
		return result;
	}

	public void update(Collection<Long> ids)
		throws Exception
	{
		logger.debug("Updating " + ids.size() + " ways");
		logger.trace("Updating ways with ids: " + ids);
		execute(Queries.WAY_UPDATE_QUERY, Void.class, ids.toArray());		
	}
	
	public int step()
		throws Exception
	{
		List<Long> ids = getCandidates();
		
		if(ids.size() == 0)
			return ids.size();
	
		update(ids);
	
		return ids.size();
	}
	
	
}

public class LineStringUpdater
{
	private static final Logger logger = Logger.getLogger(LineStringUpdater.class);
	
    protected static Options cliOptions;
	
	
	public static void run(Connection conn, int batchSize)
		throws Exception
	{
		Updater updater = new Updater(batchSize);
		updater.setConnection(conn);
		
		StopWatch sw = new StopWatch();
		sw.start();
		long updateCounter = 0;
		float lastTime = 0.0f;
		for(;;) {
			int stepCount = updater.step(); 
			updateCounter += stepCount;
			
			float elapsed = sw.getTime() / 1000.0f;
			float delta = elapsed - lastTime;
			
			float ratio = updateCounter / elapsed;
			
			if(delta >= 5.0f) {
				logger.info("Elapsed: " + elapsed + ", Counter: " + updateCounter + ", Ratio = " + ratio + ", Recent step: " + stepCount);
				
				lastTime = elapsed;
			}
			
			
		}
		
		
	}

	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");

		initCLIOptions();

		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);

		String hostName = commandLine.getOptionValue("h", "localhost");
		String dbName   = commandLine.getOptionValue("d", "lgd");
		String userName = commandLine.getOptionValue("u", "lgd");
		String passWord = commandLine.getOptionValue("p", "lgd");

		String batchSizeStr = commandLine.getOptionValue("n", "1000");

		int batchSize = Integer.parseInt(batchSizeStr);
		if(batchSize <= 0)
			throw new RuntimeException("Invalid argument for batchsize");
		
		Connection conn = connectPostGIS(hostName, dbName, userName, passWord);
		logger.info("Connected to db");
		

		run(conn, batchSize);		
	}
    
	/*************************************************************************/
	/* Init                                                                  */
	/*************************************************************************/	
	private static void initCLIOptions()
	{
		cliOptions = new Options();
		
		cliOptions.addOption("t", "type", true, "Database type (posgres, mysql,...)");
		cliOptions.addOption("d", "database", true, "Database name");
		cliOptions.addOption("u", "user", true, "");
		cliOptions.addOption("p", "password", true, "");
		cliOptions.addOption("h", "host", true, "");
		cliOptions.addOption("n", "batchSize", true, "Batch size");
	}


	
	private static Connection connectSQL(String hostName, String dbName, String userName, String passWord)
		throws Exception
	{
		//ALTER TABLE Point OWNER TO testgis;
		//psql -Utestgis -d testgisdb
		//http://www.paolocorti.net/2008/01/30/installing-postgis-on-ubuntu/
		//http://www.enterprisedb.com/learning/tutorial/postgis_ppss.do
		//http://postgis.refractions.net/docs/ch02.html
		//http://www.giswiki.org/wiki/PostGIS_Tutorial
		
		String url = "jdbc:mysql://" + hostName + "/" + dbName;
		//Connection connection = DriverManager.getConnection(url);
		Connection conn = DriverManager.getConnection(url, userName, passWord);
		
		return conn;
	}

	private static Connection connectPostGIS(String hostName, String dbName, String userName, String passWord)
		throws Exception
	{
		String url = "jdbc:postgresql://" + hostName + "/" + dbName;
		//Connection connection = DriverManager.getConnection(url);
		Connection conn = DriverManager.getConnection(url, userName, passWord);
		
		return conn;
	}
	
}
