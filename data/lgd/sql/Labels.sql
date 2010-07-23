/**
 * A table for mapping tags to labels, such as the labels provided at
 * translate-wiki.
 * 
 * http://translatewiki.net/w/i.php?title=Special%3ATranslate&task=view&group=out-osm-site&language=ru&limit=2500
 * 
 * Labels for tags should be kept on tag level rather than the resource level,
 * as this allows changing the mapping between a tag and a resource without
 * breaking the link between the tag and its label.
 * (Assume you map (amenity, park) to lgd:AmenityPark: If the label was assigned
 * on resource level, we would break the link if we remapped (amenity, park) to
 * lgd:Park.
 * 
 */
DROP TABLE lgd_tags_labels;
CREATE TABLE lgd_tags_labels (
	k TEXT NOT NULL,
	v TEXT NOT NULL,
	language VARCHAR(16) NOT NULL,
	label TEXT NOT NULL,
	
	UNIQUE(k, v, language, label)
);
