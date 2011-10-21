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
package org.linkedgeodata.osm.osmosis.plugins;

import org.linkedgeodata.util.IDiff;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * This interface is more or less a duplicate of the osmosis ChangeSink
 * interface.
 * 
 * Maybe it should be removed.
 * 
 * @author raven
 *
 */
public interface IUpdateStrategy
	extends ChangeSink
{
	/*
	void update(ChangeContainer c);
	void complete();
	
	void release();
	*/
	
	// This method may only be called after complete()
	IDiff<Model> getMainGraphDiff();
	
	TreeSetDiff<Node> getNodeDiff();
}
