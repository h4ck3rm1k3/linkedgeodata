/**
 * Copyright (C) 2009-2010, LinkedGeoData developers
 *
 * This file is part of LinkedGeoData.
 * 
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.linkedgeodata.mapping;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

/**
 * A LinkedGeoData point.
 * 
 * @author Jens Lehmann
 *
 */
public class LGDPoint extends Point {

	static MultiMap<POIClass,String> classRestrictions = null;

	private double name;

	public LGDPoint(URI uri, double geoLat, double geoLong) {
		super(uri, null, geoLat, geoLong);
	}

	/**
	 * @return the name
	 */
	public double getName() {
		return name;
	}

	private static String typeRestriction(String type)
	{
		String lgdO = "<http://linkedgeodata.org/ontology/";
		return "a "+lgdO+type+">";
	}

	private static MultiMap<POIClass,String> readClasses() throws IOException
	{
		final String filename = "cfg/lgd_classes.cfg";
		BufferedReader in = new BufferedReader(new FileReader(filename));
		MultiMap<POIClass,String> classes = new MultiHashMap<POIClass,String>();
		String line;
		while((line = in.readLine()) != null)
		{
			String[] tokens = line.split("\t");
			if(tokens.length<1) continue;
			
			POIClass poiClass = POIClass.valueOf(tokens[0].trim());
			for(int i=1;i<tokens.length;i++)
			{
				String statement = tokens[i].trim();
				String[] elements = statement.split(" ");
				if(elements.length>2)
				{
					throw new RuntimeException("Error in config file "+filename+" at line '"+line+"', to many spaces (labels with spaces are not supported, please rewrite this code passage if you need this.)");
				}
				if(elements.length==2)	// e.g. '<http://linkedgeodata.org/property/place> "island"'
				{
					classes.put(poiClass, statement);
				}
				else	// e.g. 'railway_station'
				{
					classes.put(poiClass, typeRestriction(statement));
				}
			}
		}
		in.close();
		return classes;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(double name) {
		this.name = name;
	}
	
	// todo : dont return 0 if nothing found but do something safer (maybe exception)
	public static String getSPARQLRestriction(POIClass poiClass, String variable)
	{
		if(classRestrictions==null)
		{
			try{classRestrictions = readClasses();}
			catch (IOException e) 	{throw new RuntimeException("Error loading config file.",e);}
		}

		Collection<String> restrictions = classRestrictions.get(poiClass);
		if(restrictions == null) {System.err.println(("Cannot restrict poiClass "+poiClass+". No entry found.")); return null;}//throw new Error("Cannot restrict poiClass "+poiClass+". No entry found.");
		String finalRestriction;
		
		switch(restrictions.size())
		{
		case 0:		throw new Error("This should never happen (assumption that multimap.get never returns an empty collection.");
		case 1:		finalRestriction = variable+" "+restrictions.iterator().next();break;
		default:
		{
			StringBuffer combinedRestriction = new StringBuffer("{");
			for(String restriction: restrictions)
			{
				combinedRestriction.append(variable+" "+restriction+"} UNION {");
			}
			finalRestriction = combinedRestriction.substring(0,combinedRestriction.length()-8); // removing the last " UNION {"
		}
		}		
		
		return finalRestriction + " .";
		//		String lgdP = "<http://linkedgeodata.org/property/";
		//		String a = "rdf:type";
		//		String prefix2 = "";
		//		String suffix2 = "";

		//		System.out.println(poiClass.CITY.toString());
		//		switch(poiClass) {
		//		case CITY : 			return "{ " + variable + " "+ lgdP + "place> \"city\" } UNION {" + variable + " "+ lgdP + "place> \"village\" } UNION {" + variable + " "+ lgdP + "place> \"town\" } UNION {" + variable + " "+ lgdP + "place> \"suburb\" }";		
		//		//		case AIRPORT :			return variable + " "+ lgdP + "aeroway> \"aerodrome\" . ";
		//		case AIRPORT :			return variable +" "+ typeRestriction("helipad");		
		//		case UNIVERSITY :		return variable + " "+ typeRestriction("amenity_university"); // später nur university 
		//
		//		case SCHOOL :			return variable + " "+ lgdP + "amenity> \"school\" . ";
		//
		//
		//		case LAKE :				return variable + " "+ lgdP + "natural> \"water\" . ";
		//		case COUNTRY :			return variable + " "+ lgdP + "place> \"country\" . ";
		//		case RAILWAY_STATION : 	return variable + " "+ lgdP + "railway> \"station\" . ";
		//		case ISLAND : 			return variable + " "+ lgdP + "place> \"island\" . ";
		//		case STADIUM : 			return variable + " "+ lgdP + "leisure> \"stadium\" . ";
		//		case RIVER :			return variable + " "+ lgdP + "waterway> ?something . ";
		//		case BRIDGE :			return variable + " "+ lgdP + "bridge> ?something . ";				
		//		case MOUNTAIN :			return variable + " "+ lgdP + "natural> \"peak\" . ";				
		//		case RADIO_STATION :	return variable + " "+ lgdP + "amenity> \"studio\" . ";				
		//		case LIGHT_HOUSE :		return variable + " "+ lgdP + "man_made> \"lighthouse\" . ";				
		//		default: throw new Error("Cannot restrict.");
		//		}
	}
}