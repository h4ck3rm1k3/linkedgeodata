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
DROP TABLE lgd_tag_labels;
CREATE TABLE lgd_tag_labels (
	k TEXT NOT NULL,
	v TEXT, /* Note: If v is NULL, the label applies to the class identified solely by k */ 
	language VARCHAR(16) NOT NULL,
	label TEXT NOT NULL,

	/* Avoid duplicates */
	UNIQUE(k, v, language, label)
);

/* Index for searching by label */ 
CREATE INDEX idx_lgd_tag_labels_lablan ON lgd_tag_labels(label, language);

/* Index for searching by language */
CREATE INDEX idx_lgd_tag_labels_lanlab ON lgd_tag_labels(language, label);

/* Index for joins on (k, v) */
CREATE INDEX idx_lgd_tag_labels_k_v ON lgd_tag_labels(v, k);
