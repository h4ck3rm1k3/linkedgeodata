package org.linkedgeodata.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class ModelUtil
{
	/**
	 * TODO copy the namespaces
	 * 
	 * @param model
	 * @param resource
	 * @return
	 */
	public static Model filterBySubject(Model model, Resource resource)
	{
		Iterator<Statement> it = model.listStatements(resource, (Property)null, (RDFNode)null);
		Model result = ModelFactory.createDefaultModel();
		
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
	
	
	// FIXME: Automatically detect file type from extension
	// On failure retry different parser
	public static Model read(Model model, File file, String lang)
	throws IOException
	{
		InputStream in = new FileInputStream(file);
		try {
			model.read(in, null, lang);
		}
		finally {
			in.close();
		}
		
		return model;
	}
	
	public static String toString(Model model)
	{
		return toString(model, "N3");
	}

	public static String toString(Model model, String format)
	{
		if(model == null)
			return "null";

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		//model.write(baos, "N-TRIPLE", "");
		model.write(baos, format, "");
		//model.write(baos, null, "");
		//System.out.println("WROTE RDF: " + baos.toString());
		return baos.toString();
	}
}
