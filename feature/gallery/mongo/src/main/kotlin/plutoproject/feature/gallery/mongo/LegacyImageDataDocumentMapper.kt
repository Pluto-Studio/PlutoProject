package plutoproject.feature.gallery.infra.mongo

import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.infra.mongo.model.ImageDataDocument

fun ImageDataDocument.toImageData(): ImageData = toDomain()
