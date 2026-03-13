package plutoproject.feature.gallery.adapter.common

import org.koin.core.qualifier.named
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import plutoproject.feature.gallery.core.decode.decoder.ImageDecoder
import plutoproject.feature.gallery.core.decode.decoder.defaultGifDecoder
import plutoproject.feature.gallery.core.decode.decoder.defaultStaticImageDecoder
import plutoproject.feature.gallery.core.render.AnimatedImageRenderer
import plutoproject.feature.gallery.core.render.DefaultAnimatedImageRenderer
import plutoproject.feature.gallery.core.render.DefaultStaticImageRenderer
import plutoproject.feature.gallery.core.render.StaticImageRenderer
import plutoproject.feature.gallery.core.render.defaultFrameSampler
import plutoproject.feature.gallery.core.render.mapcolor.defaultAlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.defaultMapColorQuantizer
import plutoproject.feature.gallery.core.usecase.DecodeImageUseCase
import plutoproject.feature.gallery.core.usecase.RenderAnimatedImageUseCase
import plutoproject.feature.gallery.core.usecase.RenderStaticImageUseCase
import java.time.Clock

val commonModule = module {
    single { Clock.systemUTC() }

    single<ImageDecoder>(named("gallery_static_decoder")) { defaultStaticImageDecoder() }
    single<ImageDecoder>(named("gallery_gif_decoder")) { defaultGifDecoder() }

    single { defaultFrameSampler() }
    single { defaultAlphaCompositor() }
    single { defaultMapColorQuantizer() }

    single<StaticImageRenderer> {
        DefaultStaticImageRenderer(
            alphaCompositor = get(),
            mapColorQuantizer = get(),
            logger = get(named("gallery_logger")),
        )
    }
    single<AnimatedImageRenderer> {
        DefaultAnimatedImageRenderer(
            frameSampler = get(),
            alphaCompositor = get(),
            mapColorQuantizer = get(),
            logger = get(named("gallery_logger")),
        )
    }

    single {
        DecodeImageUseCase(
            pngDecoder = get(named("gallery_static_decoder")),
            jpgDecoder = get(named("gallery_static_decoder")),
            webpDecoder = get(named("gallery_static_decoder")),
            gifDecoder = get(named("gallery_gif_decoder")),
            logger = get(named("gallery_logger")),
        )
    }
    singleOf(::RenderStaticImageUseCase)
    singleOf(::RenderAnimatedImageUseCase)
}
