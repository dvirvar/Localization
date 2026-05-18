package com.localization.offline.extension

inline fun <E, R> List<E>.hasDuplicateBy(crossinline keySelector: (E) -> R): Boolean {
    val set = mutableSetOf<R>()
    for (index in indices) {
        if (!set.add(keySelector(this[index]))) {
            return true
        }
    }
    return false
}