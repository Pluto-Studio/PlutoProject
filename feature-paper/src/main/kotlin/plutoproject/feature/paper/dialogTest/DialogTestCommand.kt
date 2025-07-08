package plutoproject.feature.paper.dialogTest

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback.Options
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.paper.util.inventory.addItemOrDrop

@Suppress("UnstableApiUsage")
object DialogTestCommand {
    @Command("dialogtest <player>")
    fun dialogTest(sender: CommandSender, @Argument("player") player: Player) {
        val title = component {
            text("修改家名称") with mochaText
        }
        val externalTitle = component {
            text("这是额外标题") with mochaText
        }

        val plainMessage = DialogBody.plainMessage(component {
            repeat(3) {
                text("这是文本这是文本这是文本这是文本这是文本这是文本这是文本这是文本这是文本这是文本这是文本这是文本这是文本这是文本这是文本") with mochaText
                newline()
            }
        }, 1024)

        val descMessage = DialogBody.plainMessage(component {
            text("正在修改家 ") with mochaText
            text("TestHome123 ") with mochaYellow
            text("的名称") with mochaText
        })

        val item = DialogBody.item(ItemStack(Material.NAME_TAG).apply {
            editMeta {
                it.isHideTooltip = true
            }
        }).description(descMessage).build()

        val textInputLabel = component {
            text("输入家名称") with mochaText
            text(" (最长 ") with mochaSubtext0
            text("15 ") with mochaText
            text("个字符)") with mochaSubtext0
        }
        val textInput = DialogInput.text("text_input", textInputLabel).build()

        val boolInputLabel = component { text("这是布尔选项") with mochaText }
        val boolInput = DialogInput.bool("bool_input", boolInputLabel).build()

        val numberRangeLabel = component { text("这是数字范围") with mochaText }
        val numberRangeInput =
            DialogInput.numberRange("number_range_input", numberRangeLabel, 0f, 100f).step(1f).build()

        val singleOptionLabel = component { text("这是单一选项") with mochaText }
        val singleOption = DialogInput.singleOption(
            "single_option_input",
            singleOptionLabel,
            listOf(
                SingleOptionDialogInput.OptionEntry.create("1", component { text("选项 1") with mochaText }, true),
                SingleOptionDialogInput.OptionEntry.create("2", component { text("选项 2") with mochaText }, false),
                SingleOptionDialogInput.OptionEntry.create("3", component { text("选项 3") with mochaText }, false),
            )
        ).build()

        val dialogBase =
            DialogBase.builder(title)
                .externalTitle(externalTitle)
                .canCloseWithEscape(false)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE).body(listOf(/*plainMessage,*/ item))
                .inputs(listOf(textInput, /*boolInput, numberRangeInput, singleOption*/))
                .build()

        val actionButtonText = component { text("保存") }
        val callback = DialogActionCallback { view, audience ->
            val clicker = audience as Player
            clicker.send {
                text("给你一个苹果！") with mochaMaroon
            }
            clicker.send {
                text("文字输入内容: ") with mochaText
                text(view.getText("text_input") ?: "null") with mochaLavender
            }
            clicker.send {
                text("布尔选项内容: ") with mochaText
                text("${view.getBoolean("bool_input") ?: "null"}") with mochaLavender
            }
            clicker.send {
                text("数字范围内容: ") with mochaText
                text("${view.getFloat("number_range_input") ?: "null"}") with mochaLavender
            }
            clicker.inventory.addItemOrDrop(ItemStack(Material.APPLE))
        }
        val options = Options.builder().uses(1).build()
        val actionButton = ActionButton.create(
            actionButtonText, null, 100, DialogAction.customClick(callback, options)
        )

        val dialog = Dialog.create {
            it.empty().base(dialogBase).type(DialogType.notice(actionButton))
        }

        player.showDialog(dialog)
    }
}
