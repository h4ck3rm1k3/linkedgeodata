package org.linkedgeodata.jtriplify;

import java.net.URI;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections15.Transformer;
import org.linkedgeodata.core.dao.AbstractDAO;
import org.linkedgeodata.util.SQLUtil;

import com.hp.hpl.jena.rdf.model.Model;


public class LinkedGeoDataDAO
	extends AbstractDAO
{
	enum Queries {
		NODE_FIND,
		NODE_FIND_K,
		NODE_FIND_K_AND_V,
		NODE_FIND_K_OR_V,
		
		WAY_FIND,
		WAY_FIND_K,
		WAY_FIND_K_AND_V,
		WAY_FIND_K_OR_V
	}

	private Transformer<String, URI> uriResolver;
	
	public LinkedGeoDataDAO(Transformer<String, URI> uriResolver)
	{
		this.uriResolver = uriResolver;
	}

	/*
	public void init(int n)
	{
		String nodeFindQuery = LGDQueries.findNodesQuery(null, null, false, distance_m)

		setPreparedStatement(Queries.NODE_FIND, nodeFindQuery);

		//select wn.way_id, wn.sequence_id, ST_AsEWKT(n.geom) from way_nodes wn JOIN nodes n ON (n.id = wn.node_id) where wn.way_id = 2598935;
		// For testing reasons do not update the database.
		String placeHolders = SQLUtil.placeHolder(n, 1);		
	}
	*/
	
	public List<Long> getNodesWithinDistance(
			double lat, 
			double lon, 
			double distance,
			String k,
			String v,
			boolean bOr,
			int limit)
		throws Exception
	{
		return null;
		/*
		String query = LGDQueries.findNodesQuery(k, v, bOr, distance)

		
		List<Long> result =
			SQLUtil.executeList(this.conn, query, Long.class,
					lat, lon, distance);
		
		return result;
		*/
	}
	
	
	// TODO handle empty id set
	public Model getWayGeoRSS(Collection<Long> ids)
		throws Exception
	{
		String sql = LGDQueries.wayGeoRSSQuery;
		
		String placeHolders = SQLUtil.placeHolder(ids.size(), 1);
		sql = sql.replace("$1", placeHolders);
		
		
		ResultSet rs = SQLUtil.executeCore(conn, sql, ids.toArray());

		Model result = TriplifyUtil.triplify(rs, uriResolver);
		
		return result;
	}
}
