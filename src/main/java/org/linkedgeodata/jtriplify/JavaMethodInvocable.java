package org.linkedgeodata.jtriplify;

import java.lang.reflect.Method;
import java.util.List;

class JavaMethodInvocable
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
