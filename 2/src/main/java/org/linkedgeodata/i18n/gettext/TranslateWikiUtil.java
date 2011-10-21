package org.linkedgeodata.i18n.gettext;

import java.net.URL;

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