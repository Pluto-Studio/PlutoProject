package plutoproject.framework.common.util.database

import com.mongodb.client.model.ReplaceOptions

fun upsertReplaceOptions(): ReplaceOptions {
    return ReplaceOptions().upsert(true)
}
