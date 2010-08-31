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

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;
import org.linkedgeodata.util.ConnectionConfig;
import org.linkedgeodata.util.VirtuosoUtils;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.linkedgeodata.util.sparql.VirtuosoJdbcSparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.XmlChangeReader;


public class IgnoreModifyDeleteDiffUpdateStrategyTest
{
	/***
	 * NOTE Appearantly there is type magic going on with virtuoso and RDF
	 * datatypes: When inserting a value of 10.0^^xsd:decimal it may come
	 * back as 10^^xsd:integer. According to the xml spec
	 * (http://www.w3.org/TR/xmlschema-2/)
	 * integer is a subclass of decimal, so semantically this behaviour is valid.
	 * However if we rely on comparisions on lexical level, things might go
	 * wrong.
	 * Specifically in our case it is rather harmless, the only effect of this
	 * type magic is, that in such cases unneccessary update statements are
	 * being fired.
	 * 
	 * 
	 * @throws Exception
	 */
	@Test
	public void nodeTest()
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
		
		Collection<Tag> tags = new ArrayList<Tag>();
		tags.add(new Tag("amenity", "school"));
		tags.add(new Tag("name", "Meine Schule"));
		
		CommonEntityData ced = new CommonEntityData(666, 1, new Date(), new OsmUser(10, "user"), 0, tags);
		Node node = new Node(ced, 51.2f, 10.1f);
		

		ConnectionConfig cConfig = new ConnectionConfig(
				"localhost",
				"http://test.org",
				"dba",
				"dba");
		
		Connection conn = VirtuosoUtils.connect(
				cConfig.getHostName(),
				cConfig.getUserName(),
				cConfig.getPassWord());

		ISparulExecutor graphDAO =
			new VirtuosoJdbcSparulExecutor(conn, cConfig.getDataBaseName());
		
		RDFDiffWriter rdfDiffWriter = new RDFDiffWriter("/tmp/test");

		XmlChangeReader reader = new XmlChangeReader(new File("/home/raven/Projects/Current/Eclipse/GoogleCodeLinkedGeoData/data/live/diff.osc"), true, CompressionMethod.None);
		
		LiveRDFDeltaPlugin task = new LiveRDFDeltaPlugin(graphDAO, cConfig.getDataBaseName(), rdfDiffWriter);

		reader.setChangeSink(task);
		
		reader.run();

		
		//graphDAO.insert(
		
		//IUpdateStrategy updateStrategy = new IgnoreModifyDeleteDiffUpdateStrategy(vocab, entityTransformer, graphDAO, graphName)
		//LiveRDFDeltaPlugin pluginFac
		//LiveRDFDeltaPlugin plugin = new LiveRDFDeltaPlugin();

		//EntityContainer entityContainer = new NodeContainer(node);
		//hangeContainer changeContainer = new ChangeContainer(entityContainer, ChangeAction.Modify);

		//task.process(changeContainer);
		/*
		plugin.process(changeContainer);
		plugin.complete();
		plugin.release();
		*/
	}

}
