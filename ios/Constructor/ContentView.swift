import SwiftUI
import WebKit

struct ContentView: View {
    var body: some View {
        ConstructorWebView(urlString: "https://constructor-beta.vercel.app")
            .ignoresSafeArea()
    }
}

struct ConstructorWebView: UIViewRepresentable {
    let urlString: String

    func makeUIView(context: Context) -> WKWebView {
        let preferences = WKWebpagePreferences()
        preferences.allowsContentJavaScript = true
        
        let configuration = WKWebViewConfiguration()
        configuration.defaultWebpagePreferences = preferences
        configuration.allowsInlineMediaPlayback = true

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.scrollView.bounces = true
        webView.isOpaque = false
        webView.backgroundColor = .clear

        if let url = URL(string: urlString) {
            let request = URLRequest(url: url)
            webView.load(request)
        }
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}
}

#Preview {
    ContentView()
}
