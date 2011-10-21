package org.linkedgeodata.jtriplify.methods;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;






/**
 * +(int, int)
 * +(float, float)
 * 
 * given
 * +(int, double)
 * 
 * wanted:
 * chose +(float, float)
 * 
 * 
 * coercions + costs:
 * 
 * problem
 * Cost functions?
 * 
 * cost(double, int)   = 
 * cost(double, float) =
 * cost(int, float)    =
 * 
 * cost(int, int)    =  0
 * cost(double, int)   = cost(coercion(double, int)) = 2
 * 
 * cost(int, float)    = cost(coercion(int, float))  = 0.5
 * cost(double, float) = cost(coercion(double, float)) = 1
 * 
 * cost(coercion(double, int)) < cost(coercion(int, float)) + cost(coercion(double, float))
 * 
 * 
 * What about
 * f(A, A)
 * f(B, A)
 * 
 * A subclass B
 * 
 * given: B, B?
 * 
 * 
 * How to efficiently index half orders (like type hierarchies)?
 * 
 * dataset contains set of classes O.
 * queries lookup for subclasses of O.
 * 
 * -> query cache;
 * http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc.html
 */

public class CustomizableCostFunctionCollection
	implements ICostFunctionCollection<IInvocable, Float>
{	
	private ICostFunctionCollection<IInvocable, Float> coercions;
	
	private Map<JavaMethodInvocable, Float> methods = new HashMap<JavaMethodInvocable, Float>();
	
	
	public CustomizableCostFunctionCollection(ICostFunctionCollection<IInvocable, Float> coercions)
	{
		this.coercions = coercions;
	}
	
	
	
	/**
	 * Registers a method with an corresponding object (may be null if the method is static).
	 * The method must take only one argument, and return a non-void class.
	 * 
	 * @param m
	 * @param o
	 */
	public void registerMethod(Method m, Object o, float cost)
	{
		JavaMethodInvocable invokable = new JavaMethodInvocable(m, o);
		
		//Class<?>[] types = m.getParameterTypes();

		//classesToMethod.put(Arrays.asList(types), invokable);
		methods.put(invokable, cost);
	}


	
	/**
	 * Note the return type behaves inverse to the paramTypes:
	 * 
	 * If we are looking for a function with a return type 
	 * compatible to A then any function returning a subclass of A is ok.
	 * 
	 * If we are looking for a function where one of the parameters in
	 * compatible to A then any parameter which is a superclass is ok.
	 * 
	 * 
	 * Arguments may null. In this case they are treated a wildcards.
	 */
	@Override
	public Pair<IInvocable, Float> lookupCheapest(Class<?> returnType, Class<?>... args)
	{
		Pair<IInvocable, Float> tmp = FunctionUtil.lookupCheapest(returnType, args, methods.keySet(), coercions);
		if(tmp == null || tmp.getKey() == null)
			return null;
		
		Float cost = methods.get(tmp.getKey());
		return new Pair<IInvocable, Float>(tmp.getKey(), cost);
	}
	
	
	
	
	// return positive infinity is the method is no match
	/*
	private Pair<Float, IInvocable> calculateCost(Class<?> given, Class<?> there)
	{
		if(given == null || there == null)
			return new Pair<Float, IInvocable>(0.0f, null);
		
		Integer distance = ClassUtil.getDistance(given, there);
		if(distance != null)
			return new Pair<Float, IInvocable>(distance.floatValue(), null);

		if(coercions != null)
			return coercions.lookupCheapest(given, there);
		
		return null;
	}



	@Override
	public Pair<Float, IInvocable> lookupCheapest(Class<?> returnType, Class<?>... args)
	{
		//org.apache.commons.collections15.multimap.
		float bestCost = Float.MAX_VALUE;
		IInvocable result = null;

		
		for(Pair<Float, JavaMethodInvokable> tmp : methods) {
			JavaMethodInvokable item = tmp.getSecond();
			
			boolean skip = false;
			
			boolean hasTransforms = false;
			IInvocable[] transforms = new IInvocable[args.length];
			float currentCost = 0.0f;
			
			Method method = item.getMethod();
			Class<?>[] paramTypes = method.getParameterTypes();
			
			for(int i = 0; i < paramTypes.length; ++i) {
				Class<?> given = args[i];
				Class<?> there = paramTypes[i];
				
				Pair<Float, IInvocable> pair = calculateCost(given, there);
				if(pair == null) {
					skip = true;
					break;
				}
				
				if(pair.getSecond() != null)
					hasTransforms = true;
				
				currentCost += pair.getFirst();
				transforms[i] = pair.getSecond();
			}
			
			if(skip)
				continue;
			
			if(currentCost == bestCost)
				throw new RuntimeException("Ambigous methods:" +  result + " and " + item);
			
			if(currentCost < bestCost) {
				bestCost = currentCost;
				
				if(hasTransforms)
					result = new CompoundInvokable(item, transforms);
				
				result = item;
			}
		}
		
		if(result != null)
			return new Pair<Float, IInvocable>(bestCost, result);

		return null;
	}
	*/
}
