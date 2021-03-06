package org.linkedgeodata.osm.mapping;


import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.osm.mapping.impl.IOneOneTagMapperVisitor;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.RegexTextTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleDataTypeTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTextTagMapper;
import org.linkedgeodata.tagmapping.client.entity.SimpleDataTypeTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleObjectPropertyTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleTagPattern;
import org.linkedgeodata.tagmapping.client.entity.SimpleTextTagMapperState;



// FIXME The functionality of this class should be part of the TagMapperDAO
public class MappingRulesConverter
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
	
		Session session = TagMappingDB.getSession();
		MappingRulesConverter converter = new MappingRulesConverter(session);
		Transaction tx = session.beginTransaction();
		
		for(IOneOneTagMapper item : mapper.getAllMappers()) {
			if(item instanceof ISimpleOneOneTagMapper) {
				ISimpleOneOneTagMapper x = (ISimpleOneOneTagMapper)item;
				
				x.accept(converter);
			}
		}	
		
		
		tx.commit();
		
	}
	
	
	public MappingRulesConverter(Session session)
		throws Exception
	{
		this.session = session;		
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
		Object entity = new SimpleObjectPropertyTagMapperState(mapper.getProperty(), mapper.getObject(), mapper.isObjectAsPrefix(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.describesOSMEntity());
		session.persist(entity);

		return null;
	}

	@Override
	public Void accept(RegexTextTagMapper mapper)
	{
		throw new NotImplementedException();
	}
}
