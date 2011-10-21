package org.linkedgeodata.jtriplify.methods;

public class ClassUtil
{
	/**
	 * Returns the minimum distance of two classes in an inheritance hierarchy
	 * Null if there is no distance 
	 */
	public static Integer getDistance(Class<?> given, Class<?> there)
	{
		int result;
		if(there.isInterface())
			result = _getDistanceInterface(given, there, 0);
		else
			result = _getDistanceClass(given, there);
		
		return result == Integer.MAX_VALUE ? null : result;
	}

	private static int _getDistanceClass(Class<?> given, Class<?> there)
	{
		int distance = 0;
		do {
			if(given == there)
				return distance;

			distance += 1;
			given = given.getSuperclass();		
		
						
		} while (given != null);
		
		return Integer.MAX_VALUE;
	}

	private static int _getDistanceInterface(Class<?> given, Class<?> there, int depth)
	{
		if(given == there)
			return depth;
		
		++depth;
		
		int result = Integer.MAX_VALUE;
		for(Class<?> item : given.getInterfaces())
			result = Math.min(result, _getDistanceInterface(item, there, depth));

		Class<?> superClass = given.getSuperclass();
		if(superClass != null)
			result = Math.min(result, _getDistanceInterface(superClass, there, depth));
		
		return result;
	}

}
