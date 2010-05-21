package org.linkedgeodata.jtriplify.mapping;

import java.io.Serializable;
import java.net.URI;
import java.util.Random;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;


public class SimpleClassTagMapper
	extends AbstractOneOneTagMapper
	implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static final Logger logger = Logger.getLogger(SimpleDataTypeTagMapper.class);
	
	
	/**
	 * Valid types are:
	 * type=int
	 * type=float
	 * type=bool
	 * type=uri
	 * type=lang, langTag=<>
	 * type=class
	 * 
	 * @param resource
	 * @param method
	 * @param tag
	 */
	public SimpleClassTagMapper(String clazz, TagPattern tagPattern, boolean isOSMEntity)
		throws Exception
	{
		super(clazz, tagPattern, isOSMEntity);	
	}
	
	public Model map(String subject, Tag tag)
	{
		if(!isOSMEntity())
			subject += "#id";

		if(!super.matches(tag)) {
			return null;
		}
		
		String suffix = "";
		
		if(super.getTagPattern().getValue() == null) {
			suffix = tag.getValue();
		}
		
		Model result = ModelFactory.createDefaultModel();
		result.add(
				result.getResource(subject.toString()),
				RDF.type,
				result.getResource(super.getResource().toString() + suffix)
		);
		
		return result;
	}
	

	public Tag reverseMap(Triple triple)
	{
		// Predicate must be rdf:type
		if(!RDF.type.equals(triple.getPredicate()) && !triple.getObject().isURI())
		{
			return null;
		}
		
		String classURI = triple.getObject().getURI();

		String v = super.getTagPattern().getValue();
		if(v == null) {
			if(!classURI.startsWith(super.getResource().toString())) {
				return null;
			}
			
			v = classURI.substring(super.getResource().toString().length());
		}
		else {
			if(!classURI.equals(super.getResource().toString())) {
				return null;
			}
		}

		Tag result = new Tag(super.getTagPattern().getKey(), v);
		
		if(!super.matches(super.getTagPattern(), result))
			result = null;
			
		return result;
	}

	/*
	@Override
	public String toString()
	{
		try {
			return TagMapperFactory.buildClassURI(super.getResource().toString(), super.getTagPattern().getKey(), super.getTagPattern().getValue()).toString();
		} catch(Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	*/
}
