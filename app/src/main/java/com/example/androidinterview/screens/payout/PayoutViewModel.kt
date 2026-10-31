package com.example.androidinterview.screens.payout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidinterview.core.formatting.toCurrencyText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import manoellribeiro.dev.domain.model.Currency
import manoellribeiro.dev.domain.usecase.CreatePayoutUseCase
import manoellribeiro.dev.domain.usecase.IsPayoutFormCompleteUseCase
import manoellribeiro.dev.domain.usecase.MaskIbanUseCase
import manoellribeiro.dev.domain.usecase.ObserveIsOnlineUseCase
import manoellribeiro.dev.domain.usecase.ParsePayoutAmountUseCase
import manoellribeiro.dev.domain.usecase.ValidateIbanUseCase

class PayoutViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val isPayoutFormComplete: IsPayoutFormCompleteUseCase,
    private val parsePayoutAmount: ParsePayoutAmountUseCase,
    private val validateIban: ValidateIbanUseCase,
    private val maskIban: MaskIbanUseCase,
    private val createPayout: CreatePayoutUseCase,
    private val observeIsOnline: ObserveIsOnlineUseCase,
) : ViewModel() {

    private var isOnline = true
    private var observeIsOnlineJob: Job? = null

    private val _uiState: MutableStateFlow<PayoutUiState> = MutableStateFlow(savedForm())
    val uiState: StateFlow<PayoutUiState> = _uiState.asStateFlow()

    private fun emitNewState(newState: PayoutUiState) {
        _uiState.value = newState
    }

    fun startObservingConnectivity() {
        if (observeIsOnlineJob?.isActive == true) return
        observeIsOnlineJob = viewModelScope.launch {
            observeIsOnline().collect { online ->
                isOnline = online
                val form = _uiState.value as? PayoutUiState.Form ?: return@collect
                emitForm(form.copy(isOnline = online))
            }
        }
    }

    fun onAmountChange(amount: String) {
        val form = _uiState.value as? PayoutUiState.Form ?: return
        savedStateHandle[KEY_AMOUNT] = amount
        emitForm(form.copy(amount = amount, hasAmountError = false))
    }

    fun onCurrencyChange(currency: Currency) {
        val form = _uiState.value as? PayoutUiState.Form ?: return
        savedStateHandle[KEY_CURRENCY] = currency
        emitForm(form.copy(currency = currency, hasAmountError = false))
    }

    fun onIbanChange(input: String) {
        val form = _uiState.value as? PayoutUiState.Form ?: return
        // IBANs are often pasted with the spaces they're printed with.
        val iban = input.filterNot(Char::isWhitespace)
        savedStateHandle[KEY_IBAN] = iban
        emitForm(form.copy(iban = iban, hasIbanError = false))
    }

    /** Opens the review dialog if the form is valid, otherwise shows the inline errors. */
    fun onConfirmClick() {
        val form = _uiState.value as? PayoutUiState.Form ?: return
        val amount = parsePayoutAmount(form.amount, form.currency)
        val isIbanValid = validateIban(form.iban)
        emitForm(
            form.copy(
                hasAmountError = amount == null,
                hasIbanError = !isIbanValid,
                confirmation = if (amount != null && isIbanValid) {
                    amount.mapToPayoutConfirmationState(maskIban(form.iban))
                } else {
                    null
                },
            )
        )
    }

    fun onCancelConfirmation() {
        val form = _uiState.value as? PayoutUiState.Form ?: return
        if (form.confirmation?.isSubmitting == false) emitForm(form.copy(confirmation = null))
    }

    fun onSubmit() {
        val form = _uiState.value as? PayoutUiState.Form ?: return
        val confirmation = form.confirmation ?: return
        // Reaching here twice would send the payout twice.
        if (confirmation.isSubmitting || !form.isOnline) return
        val amount = parsePayoutAmount(form.amount, form.currency) ?: return
        emitForm(form.copy(confirmation = confirmation.copy(isSubmitting = true)))
        viewModelScope.launch {
            createPayout(amount, form.iban).fold(
                onSuccess = { payout ->
                    // Cleared now, so process death can't bring back the form of a payout that already went through.
                    clearSavedForm()
                    emitNewState(PayoutUiState.Success(payout.amount.toCurrencyText()))
                },
                onFailure = { error -> emitNewState(PayoutUiState.Failure(error.mapToPayoutFailureReason())) },
            )
        }
    }

    /** Try Again returns to the filled form and doesn't resend: the merchant reviews the payout before each attempt. */
    fun onStartAgain() {
        emitNewState(savedForm())
    }

    private fun emitForm(form: PayoutUiState.Form) {
        emitNewState(form.copy(isConfirmEnabled = isPayoutFormComplete(form.amount, form.iban) && form.isOnline))
    }

    private fun savedForm(): PayoutUiState.Form {
        val amount = savedStateHandle.get<String>(KEY_AMOUNT).orEmpty()
        val iban = savedStateHandle.get<String>(KEY_IBAN).orEmpty()
        return PayoutUiState.Form(
            amount = amount,
            currency = savedStateHandle[KEY_CURRENCY] ?: Currency.GBP,
            iban = iban,
            isOnline = isOnline,
            isConfirmEnabled = isPayoutFormComplete(amount, iban) && isOnline,
        )
    }

    private fun clearSavedForm() {
        savedStateHandle.remove<String>(KEY_AMOUNT)
        savedStateHandle.remove<Currency>(KEY_CURRENCY)
        savedStateHandle.remove<String>(KEY_IBAN)
    }

    private companion object {
        const val KEY_AMOUNT = "amount"
        const val KEY_CURRENCY = "currency"
        const val KEY_IBAN = "iban"
    }
}
