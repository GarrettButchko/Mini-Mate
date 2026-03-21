package com.garrettbutchko.minimate.utliities

import com.garrettbutchko.minimate.repositories.userRepos.RemoteUserRepository
import com.garrettbutchko.minimate.viewModels.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.Foundation.NSError
import platform.StoreKit.SKPayment
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKPaymentTransaction
import platform.StoreKit.SKPaymentTransactionObserverProtocol
import platform.StoreKit.SKProduct
import platform.StoreKit.SKProductsRequest
import platform.StoreKit.SKProductsRequestDelegateProtocol
import platform.StoreKit.SKProductsResponse
import platform.StoreKit.SKRequest
import platform.StoreKit.SKPaymentTransactionState
import platform.darwin.NSObject

class IAPManager : NSObject(), SKProductsRequestDelegateProtocol, SKPaymentTransactionObserverProtocol {
    private val _products = MutableStateFlow<List<SKProduct>>(emptyList())
    val products: StateFlow<List<SKProduct>> = _products.asStateFlow()

    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Main)

    private var currentAuthModel: AuthViewModel? = null
    private var currentPurchaseCompletion: ((Boolean) -> Unit)? = null

    // Call this after UI is rendered
    fun initialize() {
        println("Retrieving products")
        if (isInitialized) return
        isInitialized = true

        SKPaymentQueue.defaultQueue().addTransactionObserver(this)
        retrieveProducts()
    }

    fun retrieveProducts() {
        val productIDs = setOf("com.minimate.pro")
        val request = SKProductsRequest(productIdentifiers = productIDs)
        request.delegate = this
        request.start()
    }

    override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
        val fetchedProducts = didReceiveResponse.products.filterIsInstance<SKProduct>()
        _products.value = fetchedProducts
        println("Products retrieved: ${fetchedProducts.map { it.localizedTitle }}")
    }

    override fun request(request: SKRequest, didFailWithError: NSError) {
        println("Failed to fetch products: ${didFailWithError.localizedDescription}")
    }

    fun purchase(
        product: SKProduct,
        authModel: AuthViewModel,
        completion: (Boolean) -> Unit
    ) {
        currentAuthModel = authModel
        currentPurchaseCompletion = completion

        val payment = SKPayment.paymentWithProduct(product)
        SKPaymentQueue.defaultQueue().addPayment(payment)
    }

    override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
        for (transaction in updatedTransactions.filterIsInstance<SKPaymentTransaction>()) {
            when (transaction.transactionState) {
                SKPaymentTransactionState.SKPaymentTransactionStatePurchased,
                SKPaymentTransactionState.SKPaymentTransactionStateRestored -> {
                    handleSuccessfulTransaction(transaction)
                    queue.finishTransaction(transaction)
                }
                SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                    println("Transaction failed: ${transaction.error?.localizedDescription}")
                    queue.finishTransaction(transaction)
                    currentPurchaseCompletion?.invoke(false)
                    clearPurchaseState()
                }
                else -> {
                    // Pending or other states
                }
            }
        }
    }

    private fun handleSuccessfulTransaction(transaction: SKPaymentTransaction) {
        println("Transaction processed: ${transaction.payment.productIdentifier}")
        val authModel = currentAuthModel

        if (authModel != null) {
            val userModel = authModel.userModel.value
            if (userModel != null) {
                val updatedUser = userModel.copy(isPro = true)
                authModel.setUserModel(updatedUser)

                scope.launch {
                    val result = RemoteUserRepository().save(updatedUser)
                    if (result.isSuccess) {
                        println("Updated online user")
                    } else {
                        println("Failed to update online user")
                    }
                }
            }
        }

        currentPurchaseCompletion?.invoke(true)
        clearPurchaseState()
    }

    private fun clearPurchaseState() {
        currentAuthModel = null
        currentPurchaseCompletion = null
    }

    fun isPurchasedPro(authModel: AuthViewModel) {
        // In StoreKit 1, checking if a user has purchased a non-consumable
        // usually requires verifying the local receipt.
        // For simplicity, we can rely on restoring purchases or the app's 
        // existing knowledge of the user's pro status.
        // A true robust check would validate the NSBundle.mainBundle.appStoreReceiptURL.
        
        // As a fallback, we can trigger a restore (which will call updatedTransactions with Restored).
        // If the user is already pro in the model, we can just ensure it.
        val userModel = authModel.userModel.value
        if (userModel?.isPro == true) {
            // Already pro
            return
        }
    }

    fun restorePurchases(authModel: AuthViewModel, completion: (Boolean) -> Unit) {
        currentAuthModel = authModel
        currentPurchaseCompletion = completion
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
    }

    override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
        println("Restore completed successfully")
        // If no transactions were restored, handle it
        if (currentPurchaseCompletion != null) {
            // The updatedTransactions callback handles individual restored items.
            // But if there were 0 items to restore, we should call completion(false).
            // Usually we'd track if any items were restored.
            currentPurchaseCompletion?.invoke(false)
            clearPurchaseState()
        }
    }

    override fun paymentQueue(queue: SKPaymentQueue, restoreCompletedTransactionsFailedWithError: NSError) {
        println("Restore failed: ${restoreCompletedTransactionsFailedWithError.localizedDescription}")
        currentPurchaseCompletion?.invoke(false)
        clearPurchaseState()
    }
}
