package ink.pmc.menu

data class MenuConfig(
    val prebuiltPages: PrebuiltPages = PrebuiltPages(),
    val prebuiltButtons: PrebuiltButtons = PrebuiltButtons(),
    val pages: List<Page> = listOf()
)

data class PrebuiltPages(
    val assistant: Boolean = false
)

data class PrebuiltButtons(
    val inspect: Boolean = false,
    val wiki: Boolean = false,
    val balance: Boolean = false
)

data class Page(
    val id: String,
    val patterns: List<String> = listOf(),
    val buttons: List<Button> = listOf()
)

data class Button(
    val id: String,
    val pattern: Char
)