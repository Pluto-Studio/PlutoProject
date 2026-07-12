package plutoproject.capability.serveridentifier.api

interface ServerIdentifier {
    val identifier: String?

    fun identifierOrThrow(): String = checkNotNull(identifier) { "Server identifier not present" }

    fun isPresent(): Boolean = identifier != null
}
