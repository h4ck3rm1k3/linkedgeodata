package org.linkedgeodata.jtriplify.methods;

import java.lang.reflect.Method;
import java.util.Collection;


public class FunctionUtil
{
	public static Class<?>[] getTypes(Object ...args)
	{
		Class<?>[] types = new Class<?>[args.length];
		for(int i = 0; i < args.length; ++ i)
		{
			Class<?> type = args[i] == null ? null : args[i].getClass();
			types[i] = type;
		}
		
		return types;
	}
	
	
	
	public static Object invoke(IFunctionContainer functions, Object...args)
		throws Exception
	{
		IInvocable invocable = FunctionUtil.lookup(functions, args);
		if(invocable == null)
			 throw new NoSuchMethodException();
		 
		 return invocable.invoke(args);
	}
	
	public static <T> IInvocable lookup(IFunctionContainer functions, T ...args)
	{
		Class<?>[] types = FunctionUtil.getTypes(args);
		
		return functions.lookup(null, types);
	}
	
	/*
	public static Object invoke(IInvokable invocable, Object ...args)
		throws Throwable
	{
		return invocable.invoke(args);
	}
	
	public static IInvokable lookup(IFunctionContainer container, Class<?> returnType, Class<?>... paramTypes)
	{
		return container.lookup(returnType, paramTypes);
	}
	*/
	/**
	 * Looks up the function with the cheapest _invocation_ cost.
	 * (So this method does not take a cost of a function itself into account)
	 * 
	 * @param returnType
	 * @param args
	 * @param items
	 * @param coercions
	 * @return
	 */
	public static Pair<IInvocable, Float> lookupCheapest(Class<?> returnType, Class<?>[] args, Collection<JavaMethodInvocable> items, ICostFunctionCollection<IInvocable, Float> coercions)
	{
		// org.apache.commons.collections15.multimap.
		float bestCost = Float.MAX_VALUE;
		IInvocable result = null;

		for (JavaMethodInvocable item : items) {
			Method method = item.getMethod();
			Class<?>[] paramTypes = method.getParameterTypes();

			// Reject if there are more arguments given than the method accepts
			if(!method.isVarArgs() && args.length > paramTypes.length)
				continue;
			
			if(returnType != null) {
				if(null == ClassUtil.getDistance(returnType, method.getReturnType()))
					continue;
			}
			
			
			
			boolean skip = false;

			boolean hasTransforms = false;
			IInvocable[] transforms = new IInvocable[args.length];
			float currentCost = 0.0f;


			for (int i = 0; i < paramTypes.length; ++i) {
				Class<?> given = args[i];
				Class<?> there = paramTypes[i];

				Pair<IInvocable, Float> pair = calculateCost(given, there, coercions);
				if (pair == null) {
					skip = true;
					
					System.out.println("No coercion found for " + given + " -> " + there);
					
					break;
				}

				if (pair.getFirst() != null)
					hasTransforms = true;

				transforms[i] = pair.getFirst();
				currentCost += pair.getSecond();
			}

			if (skip)
				continue;

			if (currentCost == bestCost)
				throw new RuntimeException("Ambigous methods:" + result
						+ " and " + item);

			if (currentCost < bestCost) {
				bestCost = currentCost;

				if (hasTransforms)
					result = new CompoundInvocable(item, transforms);
				else
					result = item;
			}
		}

		if (result != null)
			return new Pair<IInvocable, Float>(result, bestCost);

		return null;
	}
	
	// return positive infinity is the method is no match
	private static Pair<IInvocable, Float> calculateCost(Class<?> given, Class<?> there, ICostFunctionCollection<IInvocable, Float> coercions)
	{
		if (given == null || there == null)
			return new Pair<IInvocable, Float>(null, 0.0f);

		Integer distance = ClassUtil.getDistance(given, there);
		if (distance != null)
			return new Pair<IInvocable, Float>(null, distance.floatValue());

		if (coercions != null) {
			Pair<IInvocable, Float> result = coercions.lookupCheapest(there, given);
			return result;
		}

		return null;
	}
}
