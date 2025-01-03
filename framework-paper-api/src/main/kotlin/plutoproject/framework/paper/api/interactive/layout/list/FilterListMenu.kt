package plutoproject.framework.paper.api.interactive.layout.list

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.components.Selector
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxSize
import plutoproject.framework.paper.api.interactive.jetpack.Arrangement
import plutoproject.framework.paper.api.interactive.layout.Row

abstract class FilterListMenu<E, F : Any, M : FilterListMenuModel<E, F>>(
    private val filters: Map<F, String>
) : ListMenu<E, M>() {
    @Composable
    override fun BottomBorderAttachment() {
        if (LocalListMenuModel.current.isLoading) return
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
            PreviousTurner()
            FilterSelector()
            NextTurner()
        }
    }

    @Composable
    override fun reloadConditionProvider(): Array<Any> {
        val model = LocalListMenuModel.current
        return arrayOf(model.page, model.filter)
    }

    @Composable
    @Suppress("FunctionName")
    open fun FilterSelector() {
        val model = LocalListMenuModel.current
        Selector(
            title = component {
                text("筛选") with mochaText without italic()
            },
            options = filters.values.toList(),
            goNext = model::internalNextFilter,
            goPrevious = model::internalPreviousFilter
        )
    }
}
