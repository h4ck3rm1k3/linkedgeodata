package org.linkedgeodata.osm.osmosis.plugins;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;


public class TreeSetDiff<T>
		extends CollectionDiff<T, Set<T>>
{
	/*
	 * public SetDiff(Comparator<T> comparator) { }
	 */
	public TreeSetDiff()
	{
		super(new TreeSet<T>(), new TreeSet<T>(), new TreeSet<T>());
	}

	public TreeSetDiff(Comparator<T> comparator)
	{
		super(new TreeSet<T>(comparator), new TreeSet<T>(comparator),
				new TreeSet<T>(comparator));
	}
}
