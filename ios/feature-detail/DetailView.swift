import SwiftUI
import shared

// ══════════════════════════════════════════════════════════════════
// DetailViewModel - 电影详情 ViewModel
// 对标 Android 的 DetailViewModel.kt（suspend + Flow 混合型）
//
// ① suspend 拉详情：repository.getMovieDetail(id)
// ② Flow 订阅收藏状态：repository.isFavorite(id): Flow<Boolean>
// ③ ToggleFavorite：写 Room 后 Flow 自动回推
//
// 🍎 UIKit 类比：详情页同时用 URLSession 请求 + NSFetchedResultsController 监听收藏状态
// 🍎 SwiftUI 类比：@State + @FetchRequest 联动
// ══════════════════════════════════════════════════════════════════

@Observable
final class DetailViewModel {
    private(set) var movie: MovieDetail?
    private(set) var isFavorite = false
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    private let repository = AppDependencies.repository()
    private let movieId: Int32
    private var favoriteFlowWrapper: FlowWrapper<ObjCBool>?

    init(movieId: Int32) {
        self.movieId = movieId
        loadDetail()
        observeFavorite()
    }

    /// ① 一次性拉详情
    private func loadDetail() {
        isLoading = true
        Task {
            do {
                let result = try await repository.getMovieDetail(id: movieId)
                if let success = result as? ResultSuccess {
                    if let detail = success.data as? MovieDetail {
                        movie = detail
                        isFavorite = detail.isFavorite  // 初始值，随后由 Flow 接管
                    }
                }
                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    /// ② 订阅收藏状态（Room Flow 驱动）
    private func observeFavorite() {
        // Kotlin Flow<Boolean> → FlowWrapper<ObjCBool>
        favoriteFlowWrapper = wrapFlow(flow: repository.isFavorite(id: movieId))
        favoriteFlowWrapper?.subscribe(
            onEach: { [weak self] bool in
                // Kotlin Boolean → ObjCBool → Swift Bool
                self?.isFavorite = bool.boolValue
            },
            onError: { _ in }
        )
    }

    /// ③ 收藏/取消收藏
    /// 对标 Android 的 DetailIntent.ToggleFavorite
    func toggleFavorite() {
        guard let movie else { return }
        Task {
            if isFavorite {
                _ = try await repository.removeFavorite(id: movieId)
            } else {
                // MovieDetail → Movie 转换（收藏表写入需要 Movie 类型）
                let favoriteMovie = Movie(
                    id: movie.id,
                    title: movie.title,
                    overview: movie.overview,
                    posterPath: movie.posterPath,
                    backdropPath: movie.backdropPath,
                    voteAverage: movie.voteAverage,
                    releaseDate: movie.releaseDate,
                    genreIds: []  // 详情接口不返回 genreIds，给空即可
                )
                _ = try await repository.addFavorite(movie: favoriteMovie)
            }
            // 不需要手动改 isFavorite：Flow 自动回推新值
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// DetailView - 电影详情 SwiftUI 视图
// 对标 Android 的 DetailScreen.kt
//
// 布局：背景大图 → 海报+信息 → 收藏按钮 → 简介 → 演员表 → 相似电影
// ══════════════════════════════════════════════════════════════════

struct DetailView: View {
    let movieId: Int32
    @State private var viewModel: DetailViewModel

    init(movieId: Int32) {
        self.movieId = movieId
        _viewModel = State(initialState: DetailViewModel(movieId: movieId))
    }

    var body: some View {
        ScrollView {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.top, 100)
            } else if let error = viewModel.errorMessage {
                ErrorBanner(message: error) {
                    viewModel.toggleFavorite()  // 重新触发加载
                }
            } else if let movie = viewModel.movie {
                detailContent(movie: movie)
            }
        }
        .navigationTitle(viewModel.movie?.title ?? "详情")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func detailContent(movie: MovieDetail) -> some View {
        // 背景大图
        AsyncImage(url: URL(string: movie.backdropUrl)) { image in
            image.resizable().aspectRatio(contentMode: .fill)
        } placeholder: {
            Rectangle().fill(.gray.opacity(0.3))
        }
        .frame(height: 220)

        // 海报 + 基础信息
        HStack(alignment: .top, spacing: 16) {
            AsyncImage(url: URL(string: movie.posterUrl)) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Rectangle().fill(.gray.opacity(0.3))
            }
            .frame(width: 100, height: 150)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 4) {
                Text(movie.title).font(.title3.bold())
                Text("\(movie.year) · \(movie.formattedRuntime)")
                    .font(.subheadline).foregroundStyle(.secondary)
                Text("评分: \(movie.formattedVote)")
                    .font(.subheadline).foregroundStyle(.secondary)
                if !movie.genresText.isEmpty {
                    Text(movie.genresText).font(.caption).foregroundStyle(.secondary)
                }
            }
            Spacer()
        }
        .padding()

        // 收藏按钮
        Button {
            viewModel.toggleFavorite()
        } label: {
            Label(
                viewModel.isFavorite ? "已收藏（点击取消）" : "收藏",
                systemImage: viewModel.isFavorite ? "heart.fill" : "heart"
            )
            .frame(maxWidth: .infinity)
            .padding()
            .background(viewModel.isFavorite ? Color.red.opacity(0.15) : Color.blue.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .padding(.horizontal)

        // 剧情简介
        VStack(alignment: .leading, spacing: 8) {
            Text("剧情简介").font(.headline)
            Text(movie.overview.isEmpty ? "暂无简介" : movie.overview)
                .font(.body)
                .foregroundStyle(.secondary)
        }
        .padding()

        // 演员表
        if let cast = movie.cast, !cast.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("主要演员").font(.headline).padding(.horizontal)
                ScrollView(.horizontal) {
                    LazyHStack(spacing: 12) {
                        ForEach(cast.prefix(12), id: \.id) { person in
            VStack {
                AsyncImage(url: URL(string: person.profileUrl)) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Rectangle().fill(.gray.opacity(0.3))
                }
                .frame(width: 96, height: 120)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                Text(person.name).font(.caption).lineLimit(1)
                Text(person.character).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
            }
            .frame(width: 96)
                        }
                    }
                    .padding(.horizontal)
                }
            }
        }

        // 相似电影
        if let similar = movie.similarMovies, !similar.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("你可能还喜欢").font(.headline).padding(.horizontal)
                ScrollView(.horizontal) {
                    LazyHStack(spacing: 12) {
                        ForEach(similar, id: \.id) { m in
                            NavigationLink(value: m.id) {
                                VStack {
                                    AsyncImage(url: URL(string: m.posterUrl)) { image in
                                        image.resizable().aspectRatio(contentMode: .fill)
                                    } placeholder: {
                                        Rectangle().fill(.gray.opacity(0.3))
                                    }
                                    .frame(width: 110, height: 165)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                    Text(m.title).font(.caption).lineLimit(1).frame(width: 110)
                                }
                            }
                        }
                    }
                    .padding(.horizontal)
                }
            }
        }
    }
}
