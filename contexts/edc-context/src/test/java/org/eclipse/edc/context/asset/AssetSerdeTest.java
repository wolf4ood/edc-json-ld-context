/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.context.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.context.EdcContextConstants.EDC_CONTEXT_URL;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_CONTENT_TYPE;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_DESCRIPTION;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_ID;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_NAME;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_VERSION;
import static org.mockito.Mockito.mock;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.net.URISyntaxException;
import java.util.Map;
import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromAssetTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToAssetTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public class AssetSerdeTest {

  private static final String TEST_ASSET_ID = "some-asset-id";
  private static final String TEST_ASSET_NAME = "some-asset-name";
  private static final String TEST_ASSET_DESCRIPTION = "some description";
  private static final String TEST_ASSET_CONTENTTYPE = "application/json";
  private static final String TEST_ASSET_VERSION = "0.2.1";

  private final TitaniumJsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));

  private TypeTransformerRegistry typeTransformerRegistry;

  private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());

  @BeforeEach
  void setUp() throws URISyntaxException {
    var objectMapper = createObjectMapper();
    typeTransformerRegistry = new TypeTransformerRegistryImpl();
    typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(objectMapper));
    typeTransformerRegistry.register(new JsonObjectToAssetTransformer());
    typeTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
    typeTransformerRegistry.register(
        new JsonObjectFromAssetTransformer(Json.createBuilderFactory(Map.of()),
            createObjectMapper()));
    typeTransformerRegistry.register(
        new JsonObjectFromDataAddressTransformer(Json.createBuilderFactory(Map.of())));

    var edc = getClass().getClassLoader().getResource("document/edc-v1.jsonld").toURI();
    var odrl = getClass().getClassLoader().getResource("document/odrl.jsonld").toURI();
    jsonLd.registerCachedDocument("http://www.w3.org/ns/odrl.jsonld", odrl);
    jsonLd.registerCachedDocument(EDC_CONTEXT_URL, edc);
  }

  @Test
  void serde_Asset() {
    var jsonObj = jsonFactory.createObjectBuilder()
        .add(CONTEXT, createContextBuilder().build())
        .add(TYPE, "Asset")
        .add(ID, TEST_ASSET_ID)
        .add("properties", createPropertiesBuilder().build())
        .add("dataAddress", jsonFactory.createObjectBuilder().add("type", "address-type"))
        .build();
    jsonObj = expand(jsonObj);

    var result = typeTransformerRegistry.transform(jsonObj, Asset.class);

    assertThat(result).isSucceeded().satisfies(asset -> {
      assertThat(asset.getProperties())
          .hasSize(5)
          .containsEntry(PROPERTY_ID, TEST_ASSET_ID)
          .containsEntry(PROPERTY_ID, result.getContent().getId())
          .containsEntry(PROPERTY_NAME, TEST_ASSET_NAME)
          .containsEntry(PROPERTY_DESCRIPTION, TEST_ASSET_DESCRIPTION)
          .containsEntry(PROPERTY_CONTENT_TYPE, TEST_ASSET_CONTENTTYPE)
          .containsEntry(PROPERTY_VERSION, TEST_ASSET_VERSION);
      assertThat(asset.getDataAddress()).isNotNull().extracting(DataAddress::getType)
          .isEqualTo("address-type");
    });

    var object = typeTransformerRegistry.transform(result.getContent(), JsonObject.class)
        .getContent();

    var asset = jsonLd.compact(object);

    System.out.println(asset.getContent());

  }

  private JsonObjectBuilder createPropertiesBuilder() {
    return jsonFactory.createObjectBuilder()
        .add("name", TEST_ASSET_NAME)
        .add("description", TEST_ASSET_DESCRIPTION)
        .add("version", TEST_ASSET_VERSION)
        .add("contenttype", TEST_ASSET_CONTENTTYPE);
  }

  private JsonArrayBuilder createContextBuilder() {
    return jsonFactory.createArrayBuilder()
        .add(EDC_CONTEXT_URL);
  }

  private JsonObject expand(JsonObject jsonObject) {
    return jsonLd.expand(jsonObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
  }

}
