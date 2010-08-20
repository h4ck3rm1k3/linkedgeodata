package org.linkedgeodata.osm.osmosis.plugins;

import java.sql.Connection;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.TagMapperInstantiator;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.impl.OSMEntityToRDFTransformer;
import org.linkedgeodata.tagmapping.client.entity.AbstractSimpleTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.AbstractTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.IEntity;
import org.linkedgeodata.util.Diff;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.VirtuosoUtils;
import org.linkedgeodata.util.sparql.ISparqlExecutor;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.linkedgeodata.util.sparql.VirtuosoJdbcSparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;




public class LiveRDFDeltaPlugin
	implements ChangeSink 
{
	private IUpdateStrategy updateStrategy;	


	public LiveRDFDeltaPlugin(String fileName)
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

		String graphName = "http://example.org";
		
		Connection conn = VirtuosoUtils.connect("localhost", "dba", "dba");

		ISparulExecutor graphDAO =
			new VirtuosoJdbcSparulExecutor(conn, graphName);
		
		IUpdateStrategy updateStrategy = new IgnoreModifyDeleteDiffUpdateStrategy(
				vocab, entityTransformer, graphDAO, graphName);
		
		this.updateStrategy = updateStrategy;
		
		System.out.println("Constructing " + this.getClass() + ", arg=" + fileName);
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
