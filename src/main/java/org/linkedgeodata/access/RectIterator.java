package org.linkedgeodata.access;

import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.Arrays;
import java.util.Iterator;


public class RectIterator
	implements Iterator<RectangularShape>
{
	private Iterator<RectangularShape> it;
	private RectangularShape current = null;
	
	public RectIterator(Iterator<RectangularShape> it)
	{
		this.it = it;
	}
	
	public IterableRect descend()
	{
		return split(0.5, 0.5);
	}
	
	public IterableRect split(double rx, double ry)
	{
		if(current == null) {
			throw new IllegalStateException();
		}

		RectangularShape rect = current;
		
		double cx = rect.getMinX() + rx * rect.getWidth();
		double cy = rect.getMinY() + ry * rect.getHeight();
		
		RectangularShape[] rs = {
				new Rectangle2D.Double(rect.getMinX(), rect.getMinY(), cx, cy),
				new Rectangle2D.Double(cx, rect.getMinY(), rect.getMaxX(), cy),
				new Rectangle2D.Double(rect.getMinX(), cy, cx, rect.getMaxY()),
				new Rectangle2D.Double(cx, cy, rect.getMaxX(), rect.getMaxY())
		};
		
		return new IterableRect(Arrays.asList(rs));
	}

	@Override
	public boolean hasNext()
	{
		return it.hasNext();
	}

	@Override
	public RectangularShape next()
	{
		current = it.next();
		
		return current;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
	
}