package org.linkedgeodata.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class ModelUtil
{
	/**
	 * 
	 * @param model
	 * @param resource
	 * @return
	 */
	public static Model filterBySubject(Model model, Resource resource)
	{
		Iterator<Statement> it = model.listStatements(resource, (Property)null, (RDFNode)null);
		Model result = ModelFactory.createDefaultModel();
		
		result.setNsPrefixes(model.getNsPrefixMap());
		
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		return result;
	}
	
	public static Model combine(Collection<Model> models)
	{
		Model result = ModelFactory.createDefaultModel();

		for(Model model : models) {
			result.add(model);
		}
		
		return result;
	}

	public static Model read(InputStream in, String lang)
		throws IOException
	{
		return read(ModelFactory.createDefaultModel(), in, lang);
	}

	
	public static Model read(File file, String lang)
		throws IOException
	{
		return read(new FileInputStream(file), lang);
	}
	
	
	public static Model read(Model model, InputStream in, String lang)
		throws IOException
	{
		try {
			model.read(in, null, lang);
		}
		finally {
			in.close();
		}
		
		return model;
	}

	// FIXME: Automatically detect file type from extension
	// On failure retry different parser
	public static Model read(Model model, File file, String lang)
		throws IOException
	{
		return read(model, new FileInputStream(file), lang);
	}
	
	public static String toString(Model model)
	{
		return toString(model, "N3");
	}

	public static String toString(Model model, RDFWriter writer)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer.write(model, baos, ""); 
		
		return baos.toString();
		
	}
	
	public static String toString(Model model, String format)
	{
		if(model == null)
			return "null";

		RDFWriter writer = model.getWriter(format);

		return toString(model, writer);
	}

	
	public static NsURI decompose(String uri, NavigableMap<String, String> prefixMap)
	{
		String prefix = "";
		String name = uri;

		NavigableMap<String, String> candidates = prefixMap.headMap(uri, false).descendingMap();
		Map.Entry<String, String> candidate = candidates.firstEntry();
				
		if(candidate != null && uri.startsWith(candidate.getKey())) {
			String candidateNs = candidate.getKey();
			String candidatePrefix = candidate.getValue();
			
			int splitIdx = candidateNs.length(); 

			prefix = candidatePrefix;
			name = uri.substring(splitIdx);
		}
		
		NsURI result = new NsURI(prefix, name);
		return result;
	}

	public static String prettyURI(String uri, NavigableMap<String, String> prefixMap)
	{
		NsURI tmp = decompose(uri, prefixMap);
		
		String result = (tmp.getPrefix().isEmpty())
			? URIUtil.decodeUTF8(tmp.getName())
			: tmp.getPrefix() + ":" + URIUtil.decodeUTF8(tmp.getName());
			
			return result;
	}
}
