package org.linkedgeodata.jtriplify;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.StreamUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * This file contains a small test implementation of a http server.
 * 
 * @author raven
 *
 */
class TestHttpHandler
	implements HttpHandler
{
	@Override
	public void handle(HttpExchange x) throws IOException
	{
		try {
			System.out.println(x.getRequestMethod());
			System.out.println(x.getRequestHeaders());
			System.out.println(StreamUtil.toString(x.getRequestBody()));
			
			Model model = ModelFactory.createDefaultModel();
			
			Resource r = ResourceFactory.createResource("http://ex.org/resource/a");
			Property p = ResourceFactory.createProperty("http://ex.org/ontology/b");
			model.add(r, p, r);
			
			x.getResponseHeaders().set("Content-Type", "application/rdf+xml");
			x.sendResponseHeaders(200, 0);
			
			OutputStream out = x.getResponseBody();
			String str = ModelUtil.toString(model, "RDF/XML");
			out.write(str.getBytes());
			out.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}


public class TestServer
{
	public static void main(String[] args)
		throws Exception
	{

		InetSocketAddress socketAddress = new InetSocketAddress(7001);
		HttpServer server = HttpServer.create(socketAddress, 100);
		
		HttpHandler handler = new TestHttpHandler();
		server.createContext("/test/", handler);
		server.setExecutor(null);
		server.start();

	}
}
