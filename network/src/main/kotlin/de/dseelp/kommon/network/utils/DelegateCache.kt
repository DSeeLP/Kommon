package de.dseelp.kommon.network.utils

import de.dseelp.kommon.network.codec.packet.Packet
import de.dseelp.kommon.network.codec.packet.PacketData
import de.dseelp.kommon.network.utils.DelegateCache.getCachedDelegates
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible

object DelegateCache {
    fun <T : Packet> KClass<T>.getCachedDelegates(obj: Any): Array<PacketData<*, *>> {
        obj as T
        return if (cache.containsKey(this)) cache[this]!!.map { (it as KProperty1<T, *>).getDelegate(obj) as PacketData<*, *> }.toList().toTypedArray()
        else {
            cacheDelegates(this, obj)
            return getCachedDelegates(obj)
        }
    }

    private fun <T: Packet> cacheDelegates(clazz: KClass<T>, obj: Any){
        obj as T
        cache[clazz] = clazz.declaredMemberProperties.filter { it.visibility == KVisibility.PUBLIC }.onEach { it.isAccessible = true }.filter { it.getDelegate(obj) != null }.toList()
    }


    val cache = hashMapOf<KClass<out Packet>, List<KProperty1<*, *>>>()
}