package org.linkedgeodata.util.tiles;

public class TileInfo
{
	private int x;
	private int y;
	private int zoom;
	private long zipped;
	
	public TileInfo(int x, int y)
	{
		this.x = x;
		this.y = y;
		
		zipped = TileUtil.zip(y, x);
	}
	
	public long getX()
	{
		return x;
	}
	
	public long getY()
	{
		return y;
	}
	
	public int getZoom()
	{
		return zoom;
	}
	
	public long getZipped()
	{
		return zipped;
	}
}