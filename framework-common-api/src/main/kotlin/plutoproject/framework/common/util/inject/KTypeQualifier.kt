package plutoproject.framework.common.util.inject

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.QualifierValue
import kotlin.reflect.KType

class KTypeQualifier(type: KType) : Qualifier {
    override val value: QualifierValue = type.toString()
}
