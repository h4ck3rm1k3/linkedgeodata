package org.linkedgeodata.access;

import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

import org.linkedgeodata.dao.OSMEntityType;


public class URLBuilderImpl
	implements IURLBuilder
{
	private String baseURI;
	
	public URLBuilderImpl(String baseURI)
	{
		this.baseURI = baseURI;
	}

	@Override
	public String getData(RectangularShape rect)
	{
		return
			baseURI + "near/" + 
			rect.getMinY() + "-" + rect.getMaxY() + "," +
			rect.getMinX() + "-" + rect.getMaxX();
	}
	
	@Override
	public String getData(Point2D point, double radius)
	{
		return baseURI + 
			"near/" + point.getY() + "," + point.getX() + "/" + radius; 
	}
	
	
	@Override
	public String getEntity(OSMEntityType type, long id)
	{
		switch(type) {
		case NODE:
			return baseURI + "node/" + id;
		case WAY:
			return baseURI + "way/" + id;
		case RELATION:
			return baseURI + "relation/" + id;
		default:
			throw new IllegalStateException("Unsupported entity type: " + type);
		}
	}
}