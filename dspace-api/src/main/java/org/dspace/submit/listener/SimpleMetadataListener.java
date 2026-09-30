/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.listener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.dspace.content.dto.MetadataValueDTO;
/**
 * This is the basic implementation for the MetadataListener interface.
 * 
 * It got the a map of metadata and related External Data Provider that can be
 * used to retrieve further information using the updated metadata in the item
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class SimpleMetadataListener implements MetadataListener {
    /**
     * A map to link a specific metadata with an ExternalDataProvider
     */
    private Map<String, List<ExternalDataProvider>> externalDataProvidersMap;

    private List<ExternalIdGenerator> generators;

    public Map<String, List<ExternalDataProvider>> getExternalDataProvidersMap() {
        return externalDataProvidersMap;
    }

    public void setExternalDataProvidersMap(Map<String, List<ExternalDataProvider>> externalDataProvidersMap) {
        this.externalDataProvidersMap = externalDataProvidersMap;
    }

    @Override
    public Set<String> getMetadataToListen() {
        return externalDataProvidersMap.keySet();
    }


@Override
public ExternalDataObject getExternalDataObject(Context context, Item item, Set<String> changedMetadata) {
    for (String metadata : changedMetadata) {
        List<ExternalDataProvider> providers = externalDataProvidersMap.get(metadata);

        if (providers == null || providers.isEmpty()) {
            continue;
        }

        ExternalDataObject mergedObject = null;

        for (ExternalDataProvider prov : providers) {
            String id = generateExternalId(context, prov, item, metadata);

            if (StringUtils.isBlank(id)) {
                continue;
            }

            Optional<ExternalDataObject> result = prov.getExternalDataObject(id);

            if (result.isEmpty()) {
                continue;
            }

            ExternalDataObject currentObject = result.get();

            if (mergedObject == null) {
                mergedObject = currentObject;
                mergedObject.setMetadata(new ArrayList<>(currentObject.getMetadata()));
            } else {
LinkedHashSet<String> existing = new LinkedHashSet<>();

for (MetadataValueDTO value : mergedObject.getMetadata()) {
    String normalizedValue = value.getValue();

    if ("dc".equals(value.getSchema())
            && "contributor".equals(value.getElement())
            && "author".equals(value.getQualifier())) {

        normalizedValue = Arrays.stream(
                value.getValue()
                        .replaceAll("[^\\p{L}\\p{N}]+", " ")
                        .toLowerCase()
                        .trim()
                        .split("\\s+"))
                .sorted()
                .collect(Collectors.joining(" "));
    }

    existing.add(
        value.getSchema() + "|" +
        value.getElement() + "|" +
        value.getQualifier() + "|" +
        normalizedValue
    );
}

for (MetadataValueDTO value : currentObject.getMetadata()) {
    String normalizedValue = value.getValue();

    if ("dc".equals(value.getSchema())
            && "contributor".equals(value.getElement())
            && "author".equals(value.getQualifier())) {

        normalizedValue = Arrays.stream(
                value.getValue()
                        .replaceAll("[^\\p{L}\\p{N}]+", " ")
                        .toLowerCase()
                        .trim()
                        .split("\\s+"))
                .sorted()
                .collect(Collectors.joining(" "));
    }

    String key =
        value.getSchema() + "|" +
        value.getElement() + "|" +
        value.getQualifier() + "|" +
        normalizedValue;

    if (existing.add(key)) {
        mergedObject.addMetadata(value);
    }
}
            }
        }

        if (mergedObject != null) {
            return mergedObject;
        }
    }

    return null;
}

    /**
     * This is the simpler implementation, it assumes that the value of the metadata
     * listened by the DataProvider can be used directly as identifier. Subclass may
     * extend it to add support for identifier normalization or combine multiple
     * information to build the identifier
     * 
     * @param context         the DSpace Context Object
     * @param prov            the ExternalDataProvider that need to received an Id
     * @param item            the item
     * @param metadata        the changed metadata that lead to the selected
     *                        ExternalDataProvider
     * @return an Id if any that can be used to query the {@link ExternalDataProvider}
     */
    protected String generateExternalId(Context context, ExternalDataProvider provider, Item item, String metadata) {
        for (ExternalIdGenerator generator : generators) {
            if (generator.support(provider)) {
                return generator.generateExternalId(context, provider, item, metadata);
            }
        }
        return StringUtils.EMPTY;
    }

    public List<ExternalIdGenerator> getGenerators() {
        return generators;
    }

    public void setGenerators(List<ExternalIdGenerator> generators) {
        this.generators = generators;
    }

}

