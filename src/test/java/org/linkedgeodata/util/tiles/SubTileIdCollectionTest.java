package org.linkedgeodata.util.tiles;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;


public class SubTileIdCollectionTest
{
	/**
	 * Test which checks whether the SubTileIdCollection returns the correct
	 * ids for the children of a given tile.
	 * 
	 */
	@Test
	public void test()
	{
		Collection<Long> subTileIds =
			new SubTileIdCollection(0, 2);
		
		Iterator<Long> it = subTileIds.iterator();
		for(long i = 0l; i < 16; ++i) {
			long actual = it.next();
			Assert.assertEquals(i, actual);
		}
	}

	@Test
	public void test2()
	{
		Collection<Long> subTileIds =
			new SubTileIdCollection(1234, 2);
		
		Iterator<Long> it = subTileIds.iterator();
		for(Long i = 0l; i < 16; ++i) {
			long actual = it.next();
			//System.out.println(it.next());
			Assert.assertEquals((1234l << 4) | i, actual);
		}
	}

}
