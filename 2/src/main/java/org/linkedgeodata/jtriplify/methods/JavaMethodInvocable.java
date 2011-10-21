package org.linkedgeodata.jtriplify.methods;

import java.lang.reflect.Method;
import java.util.List;


public class JavaMethodInvocable
		implements IInvocable
{
	private final Object	object;
	private final Method	method;
	
	public JavaMethodInvocable(Method method, Object object)
	{
		this.method = method;
		this.object = object;
	}

	@Override
	public Object invoke(Object... args)
		throws Exception
	{
		System.out.println("Invoking: " + method + args);
		return method.invoke(object, args);
	}
	
	public Method getMethod()
	{
		return method;
	}
	
	@Override
	public String toString()
	{
		return method == null ? null : method.toString();
	}
}
