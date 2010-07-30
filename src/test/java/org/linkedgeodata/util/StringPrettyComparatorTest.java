package org.linkedgeodata.util;

import org.junit.Assert;
import org.junit.Test;


public class StringPrettyComparatorTest
{
	@Test
	public void test()
	{
		StringPrettyComparator comp = new StringPrettyComparator();

		Assert.assertEquals(1, comp.compare("lgd:node1000", "lgd:node1"));
		
		Assert.assertEquals(1, comp.compare("1000a", "1a"));
		
		Assert.assertEquals(1, comp.compare("lgd:node1000a", "lgd:node1a"));
		Assert.assertEquals(1, comp.compare("lgd:node1000a", "lgd:node1b"));
		
		//Assert.assertEquals(1, comp.compare("lgd:node1000a", "lgd:node1a"));
		
		Assert.assertEquals(1, comp.compare("abc1", "1000"));
		Assert.assertEquals(-1, comp.compare("1000", "xyz1"));
				
		Assert.assertEquals(-1, comp.compare("1", "1000"));
		Assert.assertEquals(1, comp.compare("1000", "1"));
	}
}
