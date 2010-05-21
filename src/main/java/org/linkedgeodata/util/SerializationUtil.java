/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.linkedgeodata.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


/**
 * Utility methods for de-/serializing objects to streams and files.
 * 
 * @author Claus Stadler
 *
 */
public class SerializationUtil
{		
	
	public static void serializeXML(Object obj, File file)
		throws JAXBException, IOException
	{
		OutputStream out = new FileOutputStream(file);
		serializeXML(obj, out);
		out.close();
	}

	public static void serializeXML(Object obj, OutputStream out)
	{
		XStream xstream = new XStream(new DomDriver());
		xstream.toXML(obj, out);
	}
	/*
	public static void serializeXML(Object o, OutputStream out)
		throws JAXBException
	{
		JAXBContext jaxbContext = JAXBContext.newInstance(o.getClass());
	    jaxbContext.createMarshaller().marshal(o, out);
	}*/

	/*
	public static void serializeXML(Object o, OutputStream out)
		throws IOException
	{
		XMLEncoder e = new XMLEncoder(out);
		
		e.writeObject(o);
		e.flush();
	}
	*/

	@SuppressWarnings("unchecked")
	public static Object deserializeXML(InputStream in)
		throws JAXBException, IOException
	{
		XStream xstream = new XStream(new DomDriver());
		Object result = xstream.fromXML(in);
		//Object result = jaxbContext.createUnmarshaller().unmarshal(in);

		return result;
	}

	/*
	@SuppressWarnings("unchecked")
	public static <T> T deserializeXML(InputStream in, Class<T> clazz)
		throws JAXBException, IOException
	{
		JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
		Object result = jaxbContext.createUnmarshaller().unmarshal(in);

		return (T)result;
	}
	*/
	/*
	public static Object deserializeXML(InputStream in)
	{
		XMLDecoder d = new XMLDecoder(in);
		return d.readObject();
	}
	*/
	
	public static Object deserializeXML(File file)
		throws IOException, JAXBException
	{
		InputStream in = new FileInputStream(file);
		Object result = deserializeXML(in);
		
		in.close();
		return result;
		//return (T)result;
	}
}
