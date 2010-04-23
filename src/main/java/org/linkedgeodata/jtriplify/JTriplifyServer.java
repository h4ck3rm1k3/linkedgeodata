package org.linkedgeodata.jtriplify;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.scripts.LineStringUpdater;
import org.linkedgeodata.util.ExceptionUtil;
import org.linkedgeodata.util.StreamUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;



class RegexInvocationContainer
{
	private Map<Pattern, IInvocable> patternToInvocable = new HashMap<Pattern, IInvocable>();
	
	public void put(String regex, IInvocable invocable)
	{
		Pattern pattern = Pattern.compile(regex);

		patternToInvocable.put(pattern, invocable);
	}
	
	private static Object invoke(Matcher matcher, String arg, IInvocable invocable)
		throws Exception
	{
		System.out.println("invoking");
		int groupCount = matcher.groupCount();
		Object[] args = new Object[groupCount];
		
		for(int i = 0; i < groupCount; ++i) {
			args[i] = matcher.group(i + 1);
		}
		
		Object result = invocable.invoke(args);
		return result;
	}
	
	public Object invoke(String arg)
		throws Exception
	{
		for(Map.Entry<Pattern, IInvocable> entry : patternToInvocable.entrySet()) {
			Pattern pattern = entry.getKey();
			IInvocable invocable = entry.getValue();
			
			Matcher matcher = pattern.matcher(arg);
			if(matcher.matches()) {
				return invoke(matcher, arg, invocable);
			}
		}
		
		return null;
	}
}



class MyHandler
	implements HttpHandler
{
	private static final Logger logger = Logger.getLogger(JTriplifyServer.class);

	private RegexInvocationContainer ric = null;
	
	public void setInvocationMap(RegexInvocationContainer ric)
	{
		this.ric = ric;
	}
	
	public void handle(HttpExchange t)
		throws IOException
	{
    	InputStream is = t.getRequestBody();
    	StreamUtil.toString(is);
    	
    	String request = t.getRequestURI().toString();
    	
    	logger.info("Received request: " + request);
    	
    	Object result = null;
    	
    	try {
    		result = ric.invoke(request);
    	}
    	catch(Throwable x) {
    		result = ExceptionUtil.toString(x);
    	}

    	String response = result == null
    		? "No such method"
    		: result.toString()
    		;  	
    	
    	/*
    	String str = StreamUtil.toString(is);
    	System.out.println(str);        
        String response = "This is the response: " + str;
    
        response += 	t.getAttribute("attr");
        response += t.getRequestURI();
		*/
        
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}


public class JTriplifyServer
{
	private static final Logger logger = Logger.getLogger(JTriplifyServer.class);
	
	protected static final Options cliOptions = new Options();
	
	{
		cliOptions.addOption("p", "port", true, "Server port");
		cliOptions.addOption("c", "context", true, "Context e.g. /triplify/");
		cliOptions.addOption("b", "backlog", true, "Maximum number of connections");

		cliOptions.addOption("t", "type", true, "Database type (posgres, mysql,...)");
		cliOptions.addOption("d", "database", true, "Database name");
		cliOptions.addOption("u", "user", true, "");
		cliOptions.addOption("p", "password", true, "");
		cliOptions.addOption("h", "host", true, "");

		cliOptions.addOption("n", "batchSize", true, "Batch size");
	}

	public static String test(String a, String b)
	{
		System.out.println("In test");
		return "Hello " + a + " and " + b + "!";
	}

	/*
	public static void main(String[] args)
		throws Exception
	{
		String regex = ".*test/([^/]*)/(.*)";
		Pattern pattern = Pattern.compile(regex);
		
		Matcher m = pattern.matcher("hi/test/a/b");
		m.matches();
		
		System.out.println(m.group());
		for(int i = 0; i < m.groupCount(); ++i) {
			System.out.println(m.group(1 + i));
		}
	}
	*/
	/*************************************************************************/
	/* Init                                                                  */
	/*************************************************************************/	

	
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");

		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);

		// Parsing of command line args
		String portStr    = commandLine.getOptionValue("p", "7000");
		String backLogStr = commandLine.getOptionValue("b", "100");
		String context = commandLine.getOptionValue("c", "/triplify");
		int port = Integer.parseInt(portStr);
		int backLog = Integer.parseInt(backLogStr);
		
		String hostName = commandLine.getOptionValue("h", "localhost");
		String dbName   = commandLine.getOptionValue("d", "lgd");
		String userName = commandLine.getOptionValue("u", "lgd");
		String passWord = commandLine.getOptionValue("p", "lgd");

		String batchSizeStr = commandLine.getOptionValue("n", "1000");
		int batchSize = Integer.parseInt(batchSizeStr);
		
		// Validation
		if(batchSize <= 0)
			throw new RuntimeException("Invalid argument for batchsize");
		
		// Setup
		logger.info("Connecting to db");
		Connection conn = LineStringUpdater.connectPostGIS(hostName, dbName, userName, passWord);

		RegexInvocationContainer ric = new RegexInvocationContainer();
		
		
		Method m = JTriplifyServer.class.getMethod("test", String.class, String.class);
		ric.put(".*test/([^/]*)/(.*)", new JavaMethodInvocable(m, null));
		
		
		MyHandler handler = new MyHandler();
		handler.setInvocationMap(ric);
		
		
		// Start
		runServer(context, port, backLog, handler);
	}
		
	public static void runServer(String context, int port, int backLog, HttpHandler handler)
		throws IOException
	{
		logger.info("Starting JTriplify Server");
		
		InetSocketAddress socketAddress = new InetSocketAddress(port);
		HttpServer server = HttpServer.create(socketAddress, backLog);
		
		server.createContext(context, handler);
		server.setExecutor(null);
		server.start();
	}
}
