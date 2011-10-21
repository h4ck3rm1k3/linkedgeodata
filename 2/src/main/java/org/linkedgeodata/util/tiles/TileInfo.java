package org.linkedgeodata.util.tiles;

public class TileInfo
	implements Comparable<TileInfo>
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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (zipped ^ (zipped >>> 32));
		result = prime * result + zoom;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TileInfo other = (TileInfo) obj;
		if (zipped != other.zipped)
			return false;
		if (zoom != other.zoom)
			return false;
		return true;
	}

	@Override
	public int compareTo(TileInfo o)
	{
		int dz = o.zoom - this.zoom;
		if(dz != 0) {
			return dz;
		}
		
		return (int)(o.zipped - this.zipped);
	}	
}