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

	/**
	 * @param name the name to set
	 */
	public void setName(double name) {
		this.name = name;
	}

	public static String getSPARQLRestriction(POIClass poiClass, String variable) {
		String prefix = "<http://linkedgeodata.org/property/";
		String prefix2 = "";
		String suffix2 = "";
		switch(poiClass) {
		case CITY : 			return "{ " + variable + " "+ prefix + "place> \"city\" } UNION {" + variable + " "+ prefix + "place> \"village\" } UNION {" + variable + " "+ prefix + "place> \"town\" } UNION {" + variable + " "+ prefix + "place> \"suburb\" }";
		case UNIVERSITY :		return variable + " "+ prefix + "amenity"+"> "+"\"university\" . ";
		case SCHOOL :			return variable + " "+ prefix + "amenity> \"school\" . ";
		case AIRPORT :			return variable + " "+ prefix + "aeroway> \"aerodrome\" . ";
		case LAKE :				return variable + " "+ prefix + "natural> \"water\" . ";
		case COUNTRY :			return variable + " "+ prefix + "place> \"country\" . ";
		case RAILWAY_STATION : 	return variable + " "+ prefix + "railway> \"station\" . ";
		case ISLAND : 			return variable + " "+ prefix + "place> \"island\" . ";
		case STADIUM : 			return variable + " "+ prefix + "leisure> \"stadium\" . ";
		case RIVER :			return variable + " "+ prefix + "waterway> ?something . ";
		case BRIDGE :			return variable + " "+ prefix + "bridge> ?something . ";				
		case MOUNTAIN :			return variable + " "+ prefix + "natural> \"peak\" . ";				
		case RADIO_STATION :	return variable + " "+ prefix + "amenity> \"studio\" . ";				
		case LIGHT_HOUSE :		return variable + " "+ prefix + "man_made> \"lighthouse\" . ";				
		default: throw new Error("Cannot restrict.");
		}
	}
}
