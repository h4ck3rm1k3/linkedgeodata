package org.linkedgeodata.i18n.gettext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.jtriplify.TagMapper;
import org.linkedgeodata.util.SinglePrefetchIterator;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Currently this file is not cleanup up.
 * 
 * Essentially it contains a .po file parser for the "gettext" format and an
 * exporter for Translate Wiki.
 * 
 * 
 * @author Claus Stadler
 * 
 */

class TranslateWikiUtil
{
	public static URL getOSMExportURL(String langCode)
	{
		return getExportURL("out-osm-site", langCode);
	}

	// out-osm-site
	public static URL getExportURL(String groupId, String langCode)
	{
		try {
			return new URL(
					"http://translatewiki.net/w/i.php?title=Special%3ATranslate&task=export-as-po&group="
							+ groupId + "&language=" + langCode);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}

class GetTextIterator
		extends SinglePrefetchIterator<GetTextRecord>
{
	private BufferedReader	reader;

	public GetTextIterator(BufferedReader reader)
	{
		this.reader = reader;
	}

	private static String stripDoubleQuotes(String str)
	{
		int offset = str.startsWith("\"") ? 1 : 0;
		int deltaLen = str.endsWith("\"") ? 1 : 0;

		String result = str.substring(offset, str.length() - deltaLen);
		return result;
	}

	@Override
	protected GetTextRecord prefetch() throws Exception
	{
		GetTextRecord record = new GetTextRecord();

		String line = "";
		while ((line = reader.readLine()) != null) {
			if (line.trim().isEmpty()) {
				if (record.isEmpty() && record.getPlainValues().isEmpty())
					continue;

				return record;
			}

			if (line.startsWith(GetTextRecord.Msg.COMMENT.getValue()))
				continue;

			for (GetTextRecord.Msg msg : GetTextRecord.Msg.values()) {
				if (line.startsWith(msg.getValue())) {
					String sub = line.substring(msg.getValue().length()).trim();

					sub = stripDoubleQuotes(sub);

					record.put(msg, sub);
					continue;
				}
			}

			record.getPlainValues().add(stripDoubleQuotes(line));
		}

		return finish();
	}

}

class GetTextRecord
		extends HashMap<GetTextRecord.Msg, String>
{
	private List<String>	plainValues	= new ArrayList<String>();

	public List<String> getPlainValues()
	{
		return plainValues;
	}

	@Override
	public String get(Object msg)
	{
		String result = super.get(msg);

		return result == null ? "" : result;
	}

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7884124527648846411L;

	enum Msg
	{
		COMMENT("#"), MSGCTXT("msgctxt"), MSGID("msgid"), MSGSTR("msgstr");

		private String	value;

		Msg(String value)
		{
			this.value = value;
		}

		public String getValue()
		{
			return value;
		}
	}
}

interface IEntityResolver
{
	public String resolve(String key, String value);
}

class EntityResolver2
	implements IEntityResolver
{
	private TagMapper tagMapper;
	
	public EntityResolver2(TagMapper tagMapper)
	{
		this.tagMapper = tagMapper;
	}
	
	@Override
	public String resolve(String key, String value)
	{
		Model model = tagMapper.map("http://ex.org", new Tag(key, value), null);
	
		Iterator<Statement> it = model.listStatements();

		while(it.hasNext()) {
			Statement stmt = it.next();
			
			if(!stmt.getPredicate().equals(RDF.type))
				continue;
			
			String classRes = stmt.getObject().asNode().getURI();
			
			return classRes;			
		}
		
		return null;
	}
	
}

class EntityResolverImpl
		implements IEntityResolver
{
	private static final Logger logger = Logger.getLogger(EntityResolverImpl.class);
	
	private Map<List<String>, URI>	map	= new HashMap<List<String>, URI>();

	public EntityResolverImpl() throws Exception
	{
		URL url = new URL("http://linkedgeodata.org/vocabulary/core");
		InputStream in = null;
		try {
			in = url.openStream();

			Model model = ModelFactory.createDefaultModel();
			model.read(in, "", "N-TRIPLE");

			process(model);
		} finally {
			if (in != null)
				in.close();
		}
	}

	private void process(Model model)
	{
		Property subClassOf = model.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf");
		Iterator<Statement> it = model.listStatements((Resource)null, subClassOf, (RDFNode)null);
	
		int prefixLength = "http://linkedgeodata.org/vocabulary#".length();
		
		while(it.hasNext()) {
			Statement stmt = it.next();
			
			URI uri = URI.create(stmt.getSubject().getURI());
			
			String v = stmt.getSubject().getURI().substring(prefixLength);
			String k = stmt.getObject().asNode().getURI().substring(prefixLength);
			
			k = k.trim().toLowerCase();
			v = v.trim().toLowerCase();
			
			List<String> list = new ArrayList<String>();
			list.add(k);
			list.add(v);
			
			logger.trace("Loaded Mapping: (" + k + ", " + v + ") -> " + uri); 
			
			map.put(list, uri);
		}
	}

	@Override
	public String resolve(String key, String value)
	{		
		String k = key.trim().toLowerCase();
		String v = value.trim().toLowerCase();
		
		List<String> list = new ArrayList<String>();
		list.add(k);
		list.add(v);
		
		URI result = map.get(list);

		return result.toString();
	}
}


/**
 * Ok, simply loading the po.file seems to be out of scope of the
 * gettext-commons library sigh...
 * http://code.google.com/p/gettext-commons/wiki/Tutorial
 * 
 * @author raven
 * 
 */
public class TranslateWikiExporter
{
	private static Logger	logger	= Logger
											.getLogger(TranslateWikiExporter.class);

	public static void validateNTriple(InputStream in) throws Exception
	{
		Model model = ModelFactory.createDefaultModel();
		model.read(in, "", "N-TRIPLE");

		Iterator<Statement> it = model.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.next();

			System.out.println(stmt.toString());
		}
	}

	/*
	 * private static void validate(InputStream in) { URL url = newURL(
	 * "file:///home/raven/EclipseProjects/LinkedGeoDataTagLanguageExtraction/en.n3"
	 * ); InputStream in = url.openStream(); validateNTriple(in);
	 * System.exit(0); }
	 */
	public static final String	prefix	= "geocoder.search_osm_nominatim.prefix.";

	public static void main(String[] args) throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
	
		logger.info("Loading tag mappings");
		TagMapper tagMapper = new TagMapper();
		tagMapper.load(new File("LGDMappingRules.xml"));
		
		logger.info("Initializing EntityResolver");
		IEntityResolver resolver = new EntityResolver2(tagMapper);

		logger.info("Starting export");
		//export("de", false, null, resolver);
		
		String[] langs = {"de", "es", "ru", "ja", "it", "fr", "ar"};
		for(String lang : langs) {
			logger.info("Processing language: " + lang);
			export(lang, false, null, resolver);
		}

		export("de", true, "en", resolver);
	}

