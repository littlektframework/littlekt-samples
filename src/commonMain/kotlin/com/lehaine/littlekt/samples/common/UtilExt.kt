package com.lehaine.littlekt.samples.common

/**
 * @author Colton Daily
 * @date 12/27/2021
 */

fun <T> MutableList<T>.iterateForEach(action: (T, MutableIterator<T>) -> Unit) {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val item = iterator.next()
        action(item, iterator)
    }
}