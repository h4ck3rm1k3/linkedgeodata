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
package org.aksw.resparql;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.commons.collections.MultiMaps;
import org.apache.commons.collections15.MultiMap;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.access.TagFilterUtils;
import org.linkedgeodata.dao.IConnectionFactory;
import org.linkedgeodata.dao.IHibernateDAO;
import org.linkedgeodata.dao.ISQLDAO;
import org.linkedgeodata.dao.ISessionProvider;
import org.linkedgeodata.dao.LGDQueries;
import org.linkedgeodata.dao.LGDRDFDAO;
import org.linkedgeodata.dao.NodeStatsDAO;
import org.linkedgeodata.dao.OntologyDAO;
import org.linkedgeodata.dao.TagMapperDAO;
import org.linkedgeodata.jtriplify.methods.Pair;
import org.linkedgeodata.osm.mapping.CachingTagMapper;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * 
 * TODO All methods consisting of more than 1 line should be put into the
 * LGDRDFDAO
 * 
 * TODO Use Jersey for providing the REST-interface
 * http://docs.sun.com/app/docs/doc/820-4867/ggnxo?l=en&a=view
 * 
 * @author raven
 * 
 */
public class ServerMethods
		implements IRestApi
{
	// A model containing metadata definitions
	// Such as the definition of the lgdo:hasNodes property
	private Model				ontologyModel;

	// private ConnectionConfig connectionConfig;
	private IConnectionFactory	connectionFactory;
	private ISessionProvider	sessionFactory;

	// FIXME Refactor these dao lists and the corresponding methods into a
	// separate class
	private List<IHibernateDAO>	hibernateDAOs	= new ArrayList<IHibernateDAO>();
	private List<ISQLDAO>		jdbcDAOs		= new ArrayList<ISQLDAO>();

	private LGDRDFDAO			lgdRDFDAO;											// =
																					// new
																					// LGDRDFDAO(dao,
																					// tagMapper,
																					// vocab)

	// private ExecutorService executor = Executors.newFixedThreadPool(2);
	// private ExecutorService executor = Executors.newCachedThreadPool();

	private Map<String, String>	prefixMap		= null;

	public ServerMethods(LGDRDFDAO lgdRDFDAO, Map<String, String> prefixMap,
			IConnectionFactory connectionFactory,
			ISessionProvider sessionFactory, Model ontologyModel)
	{
		this.ontologyModel = ontologyModel;

		hibernateDAOs.add(new TagMapperDAO());
		/*
		 * NodeDAO nodeDAO; WayDAO wayDAO; LGDDAO lgdDAO;
		 * 
		 * NodeStatsDAO nodeStatsDAO;
		 * 
		 * TagDAO tagDAO; TagLabelDAO tagLabelDAO;
		 * 
		 * LGDRDFDAO lgdRdfDao;
		 * 
		 * TagMapperDAO tagMapperDAO;
		 */

		this.lgdRDFDAO = lgdRDFDAO;

		hibernateDAOs.add(lgdRDFDAO);
		hibernateDAOs.add(lgdRDFDAO.getOntologyDAO());
		jdbcDAOs.add(lgdRDFDAO);

		jdbcDAOs.add(lgdRDFDAO.getOntologyDAO().getTagDAO());
		jdbcDAOs.add(lgdRDFDAO.getOntologyDAO().getTagLabelDAO());

		ITagMapper tagMapper = lgdRDFDAO.getOntologyDAO().getTagMapper();

		if (tagMapper instanceof CachingTagMapper) {
			tagMapper = ((CachingTagMapper) tagMapper).getSource();
		}

		if (tagMapper instanceof ISQLDAO)
			jdbcDAOs.add((ISQLDAO) tagMapper);

		if (tagMapper instanceof IHibernateDAO)
			hibernateDAOs.add((IHibernateDAO) tagMapper);

		this.prefixMap = prefixMap;

		this.connectionFactory = connectionFactory;
		this.sessionFactory = sessionFactory;
	}

	/**
	 * This method is a hack right now. It seems that hibernate based DAOs
	 * should themselve retrieve a session object from the SessionFactory (e.g.
	 * HibernateUtil.getSessionFactory().getCurrentSession()). Rather than
	 * having the session set from the outside.
	 * 
	 * 
	 */
	void prepare() throws Exception
	{
		prepareConnection();
		prepareSession();
	}

	void prepareConnection() throws Exception
	{
		Connection conn = connectionFactory.getConnection();

		setConnection(conn);
	}

	void setConnection(Connection conn) throws SQLException
	{
		for (ISQLDAO dao : jdbcDAOs)
			dao.setConnection(conn);
	}

	void prepareSession() throws SQLException
	{
		Session session = sessionFactory.getSession();

		setSession(session);
	}

	void setSession(Session session) throws SQLException
	{
		for (IHibernateDAO dao : hibernateDAOs)
			dao.setSession(session);
	}

	private Model createModel()
	{
		Model result = ModelFactory.createDefaultModel();
		result.setNsPrefixes(prefixMap);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.linkedgeodata.jtriplify.IRestApi#getNode(java.lang.String)
	 */
	@Override
	public Model getNode(Long id) throws Exception
	{
		Model result = createModel();

		prepare();
		Transaction tx = lgdRDFDAO.getSession().beginTransaction();
		try {
			lgdRDFDAO.resolveNodes(result, Collections.singleton(id), false,
					null);
			tx.commit();
			return result;
		} catch (Throwable t) {
			tx.rollback();
			throw new RuntimeException(t);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.linkedgeodata.jtriplify.IRestApi#getWayNode(java.lang.String)
	 */
	@Override
	public Model getWayNode(Long id) throws Exception
	{
		Resource subject = lgdRDFDAO.getVocabulary().wayToWayNode(
				lgdRDFDAO.getVocabulary().createNIRWayURI(id));

		Model tmp = getWay(id);

		Model result = createModel();
		result.add(tmp.listStatements(subject, null, (RDFNode) null));

		return result;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.linkedgeodata.jtriplify.IRestApi#getWay(java.lang.String)
	 */
	@Override
	public Model getWay(Long id) throws Exception
	{
		Model result = createModel();

		prepare();
		Transaction tx = lgdRDFDAO.getSession().beginTransaction();
		try {
			lgdRDFDAO.resolveWays(result, Collections.singleton(id), false,
					null);
			tx.commit();
			return result;
		} catch (Throwable t) {
			tx.rollback();
			throw new RuntimeException(t);
		}
	}

	/*
	 * public Model publicGetEntitiesWithinRadius2010Aug5(Double lat, Double
	 * lon, Double radius, String k, String v, Boolean bOr) throws Exception {
	 * prepare();
	 * 
	 * String tagFilter = LGDQueries.createPredicate("", k, v, bOr);
	 * if(tagFilter.isEmpty()) tagFilter = null;
	 * 
	 * Model result = createModel(); lgdRDFDAO.getNodesWithinRadius(result, new
	 * Point2D.Double(lon, lat), radius, false, tagFilter, null, null);
	 * 
	 * //System.out.println(ModelUtil.toString(result));
	 * 
	 * lgdRDFDAO.getWaysWithinRadius(result, new Point2D.Double(lon, lat),
	 * radius, false, tagFilter, null, null);
	 * //System.out.println(ModelUtil.toString(result));
	 * 
	 * return result; }
	 */

	public Model publicGetEntitiesWithinRectOld(Double latMin, Double latMax,
			Double lonMin, Double lonMax, String k, String v, Boolean bOr)
			throws Exception
	{
		prepare();

		String tagFilter = LGDQueries.createPredicate("", k, v, bOr);
		if (tagFilter.isEmpty())
			tagFilter = null;

		Rectangle2D rect = new Rectangle2D.Double(lonMin, latMin, lonMax
				- lonMin, latMax - latMin);

		Model result = createModel();
		lgdRDFDAO
				.getNodesWithinRect(result, rect, false, tagFilter, null, null);
		lgdRDFDAO.getWaysWithinRect(result, rect, false, tagFilter, null, null);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.linkedgeodata.jtriplify.IRestApi#publicGetEntitiesWithinRadius(java
	 * .lang.Double, java.lang.Double, java.lang.Double, java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.Long,
	 * java.lang.Long)
	 */
	@Override
	public Model publicGetEntitiesWithinRadius(Double lat, Double lon,
			Double radius, String className, String language, String matchMode,
			String label, Long offset, Long limit) throws Exception
	{
		prepare();

		if (limit == null || limit > 1000l)
			limit = 1000l;

		Transaction tx = lgdRDFDAO.getSession().beginTransaction();
		try {
			List<String> entityTagConditions = getEntityTagCondititions(
					className, label, language, matchMode);
			Ellipse2D circle = new Ellipse2D.Double(lon, lat, radius, radius);

			Model result = getEntitiesWithinShape(circle, entityTagConditions,
					offset, limit);

			tx.commit();
			return result;
		} catch (Throwable e) {
			tx.rollback();
			throw new Exception(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.linkedgeodata.jtriplify.IRestApi#publicGetEntitiesWithinRect(java
	 * .lang.Double, java.lang.Double, java.lang.Double, java.lang.Double,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.Long, java.lang.Long)
	 */
	@Override
	public Model publicGetEntitiesWithinRect(Double latMin, Double latMax,
			Double lonMin, Double lonMax, String className, String language,
			String matchMode, String label, Long offset, Long limit)
			throws Exception
	{
		prepare();

		if (limit == null || limit > 1000l)
			limit = 1000l;

		Transaction tx = lgdRDFDAO.getSession().beginTransaction();
		try {
			Rectangle2D rect = new Rectangle2D.Double(lonMin, latMin, lonMax
					- lonMin, latMax - latMin);
			List<String> entityTagConditions = getEntityTagCondititions(
					className, label, language, matchMode);

			Model result = getEntitiesWithinShape(rect, entityTagConditions,
					offset, limit);

			tx.commit();
			return result;
		} catch (Throwable e) {
			tx.rollback();
			throw new Exception(e);
		}
	}

	private Model getEntitiesWithinShape(RectangularShape shape,
			List<String> entityTagConditions, Long offset, Long limit)
			throws Exception
	{
		if (limit == null || limit > 1000l)
			limit = 1000l;

		Model result = createModel();
		NodeStatsDAO nodeStatsDAO = new NodeStatsDAO(lgdRDFDAO.getSQLDAO()
				.getConnection());

		Collection<Long> tileIds = null; // NodeStatsDAO.getTileIds(rect, 16);
		Collection<Long> nodeIds = nodeStatsDAO.getNodeIds(tileIds, 16, shape,
				entityTagConditions, offset, limit);

		lgdRDFDAO.resolveNodes(result, nodeIds, false, null);
		

		// dao.getNodesWithinRect(result, rect, false, tagFilter, null, null);
		lgdRDFDAO.getWaysWithinRect(result, shape, false, entityTagConditions, offset, limit);

		return result;
	}

	public Model publicGetOntology() throws Exception
	{
		prepare();

		Transaction tx = lgdRDFDAO.getSession().beginTransaction();
		Model model = createModel();
		try {
			// lgdRDFDAO.getOntologyDAO().getOntology(model);
			model.add(ontologyModel.listStatements(null, null, (RDFNode) null));

		} catch (Throwable e) {
			tx.rollback();
			throw new Exception(e);
		}

		tx.commit();
		return model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.linkedgeodata.jtriplify.IRestApi#publicDescribe(java.lang.String)
	 */
	@Override
	public Model publicDescribe(String uri) throws Exception
	{
		prepare();

		Model model = createModel();

		// Replace the prefix with the appropriate namespace
		// The reason not to use the full original URI is because in testing
		// the domain and port may be differ.
		uri = uri.replaceFirst("^ontology/", lgdRDFDAO.getVocabulary()
				.getOntologyNS());

		Resource subject = ResourceFactory.createResource(uri);

		// FIXME The model returned differs from the one being passed in!
		Transaction tx = lgdRDFDAO.getSession().beginTransaction();
		try {
			// System.out.println(ModelUtil.toString(ontologyModel,
			// "N-TRIPLE"));

			model.add(ontologyModel.listStatements(subject, null,
					(RDFNode) null));

			// model = lgdRDFDAO.getOntologyDAO().describe(uri, model);
		} catch (Throwable e) {
			tx.rollback();
			throw new Exception(e);
		}
		tx.commit();

		// Check the metadata

		return model;
	}

	/*************************************************************************/
	/* Helper methods for processing class and label restrictions */
	/*************************************************************************/
	private Pair<String, TagFilterUtils.MatchMode> getMatchConfig(String label,
			String matchMode)
	{
		if (label == null || matchMode == null)
			return null;

		TagFilterUtils.MatchMode mm = TagFilterUtils.MatchMode.EQUALS;
		if (matchMode.equalsIgnoreCase("contains")) {
			mm = TagFilterUtils.MatchMode.ILIKE;
			label = "%" + label.replace("%", "\\%") + "%";
		} else if (matchMode.equalsIgnoreCase("startsWith")) {
			mm = TagFilterUtils.MatchMode.ILIKE;
			label = label.replace("%", "\\%") + "%";
		}
		if (matchMode.equalsIgnoreCase("ccontains")) {
			mm = TagFilterUtils.MatchMode.LIKE;
			label = "%" + label.replace("%", "\\%") + "%";
		} else if (matchMode.equalsIgnoreCase("cstartsWith")) {
			mm = TagFilterUtils.MatchMode.LIKE;
			label = label.replace("%", "\\%") + "%";
		}

		return new Pair<String, TagFilterUtils.MatchMode>(label, mm);
	}

	private List<String> getEntityTagCondititions(String className,
			String label, String language, String matchMode) throws Exception
	{
		if (language != null && language.equalsIgnoreCase("any"))
			language = null;

		Pair<String, TagFilterUtils.MatchMode> lmm = getMatchConfig(label,
				matchMode);

		// FIXME Add this to some kind of facade
		TagFilterUtils filterUtil = new TagFilterUtils(
				lgdRDFDAO.getOntologyDAO());
		filterUtil.setSession(lgdRDFDAO.getOntologyDAO().getSession());

		List<String> entityTagConditions = new ArrayList<String>();

		if (className != null)
			entityTagConditions.add(filterUtil.restrictByObject(
					RDF.type.toString(), "http://linkedgeodata.org/ontology/"
							+ className, "$$"));

		if (label != null)
			entityTagConditions.add(filterUtil.restrictByText(
					RDFS.label.toString(), lmm.getKey(), language,
					lmm.getValue(), "$$"));

		return entityTagConditions;
	}

	public Model publicGetAreaStatistics(Double latMin, Double latMax,
			Double lonMin, Double lonMax) throws Exception
	{
		Rectangle2D rect = new Rectangle2D.Double(lonMin, latMin, lonMax
				- lonMin, latMax - latMin);

		prepare();
		Transaction tx = lgdRDFDAO.getSession().beginTransaction();
		try {
			return getAreaStatistics(rect, null);
		} finally {
			tx.commit();
		}
	}

	public Model getAreaStatistics(RectangularShape shape, List<String> classUris) throws Exception {
		Model result = ModelFactory.createDefaultModel();
		
		classUris = new ArrayList<String>();
		classUris.add("http://linkedgeodata.org/ontology/Amenity");
		classUris.add("http://linkedgeodata.org/ontology/Tourism");
		classUris.add("http://linkedgeodata.org/ontology/Leisure");
		classUris.add("http://linkedgeodata.org/ontology/Shop");
		
		Map<String, Set<Tag>> classToTags = new HashMap<String, Set<Tag>>();
		

		
		// Reverse map all classUris
		OntologyDAO dao = lgdRDFDAO.getOntologyDAO();
		for(String uri : classUris) {
			Set<Tag> tags = MultiMaps.addKey(classToTags, uri);

			MultiMap<Tag, IOneOneTagMapper> rev = dao.reverseMapResourceObject(RDF.type.getURI(), uri);

			for(Tag tag : rev.keySet()) {
				if(tag.getKey() == null) {
					tags.clear();
					tags.add(tag);
				} else if(tag.getValue() == null) {
					for(Iterator<Tag> it = tags.iterator(); it.hasNext();) {
						if(it.next().getKey().equals(tag.getKey())) {
							it.remove();
						}
					}
					tags.add(tag);
				} else {
					boolean isSubsumed = false;
					for(Tag tmp : tags) {
						if(tmp.getKey() == null || tmp.getValue() == null) {
							isSubsumed = true;
							break;
						}
					}
					
					if(!isSubsumed) {
						tags.add(tag);
					}
				}
			}
		}
		
		Set<String> keys = new HashSet<String>();
		//Set<Tag> tags = new HashSet<Tag>();
		for(Set<Tag> tags : classToTags.values()) {
			for(Tag tag : tags) {
				if(tag.getValue() == null) {
					keys.add(tag.getKey());
				}
			}
		}
		
		
		Connection conn = lgdRDFDAO.getConnection();
		String sql = LGDQueries.buildAreaStatsQueryExact(shape, keys);
		
		java.sql.ResultSet rs = conn.createStatement().executeQuery(sql);
		Map<String, Long> keyToCount = new HashMap<String, Long>();
		while(rs.next()) {			
			keyToCount.put(rs.getString("k"), rs.getLong("c"));
		}
		//for(
		
		Map<String, Long> uriToCount = new HashMap<String, Long>();
		// Cross check the tags with the requested classes
		
		return result;
	}

	public static String toUriPart(RectangularShape rect)
	{
		return rect.getMinX() + "-" + rect.getMaxX() + "/" + rect.getMinY() + "-" + rect.getMaxY();
	}
	
	public Model classStatsToRdf(RectangularShape rect,
			Map<String, Long> uriToCount)
	{
		Model result = ModelFactory.createDefaultModel();
		Resource area = ResourceFactory.createResource("http://linkedgeodata.org/area/" + toUriPart(rect));
		
		Property count = ResourceFactory.createProperty("http://linkedgeodata.org/ontology/frequency");
		Property correspondsTo = ResourceFactory.createProperty("http://linkedgeodata.org/ontology/correspondsTo");
		Property hasOccurrence = ResourceFactory.createProperty("http://linkedgeodata.org/ontology/hasOccurrence");
	
		/*
		for(Entry<String, Long> entry : uriToCount) {
			
		}
		*/
		
		return null;
	}
}

/**
 * Validates a hibernate session object. Cannot be used when there is already an
 * open transaction on the session.
 * 
 * @param session
 */
/*
 * void sendTestQuery(Session session) throws SQLException { Transaction tx =
 * session.beginTransaction(); SQLQuery q = session.createSQLQuery("SELECT 1");
 * q.uniqueResult();
 * 
 * tx.commit(); }
 * 
 * void sendTestQuery(Connection conn) throws SQLException {
 * SQLUtil.execute(conn, "SELECT 1", Integer.class);
 * 
 * if(conn.getAutoCommit() == false) { conn.commit(); } }
 * 
 * void initConnections() throws Exception { Connection conn =
 * lgdRDFDAO.getConnection(); if(conn == null) { conn =
 * connectionFactory.createConnection();
 * 
 * if(conn == null) { throw new SQLException("Problem connecting to database");
 * }
 * 
 * for(ISQLDAO dao : jdbcDAOs) dao.setConnection(conn); }
 * 
 * Session session = lgdRDFDAO.getSession(); if(session == null) { session =
 * sessionFactory.createSession(); if(session == null) { throw new
 * SQLException("Problem connecting to database"); }
 * 
 * for(IHibernateDAO dao : hibernateDAOs) dao.setSession(session); }
 * 
 * } void validateConnection() throws Exception { Connection conn =
 * lgdRDFDAO.getConnection();
 * 
 * while(true) { try { sendTestQuery(conn); break; } catch(Throwable t) {
 * 
 * // Wait a while, then aquire a fresh connection Thread.sleep(5000);
 * conn.close(); conn = connectionFactory.createConnection();
 * lgdRDFDAO.setConnection(conn); } } }
 * 
 * /* void validateSession() throws Exception { Session session =
 * lgdRDFDAO.getSession();
 * 
 * while(true) { try { sendTestQuery(session); break; } catch(Throwable t) {
 * 
 * // Wait a while, then aquire a fresh connection Thread.sleep(5000);
 * session.close(); session = sessionFactory.createSession();
 * lgdRDFDAO.setSession(session); } } }
 */

/**
 * Checks for connection problems by sending simple test queries. Common
 * connections problems arise when the database was shut down or when the
 * connection exceeds its time-to-live.
 * 
 * @throws SQLException
 * 
 *             / void validateConnections() throws Exception {
 *             validateConnection(); //validateSession(); }
 */
