package plutoproject.framework.common.util.data.json

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun Any.toJsonObject(gson: Gson = Gson): JsonObject {
    return gson.toJsonTree(this).asJsonObject
}

fun Any.toJsonElement(gson: Gson = Gson): JsonElement {
    return gson.toJsonTree(this)
}

fun Any.toJsonString(gson: Gson = Gson): String {
    return gson.toJson(this)
}

inline fun <reified T> JsonElement.toObject(gson: Gson = Gson): T {
    return gson.fromJson(this, T::class.java)
}

inline fun <reified T> JsonObject.toObject(gson: Gson = Gson): T {
    return gson.fromJson(this, T::class.java)
}

inline fun <reified T> String.convertJsonToObject(gson: Gson = Gson): T {
    return gson.fromJson(this, T::class.java)
}

fun <T> String.convertJsonToObject(type: Class<T>, gson: Gson = Gson): T {
    return gson.fromJson(this, type)
}
