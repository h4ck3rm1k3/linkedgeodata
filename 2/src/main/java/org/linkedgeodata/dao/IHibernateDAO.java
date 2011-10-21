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
package org.linkedgeodata.dao;

import org.hibernate.Session;


/**
 * Simple interface for Data Access Objects that are based on hibernate session
 * objects.
 * 
 * @author raven
 *
 */
public interface IHibernateDAO
{
	/**
	 * Set the hibernate session object to be used by the DAO.
	 * 
	 * @param session The hibernate session object
	 */
	public void setSession(Session session);
	
	/**
	 * Retrieve the hibernate session object that is currently associated with
	 * this DAO.
	 * 
	 * @return The hibernate session object currently associated with this DAO.
	 */
	public Session getSession();
}
