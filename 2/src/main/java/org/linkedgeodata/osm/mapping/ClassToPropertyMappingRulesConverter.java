package org.linkedgeodata.osm.mapping;


import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.linkedgeodata.osm.mapping.impl.IOneOneTagMapperVisitor;
import org.linkedgeodata.osm.mapping.impl.RegexTextTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleClassTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleDataTypeTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTextTagMapper;
import org.linkedgeodata.tagmapping.client.entity.SimpleDataTypeTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleObjectPropertyTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleTagPattern;
import org.linkedgeodata.tagmapping.client.entity.SimpleTextTagMapperState;

import com.hp.hpl.jena.vocabulary.RDF;




public class ClassToPropertyMappingRulesConverter
	implements IOneOneTagMapperVisitor<Void>
{
	private Session session; 

	protected static final Options cliOptions = new Options();
	
	private static void initCLIOptions()
	{
		cliOptions.addOption("f", "filename", true, "LGD-Mapping file name");
	}
	
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
		
		initCLIOptions();
		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);

		String fileName = commandLine.getOptionValue("f");
		
		File file = new File(fileName);
		
		InMemoryTagMapper mapper = new InMemoryTagMapper();
		mapper.load(file);

		
		InMemoryTagMapper output = new InMemoryTagMapper();
		
		for(IOneOneTagMapper item : mapper.getAllMappers()) {
			if(item instanceof SimpleClassTagMapper) {
				SimpleClassTagMapper m  = (SimpleClassTagMapper)item;
				
				SimpleObjectPropertyTagMapper x = new SimpleObjectPropertyTagMapper(RDF.type.toString(), m.getProperty(), false, m.getTagPattern(), m.describesOSMEntity());
			
				output.add(x);
			}
			else {
				output.add(item);
			}
			
		}
		
		output.save(new File("data/triplify/config/LGDMappingRules.3.0.xml.new"));


		
	}
	
	
	
	/*
	@Override
	public Void accept(SimpleClassTagMapper mapper)
	{
		Object entity = new SimpleClassTagMapperState(mapper.getProperty(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.describesOSMEntity());
		session.persist(entity);
		
		return null;
	}*/


	@Override
	public Void accept(SimpleDataTypeTagMapper mapper)
	{
		Object entity = new SimpleDataTypeTagMapperState(mapper.getProperty(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.getDataType(), mapper.describesOSMEntity());
		session.persist(entity);

		return null;
	}

	@Override
	public Void accept(SimpleTextTagMapper mapper)
	{
		Object entity = new SimpleTextTagMapperState(mapper.getProperty(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.getLanguageTag(), mapper.describesOSMEntity());
		session.persist(entity);

		return null;
	}

	@Override
	public Void accept(SimpleObjectPropertyTagMapper mapper)
	{
		Object entity = new SimpleObjectPropertyTagMapperState(mapper.getProperty(), mapper.getProperty(), mapper.isObjectAsPrefix(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.describesOSMEntity());
		session.persist(entity);

		return null;
	}

	@Override
	public Void accept(RegexTextTagMapper mapper)
	{
		throw new NotImplementedException();
	}
}
