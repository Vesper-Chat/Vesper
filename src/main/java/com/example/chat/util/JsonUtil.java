package com.example.chat.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * JSON 工具类，屏蔽 Gson 细节。
 */
public final class JsonUtil {
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static JsonObject toJsonObject(Object obj) {
        JsonElement element = GSON.toJsonTree(obj);
        return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    public static Gson gson() {
        return GSON;
    }
}