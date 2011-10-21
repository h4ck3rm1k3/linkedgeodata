package org.linkedgeodata.access;

import java.awt.geom.RectangularShape;
import java.util.Collection;
import java.util.Collections;


public class IterableRect
{
	private Collection<RectangularShape> rects;
	
	public IterableRect(RectangularShape rect)
	{
		this.rects = Collections.singleton(rect);
	}
	
	public IterableRect(Collection<RectangularShape> rects)
	{
		this.rects = rects;
	}

	public RectIterator iterator()
	{
		return new RectIterator(rects.iterator());
	}
}
