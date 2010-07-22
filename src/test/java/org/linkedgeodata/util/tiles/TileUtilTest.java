package org.linkedgeodata.util.tiles;

import java.awt.Point;
import java.awt.geom.Rectangle2D;

import junit.framework.Assert;

import org.junit.Test;


public class TileUtilTest
{
	@Test
	public void testTiles()
	{
		long tileId;
		
		tileId = TileUtil.toTileInfo(-180.0, -90.0, 16).getZipped();
		Assert.assertEquals(0l, tileId);
		
		tileId = TileUtil.toTileInfo(180.0, 90.0, 16).getZipped();
		Assert.assertEquals(4294967295l, tileId);
				
		tileId = TileUtil.toTileInfo(180.0, -90.0, 16).getZipped();
		Assert.assertEquals(2863311530l, tileId);
		
		tileId = TileUtil.toTileInfo(-180.0, 90.0, 16).getZipped();
		Assert.assertEquals(1431655765l, tileId);
		
		tileId = TileUtil.toTileInfo(0.0, 0.0, 16).getZipped();
		System.out.println(TileUtil.bitsToString(tileId));
		System.out.println(TileUtil.unzip(tileId));
		
		Assert.assertEquals(3221225472l, tileId);
	}
	
	@Test
	public void testRectangles()
	{
		long tileId;
		Rectangle2D rect;
		
		tileId = TileUtil.toTileInfo(0.0, 0.0, 16).getZipped();
		rect = TileUtil.getRectangle(tileId, 16);

		//Assert.assertTrue(new Rectangle2D.Double(0.0, 0.0, 0.00274658203125, 0.0054931640625).equals(rect));
		
		tileId = TileUtil.toTileInfo(-180.0, -90.0, 16).getZipped();
		rect = TileUtil.getRectangle(tileId, 16);
		
		//Assert.assertTrue(
		
		System.out.println(rect);		
	}
}
