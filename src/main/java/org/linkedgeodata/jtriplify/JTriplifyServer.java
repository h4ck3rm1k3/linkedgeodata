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
package org.linkedgeodata.jtriplify;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.UnsupportedDataTypeException;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.dao.LGDDAO;
import org.linkedgeodata.dao.LGDRDFDAO;
import org.linkedgeodata.jtriplify.methods.DefaultCoercions;
import org.linkedgeodata.jtriplify.methods.FunctionUtil;
import org.linkedgeodata.jtriplify.methods.IInvocable;
import org.linkedgeodata.jtriplify.methods.JavaMethodInvocable;
import org.linkedgeodata.jtriplify.methods.Pair;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.util.ExceptionUtil;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.StreamUtil;
import org.linkedgeodata.util.URIUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


interface IMyHandler
{
	/**
	 * 
	 * @param x
	 * @return true if accepted, false otherwise
	 */
	boolean handle(HttpExchange x)
		throws Exception;
}


class MyHttpHandler
	implements HttpHandler
{
	private static final Logger logger = Logger.getLogger(MyHttpHandler.class);
	private List<IMyHandler> subHandlers = new ArrayList<IMyHandler>();
	
	public List<IMyHandler> getSubHandlers()
	{
		return subHandlers;
	}

	@Override
	public void handle(HttpExchange x)
	{
		try {
			_handle(x);
		} catch(Throwable t) {
			logger.error(ExceptionUtil.toString(t));
		}
	}


	public void _handle(HttpExchange x)
		throws Exception
	{
		for(IMyHandler item : subHandlers) {
			if(item.handle(x)) {
				return;
			}
		}
		
		MyHandler.sendResponse(x, 500, null, null);
	}
}



