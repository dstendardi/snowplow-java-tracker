/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.snowplow.tracker.payload;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snowplowanalytics.snowplow.tracker.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TrackerPayload implements Payload {

    private final ObjectMapper objectMapper = Util.defaultMapper();
    private final Logger LOGGER = LoggerFactory.getLogger(TrackerPayload.class);
    private ObjectNode objectNode = objectMapper.createObjectNode();

    /**
     * Add a basic parameter.
     * @param key The parameter key
     * @param value The parameter value as a String
     */
    public void add(String key, String value) {
        if (value == null || value.isEmpty()) {
            LOGGER.debug("kv-value is empty. Returning out without adding key..");
            return;
        }

        LOGGER.debug("Adding new key: {} with value: {}", key, value);
        objectNode.put(key, value);
    }

    /**
     * Add a basic parameter.
     * @param key The parameter key
     * @param value The parameter value
     */
    // TODO: remove this. Tracker Payload is all Strings,
    // so we shouldn't be passing in Objects
    public void add(String key, Object value) {
        if (value == null) {
            LOGGER.debug("kv-value is empty. Returning out without adding key..");
            return;
        }

        LOGGER.debug("Adding new key: {} with object value: {}", key, value);
        try {
            objectNode.putPOJO(key, objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add all the mappings from the specified map. The effect is the equivalent to that of calling
     * add(String key, Object value) for each mapping for each key.
     * @param map Mappings to be stored in this map
     */
    // TODO: remove this unless we have a good reason to add
    // a uni-typed Map
    public void addMap(Map<String, Object> map) {
        // Return if we don't have a map
        if (map == null) {
            LOGGER.debug("Map passed in is null. Returning without adding map..");
            return;
        }

        Set<String> keys = map.keySet();
        for(String key : keys) {
            add(key, map.get(key));
        }
    }

    /**
     * Add a map to the Payload with a key dependent on the base 64 encoding option you choose using the
     * two keys provided.
     * @param map Mapping to be stored
     * @param base64_encoded The option you choose to encode the data
     * @param type_encoded The key that would be set if the encoding option was set to true
     * @param type_no_encoded They key that would be set if the encoding option was set to false
     */
    public void addMap(Map map, Boolean base64_encoded, String type_encoded, String type_no_encoded) {
        // Return if we don't have a map
        if (map == null) {
            LOGGER.debug("Map passed in is null. Returning nothing..");
            return;
        }

        String mapString;
        try {
            mapString = objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return; // Return because we can't continue
        }

        if (base64_encoded) { // base64 encoded data
            objectNode.put(type_encoded, Util.base64Encode(mapString));
        } else { // add it as a child node
            add(type_no_encoded, mapString);
        }
    }

    public JsonNode getNode() {
        return objectNode;
    }

    @Override
    public Map getMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        try {
            LOGGER.debug("Attempting to create a Map structure from ObjectNode.");
            map = objectMapper.readValue(objectNode.toString(), new TypeReference<Map>(){});
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public String toString() {
        return objectNode.toString();
    }
}
