//
//  IAPManager.swift
//  MiniMate
//
//  Created by Garrett Butchko on 6/2/25.
//
import StoreKit
import SwiftUI
import Combine
import shared_user

class IAPManager: ObservableObject {
   @Published var products: [Product] = []
   @Published var isPurchasing = false
   private var isInitialized = false
   
   init() {
       // Lightweight init - defer heavy operations
   }
   
   // Call this after UI is rendered
   func initialize() async {
       print("Retrieving products")
       guard !isInitialized else { return }
       isInitialized = true
       
       await self.retrieveProducts()

       // Listen in the background so init doesn't block forever.
       Task { [weak self] in
           await self?.listenForTransactions()
       }
   }
   
    @MainActor
    func retrieveProducts() async {
        do {
            let productIDs = ["com.minimate.pro"]
            let fetchedProducts = try await Product.products(for: productIDs)
            self.products = fetchedProducts
            print("Products retrieved: \(fetchedProducts.map { $0.displayName })")
        } catch {
            print("Failed to fetch products: \(error)")
        }
    }
    
    func purchase(_ product: Product, authModel: AuthViewModelSwift, showSheet: Binding<Bool>) async -> Bool {
        print("🛒 Purchase started for product: \(product.id)")
        
        do {
            let result = try await product.purchase()
            
            switch result {
            case .success(let verification):
                print("✅ StoreKit: Purchase call successful. Verifying...")
                
                // Handle verification results explicitly
                let transaction: StoreKit.Transaction
                do {
                    transaction = try self.verifyPurchase(verification)
                    print("🛡️ Verification: Success for Transaction \(transaction.id)")
                } catch {
                    print("❌ Verification: Failed. The purchase might be tampered with or invalid: \(error)")
                    return false
                }
                
                // Finish the transaction first to tell Apple you've acknowledged it
                await transaction.finish()
                print("🏁 Transaction finished with Apple.")
                
                // Update UI
                await MainActor.run {
                    withAnimation {
                        authModel.userModel?.isPro = true
                        showSheet.wrappedValue = false
                        print("📱 UI: Pro status set and sheet dismissed.")
                    }
                }
                
                // Sync with your backend
                if let userModel = authModel.userModel {
                    print("☁️ Remote: Attempting to save user to KoinHelper...")
                    do {
                        _ = try await KoinHelperParent.shared.getRemoteUserRepo().save(userModel: userModel, updateLastUpdated: true)
                        print("✅ Remote: User saved successfully.")
                    } catch {
                        print("⚠️ Remote: Database sync failed, but purchase was successful locally: \(error)")
                        // Note: You might still return true here because they DID pay.
                    }
                } else {
                    print("⚠️ Remote: No userModel found in authModel to save.")
                }
                
                return true
                
            case .userCancelled:
                print("🛑 Purchase: User cancelled the payment sheet.")
                return false
                
            case .pending:
                print("⏳ Purchase: Transaction pending (e.g., Ask to Buy enabled).")
                return false
                
            @unknown default:
                print("❓ Purchase: Unknown status received.")
                return false
            }
            
        } catch {
            // This catches system-level errors (network down, iCloud issues, etc.)
            print("🚨 Purchase: System error: \(error.localizedDescription)")
            return false
        }
    }
    
    func listenForTransactions() async {
        for await update in Transaction.updates {
            do {
                let transaction = try self.verifyPurchase(update)
                // Update your app state here (e.g., unlock premium features)
                await transaction.finish()
                print("Transaction processed: \(transaction.productID)")
            } catch {
                print("Transaction verification failed: \(error)")
            }
        }
    }
    
   private func verifyPurchase(_ verification: VerificationResult<StoreKit.Transaction>) throws -> StoreKit.Transaction {
       switch verification {
       case .unverified:
           throw NSError(domain: "Verification failed", code: 1, userInfo: nil)
       case .verified(let transaction):
           return transaction
       }
   }
    
    func isPurchasedPro(authModel: AuthViewModelSwift) async {
        var hasPro = false

        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result,
               transaction.productID == "com.minimate.pro" {
                hasPro = true
                break
            }
        }
        withAnimation {
            authModel.userModel?.isPro = hasPro
        }
    }
}
