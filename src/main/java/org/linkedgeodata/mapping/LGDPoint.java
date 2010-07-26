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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * A LinkedGeoData point.
 * 
 * @author Jens Lehmann
 *
 */
public class LGDPoint extends Point {

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

	public void  
	
	/**
	 * @param name the name to set
	 */
	public void setName(double name) {
		this.name = name;
	}

	private static String typeRestriction(String type)
	{
		String lgdO = "<http://linkedgeodata.org/ontology/";
		return type;
		//return "a "+lgdO+type+"> .";
	}
	
	public static String getSPARQLRestriction(POIClass poiClass, String variable) {
		String lgdP = "<http://linkedgeodata.org/property/";
		String a = "rdf:type";
		String prefix2 = "";
		String suffix2 = "";
		
		Map<POIClass,String> classRestrictions = new HashMap<POIClass,String>(); 
		classRestrictions.put(POIClass.CITY," "+ lgdP + "place> \"city\" } UNION {" + variable + " "+ lgdP + "place> \"village\" } UNION {" + variable + " "+ lgdP + "place> \"town\" } UNION {" + variable + " "+ lgdP + "place> \"suburb\" }");
		
		//System.out.println(poiClass.CITY.toString());
		
		switch(poiClass) {
		case CITY : 			return "{ " + variable + " "+ lgdP + "place> \"city\" } UNION {" + variable + " "+ lgdP + "place> \"village\" } UNION {" + variable + " "+ lgdP + "place> \"town\" } UNION {" + variable + " "+ lgdP + "place> \"suburb\" }";		
//		case AIRPORT :			return variable + " "+ lgdP + "aeroway> \"aerodrome\" . ";
		case AIRPORT :			return variable +" "+ typeRestriction("helipad");		
		case UNIVERSITY :		return variable + " "+ typeRestriction("amenity_university"); // später nur university 

		case SCHOOL :			return variable + " "+ lgdP + "amenity> \"school\" . ";

		
		case LAKE :				return variable + " "+ lgdP + "natural> \"water\" . ";
		case COUNTRY :			return variable + " "+ lgdP + "place> \"country\" . ";
		case RAILWAY_STATION : 	return variable + " "+ lgdP + "railway> \"station\" . ";
		case ISLAND : 			return variable + " "+ lgdP + "place> \"island\" . ";
		case STADIUM : 			return variable + " "+ lgdP + "leisure> \"stadium\" . ";
		case RIVER :			return variable + " "+ lgdP + "waterway> ?something . ";
		case BRIDGE :			return variable + " "+ lgdP + "bridge> ?something . ";				
		case MOUNTAIN :			return variable + " "+ lgdP + "natural> \"peak\" . ";				
		case RADIO_STATION :	return variable + " "+ lgdP + "amenity> \"studio\" . ";				
		case LIGHT_HOUSE :		return variable + " "+ lgdP + "man_made> \"lighthouse\" . ";				
		default: throw new Error("Cannot restrict.");
		}
	}
}
// CITY
// Dresden
// Leipzig
// London