package org.linkedgeodata.osm.osmosis.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;


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
		
		//IUpdateStrategy updateStrategy = new IgnoreModifyDeleteDiffUpdateStrategy(vocab, entityTransformer, graphDAO, graphName)
		LiveRDFDeltaPlugin plugin = new LiveRDFDeltaPlugin("dummy");

		EntityContainer entityContainer = new NodeContainer(node);
		ChangeContainer changeContainer = new ChangeContainer(entityContainer, ChangeAction.Modify);

		plugin.process(changeContainer);
		plugin.complete();
		plugin.release();
	}

}
