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
 * Takes a DBpedia resource as input and outputs the most likely
 * owl:sameAs mapping in LinkedGeoData.
 * 
 * @author Jens Lehmann
 *
 */
public class SingleDBpediaMapping {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		URI uri = URI.create("http://dbpedia.org/resource/Leipzig");
		System.out.println("Trying to find a match for " + uri);
		DBpediaPoint dp = new DBpediaPoint(uri);
		URI lgdURI = DBpediaLinkedGeoData.findGeoDataMatch(dp);
		System.out.println(lgdURI);
	}

}
