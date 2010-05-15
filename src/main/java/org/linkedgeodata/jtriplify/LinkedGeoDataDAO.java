package org.linkedgeodata.jtriplify;

import java.net.URI;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.linkedgeodata.core.dao.AbstractDAO;
import org.linkedgeodata.util.ExceptionUtil;
import org.linkedgeodata.util.SQLUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


public class LinkedGeoDataDAO
	extends AbstractDAO
{
	private static final Logger logger = Logger.getLogger(LinkedGeoDataDAO.class);
	
	public enum Queries {
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
	private TagMapper tagMapper;
	
	public LinkedGeoDataDAO(Transformer<String, URI> uriResolver, TagMapper tagMapper)
	{
		this.uriResolver = uriResolver;
		
		this.tagMapper = tagMapper;
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
	
	public enum OSMEntityType
	{
		NODE,
		WAY,
		RELATION
	}


	public List<Long> getEntitiesWithinBBox(
			OSMEntityType type,
			float latMin, 
			float latMax, 
			float lonMin,
			float lonMax,
			Integer limit,
			String k,
			String v,
			boolean bOr)
		throws Exception
	{
		String sql = null;
		switch(type) {
		case NODE:
			sql = LGDQueries.buildFindTaggedNodesQuery(latMin, latMax, lonMin, lonMax, limit, k, v, bOr);
			break;
		/*
		case WAY:
			//sql = LGDQueries.buildFindTaggedWaysQuery("$3", k, v, bOr);
			break;
		*/
		default:
			throw new RuntimeException("Not implemented");
		}

		/*
		if(k != null)
			sql = sql.replace("$4", k);
		
		if(v != null)
			sql = sql.replace("$5", v);
		*/

		List<Long> result = SQLUtil.executeList(conn, sql, Long.class);
	
		return result;
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
					Model result = null;

					try {
						if(ids.isEmpty()) {
							return ModelFactory.createDefaultModel();
						}
	
						ResultSet rs = SQLUtil.executeCore(conn, finalSQL, ids.toArray());
	
						
						result = TriplifyUtil.triplify(rs, uriResolver);
					} catch(Throwable t) {
						logger.error(ExceptionUtil.toString(t));
					}
					
					return result;		
				}
			};
			
		return result;
	}
	

	private Callable<Model> prepareIdBasedTagQuery(String sql, final String osmEntityType, final Collection<Long> ids)
	{
		String placeHolders = SQLUtil.placeHolder(ids.size(), 1);
		final String finalSQL = sql.replace("$1", placeHolders);
		
		Callable<Model> result =
			new Callable<Model>() {
				@Override
				public Model call()
					throws Exception
				{
					Model result = null;

					try {
						if(ids.isEmpty()) {
							return ModelFactory.createDefaultModel();
						}
	
						String prefix = "http://linkedgeodata/triplify/" + osmEntityType + "/";
						
						result = ModelFactory.createDefaultModel();
						ResultSet rs = SQLUtil.executeCore(conn, finalSQL, ids.toArray());
						while(rs.next()) {
							Long id = rs.getLong("id");
							String k = rs.getString("k");
							String v = rs.getString("v");
							
							URI uri = URI.create(prefix + id + "#id");
							
							Model model = tagMapper.map(uri, new Tag(k, v));
							result.add(model);
						}

					} catch(Throwable t) {
						logger.error(ExceptionUtil.toString(t));
					}
					
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
		return prepareIdBasedTagQuery(LGDQueries.nodeTagsQuery, "node", ids);
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
		return prepareIdBasedTagQuery(LGDQueries.wayTagsQuery, "way", ids);
	}

	public Callable<Model> getWayNodes(Collection<Long> ids)
		throws Exception
	{
		return prepareSimpleIdBasedQuery(LGDQueries.wayNodeQuery, ids);
	}

}
