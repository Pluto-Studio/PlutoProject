package plutoproject.framework.common.util.inject

import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.java.KoinJavaComponent.getKoin
import org.koin.mp.KoinPlatformTools

fun modifyExistedKoinOrCreate(declaration: KoinAppDeclaration): KoinApplication {
    val context = KoinPlatformTools.defaultContext() as GlobalContext
    val application = context.getKoinApplicationOrNull() ?: return startKoin(declaration)
    return application.apply(declaration)
}

val Koin: Koin
    get() = getKoin()
