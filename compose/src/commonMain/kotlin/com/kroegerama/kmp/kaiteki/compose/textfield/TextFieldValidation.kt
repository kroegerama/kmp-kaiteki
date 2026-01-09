package com.kroegerama.kmp.kaiteki.compose.textfield

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Composable
public fun rememberValidatingTextFieldState(
    initialText: String = "",
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(CharSequence) -> Unit
): ValidatingTextFieldState {
    val scope = rememberCoroutineScope()
    return retain {
        ValidatingTextFieldState(
            scope = scope,
            initialText = initialText,
            autoClearError = autoClearError,
            validator = validator
        )
    }
}

@Composable
public fun <T> rememberSimpleValidatingState(
    state: State<T>,
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(T) -> Unit
): SimpleValidatingState<T> {
    val scope = rememberCoroutineScope()
    return retain {
        SimpleValidatingState(
            scope = scope,
            state = state,
            autoClearError = autoClearError,
            validator = validator
        )
    }
}

@RememberInComposition
public fun ViewModel.ValidatingTextFieldState(
    initialText: String = "",
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(CharSequence) -> Unit
): ValidatingTextFieldState = ValidatingTextFieldState(
    scope = viewModelScope,
    initialText = initialText,
    autoClearError = autoClearError,
    validator = validator
)

@RememberInComposition
public fun <T> ViewModel.SimpleValidatingState(
    state: State<T>,
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(T) -> Unit
): SimpleValidatingState<T> = SimpleValidatingState(
    scope = viewModelScope,
    state = state,
    autoClearError = autoClearError,
    validator = validator
)

@Stable
public class ValidatingTextFieldState @RememberInComposition constructor(
    scope: CoroutineScope,
    initialText: String = "",
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(CharSequence) -> Unit
) : BaseValidatingState<CharSequence>(validator) {
    public val state: TextFieldState = TextFieldState(initialText)
    public val text: CharSequence by state::text
    public val string: String by state::string
    override val value: CharSequence by state::text

    init {
        if (autoClearError) {
            scope.launch {
                snapshotFlow { value }.collect { clearError() }
            }
        }
    }

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
    scope: CoroutineScope,
    state: State<T>,
    autoClearError: Boolean = true,
    validator: ValidationRaiserScope.(T) -> Unit
) : BaseValidatingState<T>(validator) {
    override val value: T by state

    init {
        if (autoClearError) {
            scope.launch {
                snapshotFlow { value }.collect { clearError() }
            }
        }
    }
}

public abstract class BaseValidatingState<T>(
    private val validator: ValidationRaiserScope.(T) -> Unit
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
