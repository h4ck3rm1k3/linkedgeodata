package org.linkedgeodata.access;

import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.Iterator;

public class RectIterator2
	implements Iterator<RectangularShape>
{
	private RectangularShape bounds;
	
	private double x;
	private double y;
	private double stepWidth;
	private double stepHeight;
	
	
	public RectIterator2(RectangularShape bounds, int chunksX, int chunksY)
	{
		this.bounds = bounds;
		
		x = bounds.getMinX();
		y = bounds.getMinY();
		
		stepWidth = bounds.getWidth() / (double)chunksX;
		stepHeight = bounds.getHeight() / (double)chunksY;
	}
	
	@Override
	public boolean hasNext()
	{
		//return (x < bounds.getMaxX() && y > bounds.getMaxY());
		return (x < bounds.getMaxX() && y < bounds.getMaxY());
	}

	@Override
	public RectangularShape next()
	{
		if(!hasNext()) {
			return null;
		}
		
		RectangularShape result =
			new Rectangle2D.Double(x, y, stepWidth, stepHeight);
		
		x += stepWidth;
		if(x >= bounds.getMaxX()) {
			x = bounds.getMinX();
			y += stepHeight;
		}
		
		return result;
	}
	
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}	
}
