package com.otterbau.wartmal.data

class WartMalException(message: String) : IllegalArgumentException(message)

sealed class AppError(open val message: String) {
    class Validation(override val message: String) : AppError(message)
    class NotFound(override val message: String) : AppError(message)
    class Unknown(override val message: String) : AppError(message)
}

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
