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
package org.linkedgeodata.core;

public class LGDVocab
{
	public static final String NS = "http://linkedgeodata.org/";
	
	public static final String RESOURCE = NS + "triplify/";
	
	public static final String NODE = RESOURCE + "node";
	public static final String WAY = RESOURCE + "way";
	
	public static final String ONTOLOGY = NS + "ontology/";
	
	public static final String MEMBER_OF_WAY = ONTOLOGY + "memberOfWay";
	public static final String HAS_NODES = ONTOLOGY + "hasNodes";
	
	// NIR = Non-Information-Resource
	public static String createNIRNodeURI(long id)
	{
		//return NODE + "/_" + id;
		return NODE + id;
	}
	
	public static String createOSMNodeURI(long id)
	{
		return createNIRNodeURI(id);
	}
	
	public static String createNIRWayURI(long id)
	{
		//return WAY + "/_" + id;
		return WAY + id;
	}	

	public static String createOSMWayURI(long id)
	{
		return createNIRWayURI(id);
	}	
}
