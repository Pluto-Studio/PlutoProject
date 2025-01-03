package plutoproject.feature.paper.test

import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.paper.api.feature.PaperFeature

@Feature(
    id = "test_feature",
    platform = Platform.PAPER,
    dependencies = []
)
class TestFeature : PaperFeature() {
    override fun onLoad() {
        logger.info("onLoad called")
    }

    override fun onEnable() {
        logger.info("onEnable called")
    }

    override fun onReload() {
        logger.info("onReload called")
    }

    override fun onDisable() {
        logger.info("onDisable called")
    }
}
