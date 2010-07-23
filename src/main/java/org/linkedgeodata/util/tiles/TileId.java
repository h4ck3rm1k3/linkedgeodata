package org.linkedgeodata.util.tiles;

public class TileId
{
	private long hash;
	private int zoom;

	public TileId()
	{
	}
	
	public TileId(long hash, int zoom)
	{
		super();
		this.hash = hash;
		this.zoom = zoom;
	}
	
	public long getHash()
	{
		return hash;
	}
	/*
	public void setHash(long hash)
	{
		this.hash = hash;
	}
	*/
	public int getZoom()
	{
		return zoom;
	}
	/*
	public void setZoom(int zoom)
	{
		this.zoom = zoom;
	}*/

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (hash ^ (hash >>> 32));
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
		TileId other = (TileId) obj;
		if (hash != other.hash)
			return false;
		if (zoom != other.zoom)
			return false;
		return true;
	}
}
