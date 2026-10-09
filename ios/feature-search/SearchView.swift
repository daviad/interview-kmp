import SwiftUI
import shared

// ══════════════════════════════════════════════════════════════════
// SearchViewModel - 搜索 ViewModel
// 对标 Android 的 SearchViewModel.kt
//
// 数据来源：repository.searchMovies(query, page)（suspend）
// 与 Android 版的差异：Swift 版不做 debounce（打字不发请求，
// 只在点搜索键时发），与 Android 行为一致
// ══════════════════════════════════════════════════════════════════

@Observable
final class SearchViewModel {
    var query = ""                          // 输入框文本（双向绑定）
    private(set) var results: [Movie] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?
    private(set) var hasSearched = false     // 是否发起过搜索（控制 Idle 态）

    private let repository = AppDependencies.repository()

    /// 发起搜索（点击搜索键 / 搜索图标）
    /// 对标 Android 的 SearchIntent.Search
    func search() {
        let trimmed = query.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else {
            results = []
            hasSearched = false
            return
        }

        isLoading = true
        errorMessage = nil
        hasSearched = true

        Task {
            do {
                let result = try await repository.searchMovies(query: trimmed, page: 1)
                if let success = result as? ResultSuccess {
                    results = (success.data as? [Movie]) ?? []
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
// SearchView - 搜索页 SwiftUI 视图
// 对标 Android 的 SearchScreen.kt
// ══════════════════════════════════════════════════════════════════

struct SearchView: View {
    @State private var viewModel = SearchViewModel()

    var body: some View {
        VStack(spacing: 0) {
            // 搜索栏
            HStack {
                TextField("输入电影名，如：盗梦空间", text: $viewModel.query)
                    .textFieldStyle(.roundedBorder)
                    .submitLabel(.search)
                    .onSubmit { viewModel.search() }

                Button {
                    viewModel.search()
                } label: {
                    Image(systemName: "magnifyingglass")
                }
            }
            .padding()

            // 结果区域
            if viewModel.isLoading {
                Spacer()
                ProgressView()
                Spacer()
            } else if let error = viewModel.errorMessage {
                Spacer()
                ErrorBanner(message: error) { viewModel.search() }
                Spacer()
            } else if !viewModel.hasSearched {
                Spacer()
                Text("输入关键词，点击搜索开始查找电影")
                    .foregroundStyle(.secondary)
                Spacer()
            } else if viewModel.results.isEmpty {
                Spacer()
                Text("没有找到与「\(viewModel.query)」相关的电影")
                    .foregroundStyle(.secondary)
                Spacer()
            } else {
                List(viewModel.results, id: \.id) { movie in
                    NavigationLink(value: movie.id) {
                        MovieRowCard(movie: movie)
                    }
                }
            }
        }
        .navigationTitle("搜索电影")
        .navigationDestination(for: Int32.self) { movieId in
            DetailView(movieId: movieId)
        }
    }
}
