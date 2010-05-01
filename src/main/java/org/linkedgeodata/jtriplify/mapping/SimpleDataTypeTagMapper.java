package org.linkedgeodata.jtriplify.mapping;

import java.net.URI;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.XSD;

public class SimpleDataTypeTagMapper
	extends AbstractOneOneTagMapper
{		
	private static final Logger logger = Logger.getLogger(SimpleDataTypeTagMapper.class);
	private RDFDatatype dataType;

	// TODO add switch for yes/no - true/false representations

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
	public SimpleDataTypeTagMapper(URI property, Tag tag, RDFDatatype dataType)
		throws Exception
	{
		//URI.create("http://linkedgeodata.org/method/simple?type=dt&dataType=" + URLEncoder.encode(dataType.toString(), "UTF-8")),
		super(property, tag);

		this.dataType = dataType;
		// TODO Check the function type
		
	}

	public Model map(URI subject, Tag tag)
	{
		if(!matches(this.getTagPattern(), tag))
			return null;

		String str = tag.getValue().trim().toLowerCase();
		
		TypeMapper tm = TypeMapper.getInstance();
		RDFDatatype dataType = tm.getSafeTypeByName(XSD.xboolean.toString());
		if(dataType.equals(dataType)) {
			if(str.equals("yes")) str = "true";
			if(str.equals("no")) str = "false";
		}
				
				
		if(!dataType.isValid(str)) {
			logger.info("Failed to parse to'" + dataType + "', value: '" + str + "'");		
			return null;
		}

		Model result = ModelFactory.createDefaultModel();
		result.add(
				result.getResource(subject.toString()),
				result.getProperty(super.getResource().toString()),
				str,
				dataType);
		
		return result;
	}

	@Override
	public String toString()
	{
		try {
			return TagMapperFactory.buildDataTypeURI(super.getResource().toString(), super.getTagPattern().getKey(), super.getTagPattern().getValue(), dataType.getURI()).toString();
		} catch(Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
}

