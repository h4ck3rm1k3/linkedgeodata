package org.linkedgeodata.dump;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import org.linkedgeodata.jtriplify.LGDOSMEntityBuilder;
import org.linkedgeodata.util.PrefetchIterator;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;

/**
 * An iterator for the modified node_tags table.
 * Modified means, that the geom column was added to this table.
 * 
 * Assumes the following columns:
 * id   : long
 * k    : String
 * v    : String
 * geom : geometry
 * 
 * @author Claus Stadler
 *
 */
public class NodeTagIteratorDenorm1
	extends PrefetchIterator<Node>
{
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;

	private String entityFilterStr;
	private String tagFilterStr;
	
	public NodeTagIteratorDenorm1(Connection conn)
	{
		this.conn = conn;
	}
	
	public NodeTagIteratorDenorm1(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	public NodeTagIteratorDenorm1(Connection conn, int batchSize, String entityFilterStr, String tagFilterStr)
	{
		this.conn = conn;
		this.batchSize = batchSize;
		this.entityFilterStr = entityFilterStr;
		this.tagFilterStr = tagFilterStr;
	}
	
	
	@Override
	protected Iterator<Node> prefetch()
		throws Exception
	{
		String sql = "SELECT DISTINCT node_id FROM node_tags ";
		if(offset != null)
			sql += "WHERE node_id > " + offset + " ";
				

		if(entityFilterStr != null) {
			
			sql += (offset == null)
				? "WHERE "
				: "AND ";
			
			sql += "NOT EXISTS (SELECT filter.node_id FROM node_tags filter WHERE filter.node_id = nt.node_id AND " + entityFilterStr + ")";
		}

		
		sql += " ORDER BY node_id ASC ";
		
		if(batchSize != null)
			sql += "LIMIT " + batchSize + " ";

		String s = "SELECT node_id, k, v, geom::geometry FROM node_tags WHERE node_id IN (" + sql + ")";
	
		
		if(tagFilterStr != null) {
			s += " AND " + tagFilterStr;
		}
		
		
		//System.out.println(s);
		ResultSet rs = conn.createStatement().executeQuery(s);
		
		Collection<Node> coll = LGDOSMEntityBuilder.processResultSet(rs, null).values();

		for(Node node : coll) {
			offset = offset == null ? node.getId() : Math.max(offset, node.getId());
		}

		return coll.iterator();
	}	
}
