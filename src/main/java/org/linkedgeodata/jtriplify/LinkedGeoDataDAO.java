package org.linkedgeodata.jtriplify;

import java.net.URI;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.collections15.Transformer;
import org.linkedgeodata.core.dao.AbstractDAO;
import org.linkedgeodata.util.SQLUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


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
	
	enum OSMEntityType
	{
		NODE,
		WAY,
		RELATION
	}
	
	public List<Long> getEntitiesWithinDistance(
			OSMEntityType type,
			double lat, 
			double lon, 
			double distance,
			String k,
			String v,
			boolean bOr,
			int limit)
		throws Exception
	{
		String sql = null;
		switch(type) {
		case NODE:
			sql = LGDQueries.buildFindNodesQuery("$3", k, v, bOr);
			break;
		case WAY:
			sql = LGDQueries.buildFindWaysQuery("$3", k, v, bOr);
			break;
		default:
			throw new RuntimeException("Not implemented");
		}
	

		// TODO WARNING - SQL INJECTION UNSAFE CODE
		sql = sql.replace("$1", "" + lat);
		sql = sql.replace("$2", "" + lon);
		sql = sql.replace("$3", "" + distance);
		
		if(k != null)
			sql = sql.replace("$4", k);
		
		if(v != null)
			sql = sql.replace("$5", v);
		
		List<Long> result = SQLUtil.executeList(conn, sql, Long.class);
	
		return result;
	}

	
	private Callable<Model> prepareSimpleIdBasedQuery(String sql, final Collection<Long> ids)
	{
		String placeHolders = SQLUtil.placeHolder(ids.size(), 1);
		final String finalSQL = sql.replace("$1", placeHolders);
		
		Callable<Model> result =
			new Callable<Model>() {
				@Override
				public Model call()
					throws Exception
				{
					if(ids.isEmpty()) {
						return ModelFactory.createDefaultModel();
					}

					ResultSet rs = SQLUtil.executeCore(conn, finalSQL, ids.toArray());

					Model result = TriplifyUtil.triplify(rs, uriResolver);
					
					return result;		
				}
			};
			
		return result;
	}
	
	
	public Callable<Model> getNodeGeoRSS(Collection<Long> ids)
		throws Exception
	{
		return prepareSimpleIdBasedQuery(LGDQueries.nodeGeoRSSQuery, ids);
	}
	
	public Callable<Model> getNodeWGSQuery(Collection<Long> ids)
		throws Exception
	{
		return prepareSimpleIdBasedQuery(LGDQueries.nodeWGSQuery, ids);
	}
	
	
	public Callable<Model> getNodeTagsQuery(Collection<Long> ids)
		throws Exception
	{
		return prepareSimpleIdBasedQuery(LGDQueries.nodeTagsQuery, ids);
	}
	
	public Callable<Model> getNodeWayMemberQuery(Collection<Long> ids)
		throws Exception
	{
		return prepareSimpleIdBasedQuery(LGDQueries.nodeWayMemberQuery, ids);
	}
	
	public Callable<Model> getWayGeoRSS(Collection<Long> ids)
		throws Exception
	{
		return prepareSimpleIdBasedQuery(LGDQueries.wayGeoRSSQuery, ids);
	}

	public Callable<Model> getWayTags(Collection<Long> ids)
		throws Exception
	{
		return prepareSimpleIdBasedQuery(LGDQueries.wayTagsQuery, ids);
	}

	public Callable<Model> getWayNodes(Collection<Long> ids)
		throws Exception
	{
		return prepareSimpleIdBasedQuery(LGDQueries.wayNodeQuery, ids);
	}

}
