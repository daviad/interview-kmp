import SwiftUI
import shared

// ══════════════════════════════════════════════════════════════════
// 共享 UI 组件
// 对标 Android 的 Compose 公共 Composable（多个 feature 复用）
// ══════════════════════════════════════════════════════════════════

/// 电影海报卡片（横向滚动用）
/// 对标 Android HomeScreen.kt 的 MoviePosterCard Composable
struct MoviePosterCard: View {
    let movie: Movie

    var body: some View {
        VStack(alignment: .leading) {
            AsyncImage(url: URL(string: movie.posterUrl)) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Rectangle().fill(.gray.opacity(0.3))
            }
            .frame(width: 120, height: 180)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            Text(movie.title)
                .font(.caption)
                .lineLimit(1)
                .frame(width: 120)
        }
    }
}

/// 电影列表行卡片（纵向列表用）
/// 对标 Android HomeScreen.kt / FavoriteScreen.kt 的 MovieRowCard
struct MovieRowCard: View {
    let movie: Movie

    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: URL(string: movie.posterUrl)) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Rectangle().fill(.gray.opacity(0.3))
            }
            .frame(width: 60, height: 90)
            .clipShape(RoundedRectangle(cornerRadius: 6))

            VStack(alignment: .leading, spacing: 4) {
                Text(movie.title).font(.body)
                Text("评分: \(movie.formattedVote)")
                    .font(.caption).foregroundStyle(.secondary)
                Text(movie.releaseDate)
                    .font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
        }
    }
}

/// 错误提示 + 重试按钮
/// 对标 Android 的 ErrorView Composable
struct ErrorBanner: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(.orange)
            Text(message)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("重试", action: onRetry)
                .buttonStyle(.borderedProminent)
        }
        .padding()
        .frame(maxWidth: .infinity)
    }
}