class Model2Handler
	implements IMyHandler
{
	private static final Logger logger = Logger.getLogger(Model2Handler.class);
	
	private RegexInvocationContainer ric = new RegexInvocationContainer();
	
	/*
	public Model2Handler(RegexInvocationContainer ric)
	{
		this.ric = ric;
	}
	*/
	
	public RegexInvocationContainer getRIC()
	{
		return ric;
	}
	
	@Override
	public boolean handle(HttpExchange x)
	{
		try {
			return _handle(x);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private boolean _handle(HttpExchange x)
		throws Exception
	{
		// Check if the content negotiation is ok
		Map<String, ContentType> accepts = MyHandler.getPreferredFormats(x.getRequestHeaders());
		String requestedFormat = MyHandler.getJenaFormatByExtension(x.getRequestURI());

		Map.Entry<String, ContentType> resultType = MyHandler.getContentType(requestedFormat, accepts);
		
		Model model = null;
		try {
			model = (Model)ric.invoke(x.getRequestURI().toString());
		}
		catch(Throwable t) {
			logger.error(ExceptionUtil.toString(t));
		}
		
		if(model == null)
			return false;

		String body = ModelUtil.toString(model, resultType.getKey());

		if(resultType.getValue().match("text/html")) {
			body = StringEscapeUtils.escapeHtml(body);
			body = body.replace("\n", "<br />");
		}
		
		if(resultType != null) {
			MyHandler.sendResponse(x, 200, resultType.getValue().toString(), body);
			return true;
		}
		
		return false;
	}
}


class Model3Handler
	implements IMyHandler
{
	private static final Logger logger = Logger.getLogger(Model2Handler.class);
	
	private RegexInvocationContainer ric = new RegexInvocationContainer();
	
	/*
	public Model3Handler(RegexInvocationContainer ric)
	{
		this.ric = ric;
	}*/
	
	public RegexInvocationContainer getRIC()
	{
		return ric;
	}

	
	@Override
	public boolean handle(HttpExchange x)
	{
		try {
			return _handle(x);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private boolean _handle(HttpExchange x)
		throws Exception
	{
		// Check if the content negotiation is ok
		Map<String, ContentType> accepts = MyHandler.getPreferredFormats(x.getRequestHeaders());
		String requestedFormat = MyHandler.getJenaFormatByQueryString(x.getRequestURI());
	
		Map.Entry<String, ContentType> resultType = MyHandler.getContentType(requestedFormat, accepts);
		
		Model model = null;
		try {
			model = (Model)ric.invoke(x.getRequestURI().toString());
		}
		catch(Throwable t) {
			logger.error(ExceptionUtil.toString(t));
		}
		
		if(model == null) {
			return false;
		}
	
		String body = ModelUtil.toString(model, resultType.getKey());
	
		if(resultType.getValue().match("text/html"))
			body = StringEscapeUtils.escapeHtml(body);

		if(resultType != null) {
			MyHandler.sendResponse(x, 200, resultType.getValue().toString(), body);
			return true;
		}
		
		return false;
	}
}




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



class RedirectInvocable
	implements IInvocable
{
	private String pattern;
	
	public RedirectInvocable(String pattern)
	{
		this.pattern = pattern;
	}
	
	@Override
	public String invoke(Object... args)
		throws Exception
	{
		String url = pattern;
		for(int i = 0; i < args.length; ++i) {
			url = url.replace("$" + i, args[i] == null ? "" : args[i].toString());
		}
		
		return url;
	}
}


class LinkedDataRedirectHandler
	implements IMyHandler
{
	private RegexInvocationContainer pageRIC = new RegexInvocationContainer();
	private RegexInvocationContainer dataRIC = new RegexInvocationContainer();
	
	
	public RegexInvocationContainer getPageRIC()
	{
		return pageRIC;
	}
	
	public RegexInvocationContainer getDataRIC()
	{
		return dataRIC;
	}
	
	/*
	public LinkedDataRedirectHandler(RegexInvocationContainer pageRic, RegexInvocationContainer dataRic)
	{
		this.pageRic = pageRic;
		this.dataRic = dataRic;
	}
	*/
	
	@Override
	public boolean handle(HttpExchange x)
		throws Exception
	{
		Map<String, ContentType> accepts = MyHandler.getPreferredFormats(x.getRequestHeaders());
		
		if(accepts.isEmpty())
			return false;
		
		//Map.Entry<String, ContentType> type = accepts.entrySet().iterator().next();
		
		RegexInvocationContainer ric = dataRIC;
		if(accepts.containsValue(new ContentType("text/html"))) {
			ric = pageRIC;
		}

		String targetURL = (String)ric.invoke(x.getRequestURI().toString());
		if(targetURL == null) {
			return false;
		}

		MyHandler.sendRedirect(x, targetURL);
		return true;
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
	private int statusCode;
	private Map<String, List<String>> header = new HashMap<String, List<String>>();
	private String contentType;
	private String body;

	public SimpleResponse()
	{
	}

	public SimpleResponse(int statusCode)
	{
		this.statusCode = statusCode;
	}
	
	public static SimpleResponse redirect(String url)
	{
		SimpleResponse result = new SimpleResponse(303);
		result.getHeader().put("Location", Collections.singletonList(url));
		return result;
	}
	
	public SimpleResponse(int statusCode, String contentType, String body)
	{
		this.statusCode = statusCode;
		this.header.put("Content-Type", Collections.singletonList(contentType));
		this.body = body;
	}
	
	public int getStatusCode()
	{
		return statusCode;
	}

	public String getContentType()
	{
		return contentType;
	}

	public String getBody()
	{
		return body;
	}
	
	public Map<String, List<String>> getHeader()
	{
		return header;
	}
}

class HTTPErrorException
	extends Exception
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private int errorCode;
	
	public HTTPErrorException(int errorCode)
	{
		this.errorCode = errorCode;
	}
	
	public int getErrorCode()
	{
		return errorCode;
	}
}




class MyHandler
//	implements HttpHandler
{
	private static final Logger logger = Logger.getLogger(JTriplifyServer.class);

	private RegexInvocationContainer ric = null;

	private static Map<ContentType, String> contentTypeToJenaFormat = new HashMap<ContentType, String>();
	private static Map<String, String> formatToJenaFormat = new HashMap<String, String>();
	private static Map<String, String> extensionToJenaFormat = new HashMap<String, String>();
	
	{
		//contentTypeToJenaFormat.put("text/plain", "N3");
		//contentTypeToJenaFormat.put("text/html", "N3");

		try {
			//Values take from http://www.w3.org/2008/01/rdf-media-types
			contentTypeToJenaFormat.put(new ContentType("application/rdf+xml"), "RDF/XML");
			//text/plain -> N-TRIPLE
			contentTypeToJenaFormat.put(new ContentType("application/x-turtle"), "TURTLE");
			contentTypeToJenaFormat.put(new ContentType("text/turtle"), "TURTLE");
			
			contentTypeToJenaFormat.put(new ContentType("text/n3"), "N3");
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

		
		extensionToJenaFormat.put("rdf", "RDF/XML");
		extensionToJenaFormat.put("n3", "N3");
		extensionToJenaFormat.put("nt", "N-TRIPLE");
		extensionToJenaFormat.put("ttl", "TURTLE");
	}
	
	public void setInvocationMap(RegexInvocationContainer ric)
	{
		this.ric = ric;
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
	
	
	/**
	 * The question is whether a specifically requested format is conforming to the
	 * formats of the content type - therefore: This method returns a list
	 * of RDF formats in 
	 * @throws ParseException 
	 * 
	 * 
	 **/

	
	/**
	 * .rdf, .n3, .ttl, .nt 
	 *
	 */
	public static String getExtension(String str)
	{
		int index = str.lastIndexOf('.');
		if(index == -1)
			return null;
		
		return str.substring(index + 1);
	}
	
	
	public static String getFormatFromExtension(String ext)
	{
		if(ext == null)
			return null;
		
		System.out.println(extensionToJenaFormat);
		System.out.println(formatToJenaFormat);
		String result = extensionToJenaFormat.get(ext);
		return result;
	}
	
	public static Map<String, ContentType> getPreferredFormats(Headers requestHeaders)
		throws ParseException
	{
    	// Content negotiation
    	List<String> accepts = requestHeaders.get("Accept");
    	if(accepts == null)
    		accepts = Collections.emptyList();
    	
    	logger.info("Accept header: " + accepts);

    	Map<String, ContentType> result = new HashMap<String, ContentType>();
    	int acceptCounter = 0;
    	for(String accept : accepts) {
    		String[] items = accept.split(",");
    		for(String item : items) {
    			++acceptCounter;
    			
    			ContentType ct = null;
				ct = new ContentType(item);
    			
	    		// FIXME Would be nice if this was configurable
	    		if(ct.match("text/plain") || ct.match("text/html") || ct.match("*/*")) {
	    			if(!result.containsKey("N-TRIPLE")) {
	    				result.put("N-TRIPLE", ct);
	    			}
	    			
	    			//responseContentType = "text/plain; charset=UTF-8";
	    		}
	    		
	    		for(Map.Entry<ContentType, String> entry : contentTypeToJenaFormat.entrySet()) {
		    		if(!ct.match(entry.getKey()))
		    			continue;

		    		String tmp = entry.getValue();
		    		if(tmp != null) {
		    			// If a format was specified in the query string, we also need
		    			// a compatible content type
		    			// E.g. if format=N3, but accept=rdf+xml we can't use that accept type
		    			if(!result.containsKey(tmp)) {
		    				result.put(tmp, ct);
		    			}
		    		}
	    		}
	    	}
    	}
    	
    	return result;
	}
	
	
	

	/*
	return null;
	if(acceptCounter == 0)
	
	if(acceptCounter == 0) {
		logger.info("No accept header. Defaulting to 'text/plain'");
		contentFormat = StringUtil.coalesce(requestFormat, "N-TRIPLE");
		responseContentType = "text/plain";   		
	}*/	

	
	
	public static Map.Entry<String, ContentType> getContentType(String requestedFormat, Map<String, ContentType> accepts)
		throws ParseException
	{
	   	if(requestedFormat == null) {
    		if(accepts.isEmpty()) {
    			return new Pair<String, ContentType>("N-TRIPLE", new ContentType("text/plain; charset=utf-8"));
    		}
    		else {
        		return accepts.entrySet().iterator().next();
    		}
    	}
		else if(!accepts.containsKey(requestedFormat)) {
			
			//return null;
			//return new SimpleResponse(406, "text/plain", "Requested " + requestedFormat + " but accept-header " + formats + " is not compatible.");    			
		//}
    		return new Pair<String, ContentType>(requestedFormat, new ContentType("text/plain; charset=utf-8"));
		}
    	else {
    		return new Pair<String, ContentType>(requestedFormat, new ContentType("text/plain; charset=utf-8"));
    	}
	}
	
	
	public static SimpleResponse respondModel(Model model, Map.Entry<String, ContentType> contentType)
	{
		String response = ModelUtil.toString(model, contentType.getKey());
		SimpleResponse result = new SimpleResponse(200, contentType.getValue().toString(), response);
		return result;
	}
	
	
	
	private SimpleResponse process(HttpExchange t)
		throws Exception
	{
	   	String request = t.getRequestURI().toString();

    	Object o = ric.invoke(request);
		
    	if(o instanceof SimpleResponse)
    		return (SimpleResponse)o;
    	
    	throw new UnsupportedDataTypeException();
	}
	
	
	public static String getJenaFormatByExtension(URI uri)
	{
		// FIXME not correct
		String host = uri.toString();
		String ext = getExtension(host);
		String result = getFormatFromExtension(ext);
		
		return result;
	}
	
	
	public static String getJenaFormatByQueryString(URI uri)
	{
       	// Check if a particular format was requested via the query string
    	// As this excludes some of the content types that may be used
    	String query = uri.getQuery();
    	MultiMap<String, String> params = URIUtil.getQueryMap(query);
 
 
    	String rawFormat = getFirst(params.get("format"));
    	rawFormat = rawFormat == null ? null : rawFormat.trim().toLowerCase();
    	String requestFormat = formatToJenaFormat.get(rawFormat);
       	if(rawFormat != null && requestFormat == null) {
    		// FIXME Respect the accept header when returning an error
       		//return new SimpleResponse(400, "text/plain", "Unsupported format");
       		return null;
    	}
		
       	
       	return requestFormat;
	}
	
	public static void sendRedirect(HttpExchange x, String targetURL)
		throws IOException
	{
		x.getResponseHeaders().set("Location", targetURL);
		x.sendResponseHeaders(303, -1);
	}
	
	
	public static void sendResponse(HttpExchange x, int statusCode, String contentType, String body)
		throws IOException
	{
		if(contentType == null)
			contentType = "text/plain";
		
		
		int responseLength = 0;
		if(body == null)
			responseLength = -1;
		
		x.getResponseHeaders().set("Content-Type", contentType);
		
		x.sendResponseHeaders(statusCode, responseLength);
        OutputStream os = x.getResponseBody();
    	
    	if(responseLength != -1)
        	os.write(body.getBytes());
        os.close();		
	}
	
	/*
	public void handle(HttpExchange x)
		throws IOException
	{
		SimpleResponse response = null;
    	try {
    		response = process(x);
    	}
    	catch(Throwable t) {
    		logger.error(ExceptionUtil.toString(t));
    		response = new SimpleResponse(500, null, null);
    	}

    	if(response == null) {
    		response = new SimpleResponse(500, null, null);
    		logger.error("No response object was created.");
    	}
    	
    	Headers responseHeaders = x.getResponseHeaders();
  
    	responseHeaders.putAll(response.getHeader());
        OutputStream os = x.getResponseBody();
        
    	int responseLength = 0;
    	if(response.getBody() == null)
    		responseLength = -1;

        x.sendResponseHeaders(response.getStatusCode(), -1);
    	
    	if(responseLength != -1)
        	os.write(response.getBody().getBytes());
        os.close();
    }
    */
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
		
		/*
		String fileName = "NamespaceResolv.ini";
		File file = new File(fileName);
		if(!file.exists()) {
			throw new FileNotFoundException(fileName);
		}
		
		Transformer<String, URI> uriResolver = new URIResolver(file);
*/
		new MyHandler();
		
		
		logger.info("Connecting to db");
		Connection conn = PostGISUtil.connectPostGIS(hostName, dbName, userName, passWord);

		MyHttpHandler myHandler = new MyHttpHandler(); 

		
		
		RegexInvocationContainer ric = new RegexInvocationContainer();
		
		//initLegacy(myHandler, conn);
		initCurrent(myHandler, conn);
		
		//MyHandler handler = new MyHandler();
		//handler.setInvocationMap(ric);

		// Start
		runServer(context, port, backLog, myHandler);

	}
	
	
	
	
	private static void initCurrent(MyHttpHandler handler, Connection conn)
		throws Exception
	{
		String prefixModelPath = "Namespaces.2.0.ttl";
		
		// Setup
		logger.info("Loading uri namespaces");
		Model prefixModel = ModelFactory.createDefaultModel();
		ModelUtil.read(prefixModel, new File(prefixModelPath), "TTL");
		Map<String, String> prefixMap = prefixModel.getNsPrefixMap();
	
		logger.info("Loading mapping rules");
		InMemoryTagMapper tagMapper = new InMemoryTagMapper();
		tagMapper.load(new File("data/triplify/config/2.0/LGDMappingRules.2.0.xml"));
		
		LGDDAO innerDAO = new LGDDAO(conn);
		
		ILGDVocab vocab = new LGDVocab();
		LGDRDFDAO dao = new LGDRDFDAO(innerDAO, tagMapper, vocab);
		
		ServerMethods methods = new ServerMethods(dao, prefixMap);
		
		Method m;

		// Set up redirects
		LinkedDataRedirectHandler redirectHandler = new LinkedDataRedirectHandler();
		handler.getSubHandlers().add(redirectHandler);
		
		m = ServerMethods.class.getMethod("getNode", String.class);
		redirectHandler.getDataRIC().put("(.*)/triplify/(.*)", new RedirectInvocable("$0/data/$1"), "$0", "$1");
		redirectHandler.getPageRIC().put("(.*)/triplify/(.*)", new RedirectInvocable("$0/page/$1"), "$0", "$1");
		//redirectHandler.getDataRIC().put("(.*)/resource/(.*)", new RedirectInvocable("$0/data/$1"), "$0", "$1");
		//redirectHandler.getPageRIC().put("(.*)/resource/(.*)", new RedirectInvocable("$0/page/$1"), "$0", "$1");
	
		
		// Set up actual data URIs
		Model2Handler dataHandler = new Model2Handler();
		handler.getSubHandlers().add(dataHandler);
		
		m = ServerMethods.class.getMethod("getNode", String.class);
		dataHandler.getRIC().put(".*data/node([^.]*).*", new JavaMethodInvocable(m, methods), "$0");
	
		m = ServerMethods.class.getMethod("getWay", String.class);
		dataHandler.getRIC().put(".*data/way([^.]*).*", new JavaMethodInvocable(m, methods), "$0");
		
		
		// Set up page URIs
		Model3Handler pageHandler = new Model3Handler();
		handler.getSubHandlers().add(pageHandler);

		m = ServerMethods.class.getMethod("getNode", String.class);
		dataHandler.getRIC().put(".*page/node([^/?]*)/?(\\?.*)?", new JavaMethodInvocable(m, methods), "$0");
	
		m = ServerMethods.class.getMethod("getWay", String.class);
		dataHandler.getRIC().put(".*page/way([^/?]*)/?(\\?.*)?", new JavaMethodInvocable(m, methods), "$0");

		
		
		//m = ServerMethods.class.getMethod("getNear", String.class, String.class, String.class);
		IInvocable nearFn = DefaultCoercions.wrap(methods, "publicGetEntitiesWithinRadius.*");
		
		pageHandler.getRIC().put(".*/near/([^,]*),([^/]*)/([^/?]*)/?(\\?.*)?", nearFn, "$0", "$1", "$2", null, null, false);
		pageHandler.getRIC().put(".*/near/([^,]*),([^/]*)/([^/]*)/([^/=?]*)/?(\\?.*)?", nearFn, "$0", "$1", "$2", "$3", null, false);
		pageHandler.getRIC().put(".*/near/([^,]*),([^/]*)/([^/]*)/([^=]*)=([^/?]*)/?(\\?.*)?", nearFn, "$0", "$1", "$2", "$3", "$4", false);
		pageHandler.getRIC().put(".*/near/([^,]*),([^/]*)/([^/]*)/class/([^/?]*)/?(\\?.*)?", nearFn, "$0", "$1", "$2", "$3", "$3", true);
		
		
		IInvocable bboxFn = DefaultCoercions.wrap(methods, "publicGetEntitiesWithinRect.*");
		dataHandler.getRIC().put(".*/near/(-?[^-]+)-(-?[^,]+),(-?[^-]+)-(-?[^/]+)/?(\\?.*)?", bboxFn, "$0", "$1", "$2", "$3", null, null, false);
		
		
		
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
