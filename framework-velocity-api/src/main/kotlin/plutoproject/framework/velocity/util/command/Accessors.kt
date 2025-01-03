package plutoproject.framework.velocity.util.command

import plutoproject.framework.common.util.inject.Koin

val CommandManager by Koin.inject<PlatformCommandManager>()
val AnnotationParser by Koin.inject<PlatformAnnotationParser>()
