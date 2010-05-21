package org.linkedgeodata.jtriplify.mapping;

import java.net.URI;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class SimpleTextTagMapper
	extends AbstractOneOneTagMapper
{		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static final Logger logger = Logger.getLogger(SimpleTextTagMapper.class);

	private String langTag;
	

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
	public SimpleTextTagMapper(String property, TagPattern tagPattern, String langTag, boolean isOSMEntity)
		throws Exception
	{
		//URI.create("http://linkedgeodata.org/method/simple?type=dt&dataType=" + URLEncoder.encode(dataType.toString(), "UTF-8")),
		super(property, tagPattern, isOSMEntity);

		this.langTag = langTag;
	}

	/*
	public Tag reverseMap(Triple triple)
	{
		if(!triple.getObject().isLiteral())
			return false;
		
		if(super.getTagPattern().getKey() != null) {
			
		}
	}*/
	
	
	public Model map(String subject, Tag tag)
	{
		if(!isOSMEntity())
			subject += "#id";
		
		if(!matches(this.getTagPattern(), tag))
			return null;


		String suffix = "";
		if(super.getTagPattern().getKey() == null) {
			suffix = tag.getKey();
		}

		Model result = ModelFactory.createDefaultModel();
		/*
		if(langTag == null) {
			result.add(
					result.getResource(subject.toString()),
					result.getProperty(super.getResource().toString() + suffix),
					tag.getValue());
		}
		else {*/
		result.add(
				result.getResource(subject),
				result.getProperty(super.getResource().toString() + suffix),
				tag.getValue(),
				langTag);
			
		//}

		return result;
	}
	
	/*
	@Override
	public String toString()
	{
		try {
			return TagMapperFactory.buildTextURI(super.getResource().toString(), super.getTagPattern().getKey(), super.getTagPattern().getValue(), langTag).toString();
		} catch(Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	 */
}

