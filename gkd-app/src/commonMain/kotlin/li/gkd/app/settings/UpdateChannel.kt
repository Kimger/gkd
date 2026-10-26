package li.gkd.app.settings

import li.gkd.app.model.Option
import li.gkd.app.resources.*
import org.jetbrains.compose.resources.StringResource

/** Explicit values are part of the persisted settings contract. */
enum class UpdateChannel(
    override val value: Int,
    override val labelResource: StringResource,
    val url: String,
) : Option<Int> {
    // Retain persisted channel values; both select this fork's manually approved releases.
    Stable(0, Res.string.update_channel_stable, "https://github.com/Kimger/gkd/releases/latest/download/update.json"),
    Beta(1, Res.string.update_channel_beta, "https://github.com/Kimger/gkd/releases/latest/download/update.json");

    override val options get() = entries
}
