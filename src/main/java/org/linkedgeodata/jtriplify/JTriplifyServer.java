package org.linkedgeodata.jtriplify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.jtriplify.methods.DefaultCoercions;
import org.linkedgeodata.jtriplify.methods.FunctionUtil;
import org.linkedgeodata.jtriplify.methods.IInvocable;
import org.linkedgeodata.jtriplify.methods.JavaMethodInvocable;
import org.linkedgeodata.scripts.LineStringUpdater;
import org.linkedgeodata.util.ExceptionUtil;
import org.linkedgeodata.util.StreamUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class ICPair
{
	private IInvocable invocable;
	private Object[] argMap;
	
	public ICPair(IInvocable invocable, Object[] argMap)
	{
		this.invocable = invocable;
		this.argMap = argMap;
	}
	
	public IInvocable getInvocable()
	{
		return invocable;
	}
	
	public Object[] getArgMap()
	{
		return argMap;
	}
}

class RegexInvocationContainer
{
	private static final Logger logger = Logger.getLogger(RegexInvocationContainer.class);

	private Map<Pattern, ICPair> patternToInvocable = new HashMap<Pattern, ICPair>();
	
	public void put(String regex, IInvocable invocable, Object ...argMap)
	{
		Pattern pattern = Pattern.compile(regex);
		
		patternToInvocable.put(pattern, new ICPair(invocable, argMap));
	}
	
	private static Object invoke(Matcher matcher, IInvocable invocable, Object[] argMap)
		throws Exception
	{
		logger.info("Invoking: " + invocable);
		int groupCount = matcher.groupCount();
		Object[] matches = new Object[groupCount];
		
		for(int i = 0; i < groupCount; ++i) {
			matches[i] = matcher.group(i + 1);
		}
		
		Object[] args = new Object[argMap.length];
		for(int i = 0; i < argMap.length; ++i) {
			
			if(argMap[i] != null) {
				String argStr = argMap[i].toString();
				
				if(argStr.startsWith("$")) {
					String indexStr = argStr.substring(1);
					int index = Integer.parseInt(indexStr);
					
					args[i] = matches[index];
					continue;
				}
			}
		
			args[i] = argMap[i];
		}

		logger.debug("Args: " + Arrays.toString(args) + ", Types: " + Arrays.toString(FunctionUtil.getTypes(args)));
		
		Object result = invocable.invoke(args);
		return result;
	}
	
	public Object invoke(String arg)
		throws Exception
	{
		for(Map.Entry<Pattern, ICPair> entry : patternToInvocable.entrySet()) {
			Pattern pattern = entry.getKey();
			ICPair icPair = entry.getValue();
			
			Matcher matcher = pattern.matcher(arg);
			if(matcher.matches()) {
				logger.info("Value '" + arg + "' matched the pattern '" + pattern + "'");

				return invoke(matcher, icPair.getInvocable(), icPair.getArgMap());
			}
		}
		
		logger.info("No match found for '" + arg + "'");
		
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
    		logger.error(result);
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
	
	private static void initCLIOptions()
	{
		cliOptions.addOption("p", "port", true, "Server port");
		cliOptions.addOption("c", "context", true, "Context e.g. /triplify/");
		cliOptions.addOption("b", "backlog", true, "Maximum number of connections");

		cliOptions.addOption("t", "type", true, "Database type (posgres, mysql,...)");
		cliOptions.addOption("d", "database", true, "Database name");
		cliOptions.addOption("u", "user", true, "");
		cliOptions.addOption("w", "password", true, "");
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

		initCLIOptions();
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
		String passWord = commandLine.getOptionValue("w", "lgd");

		String batchSizeStr = commandLine.getOptionValue("n", "1000");
		int batchSize = Integer.parseInt(batchSizeStr);
		
		// Validation
		if(batchSize <= 0)
			throw new RuntimeException("Invalid argument for batchsize");
		
		// Setup
		logger.info("Loading uri namespaces");
		String fileName = "NamespaceResolv.ini";
		File file = new File(fileName);
		if(!file.exists()) {
			throw new FileNotFoundException(fileName);
		}
		
		Transformer<String, URI> uriResolver = new URIResolver(file);

		
		logger.info("Connecting to db");
		Connection conn = LineStringUpdater.connectPostGIS(hostName, dbName, userName, passWord);

		
		LinkedGeoDataDAO dao = new LinkedGeoDataDAO(uriResolver);
		dao.setConnection(conn);
		
		ServerMethods methods = new ServerMethods(dao);
		
		
		RegexInvocationContainer ric = new RegexInvocationContainer();
		
		Method m;
		
		m = ServerMethods.class.getMethod("getWay", String.class);
		ric.put(".*way/([^/]*)", new JavaMethodInvocable(m, methods), "$0");
		
		m = ServerMethods.class.getMethod("getNode", String.class);
		ric.put(".*node/([^/]*)", new JavaMethodInvocable(m, methods), "$0");

		
		
		//m = ServerMethods.class.getMethod("getNear", String.class, String.class, String.class);
		IInvocable nearFn = DefaultCoercions.wrap(methods, "publicNear.*");
		
		ric.put(".*near/([^/]*),([^/]*)/([^/]*)/?", nearFn, "$0", "$1", "$2", null, null, false);
		ric.put(".*near/([^/]*),([^/]*)/([^/]*)/([^/]*)/?", nearFn, "$0", "$1", "$2", "$3", null, false);
		ric.put(".*near/([^/]*),([^/]*)/([^/]*)/([^=]*)=([^/]*)/?", nearFn, "$0", "$1", "$2", "$3", "$4", false);
		ric.put(".*near/([^/]*),([^/]*)/([^/]*)/class/([^/]*)/?", nearFn, "$0", "$1", "$2", "$3", "$3", true);
		
		
		MyHandler handler = new MyHandler();
		handler.setInvocationMap(ric);
		
		
		// Start
		runServer(context, port, backLog, handler);
	}


	private static void runServer(String context, int port, int backLog, HttpHandler handler)
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
