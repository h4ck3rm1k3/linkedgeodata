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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.jtriplify.methods.DefaultCoercions;
import org.linkedgeodata.jtriplify.methods.FunctionUtil;
import org.linkedgeodata.jtriplify.methods.IInvocable;
import org.linkedgeodata.jtriplify.methods.JavaMethodInvocable;
import org.linkedgeodata.scripts.LineStringUpdater;
import org.linkedgeodata.util.ExceptionUtil;
import org.linkedgeodata.util.StreamUtil;
import org.linkedgeodata.util.StringUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javax.mail.internet.ContentType;

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


/*
class RDFResultFormat
{
	private String contentType;
	private String jenaFormat;

	public RDFResultFormat()
	{
	}
}
*/

class SimpleResponse
{
	private String contentType;
	private String text;
	
	public SimpleResponse(String contentType, String response)
	{
		this.contentType = contentType;
		this.text = response;
	}

	public String getContentType()
	{
		return contentType;
	}

	public String getText()
	{
		return text;
	}
}


class MyHandler
	implements HttpHandler
{
	private static final Logger logger = Logger.getLogger(JTriplifyServer.class);

	private RegexInvocationContainer ric = null;

	private static Map<ContentType, String> contentTypeToJenaFormat = new HashMap<ContentType, String>();
	private static Map<String, String> formatToJenaFormat = new HashMap<String, String>();
	
	{
		//contentTypeToJenaFormat.put("text/plain", "N3");
		//contentTypeToJenaFormat.put("text/html", "N3");

		try {
			//Values take from http://www.w3.org/2008/01/rdf-media-types
			contentTypeToJenaFormat.put(new ContentType("application/rdf+xml"), "RDF/XML");
			//text/plain -> N-TRIPLE
			contentTypeToJenaFormat.put(new ContentType("application/x-turtle"), "TURTLE");
			contentTypeToJenaFormat.put(new ContentType("text/rdf+n3"), "N3");
		}
		catch(Exception e) {
			logger.fatal(ExceptionUtil.toString(e));
			System.exit(1);
		}
		
		formatToJenaFormat.put("rdfxml", "RDF/XML");
		formatToJenaFormat.put("n3", "N3");
		formatToJenaFormat.put("nt", "N-TRIPLE");		
		formatToJenaFormat.put("turtle", "TURTLE");		
	}
	
	public void setInvocationMap(RegexInvocationContainer ric)
	{
		this.ric = ric;
	}
	
	
	public static MultiMap<String, String> getQueryMap(String query)  
	{  
	    MultiMap<String, String> result = new MultiHashMap<String, String>();  
	    if(query == null)
	    	return result;
	    
	    String[] params = query.split("&");  
	    for (String param : params)  
	    {
	    	String[] kv = param.split("=", 2);
	        String key = kv[0];  
	        String value = kv.length == 2 ? kv[1] : null;

	        result.put(key, value);
	    }  
	    return result;  
	}
	
	public static <K, V> Collection<V> safeGet(MultiMap<K, V> map, K key)
	{
		Collection<V> value = map.get(key);
		if(value == null)
			return Collections.emptyList();
		
		return value;
	}
	
	public static <T> T getFirst(Iterable<T> iterable)
	{
		if(iterable == null)
			return null;

		Iterator<T> it = iterable.iterator();
		if(!it.hasNext())
			return null;
		
		T result = it.next();
		
		return result;
	}


	
	// HTTP error 406: Not acceptable
	private SimpleResponse process(HttpExchange t)
		throws Exception
	{
	   	String request = t.getRequestURI().toString();
    	logger.info("Received request: " + request);

   
    	// Check if a particular format was requested via the query string
    	// As this excludes some of the content types that may be used
    	URI requestURI = t.getRequestURI();
    	String query = requestURI.getQuery();
    	MultiMap<String, String> params = getQueryMap(query);
 
 
    	String rawFormat = getFirst(params.get("format"));
    	rawFormat = rawFormat == null ? null : rawFormat.trim().toLowerCase();
    	String requestFormat = formatToJenaFormat.get(rawFormat);
       	if(rawFormat != null && requestFormat == null) {
    		// FIXME Respect the accept header when returning an error
       		return new SimpleResponse("text/plain", "Unsupported format");    			
    	}

    	// Content negotiation
    	Headers requestHeaders = t.getRequestHeaders();    	
    	List<String> accepts = requestHeaders.get("Accept");
    	if(accepts == null)
    		accepts = Collections.emptyList();
    	
    	logger.info("Accept header: " + accepts);
    	
    	// Find the first accept header we can use
    	String responseContentType = null;
    	String contentFormat = null;
    	
    	boolean exitLoop = false;

    	for(String accept : accepts) {
    		String[] items = accept.split(",");
    		for(String item : items) {
	    		ContentType ct = new ContentType(item);
	    		
	    		// This if statement is a hack right now
	    		if(ct.match("text/plain") || ct.match("text/html")) {
	    			contentFormat = StringUtil.coalesce(requestFormat, "N-TRIPLE");
	    			responseContentType = "text/plain";
	    			exitLoop = true;
	    			break;
	    		}
	    		
	    		for(Map.Entry<ContentType, String> entry : contentTypeToJenaFormat.entrySet()) {
		    		if(!ct.match(entry.getKey()))
		    			continue;

		    		String tmp = entry.getValue();
		    		if(tmp != null) {
		    			// If a format was specified in the query string, we also need
		    			// a compatible content type
		    			// E.g. if format=N3, but accept=rdf+xml we can't use that accept type
		    			if(requestFormat != null && !requestFormat.equalsIgnoreCase(tmp)) {
		    				continue;
		    			}
		    			
		    			contentFormat = tmp;
		    			responseContentType = item;
		    			exitLoop = true;
		    			break;
		    		}
		    		// FIXME This is ugly here
		    		if(exitLoop)
		    			break;
	    		}

	    		if(exitLoop)
	    			break;
	    	}

    		if(exitLoop)
    			break;
    	}

    	
    	if(contentFormat == null) {
    		if(requestFormat != null) {
    			return new SimpleResponse("text/plain", "No suitable format found (Maybe you used ?format=... with incompatible accept header?)");
    		}
    		else {
    			return new SimpleResponse("text/plain", "Not acceptable (This message will be changed to http code 406)");
    		}
    	}

    	
    	
    	InputStream is = t.getRequestBody();
    	StreamUtil.toString(is, false);
    	

    	Object o = ric.invoke(request);
    	
    	if(o instanceof Model) {
    		Model model = (Model)o;
    		String responseText = ModelUtil.toString(model, contentFormat);
    	
    		return new SimpleResponse(responseContentType, responseText);
    	}
    	
		return new SimpleResponse("text/plain", "Unsupported result type");   			
	}
	
	public void handle(HttpExchange x)
		throws IOException
	{
		SimpleResponse response = null;
    	try {
    		response = process(x);
    	} catch(Throwable t) {
    		response = new SimpleResponse("text/plain", ExceptionUtil.toString(t));
    		logger.error(response.getText());
    	}

    	Headers responseHeaders = x.getResponseHeaders();
    	responseHeaders.set("Content-Type", response.getContentType()); 
    	
        x.sendResponseHeaders(200, 0);//response.length());
        OutputStream os = x.getResponseBody();
        os.write(response.getText().getBytes());
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
		ric.put(".*way/([^/?]*)/?(\\?.*)?", new JavaMethodInvocable(m, methods), "$0");
		
		m = ServerMethods.class.getMethod("getNode", String.class);
		ric.put(".*node/([^/?]*)/?(\\?.*)?", new JavaMethodInvocable(m, methods), "$0");

		
		
		//m = ServerMethods.class.getMethod("getNear", String.class, String.class, String.class);
		IInvocable nearFn = DefaultCoercions.wrap(methods, "publicNear.*");
		
		ric.put(".*near/([^/]*),([^/]*)/([^/?]*)/?(\\?.*)?", nearFn, "$0", "$1", "$2", null, null, false);
		ric.put(".*near/([^/]*),([^/]*)/([^/]*)/([^/?]*)/?(\\?.*)?", nearFn, "$0", "$1", "$2", "$3", null, false);
		ric.put(".*near/([^/]*),([^/]*)/([^/]*)/([^=]*)=([^/?]*)/?(\\?.*)?", nearFn, "$0", "$1", "$2", "$3", "$4", false);
		ric.put(".*near/([^/]*),([^/]*)/([^/]*)/class/([^/?]*)/?(\\?.*)?", nearFn, "$0", "$1", "$2", "$3", "$3", true);
		
		
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
