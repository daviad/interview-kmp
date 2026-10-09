import SwiftUI
import shared

// ══════════════════════════════════════════════════════════════════
// HomeViewModel - 首页 ViewModel
// 对标 Android 的 HomeViewModel.kt（MVI + StateFlow）
//
// 关键差异：
//   Android: StateFlow<HomeUiState> + Intent Channel
//   iOS:     @Observable 属性 + 直接调方法（Swift 侧不走 MVI 意图模式）
//
// 数据来源：repository.getPopularMovies() + getNowPlayingMovies()（suspend）
// 对标 Android HomeViewModel 的 refresh() → HomeApiClient → TmdbClient
// ══════════════════════════════════════════════════════════════════

@Observable
final class HomeViewModel {
    // ── UI 状态 ──
    private(set) var popularMovies: [Movie] = []
    private(set) var nowPlayingMovies: [Movie] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    private let repository = AppDependencies.repository()

    init() {
        refresh()
    }

    /// 拉取首页数据（下拉刷新调用）
    /// 对标 Android 的 HomeIntent.Refresh
    func refresh() {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil

        Task {
            do {
                // Kotlin suspend → Swift async/await（KMP 自动转换）
                async let popular = repository.getPopularMovies(page: 1)
                async let playing = repository.getNowPlayingMovies(page: 1)

                let popularResult = try await popular
                let playingResult = try await playing

                // Kotlin Result sealed interface → Swift 模式匹配
                // 注意：KMP 导出的 Result 可能与 Swift 内置 Result 冲突，
                // 用 shared.Result 或全限定名消歧
                if let success = popularResult as? ResultSuccess {
                    popularMovies = (success.data as? [Movie]) ?? []
                }
                if let success = playingResult as? ResultSuccess {
                    nowPlayingMovies = (success.data as? [Movie]) ?? []
                }

                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// HomeView - 首页 SwiftUI 视图
// 对标 Android 的 HomeScreen.kt（Compose Scaffold + LazyColumn）
//
// 🍎 UIKit 类比：UICollectionView + compositional layout（横向 + 纵向混排）
// 🍎 SwiftUI 类比：ScrollView + LazyVStack + LazyHStack
// ══════════════════════════════════════════════════════════════════

struct HomeView: View {
    @State private var viewModel = HomeViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                if viewModel.isLoading && viewModel.popularMovies.isEmpty {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.top, 100)
                } else if let error = viewModel.errorMessage {
                    ErrorBanner(message: error) { viewModel.refresh() }
                } else {
                    // 热门电影 - 横向滚动
                    if !viewModel.popularMovies.isEmpty {
                        section(title: "热门电影") {
                            ScrollView(.horizontal, showsIndicators: false) {
                                LazyHStack(spacing: 12) {
                                    ForEach(viewModel.popularMovies, id: \.id) { movie in
                                        NavigationLink(value: movie.id) {
                                            MoviePosterCard(movie: movie)
                                        }
                                    }
                                }
                                .padding(.horizontal)
                            }
                        }
                    }

                    // 正在上映 - 纵向列表
                    if !viewModel.nowPlayingMovies.isEmpty {
                        section(title: "正在上映") {
                            VStack(spacing: 12) {
                                ForEach(viewModel.nowPlayingMovies, id: \.id) { movie in
                                    NavigationLink(value: movie.id) {
                                        MovieRowCard(movie: movie)
                                    }
                                }
                            }
                            .padding(.horizontal)
                        }
                    }
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Moviely")
        .navigationDestination(for: Int32.self) { movieId in
            DetailView(movieId: movieId)
        }
        .refreshable { viewModel.refresh() }
    }

    @ViewBuilder
    private func section<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.title2.bold())
                .padding(.horizontal)
            content()
        }
    }
}