	public static void export(String initLangCode, boolean idMode,
			String overrideLangCode, IEntityResolver resolver)
		throws Exception
	{		
		logger.info("Processing: " + initLangCode);

		URL source = TranslateWikiUtil.getOSMExportURL(initLangCode);

		logger.debug("Source URL: " + source);
		/**
		 * In english mode, 'msgid' will be taken instead of 'msgstr'. This is
		 * because the source language is english, and therefore the ids are
		 * already the english terms.
		 * 
		 */

		String langCode = overrideLangCode;
		// if(idMode)
		// langCode = initLangCode;

		InputStream in = source.openStream();

		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		GetTextIterator it = new GetTextIterator(reader);

		if (!it.hasNext()) {
			throw new RuntimeException("No header detected");
		}

		GetTextRecord header = it.next();

		if (langCode == null)
			langCode = detectLangCode(header);

		String targetFileName = langCode + ".nt";

		File targetFile = new File(targetFileName);

		logger.info("Using language: " + langCode);
		logger.info("IdMode: " + idMode);
		logger.info("TargetFileName: " + targetFileName);

		if (langCode == null)
			throw new RuntimeException("Language code not detected");

		Model model = extractModel(it, idMode, langCode, resolver);

		OutputStream out = new FileOutputStream(targetFile);
		writeModel(model, out);

		out.close();
	}

