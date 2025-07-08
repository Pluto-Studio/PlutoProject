package plutoproject.feature.paper.dialogTest

import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser

@Feature(
    id = "dialog_test",
    platform = Platform.PAPER
)
@Suppress("UNUSED")
class DialogTestFeature : PaperFeature() {
    override fun onEnable() {
        AnnotationParser.parse(DialogTestCommand)
    }
}
