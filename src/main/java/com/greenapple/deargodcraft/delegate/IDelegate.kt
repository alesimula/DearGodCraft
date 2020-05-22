package com.greenapple.deargodcraft.delegate

import kotlin.reflect.KProperty

interface IDelegateVal<This, Return> {
    operator fun getValue(thisRef: This, property: KProperty<*>): Return
}

interface IDelegateVar<This, Return> : IDelegateVal<This, Return> {
    operator fun setValue(thisRef: This, property: KProperty<*>, value: Return)
}