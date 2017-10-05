package ru.softbalance.equipment.model.mapping.jackson

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.IOException

val mapper = JacksonConfigurator.build()

class JacksonConfigurator {

    companion object {
        fun build(): ObjectMapper {
            return JacksonConfigurator()
                    .buildMapper()
                    .registerModule(KotlinModule())
        }
    }

    fun buildMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        val module = SimpleModule()
        val sbs = BoolAsIntSerializer()
        val sbds = BoolAsIntDeserializer()
        module.addSerializer(Boolean::class.java, sbs)
        module.addSerializer(Boolean::class.javaPrimitiveType, sbs)
        module.addDeserializer(Boolean::class.java, sbds)
        module.addDeserializer(Boolean::class.javaPrimitiveType, sbds)
        mapper.registerModule(module)

        mapper.setVisibility(mapper.serializationConfig.defaultVisibilityChecker
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE))

        mapper.setVisibility(mapper.deserializationConfig.defaultVisibilityChecker
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE))
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        return mapper
    }

    private class BoolAsIntSerializer : JsonSerializer<Boolean>() {
        @Throws(IOException::class, JsonProcessingException::class)
        override // implementing as is
        fun serialize(value: Boolean?, jgen: JsonGenerator, provider: SerializerProvider) {
            jgen.writeNumber(if (value === true) BooleanToInt.TRUE else BooleanToInt.FALSE)
        }
    }

    private class BoolAsIntDeserializer : JsonDeserializer<Boolean>() {
        @Throws(IOException::class, JsonProcessingException::class)
        override // implementing as is
        fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Boolean? {
            if (jp.currentToken == JsonToken.VALUE_NUMBER_INT) {
                return jp.intValue == 1
            } else {
                return jp.booleanValue
            }
        }
    }
}