	private static String detectLangCode(GetTextRecord record)
	{
		for (String item : record.getPlainValues()) {
			item = item.trim();

			if (!item.startsWith("X-Language-Code"))
				continue;

			String[] kv = item.split(":", 2);
			String v = kv[1];
			v = v.replace("\\n", "\n");

			String result = v.trim().toLowerCase();
			return result;
		}

		return null;
	}

	public static void writeModel(Model model, OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);

		writer
				.println("# Data following this comment was generated from data at");
		writer
				.println("# <http://translatewiki.net/wiki/Translating:OpenStreetMap>");
		writer.println("# Generation time: " + new Date());
		writer.println();
		model.write(writer, "N-TRIPLE");

		writer.println();
		writer
				.println("# Data preceeding this comment was generated from data at");
		writer
				.println("# <http://translatewiki.net/wiki/Translating:OpenStreetMap>");
		writer.close();
	}

	public static Model extractModel(GetTextIterator it, boolean idMode,
			String langCode, IEntityResolver resolver)
	{
		// File file = new
		// File("data/i18n/translate-wiki-osm/ru_out-osm-site.po");

		// BufferedReader reader = new BufferedReader(new FileReader(file));

		Model model = ModelFactory.createDefaultModel();

		// com.hp.hpl.jena.vocabulary.RDFS.label;

		while (it.hasNext()) {
			GetTextRecord record = it.next();

			if (!record.get(GetTextRecord.Msg.MSGCTXT).startsWith(prefix))
				continue;

			String entry = record.get(GetTextRecord.Msg.MSGCTXT).substring(
					prefix.length());

			String label = idMode ? record.get(GetTextRecord.Msg.MSGID)
					: record.get(GetTextRecord.Msg.MSGSTR);
			label = label.trim();
			if (label.isEmpty())
				continue;

			Literal literal = model.createLiteral(label, langCode);

			String[] kv = entry.split("\\.");
			if (kv.length == 2) {
				String key = kv[0];
				String value = kv[1]; //.trim();

				String uri = resolver.resolve(key, value);
				if(uri == null) {
					logger.warn("Skipping: (" + key + ", " + value + ")");
					continue;
				}
				logger.trace("Mapped: " + key + ", " + value + ") -> " + uri);
				
				Resource subject = model.createResource(uri.toString());

				model.add(subject, RDFS.label, literal);
			}
		}

		return model;
	}

	/**
	 * msgctxt "browse.way.way_title" msgid "Way: {{way_name}}" msgstr
	 * "Линия: {{way_name}}" /
	 * 
	 * Map<Msg, String> record = new HashMap<Msg, String>(); String line = "";
	 * while((line = reader.readLine()) != null) {
	 * 
	 * if(line.startsWith(Msg.MSGSTART.getValue())) { record.clear(); }
	 * 
	 * for(Msg msg : Msg.values()) { if(line.startsWith(msg.getValue())) {
	 * String sub = line.substring(msg.getValue().length()).trim();
	 * if(sub.length() < 2) continue;
	 * 
	 * sub = sub.substring(1, sub.length() - 1);
	 * 
	 * record.put(msg, sub); } }
	 * 
	 * if(record.size() >= 3) { //System.out.println(record.get(Msg.MSGID));
	 * 
	 * final String prefix = "geocoder.search_osm_nominatim.prefix.";
	 * if(record.get(Msg.MSGCTXT).startsWith(prefix)) { String tmp =
	 * record.get(Msg.MSGCTXT).substring(prefix.length());
	 * 
	 * String label = englishMode ? record.get(Msg.MSGID) :
	 * record.get(Msg.MSGSTR); // TODO Determine source language on start String
	 * langCode = englishMode ? "en" : "ru";
	 * 
	 * Literal literal = model.createLiteral(label, langCode);
	 * 
	 * String[] kv = tmp.split("\\."); if(kv.length == 2) { String key = kv[0];
	 * String value = kv[1];
	 * 
	 * 
	 * Resource subject =
	 * model.createResource("http://linkedgeodata.org/vocabulary#" + value);
	 * 
	 * model.add(subject, RDFS.label, literal); } } record.clear(); } } /
	 * reader.close();
	 */
}
