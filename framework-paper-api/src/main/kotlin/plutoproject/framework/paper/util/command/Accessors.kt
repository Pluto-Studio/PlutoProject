package plutoproject.framework.paper.util.command

import plutoproject.framework.common.util.inject.globalKoin

val CommandManager by globalKoin.inject<PlatformCommandManager>()
val AnnotationParser by globalKoin.inject<PlatformAnnotationParser>()
