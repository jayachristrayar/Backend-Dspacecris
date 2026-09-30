package org.dspace.importer.external.crossref;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;

public class CrossRefAuthorMetadataContributor
    implements MetadataContributor<String> {

    private static final Logger log =
        LogManager.getLogger(CrossRefAuthorMetadataContributor.class);

    private MetadataFieldConfig orcid;
    private MetadataFieldConfig affiliation;

    @Override
    public Collection<MetadatumDTO> contributeMetadata(String json) {

        Collection<MetadatumDTO> values = new ArrayList<>();

        JsonNode author;

        try {
            author = new ObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process Crossref author JSON.", e);
            return values;
        }

        String orcidValue = getText(author, "ORCID");

        if (StringUtils.isNotBlank(orcidValue) && orcid != null) {
            values.add(createMetadata(orcid, orcidValue));
        }

        JsonNode affiliations = author.get("affiliation");

        if (affiliations != null && affiliations.isArray()
                && affiliation != null) {

            for (JsonNode item : affiliations) {

                String affiliationValue = getText(item, "name");

                if (StringUtils.isNotBlank(affiliationValue)) {
                    values.add(
                        createMetadata(
                            affiliation,
                            affiliationValue
                        )
                    );
                }
            }
        }

        return values;
    }

    private String getText(JsonNode node, String field) {

        JsonNode value = node.get(field);

        if (value != null && !value.isNull()) {
            return value.asText().trim();
        }

        return StringUtils.EMPTY;
    }

    private MetadatumDTO createMetadata(
        MetadataFieldConfig config,
        String value) {

        MetadatumDTO metadata = new MetadatumDTO();

        metadata.setSchema(config.getSchema());
        metadata.setElement(config.getElement());
        metadata.setQualifier(config.getQualifier());
        metadata.setValue(value);

        return metadata;
    }

    @Override
    public void setMetadataFieldMapping(
        MetadataFieldMapping<String, MetadataContributor<String>>
            metadataFieldMapping) {
        // Not required.
    }

    public void setOrcid(MetadataFieldConfig orcid) {
        this.orcid = orcid;
    }

    public void setAffiliation(MetadataFieldConfig affiliation) {
        this.affiliation = affiliation;
    }
}

