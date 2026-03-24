import SwiftUI
import GoogleMobileAds

struct BannerAdView: UIViewRepresentable {
    let adUnitID: String

    func makeUIView(context: Context) -> BannerView {
        let bannerView = BannerView(adSize: AdSizeBanner)
        bannerView.adUnitID = adUnitID
        bannerView.rootViewController = UIApplication.shared.getRootViewController()
        bannerView.delegate = context.coordinator
        bannerView.load(Request())
        return bannerView
    }

    func updateUIView(_ uiView: BannerView, context: Context) {
        // No update needed
    }

    func makeCoordinator() -> Coordinator {
        return Coordinator()
    }

    class Coordinator: NSObject, BannerViewDelegate {
        // Optional: Implement delegate methods for logging, etc.
    }
}

extension UIApplication {
    func getRootViewController() -> UIViewController? {
        guard let scene = connectedScenes.first as? UIWindowScene,
              let root = scene.windows.first(where: \.isKeyWindow)?.rootViewController else {
            return nil
        }
        return root
    }
}
