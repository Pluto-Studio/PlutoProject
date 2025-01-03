package plutoproject.framework.common.options

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.options.*
import plutoproject.framework.common.options.repositories.OptionsContainerRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class OptionsManagerImpl : OptionsManager, KoinComponent {
    private val repo by inject<OptionsContainerRepository>()
    private val registeredDescriptors = mutableMapOf<String, OptionDescriptor<*>>()
    private val loadedOptionsMap = ConcurrentHashMap<UUID, PlayerOptions>()
    override val loadedPlayers
        get() = loadedOptionsMap.values.toList()

    override fun isPlayerLoaded(uuid: UUID): Boolean {
        return loadedOptionsMap.containsKey(uuid)
    }

    override fun unloadPlayer(uuid: UUID) {
        loadedOptionsMap.remove(uuid)
    }

    override fun getLoadedOptions(uuid: UUID): PlayerOptions? {
        return loadedOptionsMap[uuid]
    }

    override suspend fun reloadOptions(uuid: UUID) {
        getLoadedOptions(uuid)?.reload()
    }

    private suspend fun fetchFromDatabase(uuid: UUID): PlayerOptions? {
        val model = repo.findById(uuid) ?: return null
        val entriesMap = mutableMapOf<String, OptionEntry<*>>()
        model.entries.forEach {
            val entry = createEntryFromModel(it)
            entriesMap[it.key] = entry
        }
        return PlayerOptionsImpl(uuid, entriesMap)
    }

    override suspend fun loadOptions(uuid: UUID): PlayerOptions? {
        return fetchFromDatabase(uuid)?.also { loadedOptionsMap[uuid] = it }
    }

    override suspend fun createOptions(uuid: UUID): PlayerOptions {
        check(getOptions(uuid) == null) { "OptionContainer for $uuid already existed" }
        return PlayerOptionsImpl(uuid, mutableMapOf()).also {
            save(it)
        }
    }

    /*
    * 非在线玩家的 OptionsContainer 不会被持久加载。
    * */
    override suspend fun getOptions(uuid: UUID): PlayerOptions? {
        return loadedOptionsMap[uuid] ?: fetchFromDatabase(uuid)
    }

    override suspend fun getOptionsOrCreate(uuid: UUID): PlayerOptions {
        return getOptions(uuid) ?: createOptions(uuid)
    }

    override suspend fun deleteOptions(uuid: UUID) {
        unloadPlayer(uuid)
        repo.deleteById(uuid)
    }

    override suspend fun save(options: PlayerOptions) {
        options.save()
    }

    override suspend fun save(uuid: UUID) {
        loadedOptionsMap[uuid]?.save()
    }

    override fun registerOptionDescriptor(descriptor: OptionDescriptor<*>) {
        require(descriptor.type != EntryValueType.UNKNOWN) { "UNKNOWN is for internal use only" }
        require(!registeredDescriptors.containsKey(descriptor.key)) { "Descriptor for ${descriptor.key} already registered" }
        registeredDescriptors[descriptor.key] = descriptor
    }

    override fun getOptionDescriptor(key: String): OptionDescriptor<*>? {
        return registeredDescriptors[key]
    }
}
