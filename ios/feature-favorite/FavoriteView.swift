import SwiftUI
import shared

// ══════════════════════════════════════════════════════════════════
// FavoriteViewModel - 收藏列表 ViewModel
// 对标 Android 的 FavoriteViewModel.kt（Flow 订阅型）
//
// 数据来源：repository.getFavoriteMovies(): Flow<List<Movie>>
// Flow → Swift 桥接：通过 FlowWrapper.subscribe 回调
//   收藏表任何增删 → Room Flow emit → onEach 回调 → SwiftUI 自动刷新
//
// 🍎 UIKit 类比：NSFetchedResultsController 驱动 UICollectionView
// 🍎 SwiftUI 类比：@FetchRequest(sortItems:) 属性包装器
// ══════════════════════════════════════════════════════════════════

@Observable
final class FavoriteViewModel {
    private(set) var favorites: [Movie] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    private let repository = AppDependencies.repository()
    private var flowWrapper: FlowWrapper<NSArray>?  // Flow<List<Movie>> → NSArray

    init() {
        observeFavorites()
    }

    /// 订阅收藏列表（Room Flow 驱动）
    /// 对标 Android 的 observeFavorites() → repository.getFavoriteMovies().collect
    private func observeFavorites() {
        isLoading = true
        // Kotlin Flow → FlowWrapper（iosMain 的 wrapFlow）
        flowWrapper = wrapFlow(flow: repository.getFavoriteMovies())
        flowWrapper?.subscribe(
            onEach: { [weak self] nsArray in
                // Kotlin List<Movie> → Swift [Movie]
                self?.favorites = nsArray as? [Movie] ?? []
                self?.isLoading = false
            },
            onError: { [weak self] message in
                self?.errorMessage = message
                self?.isLoading = false
            }
        )
    }

    /// 取消收藏（左滑删除）
    /// 对标 Android 的 FavoriteIntent.RemoveFavorite
    /// 写 Room 后 Flow 自动推新列表，UI 自动少一行
    func removeFavorite(_ movie: Movie) {
        Task {
            _ = try await repository.removeFavorite(id: movie.id)
            // 不需要手动改 favorites：Flow 自动回推新列表
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// FavoriteView - 收藏列表 SwiftUI 视图
// 对标 Android 的 FavoriteScreen.kt
// ══════════════════════════════════════════════════════════════════

struct FavoriteView: View {
    @State private var viewModel = FavoriteViewModel()

    var body: some View {
        if viewModel.isLoading && viewModel.favorites.isEmpty {
            ProgressView()
        } else if viewModel.favorites.isEmpty {
            // 空收藏夹引导
            VStack(spacing: 12) {
                Image(systemName: "heart")
                    .font(.largeTitle)
                    .foregroundStyle(.secondary)
                Text("还没有收藏的电影")
                    .foregroundStyle(.secondary)
                Text("去首页发现喜欢的电影吧")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        } else {
            List {
                ForEach(viewModel.favorites, id: \.id) { movie in
                    NavigationLink(value: movie.id) {
                        MovieRowCard(movie: movie)
                    }
                }
                .onDelete { indexSet in
                    // 左滑删除（iOS 原生交互）
                    for index in indexSet {
                        viewModel.removeFavorite(viewModel.favorites[index])
                    }
                }
            }
        }
    }
    .navigationTitle("我的收藏")
    .navigationDestination(for: Int32.self) { movieId in
        DetailView(movieId: movieId)
    }
}
