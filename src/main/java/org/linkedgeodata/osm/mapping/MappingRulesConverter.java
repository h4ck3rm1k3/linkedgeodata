package org.linkedgeodata.osm.mapping;


import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapperVisitor;
import org.linkedgeodata.osm.mapping.impl.SimpleClassTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleDataTypeTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTextTagMapper;
import org.linkedgeodata.tagmapping.client.entity.SimpleClassTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleDataTypeTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleObjectPropertyTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleTagPattern;
import org.linkedgeodata.tagmapping.client.entity.SimpleTextTagMapperState;




public class MappingRulesConverter
	implements ISimpleOneOneTagMapperVisitor<Void>
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
		initCLIOptions();
		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);

		String fileName = commandLine.getOptionValue("f");
		
		File file = new File(fileName);
		
		TagMapper mapper = new TagMapper();
		mapper.load(file);
	
		Session session = TagMappingDB.getSession();
		MappingRulesConverter converter = new MappingRulesConverter(session);
		Transaction tx = session.beginTransaction();
		
		for(IOneOneTagMapper item : mapper.asList()) {
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

	
	
	@Override
	public Void accept(SimpleClassTagMapper mapper)
	{
		Object entity = new SimpleClassTagMapperState(mapper.getResource(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.describesOSMEntity());
		session.persist(entity);
		
		return null;
	}

	@Override
	public Void accept(SimpleDataTypeTagMapper mapper)
	{
		Object entity = new SimpleDataTypeTagMapperState(mapper.getResource(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.getDataType(), mapper.describesOSMEntity());
		session.persist(entity);

		return null;
	}

	@Override
	public Void accept(SimpleTextTagMapper mapper)
	{
		Object entity = new SimpleTextTagMapperState(mapper.getResource(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.getLanguageTag(), mapper.describesOSMEntity());
		session.persist(entity);

		return null;
	}

	@Override
	public Void accept(SimpleObjectPropertyTagMapper mapper)
	{
		Object entity = new SimpleObjectPropertyTagMapperState(mapper.getResource(), mapper.getObject(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.describesOSMEntity());
		session.persist(entity);

		return null;
	}
}
