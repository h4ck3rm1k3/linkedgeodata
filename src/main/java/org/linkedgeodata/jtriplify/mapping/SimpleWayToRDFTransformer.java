package org.linkedgeodata.jtriplify.mapping;

import org.linkedgeodata.jtriplify.TagMapper;
import org.linkedgeodata.util.ITransformer;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class SimpleWayToRDFTransformer
	implements ITransformer<Way, Model>
{
	private TagMapper tagMapper;
	
	public SimpleWayToRDFTransformer(TagMapper tagMapper)
	{
		this.tagMapper = tagMapper;
	}
	
	@Override
	public Model transform(Model model, Way way)
	{
		
		String subject = getSubject(way);
		//Resource subjectRes = model.getResource(subject + "#id");
		
		//generateWGS84(model, subjectRes, node);
		//generateGeoRSS(model, subjectRes, node);
		SimpleNodeToRDFTransformer.generateTags(tagMapper, model, subject, way.getTags());
	
		return model;
	}
	
	@Override
	public Model transform(Way way)
	{
		Model model = ModelFactory.createDefaultModel();
		
		return transform(model, way);
	}
	
	public static String getSubject(long id)
	{
		String prefix = "http://linkedgeodata.org/";
		//String result = prefix + "way/" + way.getId();
		String result = prefix + "way" + id;
		
		return result;
	}
	
	public static String getSubject(Way way)
	{
		return getSubject(way.getId());
	}
	
	//public static void generateGeoRSS(Model model, Resource subjectRes, node);

}
