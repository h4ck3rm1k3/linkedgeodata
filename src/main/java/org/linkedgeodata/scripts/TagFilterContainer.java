package org.linkedgeodata.scripts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.linkedgeodata.jtriplify.methods.Pair;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;


/**
 * An attempt for allowing filtering of tags with SQL queries using a hsqldb
 * 
 * @author raven
 *
 */
public class TagFilterContainer
{
	private Connection conn;
	private String tableName;
	
	private String entityFilterStr;
	private String tagFilterStr;
	
	//Map<Integer, Map<Integer, Entity>> typeToIdToEntity = new HashMap<Integer, Map<Integer, Entity>>();
	//Map<Pair<Byte, Long>, Entity> typeIdToEntity = new HashMap<Pair<Byte, Long>, Entity>();
	
	public TagFilterContainer(String tableName, String entityFilterStr, String tagFilterStr)
		throws SQLException
	{
		this.tableName = tableName;
		this.entityFilterStr = entityFilterStr;
		this.tagFilterStr = tagFilterStr;
		
		conn = DriverManager.getConnection("jdbc:hsqldb:mem:aname", "sa", "");
		
		String sql = "CREATE TABLE " + tableName + " (type TINYINT, id BIGINT, k VARCHAR(32767), v VARCHAR(32767))";
		SQLUtil.execute(conn, sql, null);
		
		sql = "CREATE INDEX idx" + tableName + " ON " + tableName + "(k, v)";
		SQLUtil.execute(conn, sql, null);
	}
	
	
	static String toString(Iterable<? extends Entity> entities)
	{
		String result = "";
		for(Entity entity : entities) {
			result += toString(entity) + "  ";
		}
		
		return result;
	}
	
	static String toString(Entity entity)
	{
		return entity.getClass().getSimpleName() + ":" + StringUtil.implode(", ", entity.getTags());
	}
	
	public static void main(String[] args)
		throws Exception
	{
		/*
		TagFilterContainer x = new TagFilterContainer(
				"tags",
				"(filter.k IN ('highway', 'barrier', 'power') OR (filter.k = 'railway' AND filter.v NOT IN ('station')))" ,
				"k NOT IN ('created_by','ele','time','layer','source','tiger:tlid','tiger:county','tiger:upload_uuid','attribution','source_ref','KSJ2:coordinate','KSJ2:lat','KSJ2:long','KSJ2:curve_id','AND_nodes','converted_by','TMC:cid_58:tabcd_1:LocationCode','TMC:cid_58:tabcd_1:LCLversion','TMC:cid_58:tabcd_1:NextLocationCode','TMC:cid_58:tabcd_1:PrevLocationCode','TMC:cid_58:tabcd_1:LocationCode', 'TMC:cid_58:tabcd_1:Class', 'TMC:cid_58:tabcd_1:Direction')");
		*/
		TagFilterContainer x = new TagFilterContainer(
				"tags",
				null, //"(filter.k IN ('highway', 'barrier', 'power') OR (filter.k = 'railway' AND filter.v NOT IN ('station')))" ,
				null //"k NOT IN ('created_by','ele','time','layer','source','tiger:tlid','tiger:county','tiger:upload_uuid','attribution','source_ref','KSJ2:coordinate','KSJ2:lat','KSJ2:long','KSJ2:curve_id','AND_nodes','converted_by','TMC:cid_58:tabcd_1:LocationCode','TMC:cid_58:tabcd_1:LCLversion','TMC:cid_58:tabcd_1:NextLocationCode','TMC:cid_58:tabcd_1:PrevLocationCode','TMC:cid_58:tabcd_1:LocationCode', 'TMC:cid_58:tabcd_1:Class', 'TMC:cid_58:tabcd_1:Direction')"
				);

		
		Collection<Entity> entities = new ArrayList<Entity>();
		
		for(int i = 0; i < 1000; ++i) {
			
			Collection<Tag> tags = new ArrayList<Tag>();
			tags.add(new Tag("amenity", "school" + i));
			tags.add(new Tag("name", "Meine Schule"));
			tags.add(new Tag("created_by", "blah" + i));
			tags.add(new Tag("highway", "blah"));
			
			CommonEntityData ced = new CommonEntityData(i, 1, new Date(), new OsmUser(10, "user"), 0, tags);
			Node node = new Node(ced, 51.2f, 10.1f);
	
			
			entities.add(node);
		}
			//x.insert();
		//Iterable<? extends Entity> entities = Collections.singleton(node); 
		
		//System.out.println("BEFORE: " + toString(entities));
		
		long start = System.nanoTime();
		x.filterTags(entities);
		long end = System.nanoTime();

		//System.out.println("AFTER: " + toString(entities));

		
		System.out.println("Taken = " + (end - start) / 1000000000.0);

	}
	
