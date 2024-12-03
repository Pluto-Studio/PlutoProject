package ink.pmc.framework.interactive.inventory.layout.list

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.framework.interactive.inventory.Modifier
import ink.pmc.framework.interactive.inventory.components.Selector
import ink.pmc.framework.interactive.inventory.fillMaxSize
import ink.pmc.framework.interactive.inventory.jetpack.Arrangement
import ink.pmc.framework.interactive.inventory.layout.Row
import ink.pmc.framework.utils.visual.mochaText

abstract class FilterListMenu<E, F>(
    options: ListMenuOptions = ListMenuOptions(),
    private val filters: Map<F, String>
) : ListMenu<E>(options) {
    override fun BottomBorderAttachment() {
        if (model.current.isLoading) return
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
            PreviousTurner()
            FilterSelector()
            NextTurner()
        }
    }

    abstract override fun modelProvider(): FilterListMenuModel<E, F>

    @Composable
    @Suppress("FunctionName")
    open fun FilterSelector() {
        val model = model.current as FilterListMenuModel<*, *>
        Selector(
            title = component {
                text("筛选") with mochaText without italic()
            },
            options = filters.values.toList(),
            goNext = model::nextFilter,
            goPrevious = model::previousFilter
        )
    }
}