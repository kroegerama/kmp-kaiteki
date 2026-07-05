package com.kroegerama.kmp.kaiteki.compose.textfield

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Composable
public fun rememberValidatingTextFieldState(
    initialText: String = "",
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(CharSequence) -> Unit
): ValidatingTextFieldState {
    val result = retain {
        ValidatingTextFieldState(initialText = initialText, validator = validator)
    }
    result.validator = validator
    if (autoClearError) {
        LaunchedEffect(result) { result.autoClearErrors() }
    }
    return result
}

@Composable
public fun <T> rememberSimpleValidatingState(
    state: State<T>,
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(T) -> Unit
): SimpleValidatingState<T> {
    val result = retain {
        SimpleValidatingState(state = state, validator = validator)
    }
    result.source = state
    result.validator = validator
    if (autoClearError) {
        LaunchedEffect(result) { result.autoClearErrors() }
    }
    return result
}

@RememberInComposition
public fun ViewModel.ValidatingTextFieldState(
    initialText: String = "",
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(CharSequence) -> Unit
): ValidatingTextFieldState = createValidatingTextFieldState(
    viewModel = this,
    initialText = initialText,
    autoClearError = autoClearError,
    validator = validator
)

private fun createValidatingTextFieldState(
    viewModel: ViewModel,
    initialText: String = "",
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(CharSequence) -> Unit
): ValidatingTextFieldState = ValidatingTextFieldState(
    initialText = initialText,
    validator = validator
).also { validatingState ->
    if (autoClearError) {
        viewModel.viewModelScope.launch { validatingState.autoClearErrors() }
    }
}

@RememberInComposition
public fun <T> ViewModel.SimpleValidatingState(
    state: State<T>,
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(T) -> Unit
): SimpleValidatingState<T> = createSimpleValidatingState(
    viewModel = this,
    state = state,
    autoClearError = autoClearError,
    validator = validator
)

private fun <T> createSimpleValidatingState(
    viewModel: ViewModel,
    state: State<T>,
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(T) -> Unit
): SimpleValidatingState<T> = SimpleValidatingState(
    state = state,
    validator = validator
).also { validatingState ->
    if (autoClearError) {
        viewModel.viewModelScope.launch { validatingState.autoClearErrors() }
    }
}

@Stable
public class ValidatingTextFieldState @RememberInComposition constructor(
    initialText: String = "",
    validator: ValidationRaiserScope.(CharSequence) -> Unit
) : BaseValidatingState<CharSequence>(validator) {
    public val state: TextFieldState = TextFieldState(initialText)
    public val text: CharSequence by state::text
    public val string: String by state::string
    override val value: CharSequence by state::text

    public fun clear() {
        state.clearText()
        clearError()
    }

    public fun trim() {
        state.trim()
    }
}

@Stable
public class SimpleValidatingState<T> @RememberInComposition constructor(
    state: State<T>,
    validator: ValidationRaiserScope.(T) -> Unit
) : BaseValidatingState<T>(validator) {
    internal var source: State<T> by mutableStateOf(state)
    override val value: T get() = source.value
}

public abstract class BaseValidatingState<T>(
    internal var validator: (@ValidationDSL ValidationRaiserScope).(T) -> Unit
) : Validator {
    private var validationResultState: ErrorGeneratorLambda? by mutableStateOf(null)

    override val isError: Boolean
        get() = validationResultState != null

    override val validationError: String?
        @Composable get() = validationResultState?.invoke()

    protected abstract val value: T

    override fun validate(): Boolean = ValidationRaiserImpl().apply {
        try {
            validator(this, value)
        } catch (_: ValidationRaiserCancellationException) {
            // no-op
        }
    }.also {
        validationResultState = it.raised
    }.raised == null

    override fun clearError() {
        validationResultState = null
    }

    internal suspend fun autoClearErrors() {
        snapshotFlow { value }.collect { clearError() }
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
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
public annotation class ValidationDSL

@ValidationDSL
public interface ValidationRaiserScope {
    /**
     * Raise error without error message.
     */
    public fun raise(): Nothing

    /**
     * Raise error hardcoded string.
     */
    public fun raise(string: String): Nothing

    /**
     * Raise error by executing a composable lambda.
     */
    public fun raise(block: ErrorGeneratorLambda): Nothing

    public fun require(condition: Boolean) {
        if (!condition) raise()
    }

    public fun require(condition: Boolean, error: String) {
        if (!condition) raise(error)
    }

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

@Immutable
public data class TextFieldValidationResult(
    val errorCount: Int
) {
    public inline fun onValid(block: () -> Unit): TextFieldValidationResult {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return also { if (errorCount == 0) block() }
    }

    public inline fun onError(block: (Int) -> Unit): TextFieldValidationResult {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return also { if (errorCount > 0) block(errorCount) }
    }
}
