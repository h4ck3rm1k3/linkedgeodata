package org.linkedgeodata.dump;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
public class NodeTagIterator
	extends PrefetchIterator<Node>
{
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;

	private String entityFilterStr;
	private String tagFilterStr;
	
	private PreparedStatement prepStmt = null;
	
	public NodeTagIterator(Connection conn)
	{
		this.conn = conn;
	}
	
	public NodeTagIterator(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	public NodeTagIterator(Connection conn, int batchSize, String entityFilterStr, String tagFilterStr)
	{
		this.conn = conn;
		this.batchSize = batchSize;
		this.entityFilterStr = entityFilterStr;
		this.tagFilterStr = tagFilterStr;
	}
	
	
	public String buildQuery()
	{
		String sql = "SELECT DISTINCT nt2.node_id FROM node_tags nt2 ";
		if(offset != null)
			sql += "WHERE nt2.node_id > ? ";
				

		if(entityFilterStr != null) {
			
			sql += (offset == null)
				? "WHERE "
				: "AND ";
			
			sql += "NOT EXISTS (SELECT filter.node_id FROM node_tags filter WHERE filter.node_id = nt2.node_id AND " + entityFilterStr + ") ";
		}

		sql += " ORDER BY node_id ASC ";
		
		if(batchSize != null)
			sql += "LIMIT " + batchSize + " ";

		String s = "SELECT node_id, k, v, n.geom::geometry FROM node_tags JOIN nodes n ON (node_id = id) WHERE node_id IN (" + sql + ") ";
	
		
		if(tagFilterStr != null) {
			s += "AND " + tagFilterStr + " ";
		}
		
		return s;
	}
	
	@Override
	protected Iterator<Node> prefetch()
		throws Exception
	{	
		ResultSet rs;		
		if(offset == null) {
			String s = buildQuery();
			System.out.println(s);
			rs = conn.createStatement().executeQuery(s);
		}
		else  {
			if(prepStmt == null) {
				String s = buildQuery();
				System.out.println(s);
				prepStmt = conn.prepareStatement(s);
			}
			
			prepStmt.setLong(1, offset);
			rs = prepStmt.executeQuery();
		}
		
		Collection<Node> coll = LGDOSMEntityBuilder.processResultSet(rs);

		if(coll == null) {
			prepStmt.close();
			return null;
		}
		
		for(Node node : coll) {
			offset = offset == null ? node.getId() : Math.max(offset, node.getId());
		}

		return coll.iterator();
	}	
}
