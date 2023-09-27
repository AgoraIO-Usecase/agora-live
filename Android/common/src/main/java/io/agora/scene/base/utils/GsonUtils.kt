package io.agora.scene.base.utils

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Gson utils
 *
 * @constructor Create empty Gson utils
 */
open class GsonUtils {
    /**
     * String converter
     *
     * @constructor Create empty String converter
     */
    class StringConverter : JsonSerializer<String>, JsonDeserializer<String> {
        /**
         * Deserialize
         *
         * @param json
         * @param typeOfT
         * @param context
         * @return
         */
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): String {
            return json.asJsonPrimitive.asString
        }

        /**
         * Serialize
         *
         * @param src
         * @param typeOfSrc
         * @param context
         * @return
         */
        override fun serialize(
            src: String?,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement =
            if (src == null || src == "null") JsonPrimitive("") else JsonPrimitive(src.toString())
    }

    companion object {
        /**
         * Gson
         */
        @JvmStatic
        val gson: Gson = Gson()

        /**
         * Covert to string
         *
         * @param obj
         * @return
         */
        fun covertToString(obj: Any): String {
            return gson.toJson(obj)
        }

        /**
         * Covert to string
         *
         * @param obj
         * @param typeOfSrc
         * @return
         */
        fun covertToString(obj: Any, typeOfSrc: Type): JsonElement {
            return gson.toJsonTree(obj, typeOfSrc)
        }

        /**
         * Covert to map
         *
         * @param obj
         * @return
         */
        fun covertToMap(obj: Any): Map<String, String>{
            return gson.fromJson(gson.toJson(obj), object: TypeToken<HashMap<String, String>>(){}.type)
        }
    }
}
