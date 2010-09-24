import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.dao.TagMapperDAO;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.MappingRulesConverter;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.TagMappingsToEntity;
import org.linkedgeodata.tagmapping.client.entity.AbstractTagMapperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoadTagMappingDb
{
	private static final Logger logger = LoggerFactory.getLogger(DumpTagMappingDb.class);
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

		String fileName = commandLine.getOptionValue("f", "TagMapping.xml");
		File file = new File(fileName);
		
		if(!file.exists()) {
			logger.error("File " + file.getAbsolutePath() + " does not exist.");
			return;
		}
		
		InMemoryTagMapper source = new InMemoryTagMapper();
		source.load(file);
		
		Set<IOneOneTagMapper> newOnes = new HashSet<IOneOneTagMapper>(source.getAllMappers());
		
		
		TagMapperDAO dao = new TagMapperDAO();
		Session session = TagMappingDB.getSession(); 
		Transaction tx = session.beginTransaction();
		dao.setSession(session);
		Set<IOneOneTagMapper> tmp = new HashSet<IOneOneTagMapper>(dao.getAllMappers());
		tx.commit();
				
		
		newOnes.removeAll(tmp);
		
		TagMappingsToEntity converter = new TagMappingsToEntity();
		
		session = TagMappingDB.getSession(); 
		tx = session.beginTransaction();
		dao.setSession(session);
		for(IOneOneTagMapper item : newOnes) {
			AbstractTagMapperState state = item.accept(converter);
			
			session.persist(state);
		}
		tx.commit();
		
		
	}

}
