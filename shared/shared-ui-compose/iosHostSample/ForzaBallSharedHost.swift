import SwiftUI
import SharedUiCompose

struct ForzaBallSharedHost: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ForzaBallSharedHost_Previews: PreviewProvider {
    static var previews: some View {
        ForzaBallSharedHost()
            .ignoresSafeArea()
    }
}
