package org.linkedgeodata.jtriplify;

import java.sql.Connection;

import org.apache.log4j.Logger;
import org.linkedgeodata.util.ConnectionConfig;
import org.linkedgeodata.util.PostGISUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class MyBeanFactory
{
	private static Logger logger = Logger.getLogger(MyBeanFactory.class);
	
    private static MyBeanFactory singleton = null;
	
    private ApplicationContext context;

    private static Connection dataBaseConnection = null;
    
    private MyBeanFactory()
    {
    	this.context = new ClassPathXmlApplicationContext("ApplicationContext.xml");
    }
    
    public static MyBeanFactory getSingleton()
    {
    	if(singleton == null)
    		singleton = new MyBeanFactory();

    	return singleton;
    }
    
    public Connection getDataBaseConnection()
		throws Exception
    {
    	return getDataBaseConnection(false);
    }
    
    public Connection getDataBaseConnection(boolean reset)
    	throws Exception
    {
    	logger.info("Connecting to database");
    	
    	if(dataBaseConnection == null || reset == true) {
    		if(dataBaseConnection != null)
    			dataBaseConnection.close();

	    	ConnectionConfig config = (ConnectionConfig)context.getBean("lgdDataBase");

	    	Connection conn = PostGISUtil.connectPostGIS(
	    			config.getHostName(),
	    			config.getDataBaseName(),
	    			config.getUserName(),
	    			config.getPassWord());
	    	
	    	dataBaseConnection = conn;
    	}
    	
    	return dataBaseConnection;
    }
    
    
    @SuppressWarnings("unchecked")
	public <T> T get(String beanName, Class<T> clazz)
    {
    	return (T)context.getBean("triplifyCSSFile");
    }
    
    
}
