package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.SalesViewModel
import com.example.ui.SalesViewModelFactory
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.NewSaleScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.SalesHistoryScreen
import com.example.ui.screens.StorefrontScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: SalesViewModel = viewModel(
                    factory = SalesViewModelFactory(application)
                )
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: SalesViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val lastSaleReceipt by viewModel.lastSaleReceipt.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.VENTE,
                    onClick = { viewModel.selectTab(AppTab.VENTE) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.VENTE) Icons.Filled.PointOfSale else Icons.Outlined.PointOfSale,
                            contentDescription = "Caisse"
                        )
                    },
                    label = { Text("Vendre", fontWeight = if (currentTab == AppTab.VENTE) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_vente"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.PRODUITS,
                    onClick = { viewModel.selectTab(AppTab.PRODUITS) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.PRODUITS) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = "Produits"
                        )
                    },
                    label = { Text("Produits", fontWeight = if (currentTab == AppTab.PRODUITS) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_produits"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORIQUE,
                    onClick = { viewModel.selectTab(AppTab.HISTORIQUE) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.HISTORIQUE) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                            contentDescription = "Historique"
                        )
                    },
                    label = { Text("Ventes", fontWeight = if (currentTab == AppTab.HISTORIQUE) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_historique"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.VITRINE,
                    onClick = { viewModel.selectTab(AppTab.VITRINE) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.VITRINE) Icons.Filled.Storefront else Icons.Outlined.Storefront,
                            contentDescription = "Vitrine"
                        )
                    },
                    label = { Text("Vitrine", fontWeight = if (currentTab == AppTab.VITRINE) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_vitrine"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.VENTE -> NewSaleScreen(viewModel = viewModel)
                AppTab.PRODUITS -> ProductsScreen(viewModel = viewModel)
                AppTab.HISTORIQUE -> SalesHistoryScreen(viewModel = viewModel)
                AppTab.VITRINE -> StorefrontScreen(viewModel = viewModel)
            }
        }
    }

    // Modal Receipt Dialog on newly completed sale
    lastSaleReceipt?.let { receipt ->
        ReceiptDialog(
            saleWithItems = receipt,
            isNewlyCreated = true,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}
