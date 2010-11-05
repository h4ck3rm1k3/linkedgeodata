package org.linkedgeodata.util.sparql.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.map.LRUMap;
import org.linkedgeodata.util.collections.CacheSet;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;


class TripleCacheIndexImpl
		implements ITripleCacheIndex
{
	private IGraph								graph;

	/**
	 * Enables/Disables tracking of incomplete partitions. Incomplete partitions
	 * can be used for answering queries for existence, however, they cannot be
	 * iterated without fetching all data (completing them)first. Therefore, if
	 * existence checks (such is there a link between :s ?p :o) are not
	 * required, disable this tracking.
	 * 
	 * 
	 */
	private boolean								trackIncompletePartitions	= true;

	/**
	 * Maps a pattern (e.g. s, null, null)
	 * 
	 */
	private LRUMap<List<Object>, IndexTable>	keyToValues					= new LRUMap<List<Object>, IndexTable>();
	private CacheSet<List<Object>>				noDataCache					= new CacheSet<List<Object>>();

	private int[]								indexColumns;
	private int[]								valueColumns;

	public int[] getIndexColumns()
	{
		return indexColumns;
	}

	
	public static void create(IGraph graph, int...indexColumns) throws Exception {
		TripleCacheIndexImpl index = new TripleCacheIndexImpl(graph, indexColumns);

		graph.getCacheProvider().getIndexes().add(index);
	}
	
		
	/**
	 * Index columns: 0: subject 1: predicate 2: object
	 * 
	 * @param filter
	 * @param indexColumns
	 * @throws Exception
	 */
	private TripleCacheIndexImpl(IGraph graph, int[] indexColumns)
			throws Exception
	{
		this.graph = graph;
		this.indexColumns = indexColumns;

		valueColumns = new int[3 - indexColumns.length];


		
		Set<Integer> cs = new HashSet<Integer>();
		for(int index : indexColumns)
			cs.add(index);
		
		Arrays.asList(0, 1, 2).remove(cs);
		
		int j = 0;
		for (int i = 0; i < 3; ++i) {
			if (cs.contains(i)) {
				continue;
			}

			valueColumns[j++] = i;
		}

		// Delta deltaGraph = new Delta(baseGraph);
	}

	public static RDFNode getItemAt(Statement stmt, int index)
	{
		switch (index) {
		case 0:
			return stmt.getSubject();
		case 1:
			return stmt.getPredicate();
		case 2:
			return stmt.getObject();
		default:
			throw new IndexOutOfBoundsException();
		}
	}

	/*
	 * public static void (Statement stmt, int index, Object value) {
	 * switch(index) { case 0: { stmt. case 1: return stmt.getPredicate(); case
	 * 2: return stmt.getObject(); default: throw new
	 * IndexOutOfBoundsException(); } }
	 */

	public List<Object> extractKey(Statement stmt)
	{
		Object[] keyTmp = new Object[indexColumns.length];
		for (int i = 0; i < indexColumns.length; ++i) {
			keyTmp[i] = getItemAt(stmt, indexColumns[i]);
		}
		return Arrays.asList(keyTmp);
	}


	public static <T> IndexTable getOrCreate(Map<List<T>, IndexTable> map,
			List<T> key)
	{
		IndexTable result = map.get(key);
		if (result == null) {
			result = new IndexTable();
			map.put(key, result);
		}
		return result;
	}

	public static <T> IndexTable getOrCreate(LRUMap<List<? super T>, IndexTable> map,
			List<T> key)
	{
		IndexTable result = map.get(key);
		if (result == null) {
			result = new IndexTable();
			map.put(key, result);
		}
		return result;
	}

	/*
	public Model getModel(List<List<Object>> keys) throws Exception
	{
		Model result = ModelFactory.createDefaultModel();

		Map<List<Object>, IndexTable> map = get(keys);

		for (Map.Entry<List<Object>, IndexTable> entry : map.entrySet()) {

			Object[] objects = new Object[3];
			for (int i = 0; i < indexColumns.length; ++i) {
				objects[indexColumns[i]] = entry.getKey().get(i);
			}

			for (List<Object> row : entry.getValue().getRows()) {
				for (int i = 0; i < valueColumns.length; ++i) {
					objects[valueColumns[i]] = row.get(i);
				}

				result.add((Resource) objects[0], (Property) objects[1],
						(RDFNode) objects[2]);
			}

		}

		return result;
	}
	*/

	
	public static <T> void fill(T[] array, Iterable<T> items, int[] map)
	{
		Iterator<T> it = items.iterator();
		
		for(int i = 0; i < map.length; ++i) {
			T item = it.next();
			array[map[i]] = item;
		}
	}
	
	
	public static Triple toTriple(Object[] array) {
		return new Triple((Node)array[0], (Node)array[1], (Node)array[2]);
	}
	
	/**
	 * lookup: returns a possible empty set of triples
	 * or null - if no 
	 * 
	 * Does not track cache misses.
	 * 
	 * @param key
	 * @return
	 */
	@Override
	public Collection<Triple> lookup(List<Object> key) {
		
		
		IndexTable table = keyToValues.get(key);
		if (table == null) {
			// Potential cache miss
			if (noDataCache.contains(key)) {
				return Collections.emptySet();
			}
		} else {
			// Cache hit
			Collection<Triple> result = new HashSet<Triple>();
			for(List<Object> value : table.getRows()) {
				Object[] array = new Object[3];
				
				fill(array, key, indexColumns);
				fill(array, value, valueColumns);
				
				Triple triple = toTriple(array);
				result.add(triple);
			}
			return result;
		}

		return null;
	}
	
	/*
	public Map<List<Object>, IndexTable> get(List<List<Object>> keys)
			throws Exception
	{

		Map<List<?>, IndexTable> result = new HashMap<List<?>, IndexTable>();

		List<List<?>> unresolveds = new ArrayList<List<?>>();
		for (List<?> key : keys) {

			IndexTable table = keyToValues.get(key);
			if (table == null || !table.isComplete()) {
				// Potential cache miss
				if (noDataCache.contains(key)) {
					continue;
				}

				unresolveds.add(key);
			} else {
				// Cache hit

				IndexTable resultTable = getOrCreate(result, key);
				resultTable.getRows().addAll(table.getRows());
			}
		}

		// Ask the underlying cache - none of the index business
		Model model = cache.construct(keys, indexColumns);

		index(model, true);

		// Perform a lookup for all unresolved resources
		// listStatements(map(key);
		return null;
	}

	private void index(Model model, boolean isComplete)
	{
		for (Statement stmt : new ModelSetView(model)) {
			List<Object> key = new ArrayList<Object>();

			List<Object> value = new ArrayList<Object>();

			for (int index : indexColumns) {
				key.add(getItemAt(stmt, index));
			}

			for (int index : valueColumns) {
				value.add(getItemAt(stmt, index));
			}

			// keyToValues.get(key);
			// IndexTable table = getOrCreate(keyToValues, key);

			IndexTable table = keyToValues.get(key);
			if (table == null) {
				table = new IndexTable(isComplete);
				keyToValues.put(key, table);
			}

			table.getRows().add(value);
		}
	}
	*/

	@Override
	public IGraph getGraph()
	{
		return graph;
	}

	@Override
	public IndexCompatibilityLevel getCompatibilityLevel(Triple pattern)
	{
		int count = 0;
		for (int indexColumn : indexColumns) {
			Node node = TripleUtils.get(pattern, indexColumn);

			count += (node != null) ? 1 : 0;
		}

		if (count == 0) {
			return IndexCompatibilityLevel.NONE;
		} else if (count == indexColumns.length) {
			return IndexCompatibilityLevel.FULL;
		} else {
			return IndexCompatibilityLevel.PARTIAL;
		}
	}

	/*
	 * // pattern (s, null, o) -> 101 private static int patternToMask(Triple
	 * triple) { int result = 0;
	 * 
	 * for(int i = 0; i < 3; ++i) { result |= ((TripleUtils.get(triple, 0) !=
	 * null) ? 1 : 0) << i; }
	 * 
	 * return result; }
	 * 
	 * // index (s) -> 001 // index (so) -> 101 private static int
	 * columnsToMask(int[] columnIds) { int result = 0;
	 * 
	 * for(int id : columnIds) { result |= 1 << id; }
	 * 
	 * return result; }
	 */
	private List<Object> tripleToKey(Triple triple)
	{
		return tripleToList(triple, indexColumns);
	}

	private List<Object> tripleToValue(Triple triple)
	{
		return tripleToList(triple, valueColumns);
	}

	private List<Object> tripleToList(Triple triple, int[] indexes)
	{
		Object[] array = new Object[indexes.length];
		for (int i = 0; i < array.length; ++i) {
			array[i] = TripleUtils.get(triple, indexes[i]);
		}

		List<Object> result = Arrays.asList(array);
		return result;
	}


	/**
	 * Note: When indexing too many triples, the LRU map might be full.
	 * We therefore 
	 * 
	 * 
	 */
	@Override
	public void index(Collection<Triple> triples)
	{
		Map<List<Object>, IndexTable> tmp = new HashMap<List<Object>, IndexTable>();
		
		for (Triple triple : triples) {
			List<Object> key = tripleToKey(triple);
			List<Object> value = tripleToValue(triple);

			IndexTable table = getOrCreate(tmp, key);

			// FIXME If the table is complete, can an incomplete insertion
			// make the table incomplete?! I think no.
			// Incomplete only means that there might be triples which have
			// not yet been by the cache. However, if the table is complete,
			// the cache already knows all triples. The triples that are being
			// inserted are additional triples for indexing.
			// NOTE Incomplete does not mean that there is a lack of information
			// e.g. triples not sent to the cache, but merely that not all
			// triples for a partition have been fetched yet.
			table.setComplete(true);

			table.getRows().add(value);
		}
		
		
		for(Map.Entry<List<Object>, IndexTable> entry : tmp.entrySet()) {
			keyToValues.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void removeSeen(Collection<Triple> triples)
	{
		for (Triple triple : triples) {
			List<Object> key = tripleToKey(triple);
			List<Object> value = tripleToValue(triple);

			IndexTable table = keyToValues.get(key);
			if (table == null)
				continue;

			// TODO We assume that the rows in the table are unique
			// but maybe this is the best way to do it in the first place
			table.getRows().remove(value);
		}
	}

	@Override
	public int[] getKeyColumns()
	{
		return indexColumns;
	}

	@Override
	public int[] getValueColumns()
	{
		return valueColumns;
	}

	@Override
	public IndexCompatibilityLevel getCompatibilityLevel(int[] columnIds)
	{
		Set<Integer> cs = new HashSet<Integer>();
		for(int id : columnIds)
			cs.add(id);
		
		int count = 0;
		for (int indexColumn : indexColumns) {
			count += cs.contains(indexColumn) ? 1 : 0;
		}

		if (count == 0) {
			return IndexCompatibilityLevel.NONE;
		} else if (count == indexColumns.length) {
			return IndexCompatibilityLevel.FULL;
		} else {
			return IndexCompatibilityLevel.PARTIAL;
		}

	}

	@Override
	public void addSeen(Collection<Triple> triples)
	{
		for (Triple triple : triples) {
			List<Object> key = tripleToKey(triple);
			List<Object> value = tripleToValue(triple);

			IndexTable table = keyToValues.get(key);
			if (table == null)
				continue;

			// TODO We assume that the rows in the table are unique
			// but maybe this is the best way to do it in the first place
			table.getRows().add(value);
		}
	}

	@Override
	public void clear()
	{
		keyToValues.clear();
	}

	/**
	 * Using ask it is possible to check for the existence of entries In this
	 * case incomplete cache entries do not matter
	 * 
	 * @param keys
	 * @return public Map<List<Object>, Boolean> ask(Collection<List<Object>>
	 *         keys) { }
	 */
}