	public void clear() throws SQLException
	{
		SQLUtil.execute(conn, "DELETE FROM " + tableName, null);
	}
	
	public void filterTags(Iterable<? extends Entity> entities)
		throws SQLException
	{
		insert(entities);

		String sql = createQuery(entityFilterStr, tagFilterStr);
		
		Map<Byte, Map<Long, Entity>> map = new HashMap<Byte, Map<Long, Entity>>();
		
		for(Entity entity : entities) {
			Map<Long, Entity> tmp = map.get(getType(entity));
			if(tmp == null) {
				tmp = new HashMap<Long, Entity>();
				map.put(getType(entity), tmp);
			}
			
			tmp.put(entity.getId(), entity);
		}
		
		/*
		HashMap<Pair<Byte, Long>, Entity> map = 
		for(Entity entity : entities) {
			entity.getTags().clear();
			map.put(new Pair<Byte, Long>(getType(entity), entity.getId()), entity);
		}*/
		
		
		//System.out.println(sql);
		ResultSet rs = SQLUtil.executeCore(conn, sql);
		
		try {
			while(rs.next()) {
				byte type = rs.getByte("type");
				long id = rs.getLong("id");
				String k = rs.getString("k");
				String v = rs.getString("v");
				
				//Pair<Byte, Long> key = new Pair<Byte, Long>(type, id);
				
				Entity e = map.get(type).get(id);
				e.getTags().add(new Tag(k, v));
				
			}
		} finally {
			rs.close();
		}
		
		clear();
	}
	
	private String createQuery(String entityFilterStr, String tagFilterStr)
	{
		/*
		String entityFilter = StringUtil.coalesce(entityFilterStr, "").trim().isEmpty()
			? "SELECT id, k, v FROM " + tableName + " WHERE TRUE "
			: "SELECT id, k, v FROM " + tableName + " WHERE id IN (SELECT DISTINCT t.id FROM " + tableName + " t WHERE t.id NOT IN (SELECT filter.id FROM " + tableName + " filter WHERE filter.id = t.id AND " + entityFilterStr + ")) "; 		
		*/
		List<String> constraints = new ArrayList<String>();
		
		if(!StringUtil.coalesce(entityFilterStr, "").trim().isEmpty()) {
			constraints.add("(type, id) IN (SELECT DISTINCT t.type, t.id FROM " + tableName + " t WHERE t.id NOT IN (SELECT filter.id FROM " + tableName + " filter WHERE filter.id = t.id AND " + entityFilterStr + ")) ");
		}		
		
		if(!StringUtil.coalesce(tagFilterStr, "").trim().isEmpty()) {
			constraints.add(tagFilterStr);
		}

		String constraintsStr = constraints.isEmpty()
			? ""
			: "WHERE " + StringUtil.implode(" AND ", constraints);
		
		
		String sql = "SELECT type, id, k, v FROM " + tableName + " " + constraintsStr;  
		
		return sql;
	}
	
	private byte getType(Entity entity)
	{
		if(entity instanceof Node) {
			return 0;
		} else if(entity instanceof Way) {
			return 1;
		} else if (entity instanceof Relation) {
			return 2;
		} else {
			throw new RuntimeException("Should not happen");
		}
	}
	
	void insert(Iterable<? extends Entity> entities)
		throws SQLException
	{	
		List<String> values = new ArrayList<String>();
		for(Entity entity : entities) {
			for(Tag tag : entity.getTags()) {
				String value = "(" + getType(entity) + "," + entity.getId() + ",'" + SQLUtil.escapePostgres(tag.getKey()) + "','" + SQLUtil.escapePostgres(tag.getValue()) + "')";
				values.add(value);
			}
		}
		
		if(values.isEmpty())
			return;
		
		String sql = "INSERT INTO tags(type, id, k, v) VALUES (" + StringUtil.implode(",", values) + ")"; 

		//System.out.println(sql);
		SQLUtil.execute(conn, sql, null);
	}
}
