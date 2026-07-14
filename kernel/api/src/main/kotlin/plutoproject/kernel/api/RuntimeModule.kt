package plutoproject.kernel.api

interface RuntimeModule {
    suspend fun onLoad(context: ModuleContext) = Unit

    suspend fun onEnable(context: ModuleContext) = Unit

    suspend fun onDisable(context: ModuleContext) = Unit
}
