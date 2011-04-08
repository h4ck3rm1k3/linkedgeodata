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
package org.linkedgeodata.osm.mapping.impl;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.vocab.WGS84Pos;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.osm.osmosis.plugins.INodeSerializer;
import org.linkedgeodata.osm.osmosis.plugins.VirtuosoOseNodeSerializer;
import org.linkedgeodata.util.ITransformer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.XSD;



/**
 * Transforms osm nodes (v0.6) into RDF using a tagMapper.
 * 
 * 
 * @author Claus Stadler
 *
 */
public class SimpleNodeToRDFTransformer
	implements ITransformer<Node, Model>
{
	private static final TypeMapper tm = TypeMapper.getInstance();
	private static final RDFDatatype virtrdfGeometry = tm.getSafeTypeByName("http://www.openlinksw.com/schemas/virtrdf#Geometry");
	
	
	private static final Logger logger = Logger.getLogger(SimpleNodeToRDFTransformer.class);
	private ITagMapper tagMapper;
	private ILGDVocab vocab;
	private INodeSerializer nodeSerializer;
	
	private static int parseErrorCount = 0;

	public SimpleNodeToRDFTransformer(ITagMapper tagMapper, ILGDVocab vocab)
	{
		this.tagMapper = tagMapper;
		this.vocab = vocab;
		this.nodeSerializer = new VirtuosoOseNodeSerializer();
	}
	
	public SimpleNodeToRDFTransformer(ITagMapper tagMapper, ILGDVocab vocab, INodeSerializer nodeSerializer)
	{
		this.tagMapper = tagMapper;
		this.vocab = vocab;
		this.nodeSerializer = nodeSerializer;
	}
	
	@Override
	public Model transform(Model model, Node node)
	{
		//model.setNsPrefix("lgd", "http://linkedgeodata.org/");
		//model.setNsPrefix("lgdn", "http://linkedgeodata.org/node/");
		//model.setNsPrefix("lgdw", "http://linkedgeodata.org/way/");
		//model.setNsPrefix("lgdv", "http://linkedgeodata.org/vocabulary#");
 
		Resource subjectRes = getSubject(node);
		
		generateWGS84(model, subjectRes, node);
		
		//generateGeoRSS(model, subjectRes, node);
		Point2D point = new Point2D.Double(node.getLongitude(), node.getLatitude());
		nodeSerializer.write(model, subjectRes, point);
		
		generateTags(tagMapper, model, subjectRes.toString(), node.getTags());
		
		
		generateAttribution(model, vocab, subjectRes, node);
		
		return model;		
	}
	

	/**
	 * 
	 * Note: For optimization it may happen that the user is not set on an osm entity
	 * 
	 * 
	 * @param model
	 * @param vocab
	 * @param subject
	 * @param entity
	 */
	public static void generateAttribution(Model model, ILGDVocab vocab, Resource subject, Entity entity) {
		
		//TypeMapper tm = TypeMapper.getInstance();
		//RDFDatatype dataType = tm.getSafeTypeByName(XSD.xint.getURI());
		
		OsmUser user = entity.getUser();
		if(user == null)
			return;
		//model.add(subject, vocab.getUserIdPredicate(), Integer.toString(userId), dataType);

		
		int userId = user.getId();
		Resource contributor = vocab.createContributorURI(userId);
		model.add(subject, vocab.getUserIdPredicate(), contributor);
	}
	

	
	@Override
	public Model transform(Node node) {
		Model model = ModelFactory.createDefaultModel();

		return transform(model, node);
	}

	
	public static Literal generateVirtuosoLiteral(Point2D point)
	{
		Literal result = ResourceFactory.createTypedLiteral("POINT(" + (float)point.getX() + " " + (float)point.getY() + ")", virtrdfGeometry);
		return result;
	}
	
	public static void generateVirtusoPosition(Model model, Resource subject, Point2D point)
	{
		Literal literal = generateVirtuosoLiteral(point);
		model.add(subject, WGS84Pos.geometry, literal);
	}

	
	// FIXME Move this method to a class common for ways, nodes and relations.
	public static void generateTags(ITagMapper tagMapper, Model model, String subject, Collection<Tag> tags)
	{
		//if(tags == null)

		// Generate RDF for the tags
		for(Tag tag : tags) {
			Model subModel = tagMapper.map(subject, tag, model);
			if(subModel == null) {
				++parseErrorCount;
				logger.warn("Failed mapping: " + tag + ", Failed mapping count: " + parseErrorCount);
				
				continue;
			}
			else if(model == null) {
				model = subModel;
			}
			/*
			else {
				model.add(subModel);
			}*/
		}		
	}


	private Resource getSubject(long id)
	{
		return vocab.createNIRNodeURI(id);
	}
	
	private Resource getSubject(Node node)
	{		
		return getSubject(node.getId());
	}

	
	private static final String geoRSSPoint = "http://www.georss.org/georss/point";
	
	public static void generateGeoRSS(Model model, Resource subject, Node node)
	{
		String str = node.getLatitude() + " " + node.getLongitude();		
		model.add(subject, model.getProperty(geoRSSPoint), str);
	}
	
	public static void generateGeoRSS(Model model, Resource subject, Point2D point)
	{
		String str = point.getY() + " " + point.getX();		
		model.add(subject, model.getProperty(geoRSSPoint), str);
	}
	
	/*
	private static final String wgs84NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
	private static final String wgs84Lat = wgs84NS + "lat";
	private static final String wgs84Long = wgs84NS + "long";
	*/
	
	public static void generateWGS84(Model model, Resource subject, Node node)
	{
		TypeMapper tm = TypeMapper.getInstance();
		RDFDatatype dataType = tm.getSafeTypeByName(XSD.decimal.getURI());

		model.add(subject, WGS84Pos.xlat, Double.toString(node.getLatitude()), dataType);
		model.add(subject, WGS84Pos.xlong, Double.toString(node.getLongitude()), dataType);		
	}	
	
	public static Node createNode(long id)
	{
		Node node = new Node(id, 0, (Date)null, null, -1, 0.0, 0.0);
		return node;
	}

	public static Node createNode(long id, double lat, double lon)
	{
		Node node = new Node(id, 0, (Date)null, null, -1, lat, lon);
		return node;
	}
}