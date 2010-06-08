package org.linkedgeodata.access;

import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

import com.hp.hpl.jena.rdf.model.Model;

public interface ILGDDAO
{
	Model getData(RectangularShape rect, Model model);
	Model getData(Point2D point, double radius, Model model);
}
