package org.linkedgeodata.access;

import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

import org.linkedgeodata.core.OSMEntityType;

public interface IURLBuilder
{
	String getData(RectangularShape rect);
	String getData(Point2D p, double radius);
	
	String getEntity(OSMEntityType type, long id);
}
