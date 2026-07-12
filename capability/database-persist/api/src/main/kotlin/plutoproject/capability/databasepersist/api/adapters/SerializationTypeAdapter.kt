package plutoproject.capability.databasepersist.api.adapters

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.BsonValue
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.kotlinx.BsonConfiguration
import org.bson.codecs.kotlinx.KotlinSerializerCodec
import org.bson.codecs.kotlinx.defaultSerializersModule
import plutoproject.capability.databasepersist.api.DataTypeAdapter
import kotlin.reflect.KClass

inline fun <reified T : Any> SerializationTypeAdapter(): SerializationTypeAdapter<T> = SerializationTypeAdapter(T::class)

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
class SerializationTypeAdapter<T : Any>(
    override val type: KClass<T>,
    serializer: KSerializer<T> = type.serializer(),
) : DataTypeAdapter<T> {
    private val codec = KotlinSerializerCodec.create(type, serializer, defaultSerializersModule, BsonConfiguration())
    private val encoderContext = EncoderContext.builder().build()
    private val decoderContext = DecoderContext.builder().build()

    override fun fromBson(bson: BsonValue): T {
        val reader = BsonDocumentReader(BsonDocument("temp", bson))
        reader.readStartDocument()
        reader.readName("temp")
        val value = codec.decode(reader, decoderContext)
        reader.readEndDocument()
        return value
    }

    override fun toBson(value: T): BsonValue {
        val writer = BsonDocumentWriter(BsonDocument())
        writer.writeStartDocument()
        writer.writeName("temp")
        codec.encode(writer, value, encoderContext)
        writer.writeEndDocument()
        return writer.document.getValue("temp")
    }
}
