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

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.TagMapperInstantiator;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.impl.OSMEntityToRDFTransformer;
import org.linkedgeodata.tagmapping.client.entity.AbstractTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.IEntity;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;

import com.hp.hpl.jena.rdf.model.Model;


public class LiveRDFDeltaPlugin
	implements ChangeSink 
{
	private IUpdateStrategy updateStrategy;	


	public LiveRDFDeltaPlugin(ISparulExecutor graphDAO, String graphName)
		throws Exception
	{
		InMemoryTagMapper tagMapper = new InMemoryTagMapper();
		
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();
		
		for(Object o : session.createCriteria(AbstractTagMapperState.class).list()) {
			IOneOneTagMapper item = TagMapperInstantiator.getInstance().instantiate((IEntity)o);
			
			tagMapper.add(item);
		}
		
		tx.commit();
		
		ILGDVocab vocab = new LGDVocab();
		ITransformer<Entity, Model> entityTransformer =
			new OSMEntityToRDFTransformer(tagMapper, vocab);
		
		IUpdateStrategy updateStrategy = new IgnoreModifyDeleteDiffUpdateStrategy(
				vocab, entityTransformer, graphDAO, graphName);
		
		this.updateStrategy = updateStrategy;		
	}


	@Override
	public void complete()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void release()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void process(ChangeContainer c)
	{
		updateStrategy.update(c);
		/*
		ChangeAction action = c.getAction();
		if(action.equals(ChangeAction.Create)) {
			System.out.println("Create");
		}
		else if(action.equals(ChangeAction.Modify)) {
			System.out.println("Modify");
		}
		else if(action.equals(ChangeAction.Delete)) {
			System.out.println("Delete");			
		}
		System.out.println(    "-> " + c.getEntityContainer().getEntity());
		*/
	}
}
