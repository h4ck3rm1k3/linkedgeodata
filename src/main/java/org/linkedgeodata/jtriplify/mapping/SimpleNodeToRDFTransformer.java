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
package org.linkedgeodata.jtriplify.mapping;

import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.jtriplify.TagMapper;
import org.linkedgeodata.util.ITransformer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
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
	private static final Logger logger = Logger.getLogger(SimpleNodeToRDFTransformer.class);
	private TagMapper tagMapper;

	
	private static int parseErrorCount = 0;
	
	public SimpleNodeToRDFTransformer(TagMapper tagMapper)
	{
		this.tagMapper = tagMapper;
	}
	
	@Override
	public Model transform(Model model, Node node)
	{
		//model.setNsPrefix("lgd", "http://linkedgeodata.org/");
		//model.setNsPrefix("lgdn", "http://linkedgeodata.org/node/");
		//model.setNsPrefix("lgdw", "http://linkedgeodata.org/way/");
		//model.setNsPrefix("lgdv", "http://linkedgeodata.org/vocabulary#");
		
		String subject = getSubject(node);
		Resource subjectRes = model.getResource(subject);
		
		generateWGS84(model, subjectRes, node);
		generateGeoRSS(model, subjectRes, node);
		generateTags(tagMapper, model, subject, node.getTags());

		return model;		
	}
	
	@Override
	public Model transform(Node node) {
		Model model = ModelFactory.createDefaultModel();

		return transform(model, node);
	}
	
	public static void generateTags(TagMapper tagMapper, Model model, String subject, Collection<Tag> tags)
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

			//model.add(subModel);
		}		
	}


	public static String getSubject(long id)
	{
		return LGDVocab.createNIRNodeURI(id);
	}
	
	public static String getSubject(Node node)
	{		
		return getSubject(node.getId());
	}

	
	private static final String geoRSSPoint = "http://www.georss.org/georss/point";
	
	public static void generateGeoRSS(Model model, Resource subject, Node node)
	{
		String str = node.getLatitude() + " " + node.getLongitude();		
		model.add(subject, model.getProperty(geoRSSPoint), str);
	}
	
	
	private static final String wgs84NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
	private static final String wgs84Lat = wgs84NS + "lat";
	private static final String wgs84Long = wgs84NS + "long";
	
	public static  void generateWGS84(Model model, Resource subject, Node node)
	{
		TypeMapper tm = TypeMapper.getInstance();
		RDFDatatype dataType = tm.getSafeTypeByName(XSD.decimal.getURI());

		model.add(subject, model.getProperty(wgs84Lat), Double.toString(node.getLatitude()), dataType);
		model.add(subject, model.getProperty(wgs84Long), Double.toString(node.getLongitude()), dataType);		
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