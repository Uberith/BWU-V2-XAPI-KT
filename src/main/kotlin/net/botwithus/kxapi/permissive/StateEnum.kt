package net.botwithus.kxapi.permissive

import kotlin.reflect.KClass

interface StateEnum {
    val description: String
    val classz: KClass<out PermissiveDSL<*>>
}
