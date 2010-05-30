package org.linkedgeodata.jtriplify;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.postgis.PGgeometry;
import org.postgis.Point;

public class LGDOSMEntityBuilder
{

	public static Map<Long, Node>  processResultSet(ResultSet rs, Map<Long, Node> idToNode)
		throws SQLException
	{
		if(idToNode == null) {
			idToNode = new HashMap<Long, Node>();
		}
		
		ResultSetMetaData md = rs.getMetaData();
	
		String[] names = {"node_id", "k", "v", "geom", "user"};
		Integer[] col = {null, null, null, null, null};
	
		for(int i = 1; i <= md.getColumnCount(); ++i) {
			for(int j = 0; j < names.length; ++j) {
				if(names[j].equals(md.getColumnName(i))) {
					col[j] = i;
				}
			}
		}
		
		//int counter = 0;
		while(rs.next()) {
			//++counter;
	
			long nodeId = rs.getLong(col[0]);
			String k = rs.getString(col[1]);
			String v = rs.getString(col[2]);
			PGgeometry g = (PGgeometry)rs.getObject(col[3]);
			
			//String userName = cIndex[4] == null ? null : rs.getString(cIndex[4]);
			
			double lat = 0.0;
			double lon = 0.0;
			
			if(g != null) {
				Point p = new Point(g.toString());
				lat = p.getY();
				lon = p.getX();
			}
			
			Node node = idToNode.get(nodeId);
			if(node == null) {
				node = new Node(nodeId, 0, (Date)null, null, -1, lat, lon);
				idToNode.put(nodeId, node);
			}
			else {
				if(g != null) {
					node.setLatitude(lat);
					node.setLongitude(lon);
				}
			}
	
			Tag tag = new Tag(k, v);
			node.getTags().add(tag);
		}
		
		return idToNode;
	}	
}
