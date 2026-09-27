package com.example.ui

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

sealed class GenerateUiState {
    object Idle : GenerateUiState()
    object Generating : GenerateUiState()
    data class Validating(val message: String = "Validating network and group...") : GenerateUiState()
    data class CreationUnknown(val message: String) : GenerateUiState()
    data class GroupNotSynchronized(val message: String = "Network group is not synchronized.") : GenerateUiState()
    data class ValidationError(val message: String, val missingField: String? = null) : GenerateUiState()
    data class PermissionError(val message: String) : GenerateUiState()
    data class SessionExpired(val message: String) : GenerateUiState()
    data class NetworkError(val message: String) : GenerateUiState()
    data class Success(val count: Int, val message: String = "") : GenerateUiState()
    data class Error(val message: String) : GenerateUiState()
}
