package plutoproject.foundation.common.mongo

import com.mongodb.client.model.ReplaceOptions

fun upsertReplaceOptions(): ReplaceOptions {
    return ReplaceOptions().upsert(true)
}
