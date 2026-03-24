//
//  InterstitialAdView.swift
//  MiniMate
//
//  Created by Garrett Butchko on 5/31/25.
//


import SwiftUI
import GoogleMobileAds

struct InterstitialAdView: UIViewControllerRepresentable {
    /// Your real Ad Unit ID here
    let adUnitID: String
    /// Called when the ad is dismissed
    var onDismiss: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIViewController(context: Context) -> UIViewController {
        let vc = UIViewController()
        vc.view.backgroundColor = .clear

        // Load and present the ad after slight delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            InterstitialAdView.loadAd(adUnitID: adUnitID) { ad in
                if let ad = ad {
                    ad.fullScreenContentDelegate = context.coordinator
                    ad.present(from: vc)
                } else {
                    // Failed to load ad, just dismiss immediately
                    onDismiss()
                }
            }
        }

        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    // Load interstitial helper
    private static func loadAd(adUnitID: String, completion: @escaping (InterstitialAd?) -> Void) {
        let request = Request()
        InterstitialAd.load(with: adUnitID, request: request) { ad, error in
            if let error = error {
                print("Failed to load interstitial ad: \(error.localizedDescription)")
                completion(nil)
                return
            }
            completion(ad)
        }
    }

    class Coordinator: NSObject, FullScreenContentDelegate {
        var parent: InterstitialAdView

        init(_ parent: InterstitialAdView) {
            self.parent = parent
        }

        func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
            parent.onDismiss()
        }

        func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
            print("Ad failed to present with error: \(error.localizedDescription)")
            parent.onDismiss()
        }
    }
}
