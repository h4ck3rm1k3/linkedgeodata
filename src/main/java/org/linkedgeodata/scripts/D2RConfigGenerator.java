package org.linkedgeodata.scripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.List;

import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapperVisitor;
import org.linkedgeodata.osm.mapping.impl.SimpleClassTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleDataTypeTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTagPattern;
import org.linkedgeodata.osm.mapping.impl.SimpleTextTagMapper;
import org.linkedgeodata.util.IOUtil;
import org.linkedgeodata.util.StringUtil;


public class D2RConfigGenerator
{
	private static void writeHead(PrintStream out)
		throws IOException
	{
		File file = new File("data/lgd/d2r/head.n3");
		InputStream in = new FileInputStream(file);
		
		IOUtil.copy(in, out);
	}
	
	private static String writeClassMap(String relationName)
	{
		String result = 
			"map:node_tags a d2rq:ClassMap;\n" +
			"\td2rq:dataStorage map:database;\n" +
			"\td2rq:uriPattern \"http://localhost:2020/resource/node@@node_tags.node_id@@\";\n" +
			"\td2rq:class vocab:node_tags;\n" +
			"\td2rq:classDefinitionLabel \"node_tags\";\n" +
			"\t.\n";

		return result;
	}
	
	public static void main(String[] args)
		throws Exception
	{
		InMemoryTagMapper tagMapper = new InMemoryTagMapper();
		tagMapper.load(new File("output/LGDMappingRules.xml"));
		
		File outFile = new File("output/d2rmapping.n3");
		PrintStream out = new PrintStream(outFile);
		
		writeHead(out);

		out.println(writeClassMap("node_tags"));
		
		D2RConfigGeneratorVisitor generator = new D2RConfigGeneratorVisitor("node_tags", out);
		
		
		List<IOneOneTagMapper> list = tagMapper.asList();
		
		for(IOneOneTagMapper item : list) {
			if(item instanceof ISimpleOneOneTagMapper) {
				((ISimpleOneOneTagMapper)item).accept(generator);
			}
		}
		
		
		out.flush();
		out.close();
	}
}


class D2RConfigGeneratorVisitor
	implements ISimpleOneOneTagMapperVisitor<Void>
{
	private PrintStream out;
	private String relationName;
	
	public D2RConfigGeneratorVisitor(String relationName, PrintStream out)
	{
		this.relationName = relationName;
		this.out = out;
	}
	
	private String buildFullCondition(SimpleTagPattern tagPattern)
	{
		String part = buildCondition(tagPattern);

		String result = part == null
			? ""
			: "\td2rq:condition \"" + part + "\"\n";

		return result;
	}
	
	private String buildCondition(SimpleTagPattern tagPattern)
	{
		String keyPart = tagPattern.getKey() == null
			? null
			: relationName + ".k = " + "'" + tagPattern.getKey() + "'";
	
		String valPart = tagPattern.getValue() == null
			? null
			: relationName + ".v = " + "'" + tagPattern.getValue() + "'";
	
		String result = (keyPart != null && valPart != null)
			? keyPart + " AND " + valPart
			: StringUtil.coalesce(keyPart, valPart, "");
		
		return result;
	}
	
	private String toLabel(SimpleTagPattern tagPattern)
	{
		String result =
			StringUtil.coalesce(tagPattern.getKey(), "") +
			StringUtil.coalesce(tagPattern.getValue(), "");

		try {
			result = URLEncoder.encode(result, "UTF-8");
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
		result = result.replace("%", "_x_");
		result = result.replace("+", "_p_");
		
		return result;
	}
	
	private String buildFullLabel(SimpleTagPattern tagPattern)
	{
		String result =
			"map:" + relationName + "__" + toLabel(tagPattern) + " a d2rq:PropertyBridge;\n" +
			"\td2rq:belongsToClassMap map:" + relationName + ";\n";
		
		return result;
	}
	
	@Override
	public Void accept(SimpleClassTagMapper mapper)
	{
		String result =
			buildFullLabel(mapper.getTagPattern()) +
			"\td2rq:property rdf:type;\n" +
			"\td2rq:constantValue " + "<" + mapper.getResource() + ">;\n" +
			buildFullCondition(mapper.getTagPattern()) +
			"\t.\n";

		out.println(result);
		return null;
	}

	
	private String buildDataType(String dataType)
	{
		String result = dataType == null
			? ""
			: "\td2rq:datatype <" + dataType + ">;\n";
		
		return result;
	}

	@Override
	public Void accept(SimpleDataTypeTagMapper mapper)
	{
		String result =
			buildFullLabel(mapper.getTagPattern()) +
			"\td2rq:property <" + mapper.getResource() + ">;\n" +
		    "\td2rq:column \"" + relationName + ".v" + "\";\n" +
		    buildDataType(mapper.getDataType()) +
			buildFullCondition(mapper.getTagPattern()) +
			"\t.\n";

		out.println(result);
		return null;
	}

	
	private String buildLang(String languageTag)
	{
		String result = languageTag == null
			? ""
			: "\td2rq:lang \"" + languageTag + "\";\n";
		
		return result;
	}

	@Override
	public Void accept(SimpleTextTagMapper mapper)
	{
		mapper.getLanguageTag();
		
		String result =
			buildFullLabel(mapper.getTagPattern()) +
			"\td2rq:property <" + mapper.getResource() + ">;\n" +
		    "\td2rq:column \"" + relationName + ".v" + "\";\n" +
		    buildLang(mapper.getLanguageTag()) +
			buildFullCondition(mapper.getTagPattern()) +
			"\t.\n";

		out.println(result);
		return null;
	}

	@Override
	public Void accept(SimpleObjectPropertyTagMapper mapper)
	{
		String result =
			buildFullLabel(mapper.getTagPattern()) +
			"\td2rq:property <" + mapper.getResource() + ">;\n" +
			"\td2rq:constantValue " + "<" + mapper.getResource() + ">;\n" +
			buildFullCondition(mapper.getTagPattern()) +
			"\t.\n";

		out.println(result);
		return null;
	}
}
