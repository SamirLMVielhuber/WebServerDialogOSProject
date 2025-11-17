package com.clt.webServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConfigReader {

    private Map<String, String> documents;

    public ConfigReader(InputStream is) throws IOException {
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject docs = json.getAsJsonObject("Documents");
            documents = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : docs.entrySet()) {
                documents.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
    }

    public Map<String, String> getDocuments() {
        return documents;
    }

    public String getDocumentPath(String name) {
        return documents.get(name);
    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        return "";
    }
}
