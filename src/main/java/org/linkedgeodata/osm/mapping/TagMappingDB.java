package org.linkedgeodata.osm.mapping;


import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;



/**
 *
 * @author raven_arkadon
 */
public class TagMappingDB
{
	private static final Logger logger = Logger.getLogger(TagMappingDB.class);
	
	private static SessionFactory sessionFactory = null;
	private static AnnotationConfiguration cfg = null;
	
	//private static String filename;
	
	public static Session getSession()
	{
		return getSessionFactory().getCurrentSession();
	}
	
    public static SessionFactory getSessionFactory()
    {
    	if(sessionFactory == null) {
    		init("TagMappingDB.postgres.cfg.xml");
    	}
    	
        return sessionFactory;
    }

   
    public static void init(String resourceName)
    {
    	cfg = new AnnotationConfiguration();

    	URL url = ClassLoader.getSystemResource(resourceName);
    	logger.info("Attempting to loading hibernate config for TagMappingDB from: " + url);

    	if(url != null) {
    		cfg.configure(url);
    	}
    	else {
    		File file = new File(resourceName);
    		
        	logger.info("Attempting to load hibernate config for TagMappingDB from: " + file.getAbsolutePath());
        	
    		cfg.configure(file);
    	}

     	reset();
    }
   
    
    public static void reset()
    {
    	if(sessionFactory != null)
    		sessionFactory.close();

		sessionFactory = cfg.buildSessionFactory();    
    }
}
