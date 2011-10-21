package org.linkedgeodata.util.tiles;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.NavigableSet;
import java.util.TreeSet;




public class TileUtil
{
	public static void main(String[] args)
	{
		//java.awt.Point[x=34279,y=51021]
		//System.out.println(zip(
		//System.out.println(unzip(3493312635l));2062342692

		double lat = 51.20;
		double lon = 12.22;

		
		//double lon = 8.3013034;
		//double lat = 50.1359444;
		//double lon = -1.8221247;
		//double lat = 52.5659333;
		
		TileInfo tile = TileUtil.toTileInfo(lon, lat, 16);
		System.out.println(tile.getX());
		System.out.println(tile.getY());
		System.out.println(tile.getZipped());
		System.out.println(bitsToString(tile.getX()));
		System.out.println(bitsToString(tile.getY()));
		System.out.println(bitsToString(tile.getZipped()));
		
		/*
		int x = 0x0000aaaa;
		int y = 0x00005555;
		System.out.println("x = " + x);
		System.out.println("y = " + y);
		System.out.println("bx = " + bitsToString(x));
		System.out.println("by = " + bitsToString(y));

		
		long zipped = zip(x, y);
		System.out.println("bz = " + bitsToString(zipped));
		
		Point p = unzip(zipped);
		System.out.println("bux = " + bitsToString(p.x));
		System.out.println("buy = " + bitsToString(p.y));
		
		System.out.println("ux = " + p.x);
		System.out.println("ux = " + p.y);
		*/
		

		//System.out.println("inter = " + zip(new Point(0x04, 0x17)));

		/*
		System.out.println("inter = " + zip(new Point(0, 0)));
		System.out.println("inter = " + zip(new Point(1, 1)));
		System.out.println("inter = " + zip(new Point(2, 2)));
		System.out.println("inter = " + zip(new Point(3, 3)));
		System.out.println("inter = " + zip(new Point(0, 3)));
		System.out.println("inter = " + zip(new Point(2, 3)));
		*/
	}
	
	/**
	 * Normalizes a longitude value in the range [-180 .. 180] to [0 .. 1] 
	 *
	 * @param unknown_type $lon
	 * @return unknown
	 */
	/*
	public static double lonNormX(double lon)
	{
		return (lon + 180.0) / 360.0;
	}
	 */
	/**
	 * Normalizes a latitude value in the range [-90 .. 90] to [0 .. 1]
	 *
	 * @param unknown_type $lon
	 * @return unknown
	 */
	/*
	public static double latNormY(double lat)
	{
		return (lat + 90.0) / 180.0;
	}
	*/
	/*
	public static Point llToXY(double lon, double lat, int zoom)
	{
		double f = Math.pow(2, zoom) - 1;

		int x = (int)Math.round(lonNormX(lon) * f);
		int y = (int)Math.round(latNormY(lat) * f);
		
		return new Point(x, y);
	}*/
	
	// lat = y, lon = x
	public static NavigableSet<TileInfo> tilesForArea(RectangularShape rect, int zoom)
	{
		// Transform the given geo-coordinates into tile coordinates.
		Point min = llToXY(rect.getMinX(), rect.getMinY(), zoom);
		Point max = llToXY(rect.getMaxX(), rect.getMaxY(), zoom);

		NavigableSet<TileInfo> result = new TreeSet<TileInfo>();

		for(int x = min.x; x <= max.x; x++) {
			for(int y = min.y; y <= max.y; y++) {
				result.add(new TileInfo(x, y));
			}
		}
		return result;
	}
	
	
	public static TileInfo toTileInfo(double lon, double lat, int zoom)
	{
		int f = (int)Math.pow(2, zoom);

		int x = (int)(normLon(lon) * f);
		int y = (int)(normLat(lat) * f);

		if(x >= f) x = f - 1;
		if(y >= f) y = f - 1;
		
		return new TileInfo(x, y);
	}

	
	/**
	 * Normalizes longitude to the range of [0 - 1].
	 * 
	 */
	public static double normLon(double lon)
	{
		return (lon + 180.0) / 360.0;
	}
	
	
	/**
	 * Normalizes a latitude value in the range [-90 .. 90] to [0 .. 1].
	 * 
	 * @param lat
	 * @return
	 */
	public static double normLat(double lat)
	{
		return (lat + 90.0) / 180.0;
	}

	/*
	public static long latToY(double lat)
	{
		return (long)(lat * 10000000);
	}

	public static long lonToX(double lon)
	{
		return (long)(lon * 10000000);
	}
	*/
	
	
	public static long getTile(long lon, long lat)
	{
		int x = (int)(lon >> 16);
		int y = (int)(lat >> 16);
		
		long result = zip(x, y);
		
		return result;
	}
	
	/*
	public static Point encodePoint(double lon, double lat)
	{
		return new Point((int)(lon * 10000000), (int)(lat * 10000000));
	}

	public Point2D.Double decodePoint(Point p)
	{
		return new Point2D.Double(p.x / 10000000.0, p.y / 10000000.0);
	}*/
	
	/**
	 * Zips the bits of x and y. y first.
	 * x=100
	 * y=001
	 * -> 01 00 10
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static long zip(int px, int py)
	{
		long result = 0;
		
		for(int i = 0; i < 16; ++i) {
			long x = (px >> i) & 1;
			long y = (py >> i) & 1;

			result |= ((y << 1) | x) << (2 * i);
		}
		
		return result;
	}
	
	public static Point unzip(long value)
	{
		//int x = value < 0 ? 0x8000 : 0; // If value is negative, the first bit is set 
		//int y = (int)(value & 0x40000000);
		int x = 0;
		int y = 0;

		int mask = 1;
		for(int i = 0; i < 16; ++i) {
			x |= (value & (mask << 1)) >> (i + 1);
			y |= (value &  mask      ) >> i;
			
			mask <<= 2;
		}
		
		//System.out.println(zip(x, y));		
		
		return new Point(x, y);
	}
	
	
	public static String bitsToString(long value)
	{
		String result = "";
		for(int i = 31; i >= 0; --i) {
			result += ((value >> i) & 1);
			
			if(i % 8 == 0)
				result += " ";
		}
		
		return result;
	}
	
	public static Point llToXY(double lon, double lat, int zoom)
	{
		double f = Math.pow(2, zoom) - 1;

		int x = (int)Math.round(normLon(lon) * f);
		int y = (int)Math.round(normLat(lat) * f);
		
		return new Point(x, y);
	}

	
	public static Rectangle2D getRectangle(long tileId, int zoom)
	{
		double invf = 1.0 / Math.pow(2, zoom);

		Point pt = unzip(tileId);
    
		Rectangle2D result = new Rectangle2D.Double(
				pt.y * invf * 360.0 - 180.0,
				pt.x * invf * 180.0 - 90.0,
				1 * invf * 360.0,
				1 * invf * 180.0);
		
		return result;
	}
}
