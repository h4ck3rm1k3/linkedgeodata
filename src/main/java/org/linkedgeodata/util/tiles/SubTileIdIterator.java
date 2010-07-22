package org.linkedgeodata.util.tiles;

import org.linkedgeodata.util.SinglePrefetchIterator;

public class SubTileIdIterator
	extends SinglePrefetchIterator<Long>
{
	private long parentTileId;
	private int current;
	
	private int delta;
	private int max;
	
	public SubTileIdIterator(long parentTileId, int delta)
	{
		this.parentTileId = parentTileId;
		this.delta = delta;
		
		this.max = (int)Math.pow(4, delta);
		current = 0;
		
	}
	
	@Override
	protected Long prefetch() throws Exception
	{
		if(current >= max)
			return finish();
		
		long result = (parentTileId << (2 * delta)) | (current++); 
		return result;
	}
}