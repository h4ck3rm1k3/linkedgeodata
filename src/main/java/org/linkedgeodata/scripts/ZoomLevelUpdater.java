package org.linkedgeodata.scripts;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.linkedgeodata.core.dao.AbstractDAO;
import org.linkedgeodata.scripts.Updater.Queries;
import org.linkedgeodata.util.SQLUtil;

import org.apache.commons.collections15.Transformer;

class Node
{
}

class ZoomLevelAssigner
	implements Transformer<OSMEntity, Integer>
{
	public Integer transform(OSMEntity osmEntity)
	{		
		return 1;
	}
}




enum OSMEntity
{
	PLACEHOLDER,
}



/**
 * Iterates over all nodes and ways. 
 * A function decides which zoom level the object should be added.
 * 
 * 
 * 
 * @author raven
 *
 */
class ZoomLevelUpdater
	extends AbstractDAO
{		
	private static final Logger logger = Logger.getLogger(Updater.class);
	
	private int n;
	
	enum Queries {
		WAY_CANDIDATE_QUERY,
		WAY_UPDATE_QUERY,
		WAY_BUILD_QUERY
	}
	
	public void createZoomLevel(int level)
	{
		String sql =
			"CREATE TABLE ZoomLevel%d (\n" +
			"	osm_entity_type OSMEntityType NOT NULL\n" +
			"	osm_entity_id   BIGINT NOT NULL\n" +
			"   geog            GEOGRAPHY NOT NULL\n" +
			")\n";
	}
	
	
	public void addToLevel(Node node, int level)
	{
	}
	
	
	
	public ZoomLevelUpdater()
	{
		init(1000);
	}
	
	public ZoomLevelUpdater(int n)
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
	
		// The idea is to sort the ways by the ways by their minimum node id
		// so that the join with the node table becomes faster
		/*
		 * 
		 SELECT
		  	wn.way_id
		 FROM
		 	way_nodes wn
		 ORDER BY
		 	wn.node_id
		 LIMIT 10
	
		 SELECT
		 	wn.way_id
		 FROM
		 	way_nodes wn
		 GROUP BY
		 	wn.way_id
		 ORDER BY
		 	MIN(wn.node_id)
		 LIMIT 10
		 */
		
		String waySelectQuery2 =
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
			"		MakeLine(c.geom::geometry)::geography AS way_line\n" +
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
