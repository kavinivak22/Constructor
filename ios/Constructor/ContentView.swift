import SwiftUI
import UIKit

#if canImport(SharedApp)
import SharedApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
#endif

struct ContentView: View {
    var body: some View {
        #if canImport(SharedApp)
        ComposeView()
            .ignoresSafeArea(.all)
        #else
        NativeFallbackView()
        #endif
    }
}

struct NativeFallbackView: View {
    var body: some View {
        ZStack {
            Color(red: 241/255, green: 245/255, blue: 249/255)
                .ignoresSafeArea()
            
            VStack(spacing: 16) {
                Text("🏗️")
                    .font(.system(size: 60))
                Text("Constructor Native iOS")
                    .font(.title2.bold())
                    .foregroundColor(Color(red: 15/255, green: 23/255, blue: 42/255))
                Text("Compose Multiplatform Architecture Initialized")
                    .font(.subheadline)
                    .foregroundColor(Color(red: 100/255, green: 116/255, blue: 139/255))
            }
        }
    }
}

#Preview {
    ContentView()
}
