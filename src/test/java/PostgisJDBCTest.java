import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;
import org.linkedgeodata.scripts.LineStringUpdater;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;



public class PostgisJDBCTest
{
	private static final Logger logger = Logger.getLogger(PostgisJDBCTest.class);
	
	@Test
	public void test()
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
		String hostName = "localhost";
		String dbName   = "lgd";
		String userName = "postgres";
		String passWord = "postgres";
	
		//org.postgis.
		//Class.forName(className);
		Class.forName("org.postgis.DriverWrapper");
		logger.info("Connecting to db");
		String url = "jdbc:postgresql_postGIS" + "://" + hostName + "/" + dbName;
		//Connection connection = DriverManager.getConnection(url);
		Connection conn = DriverManager.getConnection(url, userName, passWord);

		//PGConnection x = PG
		//conn.addDataType("geometry", "org.postgis.PGgeometry");
		//conn.addDataType("box3d", "org.postgis.PGbox3d");
		//conn.addDataType("geometry", org.postgis.PGgeometry.class);
		//conn.addDataType("box3d", org.postgis.PGbox3d.class);

		
		//Connection conn = LineStringUpdater.connectPostGIS(hostName, dbName, userName, passWord);
		

		ResultSet rs = conn.createStatement().executeQuery("SELECT geom::geometry from node_tags limit 1");
		while(rs.next()) {
			Object o = rs.getObject(1);
			System.out.println(o.getClass());
			System.out.println(o);
			
			PGgeometry p = (PGgeometry)o;
			System.out.println(p.getValue());
		
			Geometry g = p.getGeometry();
			System.out.println(g.getClass());
			
			Point pt = (Point)g;
			System.out.println(pt.getY() + ", " + pt.getX());
		}
		
		
	}
}
