package org.linkedgeodata.util.tiles;

import java.util.AbstractCollection;
import java.util.Iterator;

public class SubTileIdCollection
	extends AbstractCollection<Long>
{
	public long parentTileId;
	public int delta;
	
	public SubTileIdCollection(long parentTileId, int delta)
	{
		this.parentTileId = parentTileId;
		this.delta = delta;
	}
	
	@Override
	public Iterator<Long> iterator()
	{
		return new SubTileIdIterator(parentTileId, delta);
	}

	@Override
	public int size()
	{
		return (int)Math.pow(4, delta);
	}

}
