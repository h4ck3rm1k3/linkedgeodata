/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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
