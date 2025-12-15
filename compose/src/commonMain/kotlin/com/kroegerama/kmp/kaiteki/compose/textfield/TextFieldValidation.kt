package com.kroegerama.kmp.kaiteki.compose.textfield

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CancellationException

@Composable
public fun rememberValidatingTextFieldState(
    initialText: String = "",
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(String) -> Unit
): TextFieldValidator {
    val state: TextFieldState = rememberTextFieldState(initialText = initialText)
    val validationResultState = remember { mutableStateOf<ErrorGeneratorLambda?>(null) }
    val updatedValidator = rememberUpdatedState(validator)

    if (autoClearError) {
        LaunchedEffect(Unit) {
            snapshotFlow { state.text }.collect { validationResultState.value = null }
        }
    }

    return remember {
        TextFieldValidatorImpl(
            state = state,
            validationResultState = validationResultState,
            validatorState = updatedValidator
        )
    }
}

@Composable
public fun <T> rememberSimpleValidatingState(
    state: State<T>,
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(T) -> Unit
): Validator {
    val validationResultState = remember { mutableStateOf<ErrorGeneratorLambda?>(null) }
    val updatedValidator = rememberUpdatedState(validator)

    if (autoClearError) {
        LaunchedEffect(Unit) {
            snapshotFlow { state.value }.collect { validationResultState.value = null }
        }
    }

    return remember {
        SimpleValidatorImpl(
            state = state,
            validationResultState = validationResultState,
            validatorState = updatedValidator
        )
    }
}

public fun validate(vararg validators: Validator): TextFieldValidationResult {
    val errorCount = validators.count { validator ->
        !validator.validate()
    }
    return TextFieldValidationResult(errorCount)
}

private typealias ErrorGeneratorLambda = @Composable () -> String?

@DslMarker
public annotation class ValidationDSL

public interface ValidationRaiserScope {
    /**
     * Raise error without error message.
     */
    @ValidationDSL
    public fun raise(): Nothing

    /**
     * Raise error hardcoded string.
     */
    @ValidationDSL
    public fun raise(string: String): Nothing

    /**
     * Raise error by executing a composable lambda.
     */
    @ValidationDSL
    public fun raise(block: ErrorGeneratorLambda): Nothing

    @ValidationDSL
    public fun require(condition: Boolean) {
        if (!condition) raise()
    }

    @ValidationDSL
    public fun require(condition: Boolean, error: String) {
        if (!condition) raise(error)
    }

    @ValidationDSL
    public fun require(condition: Boolean, block: ErrorGeneratorLambda) {
        if (!condition) raise(block)
    }
}

private interface ValidationRaiserProvider {
    val raised: ErrorGeneratorLambda?
}

private class ValidationRaiserImpl : ValidationRaiserScope, ValidationRaiserProvider {

    override var raised: ErrorGeneratorLambda? = null

    override fun raise(): Nothing = raise { null }

    override fun raise(string: String): Nothing = raise { string }

    override fun raise(block: ErrorGeneratorLambda): Nothing {
        raised = block
        throw ValidationRaiserCancellationException()
    }
}

private class ValidationRaiserCancellationException : CancellationException("Validation failed")

public interface Validator {
    public val isError: Boolean
    public val validationError: String?
        @Composable get

    public fun validate(): Boolean
    public fun clearError()
}

public interface TextFieldValidator : Validator {
    public val state: TextFieldState
    public val text: CharSequence get() = state.text
    public val string: String get() = state.string
    public fun trim(): Unit = state.trim()
}

@Immutable
private data class SimpleValidatorImpl<T>(
    private val state: State<T>,
    private val validationResultState: MutableState<ErrorGeneratorLambda?>,
    private val validatorState: State<ValidationRaiserScope.(T) -> Unit>
) : Validator {
    override val isError get() = validationResultState.value != null
    override val validationError
        @Composable get() = validationResultState.value?.invoke()

    override fun validate(): Boolean = ValidationRaiserImpl().apply {
        try {
            validatorState.value.invoke(this, state.value)
        } catch (_: ValidationRaiserCancellationException) {
            // no-op
        }
    }.also {
        validationResultState.value = it.raised
    }.raised == null

    override fun clearError() {
        validationResultState.value = null
    }
}

@Immutable
private data class TextFieldValidatorImpl(
    override val state: TextFieldState,
    private val validationResultState: MutableState<ErrorGeneratorLambda?>,
    private val validatorState: State<ValidationRaiserScope.(String) -> Unit>
) : TextFieldValidator {
    override val isError get() = validationResultState.value != null
    override val validationError
        @Composable get() = validationResultState.value?.invoke()

    override fun validate(): Boolean = ValidationRaiserImpl().apply {
        try {
            validatorState.value.invoke(this, state.string)
        } catch (_: ValidationRaiserCancellationException) {
            // no-op
        }
    }.also {
        validationResultState.value = it.raised
    }.raised == null

    override fun clearError() {
        validationResultState.value = null
    }
}

@Immutable
public data class TextFieldValidationResult(
    val errorCount: Int
) {
    public fun onValid(block: () -> Unit): TextFieldValidationResult {
        if (errorCount == 0) {
            block()
        }
        return this
    }

    public fun onError(block: (Int) -> Unit): TextFieldValidationResult {
        if (errorCount > 0) {
            block(errorCount)
        }
        return this
    }
}
