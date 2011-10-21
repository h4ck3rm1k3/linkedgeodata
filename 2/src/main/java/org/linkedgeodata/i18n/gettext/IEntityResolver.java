package org.linkedgeodata.i18n.gettext;

import com.hp.hpl.jena.rdf.model.Resource;

public interface IEntityResolver
{
	public Resource resolve(String key, String value);
}