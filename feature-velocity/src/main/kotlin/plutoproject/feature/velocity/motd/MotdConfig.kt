package plutoproject.feature.velocity.motd

data class MotdConfig(
    val startDate: String = "2020-01-01",
    val line1: String = "<gold>PlutoProject</gold>",
    val line2: String = "<gray>已开服 <yellow>\$days</yellow> 天</gray>",
)
