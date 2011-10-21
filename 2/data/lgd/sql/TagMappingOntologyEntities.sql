/**
 * A simple view for keys which play a role in the tag->ontology mapping.
 * Not very efficient, however, the view can be replaced with a physical
 * table later anyway.
 */
DROP VIEW lgd_tag_ontology_k;
CREATE VIEW lgd_tag_ontology_k AS
	SELECT
		key AS k, LGDOWLEntityType('dataTypeProperty') AS owl_entity_type
	FROM
		lgd_tag_mapping_simple_base a
		INNER JOIN lgd_tag_mapping_simple_data_type b ON (b.id = a.id)
	GROUP BY
		k
	UNION ALL
	SELECT
		key AS k, LGDOWLEntityType('objectProperty') AS owl_entity_type
	FROM
		lgd_tag_mapping_simple_base a
		INNER JOIN lgd_tag_mapping_simple_object_property b ON (b.id = a.id)
	WHERE
		a.property <> 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'	
	GROUP BY
		k
	UNION ALL
	SELECT
		key AS k, LGDOWLEntityType('class') AS owl_entity_type
	FROM
		lgd_tag_mapping_simple_base a
		INNER JOIN lgd_tag_mapping_simple_object_property b ON (b.id = a.id)
	WHERE
		a.property = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
	GROUP BY
		key
;
	
		