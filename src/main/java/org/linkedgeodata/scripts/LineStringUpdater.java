package org.linkedgeodata.scripts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collection;
import java.util.List;

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
		WAY_UPDATE_QUERY
	}
	
	public Updater()
	{
		init(1000);
	}
	
	public void init(int n)
	{
		String wayCandidateQuery = "SELECT * FROM ways w WHERE w.linestring IS NULL LIMIT " + n;
		setPreparedStatement(Queries.WAY_CANDIDATE_QUERY, wayCandidateQuery);

		String placeHolders = SQLUtil.placeHolder(n, 1);		
		String wayUpdateQuery = "UPDATE ways w SET linestring = (SELECT MakeLine(c.geom) AS way_line FROM (SELECT n.geom AS geom FROM nodes n INNER JOIN way_nodes wn ON n.id = wn.node_id WHERE (wn.way_id = w.id) ORDER BY wn.sequence_id) c) WHERE w.id IN (" + placeHolders + ")";
		setPreparedStatement(Queries.WAY_UPDATE_QUERY, wayUpdateQuery);		

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
	
    protected Options cliOptions;
	
    
	/*************************************************************************/
	/* Init                                                                  */
	/*************************************************************************/	
	private void initCliOptions()
	{
		cliOptions = new Options();
		
		cliOptions.addOption("t", "type", true, "Database type (posgres, mysql,...)");
		cliOptions.addOption("db", "database", true, "Database name");
		cliOptions.addOption("u", "user", true, "");
		cliOptions.addOption("p", "password", true, "");
		cliOptions.addOption("h", "host", true, "");
	}

	public void run()
		throws Exception
	{
		Connection conn = null;
		logger.info("Connected to db");
		
		Updater updater = new Updater();
		updater.setConnection(conn);
		
		StopWatch sw = new StopWatch();
		sw.start();
		long updateCounter = 0;
		float lastTime = 0.0f;
		for(;;) {
			updateCounter += updater.step();
			
			float elapsed = sw.getTime() / 1000.0f;
			float delta = elapsed - lastTime;
			
			float ratio = updateCounter / elapsed;
			
			if(delta >= 5.0f) {
				logger.info("Elapsed: " + elapsed + ", Counter: " + updateCounter + ", Ratio = " + ratio);
				
				lastTime = elapsed;
			}
			
			
		}
		
		
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
	
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");

		LineStringUpdater o = new LineStringUpdater();
		o.run();
		
	}
	
}
