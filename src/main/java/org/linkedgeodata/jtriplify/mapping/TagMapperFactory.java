package org.linkedgeodata.jtriplify.mapping;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.commons.collections15.MultiMap;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.MultiMapUtil;
import org.linkedgeodata.util.URIUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.XSD;


/**
 * Currently tag mappers are serialized to URIs.
 * 
 * However, similar to the D2R config it might be nicer having the mapping
 * rules defined in n3.
 * 
 * Something like:
 * myMapping
 * 		a SimpleDataTypeTagMap;
 * 		targetProperty <http://ex.org/numberOfSeats>
 * 		targetDatatype xsd:int
 * 		tagPattern ("seats", nil)
 * 
 *
 * @author raven
 *
 */

public class TagMapperFactory
{
	public static void main(String[] args)
		throws Exception
	{
		Tag tag = new Tag((String)null, (String)null);
		System.out.println(tag.getKey() == null);
		System.out.println(tag.getValue() == null);
		
		
		System.out.println(URLDecoder.decode("http%3A%2F%2Flinkedgeodata.org%23VRS%2B338", "UTF-8"));
		//Tag x = new Tag((String)null, null); 
		PropertyConfigurator.configure("log4j.properties");
		
		test(buildDataTypeURI("http://linkedgeodata.org/triplify/seats", "k", null, XSD.xint.toString()), new Tag("k", "6"));
		test(buildDataTypeURI("http://linkedgeodata.org/triplify/seats", "k", "v", XSD.xint.toString()), new Tag("k", "vx"));

		test(buildClassURI("http://linkedgeodata.org/triplify#", "amenity", null), new Tag("amenity", "pub"));
	}
	
	private static void test(String str, Tag tag)
		throws Exception
	{
		System.out.println("Test: " + str);
		TagMapperFactory f = new TagMapperFactory();
		String dt = URLEncoder.encode(XSD.integer.toString(), "UTF-8");
		IOneOneTagMapper mapper =
			f.createInstance(str);
		
		Model model = mapper.map(URI.create("http://test.org"), tag);
		
		if(model == null)
			System.out.println("No model");
		else
			System.out.println(ModelUtil.toString(model, "N3"));
	}
	
	
	public static String buildDataTypeURI(String p, String k, String v, String dataType)
		throws Exception
	{
		return buildQuery("datatype", p, k, v) + "&uri=" + URLEncoder.encode(dataType, "UTF-8");
	}
	
	public static String buildClassURI(String p, String k, String v)
		throws Exception
	{
		return buildQuery("class", p, k, v);		
	}
	
	public static String buildTextURI(String p, String k, String v, String langTag)
		throws Exception
	{
		return buildQuery("text", p, k, v) + 
			(langTag == null ? "" : "&langTag=" + langTag);
	}
	
	public static String buildQuery(String type, String p, String k, String v)
		throws Exception
	{
		String result = "http://linkedgeodata.org/methods/simple";

		result += "?type=" + URLEncoder.encode(type, "UTF-8");
		result += "&property=" + URLEncoder.encode(p, "UTF-8");
		
		if(k != null)
			result += "&k=" + URLEncoder.encode(k, "UTF-8");
		
		if(v != null)
			result += "&v=" + URLEncoder.encode(v, "UTF-8");
		
		return result;
	}
		
	//public IOneOneTagMapper createInstance(String, 
	
	public IOneOneTagMapper createInstance(String methodStr)
		throws Exception
	{		
		URI method = URI.create(methodStr);
		
		String prefix = "http://linkedgeodata.org/methods/simple?";
		if(!method.toString().startsWith(prefix)) {
			throw new RuntimeException("Method '" + method + "' not supported");
		}

		MultiMap<String, String> queryMap = URIUtil.getQueryMap(method.getQuery());
		
		String k = MultiMapUtil.getOne(queryMap, "k");
		//String k = kk == null ? null : URLDecoder.decode(kk, "UTF-8");
		
		String v = MultiMapUtil.getOne(queryMap, "v");
		//String v = vv == null ? null : URLDecoder.decode(vv, "UTF-8");
		
		Tag tagPattern = new Tag(k, v);
		
		String pStr = MultiMapUtil.getOne(queryMap, "property");
		
		URI p = URI.create(pStr);
		//URI p = URI.create(URLDecoder.decode(pStr, "UTF-8"));
		
		
		String type = MultiMapUtil.getOne(queryMap, "type");
		if(type == null)
			throw new RuntimeException("No type specified");
		
		if(type.equals("datatype")) {
			String dataTypeStr = MultiMapUtil.getOne(queryMap, "uri");
			dataTypeStr = URLDecoder.decode(dataTypeStr, "UTF-8");
			
			TypeMapper tm = TypeMapper.getInstance();
			RDFDatatype dataType = tm.getSafeTypeByName(dataTypeStr);

			return new SimpleDataTypeTagMapper(p, tagPattern, dataType);
		}
		else if(type.equals("class")) {
			return new SimpleClassTagMapper(p, tagPattern);
		}
		else if(type.equals("text")) {
			String langTag = MultiMapUtil.getOne(queryMap, "langTag");

			return new SimpleTextTagMapper(p, tagPattern, langTag);
		}
		
		
		throw new RuntimeException("No suitable method found");
	
//		return null;
	}
}