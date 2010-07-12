package org.linkedgeodata.core.vocab;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class WGS84Pos
{
	protected static final String uri ="http://www.w3.org/2003/01/geo/wgs84_pos#";

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

    /*
    public static Property li( int i )
	        { return property( "_" + i ); }
     */
   	public static final Property xlat = property("lat");
	public static final Property xlong = property("long");
	
	public static final Property geometry = property("geometry");
}
