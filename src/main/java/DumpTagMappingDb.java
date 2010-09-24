import java.io.File;
import java.util.List;

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
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DumpTagMappingDb
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
		
		if(file.exists()) {
			logger.error("File already exists. Please remove it first");
			return;
		}
		
		TagMapperDAO dao = new TagMapperDAO();
		Session session = TagMappingDB.getSession(); 
		Transaction tx = session.beginTransaction();
		dao.setSession(session);
		List<IOneOneTagMapper> mappers = dao.getAllMappers();
		tx.commit();
		
		
		InMemoryTagMapper mapper = new InMemoryTagMapper();
		
		for(IOneOneTagMapper item : mappers) {
			mapper.add(item);
		}
		
		mapper.save(file);
	}
		
}
