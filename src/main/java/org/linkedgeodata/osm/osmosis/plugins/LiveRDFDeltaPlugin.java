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

import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;
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
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.sparql.ISparqlExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;


/**
 * This plugin writes out an RDF delta based on transforming incoming osm-changes
 * to RDF and comparing them to a store.
 * 
 * @author raven
 *
 */
public class LiveRDFDeltaPlugin
	implements ChangeSink 
{
	private static final Logger logger = LoggerFactory.getLogger(LiveRDFDeltaPlugin.class);
	
	private IUpdateStrategy updateStrategy;	

	private RDFDiffWriter rdfDiffWriter;
	
	public LiveRDFDeltaPlugin(ISparqlExecutor graphDAO, String graphName, RDFDiffWriter rdfDiffWriter)
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
		
		//File diffRepo = new File("/tmp/lgddiff");
		//diffRepo.mkdirs();
		
		//RDFDiffWriter rdfDiffWriter = new RDFDiffWriter(diffRepo, 0);
		
		ILGDVocab vocab = new LGDVocab();
		ITransformer<Entity, Model> entityTransformer =
			new OSMEntityToRDFTransformer(tagMapper, vocab);
		
		IUpdateStrategy updateStrategy = new IgnoreModifyDeleteDiffUpdateStrategy(
				vocab, entityTransformer, graphDAO, graphName, null);
		
		this.updateStrategy = updateStrategy;		
	}


	@Override
	public void complete()
	{
		this.updateStrategy.complete();

		IDiff<Model> diff = updateStrategy.getMainGraphDiff();
		logger.info("Diff(triples added - deleted) = " + diff.getAdded().size() + " - " + diff.getRemoved().size());
		
		try {
			rdfDiffWriter.write(diff);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getFullStackTrace(e));
		}
		
	}

	@Override
	public void release()
	{
		updateStrategy.release();
	}

	@Override
	public void process(ChangeContainer c)
	{
		updateStrategy.process(c);
	}
}
