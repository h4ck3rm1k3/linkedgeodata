package org.linkedgeodata.i18n.gettext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.dao.TagLabelDAO;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.util.ConnectionConfig;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.StreamUtil;

import scala.actors.threadpool.Arrays;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;


interface ITagLabelOutput
{
	void write(String k, String v, String language, String label);
}


class TagLabelOutputCsv
	implements ITagLabelOutput
{
	private PrintStream out;
	
	public TagLabelOutputCsv(PrintStream out)
	{
		this.out = out;
	}
	
	@Override
	public void write(String k, String v, String language, String label) {
		out.println(k + "\t" + v + "\t" + language + "\t" + label);
	}
}


class TagLabelOutputDao
	implements ITagLabelOutput
{
	public TagLabelOutputDao(TagLabelDAO out)
	{
		this.out = out;
	}

	private TagLabelDAO out;
	
	@Override
	public void write(String k, String v, String language, String label) {
		try {
			out.insert(k, v, language, label);
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public static List<String> retrieveLanguages() throws Exception {
		List<String> result = new ArrayList<String>();
		
		URL url = new URL("http://translatewiki.net/w/i.php?title=Special:MessageGroupStats&group=out-osm-site");
		String str = StreamUtil.toString(url.openStream());
		
		Pattern pattern = Pattern.compile(".*language=([^;\"]+).*");
		
		Matcher m = pattern.matcher(str);
		while(m.find()) {
			String lang = m.group(1);
			if(!result.contains(lang)) {
				result.add(lang);
			}
		}
		
		
		
		
		return result;
	}
	
	public static void main(String[] args) throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
	
		logger.info("Loading tag mappings");
		InMemoryTagMapper tagMapper = new InMemoryTagMapper();
		tagMapper.load(new File("data/triplify/config/2.0/LGDMappingRules.2.0.xml"));
		
		logger.info("Initializing EntityResolver");
		IEntityResolver resolver = new EntityResolver2(tagMapper);

		logger.info("Starting export");
		
		List<String> langs = retrieveLanguages();
		//List<String> langs = Arrays.asList(new String[] {"ce"});
		System.out.println("Retrieved languages: " + langs);
		
		/*
		List<String> tmp = new ArrayList<String>();
		for(String lang : langs) {
			if(lang.contains("-")) {
				tmp.add(lang);
			}
		}
		langs = tmp;
		*/
		//export("de", false, null, resolver);
		
		//if(true) { return; }
		
		//String[] langs = {"en-gb","de","fr","pl","ja","it","nl","pt","es","ru","sv","zh","no","fi","ca","uk","tr","cs","hu","ro","vo","eo","da","sk","id","ar","ko","he","lt","vi","sl","sr","bg","et","fa","hr","simple","new","ht","nn","gl","th","te","el","ms","eu","ceb","mk","hi","ka","la","bs","lb","br","is","bpy","mr","sq","cy","az","sh","tl","lv","pms","bn","be_x_old","jv","ta","oc","io","be","an","su","nds","scn","nap","ku","ast","af","fy","sw","wa","zh_yue","bat_smg","qu","ur","cv","ksh"};
		int counter = 0;
		for(String lang : langs) {
			/*
			++counter;
			if(counter < 15) {
				continue;
			} else if (counter > 25) {
				return;
			}
			*/
			boolean idMode = lang.equals("en-gb");
			
			logger.info("Processing language: " + lang);
			try {
				export(lang, idMode, null, resolver);
			} catch(Exception e) {
				logger.warn("Failed for language:" + lang);
			}
		}

		//export("de", true, "en", resolver);
	}

	public static void export(String initLangCode, boolean idMode,
			String overrideLangCode, IEntityResolver resolver)
		throws Exception
	{
		//exportToDataBase(initLangCode, idMode, overrideLangCode);
		//exportToTriples(initLangCode, idMode, overrideLangCode, resolver);
		exportToFile("languages", initLangCode, idMode, overrideLangCode);
	}
	
	public static void exportToDataBase(String initLangCode, boolean idMode,
			String overrideLangCode)
		throws Exception
	{
		logger.info("Connecting to Database");
		Connection conn = PostGISUtil.connectPostGIS(new ConnectionConfig("localhost", "unittest_lgd", "postgres", "postgres"));
		
		TagLabelDAO dao = new TagLabelDAO(conn);
		
		ITagLabelOutput out = new TagLabelOutputDao(dao);
		
		doExport(initLangCode, idMode, overrideLangCode, out);
	}
	
	public static void exportToFile(String basePath, String initLangCode, boolean idMode,
			String overrideLangCode)
		throws Exception
	{
		File file = new File(basePath + "/" + initLangCode + ".csv");
		if(file.getParentFile() != null) {
			file.getParentFile().mkdirs();
		}
		
		PrintStream ps = new PrintStream(new FileOutputStream(new File(basePath + "/" + initLangCode + ".csv")));
		
		ITagLabelOutput out = new TagLabelOutputCsv(ps);
		
		doExport(initLangCode, idMode, overrideLangCode, out);
		
		ps.close();
	}	
	
	public static void doExport(String initLangCode, boolean idMode,
			String overrideLangCode, ITagLabelOutput out) throws IOException
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

		logger.info("Using language: " + langCode);
		logger.info("IdMode: " + idMode);

		if (langCode == null)
			throw new RuntimeException("Language code not detected");
	
	
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

			String[] kv = entry.split("\\.");
			if (kv.length == 2) {
				String key = kv[0];
				String value = kv[1]; //.trim();

				try {
					out.write(key, value, langCode, label);
				}
				catch(Exception e) {
					logger.warn(ExceptionUtils.getStackTrace(e));
				}
			}
		}
	}
	
	public static void exportToTriples(String initLangCode, boolean idMode,
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

		String targetFileName = "output/" + langCode + ".nt";

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

				Resource subject = resolver.resolve(key, value);
				if(subject == null) {
					logger.warn("Skipping: (" + key + ", " + value + ")");
					continue;
				}
				logger.trace("Mapped: " + key + ", " + value + ") -> " + subject);
				

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
