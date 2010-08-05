/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.linkedgeodata.dao;

import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.MultiMap;
import org.hibernate.Session;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.OSMEntityType;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleNodeToRDFTransformer;
import org.linkedgeodata.osm.mapping.impl.SimpleWayToRDFTransformer;
import org.linkedgeodata.util.ITransformer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;


public class LGDRDFDAO
	implements ISQLDAO, IHibernateDAO
{
	private LGDDAO dao;
	//private ITagMapper tagMapper;

	private OntologyDAO ontologyDAO;
	
	private ITransformer<Node, Model> nodeTransformer;
	private ITransformer<Way, Model> wayTransformer;
	
	private ILGDVocab vocab;
	
	public ILGDVocab getVocabulary()
	{
		return vocab;
	}
	
	public LGDRDFDAO(LGDDAO dao, ITagMapper tagMapper, ILGDVocab vocab)
		throws SQLException
	{
		this.dao = dao;
		//this.tagMapper = tagMapper;
		this.vocab = vocab;

		// FIXME Don't pull the connection out of the DAO like this
		ontologyDAO = new OntologyDAO(tagMapper, dao.getConnection());
		
		this.nodeTransformer = new SimpleNodeToRDFTransformer(tagMapper, vocab);
		this.wayTransformer = new SimpleWayToRDFTransformer(tagMapper, vocab);
	}
	
	
	public OntologyDAO getOntologyDAO()
	{
		return ontologyDAO;
	}
	

	private void writeNodeWays(Model model, MultiMap<Long, Long> members)
	{
		for(Map.Entry<Long, Collection<Long>> entry : members.entrySet()) {
			for(Long wayId : entry.getValue()) {
				model.add(
						model.createResource(vocab.createOSMNodeURI(entry.getKey())),
						model.createProperty(vocab.getMemberOfWayPred()),
						model.createResource(vocab.createOSMWayURI(wayId))
						);
			}
		}
	}

	private void writeWayNodes(Model model, MultiMap<Long, Long> members)
	{
		for(Map.Entry<Long, Collection<Long>> entry : members.entrySet()) {
			Resource memberRes = model.createResource(new AnonId());
			
			model.add(
					model.createResource(vocab.createOSMWayURI(entry.getKey())),
					model.createProperty(vocab.getHasNodesPred()),
					memberRes);
	
			model.add(memberRes,
					RDF.type,
					RDF.Seq);
			
			int i = 0;
			for(Long nodeId : entry.getValue()) {
				model.add(
						memberRes,
						model.createProperty(RDF.getURI() + "_" + (++i)),
						model.createResource(vocab.createOSMNodeURI(nodeId)));
			}
		}	
	}
	
	public int resolveNodes(Model model, Collection<Long> ids, boolean skipUntagged, String tagFilter)
		throws SQLException
	{
		Collection<Node> nodes = dao.getNodeDAO().getNodes(ids, skipUntagged, tagFilter);
		
		List<Long> subIds = new ArrayList<Long>();
		for(Node node : nodes) {
			subIds.add(node.getId());

			nodeTransformer.transform(model, node);
		}
		
		MultiMap<Long, Long> members = dao.getNodeDAO().getWayMemberships(subIds);
		writeNodeWays(model, members);
		
		return subIds.size();
	}
	
	
	public int resolveWays(Model model, Collection<Long> ids, boolean skipUntagged, String tagFilter)
			throws SQLException
	{	
		Collection<Way> ways = dao.getWayDAO().getWays(ids, true, tagFilter);
			
		List<Long> subIds = new ArrayList<Long>();
		for(Way way : ways) {
			subIds.add(way.getId());
	
			wayTransformer.transform(model, way);
		}
	
		MultiMap<Long, Long> members = dao.getWayDAO().getNodeMemberships(subIds);
		writeWayNodes(model, members);

		return subIds.size();
	}


	public int resolve(Model model, MultiMap<OSMEntityType, Long> typeToId, boolean skipUntagged, String tagFilter)
		throws SQLException
	{
		int result = 0;
		Collection<Long> ids;
		ids = typeToId.get(OSMEntityType.NODE);
		if(ids != null && !ids.isEmpty()) {
			result += resolveNodes(model, ids, skipUntagged, tagFilter);
		}

		ids = typeToId.get(OSMEntityType.WAY);
		if(ids != null && !ids.isEmpty()) {
			result += resolveWays(model, ids, skipUntagged, tagFilter);
		}
		
		return result;
	}
	
	
	
	public int getNodesWithinRect(
			Model model,
			RectangularShape rect,
			boolean skipUntagged,
			String tagFilter,
			Long offset,
			Integer limit)
		throws SQLException
	{
		Collection<Long> ids = dao.getNodesWithinRect(rect, tagFilter, offset, limit);
		int result = resolveNodes(model, ids, skipUntagged, tagFilter);
		return result;
	}
	
	public int getWaysWithinRect(
			Model model,
			RectangularShape rect,
			boolean skipUntagged,
			String tagFilter,
			Long offset,
			Integer limit)
		throws SQLException
	{
		Collection<Long> ids = dao.getWaysWithinRect(rect, tagFilter, offset, limit);
		int result = resolveWays(model, ids, skipUntagged, tagFilter);
		return result;
	}
	

	public int getNodesWithinRadius(
			Model model,
			Point2D point,
			double radius,
			boolean skipUntagged,
			String tagFilter,
			Integer limit,
			Long offset)
		throws SQLException
	{
		Collection<Long> ids = dao.getNodesWithinRadius(point, radius, tagFilter, limit, offset);
		int result = resolveNodes(model, ids, skipUntagged, tagFilter);
		return result;
	}


	public int getWaysWithinRadius(
			Model model,
			Point2D point,
			double radius,
			boolean skipUntagged,
			String tagFilter,
			Integer limit,
			Long offset)
		throws SQLException
	{
		Collection<Long> ids = dao.getWaysWithinRadius(point, radius, tagFilter, limit, offset);
		int result = resolveWays(model, ids, skipUntagged, tagFilter);
		return result;
	}
	
	public LGDDAO getSQLDAO()
	{
		return dao;
	}

	

	/*************************************************************************/
	/* Getters and Setters for JDBC connections and hibernate sessions       */
	/*************************************************************************/
	@Override
	public void setSession(Session session)
	{
		this.ontologyDAO.setSession(session);
	}

	@Override
	public Session getSession()
	{
		return this.ontologyDAO.getSession();
	}

	@Override
	public void setConnection(Connection conn)
		throws SQLException
	{
		this.dao.setConnection(conn);
	}

	@Override
	public Connection getConnection()
	{
		return this.dao.getConnection();
	}
}
