package com.romrell4.tennisladder.support

sealed interface AsyncOperation<out T> {
    data class Success<T>(val data: T) : AsyncOperation<T>
    data class Error(val throwable: Throwable) : AsyncOperation<Nothing>
    data object Loading : AsyncOperation<Nothing>

    fun data(): T? = when (this) {
        is Success -> data
        is Error, Loading -> null
    }
}
