import SwiftUI

// ══════════════════════════════════════════════════════════════════
// ContentView - TabView 导航容器
// 对标 Android 的 android/app/.../MovielyNavHost.kt (NavHost)
//
// iOS 路由策略与 Android 的区别：
//   Android：单 NavHost 管理所有 composable 路由（home/search/favorites/detail）
//   iOS：    TabView 三个 Tab（Home/Search/Favorites），Detail 用 NavigationLink push
//
// 🍎 UIKit 类比：UITabBarController + 各 Tab 内嵌 UINavigationController
// 🍎 SwiftUI 类比：TabView { } + NavigationStack
// ══════════════════════════════════════════════════════════════════

struct ContentView: View {
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            // Tab 1: 首页（热门 + 正在上映）
            NavigationStack {
                HomeView()
            }
            .tabItem {
                Label("首页", systemImage: "house")
            }
            .tag(0)

            // Tab 2: 搜索
            NavigationStack {
                SearchView()
            }
            .tabItem {
                Label("搜索", systemImage: "magnifyingglass")
            }
            .tag(1)

            // Tab 3: 收藏列表
            NavigationStack {
                FavoriteView()
            }
            .tabItem {
                Label("收藏", systemImage: "heart")
            }
            .tag(2)
        }
    }
}
