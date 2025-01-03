package ink.pmc.menu

data class MenuConfig(
    val serverName: String,
    val item: Item = Item(),
    val prebuiltPages: PrebuiltPages = PrebuiltPages(),
    val prebuiltButtons: PrebuiltButtons = PrebuiltButtons(),
    val pages: List<Page> = listOf()
)

data class Item(
    val enabled: Boolean = true,
    val giveWhenJoin: Boolean = true,
    val alwaysGive: Boolean = false,
    val registerRecipe: Boolean = true
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