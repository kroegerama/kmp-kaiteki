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

/**
 * Creates and retains a [ValidatingTextFieldState] backed by a [TextFieldState], running
 * [validator] against the current text to derive its error.
 *
 * @param initialText Initial text of the field.
 * @param autoClearError Whether the error is cleared automatically whenever the text changes.
 * @param validator Validation logic; call [ValidationRaiserScope.raise] or
 * [ValidationRaiserScope.require] to flag the value as invalid.
 */
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

/**
 * Creates and retains a [SimpleValidatingState] that validates an arbitrary observable [state]
 * value instead of a text field, e.g. a checkbox, dropdown selection or date.
 *
 * @param state Source value to validate.
 * @param autoClearError Whether the error is cleared automatically whenever the value changes.
 * @param validator Validation logic for the value.
 */
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

/**
 * Creates a [ValidatingTextFieldState] owned by this [ViewModel], so the field state and its
 * validation survive configuration changes. Use [rememberValidatingTextFieldState] instead when the
 * state should live in the composition.
 *
 * @param initialText Initial text of the field.
 * @param autoClearError Whether the error is cleared automatically whenever the text changes.
 * @param validator Validation logic for the text.
 */
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

/**
 * Creates a [SimpleValidatingState] owned by this [ViewModel]. Use [rememberSimpleValidatingState]
 * instead when the state should live in the composition.
 *
 * @param state Source value to validate.
 * @param autoClearError Whether the error is cleared automatically whenever the value changes.
 * @param validator Validation logic for the value.
 */
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

/**
 * A [Validator] wrapping a [TextFieldState]. Pass [state] to a text field composable and read
 * [isError] / [validationError] to reflect validation results in the UI.
 */
@Stable
public class ValidatingTextFieldState @RememberInComposition constructor(
    initialText: String = "",
    validator: ValidationRaiserScope.(CharSequence) -> Unit
) : BaseValidatingState<CharSequence>(validator) {
    /** Underlying [TextFieldState] to pass to a text field composable. */
    public val state: TextFieldState = TextFieldState(initialText)

    /** Current text of the field. */
    public val text: CharSequence by state::text

    /** Current text of the field as a [String]. */
    public val string: String by state::string
    override val value: CharSequence by state::text

    /** Clears the text and any current error. */
    public fun clear() {
        state.clearText()
        clearError()
    }

    /** Trims leading and trailing whitespace from the text. */
    public fun trim() {
        state.trim()
    }
}

/**
 * A [Validator] over an arbitrary observable value, for validating non-text inputs such as a
 * checkbox, dropdown selection or date.
 */
@Stable
public class SimpleValidatingState<T> @RememberInComposition constructor(
    state: State<T>,
    validator: ValidationRaiserScope.(T) -> Unit
) : BaseValidatingState<T>(validator) {
    internal var source: State<T> by mutableStateOf(state)
    override val value: T get() = source.value
}

/**
 * Base [Validator] implementation shared by [ValidatingTextFieldState] and [SimpleValidatingState].
 * Runs [validator] against [value] and exposes the resulting error state.
 */
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

/**
 * Validates all [validators] and returns a combined [TextFieldValidationResult]. Every validator is
 * run (so all error messages update) regardless of how many already failed.
 *
 * @return result carrying the number of validators that failed.
 */
public fun validate(vararg validators: Validator): TextFieldValidationResult {
    val errorCount = validators.count { validator ->
        !validator.validate()
    }
    return TextFieldValidationResult(errorCount)
}

private typealias ErrorGeneratorLambda = @Composable () -> String?

/** DSL marker for the validation builder scope. */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
public annotation class ValidationDSL

/** Receiver scope of a validator lambda, used to flag the validated value as invalid. */
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

    /** Raises an error without a message unless [condition] holds. */
    public fun require(condition: Boolean) {
        if (!condition) raise()
    }

    /** Raises an error with the given [error] message unless [condition] holds. */
    public fun require(condition: Boolean, error: String) {
        if (!condition) raise(error)
    }

    /** Raises an error whose message is produced by [block] unless [condition] holds. */
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

/** Something whose validity can be evaluated and whose error state can be observed from Compose. */
public interface Validator {
    /** Whether the last [validate] call found the value invalid. */
    public val isError: Boolean

    /**
     * Message for the current error, or null when valid. Composable so that error strings can be
     * resolved (e.g. localized) lazily at read time.
     */
    public val validationError: String?
        @Composable get

    /** Runs validation, updates [isError] / [validationError], and returns true if the value is valid. */
    public fun validate(): Boolean

    /** Clears the current error without re-running validation. */
    public fun clearError()
}

/** Aggregate result of validating multiple [Validator]s, produced by [validate]. */
@Immutable
public data class TextFieldValidationResult(
    val errorCount: Int
) {
    /** Runs [block] if every validator passed, and returns this result for chaining. */
    public inline fun onValid(block: () -> Unit): TextFieldValidationResult {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return also { if (errorCount == 0) block() }
    }

    /** Runs [block] with the number of failed validators if any failed, and returns this result. */
    public inline fun onError(block: (Int) -> Unit): TextFieldValidationResult {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return also { if (errorCount > 0) block(errorCount) }
    }
}
