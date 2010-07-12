package org.linkedgeodata.core.vocab;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class GeoRSS
{
	protected static final String uri ="http://www.georss.org/georss/";

    public static String getURI()
    {
    	return uri;
    }

    protected static final Resource resource(String local)
    {
    	return ResourceFactory.createResource(uri + local);
    }

    protected static final Property property(String local)
    {
    	return ResourceFactory.createProperty(uri, local);
    }

    public static final Property point = property("point");
    public static final Property line = property("line");
    public static final Property polygon = property("polygon");
}
