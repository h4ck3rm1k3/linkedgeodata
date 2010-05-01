package org.linkedgeodata.util;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class ModelUtil
{
	public static Model combine(Collection<Model> models)
	{
		Model result = ModelFactory.createDefaultModel();

		for(Model model : models) {
			result.add(model);
		}
		
		return result;
	}
	

	public static String toString(Model model, String format)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		//model.write(baos, "N-TRIPLE", "");
		model.write(baos, format, "");
		//model.write(baos, null, "");
		//System.out.println("WROTE RDF: " + baos.toString());
		return baos.toString();
	}
}
