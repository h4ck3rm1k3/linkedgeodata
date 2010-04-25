package org.linkedgeodata.jtriplify.methods;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class CustomizableFunctionCollection
		implements IFunctionContainer, IInvocable
{
	private static Logger logger = Logger.getLogger(CustomizableCostFunctionCollection.class);

	private ICostFunctionCollection<IInvocable, Float>	coercions;

	private List<JavaMethodInvocable> methods	= new ArrayList<JavaMethodInvocable>();

	public CustomizableFunctionCollection(
			ICostFunctionCollection<IInvocable, Float> coercions)
	{
		this.coercions = coercions;
	}

	public void registerAll(Class<?> clazz, Pattern pattern)
	{		
		for(Method m : clazz.getMethods())
		{
			if((m.getModifiers() & Modifier.STATIC) == 0)
				continue;
		
			if(pattern == null || pattern.matcher(m.getName()).matches())
				registerMethod(m, null);
		}
	}
	
	public void registerAll(Object o, Pattern pattern)
	{
		for(Method m : o.getClass().getMethods())
		{
			if((m.getModifiers() & Modifier.STATIC) != 0)
				continue;
		
			if(pattern == null || pattern.matcher(m.getName()).matches())
				registerMethod(m, o);
		}
	}

	/**
	 * Registers a method with an corresponding object (may be null if the
	 * method is static). The method must take only one argument, and return a
	 * non-void class.
	 * 
	 * @param m
	 * @param o
	 */
	public void registerMethod(Method m, Object o)
	{
		JavaMethodInvocable invocable = new JavaMethodInvocable(m, o);

		
		logger.debug("Registered method [" + m + "] with object [" + o + "]");
		// Class<?>[] types = m.getParameterTypes();

		// classesToMethod.put(Arrays.asList(types), invocable);
		methods.add(invocable);
	}

	/**
	 * Note the return type behaves inverse to the paramTypes:
	 * 
	 * If we are looking for a function with a return type compatible to A then
	 * any function returning a subclass of A is ok.
	 * 
	 * If we are looking for a function where one of the parameters in
	 * compatible to A then any parameter which is a superclass is ok.
	 * 
	 * 
	 * Arguments may null. In this case they are treated a wildcards.
	 */
	@Override
	public IInvocable lookup(Class<?> returnType, Class<?>... args)
	{
		Pair<IInvocable, Float> tmp = FunctionUtil.lookupCheapest(returnType, args, methods, coercions);

		return tmp == null ? null : tmp.getFirst();
	}

	/**
	 * This method is prodvided for convenience.
	 * 
	 */
	@Override
	public Object invoke(Object... args)
		throws Exception
	{
		return FunctionUtil.invoke(this, args);
	}
	
	@Override
	public String toString()
	{
		return "FunctionCollection(" + methods + ")";
	}
}
