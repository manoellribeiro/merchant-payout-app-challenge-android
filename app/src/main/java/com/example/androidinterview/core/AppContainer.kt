package com.example.androidinterview.core

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.androidinterview.screens.home.HomeViewModel
import manoellribeiro.dev.data.DataLayerFactory
import manoellribeiro.dev.domain.usecase.GetActivityUseCase
import manoellribeiro.dev.domain.usecase.GetBalanceUseCase

/** The composition root: the only place that knows :data, and where implementations are chosen. */
class AppContainer(baseUrl: String) {

    private val merchantRepository = DataLayerFactory.createMerchantRepository(baseUrl)

    val homeViewModelFactory = viewModelFactory {
        initializer {
            HomeViewModel(
                getBalance = GetBalanceUseCase(merchantRepository),
                getActivity = GetActivityUseCase(merchantRepository),
            )
        }
    }
}
