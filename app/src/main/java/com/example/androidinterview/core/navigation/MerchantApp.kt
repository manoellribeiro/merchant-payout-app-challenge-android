package com.example.androidinterview.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.androidinterview.core.AppContainer
import com.example.androidinterview.R
import com.example.androidinterview.screens.home.HomeDestination
import manoellribeiro.dev.design_system.components.bottomNavigation.BottomNavigationTab
import manoellribeiro.dev.design_system.components.bottomNavigation.MerchantBottomNavigationBar
import manoellribeiro.dev.design_system.components.header.ScreenTitle
import manoellribeiro.dev.design_system.theme.MerchantColors
import manoellribeiro.dev.design_system.theme.MerchantDimens
import manoellribeiro.dev.design_system.theme.MerchantPayoutTheme

private const val HOME_TAB_INDEX = 0
private const val PAYOUTS_TAB_INDEX = 1

@Composable
fun MerchantApp(appContainer: AppContainer) {
    MerchantPayoutTheme {
        val backStack = rememberNavBackStack(HomeRoute)
        val homeLabel = stringResource(R.string.tab_home)
        val payoutsLabel = stringResource(R.string.tab_payouts)
        val tabs = remember(homeLabel, payoutsLabel) {
            listOf(
                BottomNavigationTab(homeLabel, manoellribeiro.dev.design_system.R.drawable.ic_home),
                BottomNavigationTab(payoutsLabel, manoellribeiro.dev.design_system.R.drawable.ic_payouts),
            )
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MerchantColors.ScreenBackground,
            bottomBar = {
                MerchantBottomNavigationBar(
                    tabs = tabs,
                    selectedTabIndex = if (PayoutsRoute in backStack) PAYOUTS_TAB_INDEX else HOME_TAB_INDEX,
                    onTabSelected = { index -> backStack.selectTab(index) },
                )
            },
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(innerPadding),
                onBack = { backStack.removeLastOrNull() },
                // The ViewModel decorator scopes each ViewModel to its entry, so it's cleared when the entry is popped.
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<HomeRoute> {
                        HomeDestination(
                            viewModelFactory = appContainer.homeViewModelFactory,
                            onShowMore = dropUnlessResumed { backStack.add(TransactionsRoute) },
                        )
                    }
                    // Placeholders until Step 2 and Step 3 replace them.
                    entry<TransactionsRoute> { PlaceholderScreen(stringResource(R.string.transactions_title)) }
                    entry<PayoutsRoute> { PlaceholderScreen(stringResource(R.string.payouts_title)) }
                },
            )
        }
    }
}

/** Home is always the root, so Back from any tab returns there before leaving the app. */
private fun NavBackStack<NavKey>.selectTab(index: Int) {
    removeAll { it != HomeRoute }
    if (index == PAYOUTS_TAB_INDEX) add(PayoutsRoute)
}

@Composable
private fun PlaceholderScreen(title: String) {
    ScreenTitle(text = title, modifier = Modifier.padding(MerchantDimens.ScreenPadding))
}
