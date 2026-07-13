package plutoproject.feature.gallery.paper

import kotlinx.coroutines.flow.toList
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.gallery.core.image.ImageDataRepository
import plutoproject.feature.gallery.infra.mongo.model.ImageDataDocument
import plutoproject.feature.gallery.infra.mongo.toImageData
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.capability.serveridentifier.api.ServerIdentifier
import plutoproject.foundation.common.text.replace
import java.util.logging.Level
import java.util.logging.Logger
import plutoproject.kernel.api.koinGet

private const val COMMAND_GALLERY_MIGRATE_IMAGE_DATA_PERMISSION = "plutoproject.gallery.command.gallery.migrate_image_data"
private const val GALLERY_PREFIX = "gallery_"
private const val LEGACY_IMAGE_DATA_COLLECTION = "image_data"

@Suppress("UNUSED")
object GalleryMigrateImageDataCommand {
    private val imageDataRepository = koinGet<ImageDataRepository>()
    private val logger = koinGet<Logger>()
    private val mongoConnection = koinGet<MongoConnection>()
    private val serverIdentifier = koinGet<ServerIdentifier>()
    private val legacyCollection = connectLegacyCollection()

    private fun connectLegacyCollection() = mongoConnection.getCollection<ImageDataDocument>(
        "$GALLERY_PREFIX${serverIdentifier.identifierOrThrow()}_$LEGACY_IMAGE_DATA_COLLECTION"
    )

    @Command("gallery migrate-image-data")
    @Permission(COMMAND_GALLERY_MIGRATE_IMAGE_DATA_PERMISSION)
    suspend fun CommandSender.migrateImageData() {
        sendMessage(IMAGE_DATA_MIGRATION_START)

        val legacyDocuments = legacyCollection.find().toList()
        var successCount = 0
        var failureCount = 0

        for (legacyDocument in legacyDocuments) {
            val migrated = runCatching {
                val migratedData = legacyDocument.toImageData()
                imageDataRepository.deleteByImageId(legacyDocument.imageId)
                imageDataRepository.save(legacyDocument.imageId, migratedData)

                check(imageDataRepository.findByImageId(legacyDocument.imageId) == migratedData) {
                    "read-back validation failed"
                }
            }

            if (migrated.isSuccess) {
                successCount++
                continue
            }

            failureCount++
            val cause = migrated.exceptionOrNull()
            logger.log(Level.WARNING, "Failed to migrate gallery image data ${legacyDocument.imageId}", cause)
            sendMessage(
                getImageDataMigrationFailedMessage(
                    imageId = legacyDocument.imageId.toString(),
                    reason = cause?.message ?: cause?.javaClass?.name ?: "unknown error",
                )
            )
        }

        sendMessage(
            IMAGE_DATA_MIGRATION_FINISHED
                .replace(IMAGE_PLACEHOLDER_PROCESSED, legacyDocuments.size)
                .replace(IMAGE_PLACEHOLDER_SUCCESS, successCount)
                .replace(IMAGE_PLACEHOLDER_FAILED, failureCount)
        )
    }
}
