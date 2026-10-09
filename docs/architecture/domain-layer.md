# Domain 层架构说明

> **适用范围**：`shared/src/commonMain/kotlin/com/moviely/common/domain/`
> **结论先行**：本项目 `domain` 包确实是**领域层**，但属于 **Clean Architecture（整洁架构）分层风格**，**不是完整 DDD（Domain-Driven Design）**。

---

## 1. 概念辨析

| 概念 | 是什么 | 本项目是否符合 |
|---|---|---|
| **Domain Layer（领域层）** | 分层架构中的一层，放领域模型 + 业务接口 | ✅ 是 |
| **DDD（Domain-Driven Design）** | Evans 提出的一套完整方法论，含 Aggregate / Value Object / Domain Service / Domain Event / Bounded Context 等概念 | ❌ 不是 |

很多人混用「领域层」「DDD」，但二者是不同层次的概念：
- **领域层**是一种**分层结果**（Layout）
- **DDD**是一套**设计方法论**（Methodology）

采用了「领域层」不等于采用 DDD。Android 官方架构指南推荐的 Domain + Data + UI 三层模板就属于前者——借用了 DDD 的 Repository 概念，但刻意保留贫血模型，因为移动端业务通常较薄。

---

## 2. 本项目 domain 包结构

```
shared/src/commonMain/kotlin/com/moviely/common/domain/
├── model/
│   ├── Movie.kt          # 领域模型：电影列表项
│   ├── MovieDetail.kt    # 领域模型：电影详情
│   └── Cast.kt           # 领域模型：演员
├── Result.kt             # 异步结果封装（Success / Error / Loading）
└── repository/
    └── MovieRepository.kt # 仓储接口（Domain 层定义，Data 层实现）
```

### 各文件的角色与 DDD 对应关系

| 文件 | 角色 | DDD 中对应概念 |
|---|---|---|
| `model/Movie.kt`、`model/MovieDetail.kt`、`model/Cast.kt` | 领域模型（`data class`） | 贫血 Entity（只有数据，无业务方法） |
| `Result.kt` | Result monad（Success / Error / Loading） | 不属于 DDD 概念，是通用工具 |
| `repository/MovieRepository.kt` | 仓储**接口** | ✅ DDD 的 Repository 概念 |

### 依赖反转（DIP）原则

`MovieRepository.kt` 文件头部注释已明确标注：

> Domain 层只定义「我需要什么」（接口），不关心「怎么实现」
> Data 层负责实现这个接口（`MovieRepositoryImpl`）
> 依赖方向：data → domain（Domain 不依赖 Data，Data 依赖 Domain）

这是 Clean Architecture 的核心——**内层（Domain）不依赖外层（Data）**，外层反向依赖内层。

---

## 3. 为什么不是 DDD

DDD 的完整要素本项目几乎全部缺失：

| DDD 要素 | 是否存在 | 说明 |
|---|---|---|
| **Aggregate Root（聚合根）** | ❌ | 没有定义一致性边界，`Movie` / `MovieDetail` / `Cast` 都是平级 `data class` |
| **Value Object（值对象）** | ❌ | 没有区分 Entity 与 VO，所有模型同质化 |
| **Domain Service（领域服务）** | ❌ | 业务逻辑都在 `MovieRepositoryImpl` 里，Domain 层无 Service |
| **Domain Event（领域事件）** | ❌ | 没有 `MovieFavorited` 这类事件，收藏联动靠 Flow 订阅实现 |
| **Bounded Context（限界上下文）** | ❌ | 单一 `common.domain` 包，未按子域拆分 |
| **充血模型** | ❌ | Entity 是纯 `data class`，零业务方法（DDD 原教旨视为反模式） |

**唯一借用的概念**：`Repository`（仓储模式）。这是 DDD 战术设计中的一种，但单独使用它并不构成 DDD。

---

## 4. 为什么选择 Clean Architecture 而非 DDD

### 4.1 移动端业务特征

- 业务规则通常较薄（CRUD + 简单校验为主）
- UI 驱动数据流，而非业务规则驱动
- 性能与样本代码量敏感，充血模型反而增加样板代码

### 4.2 Android 官方推荐

本项目遵循 [Android 官方架构指南](https://developer.android.com/topic/architecture) 的 **UI 层 + Domain 层（可选） + Data 层** 三层模板：

```
UI 层（feature-*）  →  Domain 层（shared/common/domain）  →  Data 层（shared/data）
   Compose + VM           接口 + 模型 + Result                Repository 实现 + API + DB
```

Domain 层在官方指南里标注为「可选」——只有在多个 ViewModel 共用业务逻辑时才提取。本项目因为 KMP 共享的需要，把 Domain 层下沉到 `shared` 模块，让 Android / iOS 都能复用。

### 4.3 KMP 共享约束

- `commonMain` 不能 import 任何 `android.*` / `androidx.*`（Room/Koin 除外）
- 平台差异通过 `expect/actual` 隔离在 `androidMain` / `iosMain`
- 贫血模型 + 仓储接口是最小公约数，方便跨平台复用

---

## 5. iOS 开发者视角类比

| 本项目（Kotlin） | UIKit 对应 | SwiftUI 对应 |
|---|---|---|
| `domain/` | `Service` / `Manager` 层的 Protocol 定义 | `@Observable` ViewModel 的 Protocol |
| `domain/model/` | `Model` 类（NSObject 子类或 struct） | `struct Identifiable` |
| `data/repository/MovieRepositoryImpl.kt` | `DataSource` / `APIClient`（实现 Protocol） | 同 UIKit |
| `Result.kt`（sealed class） | 无原生对应，用 enum + objc Associated 变通 | `enum Result<Success, Failure: Error>`（带关联值的 enum） |
| `suspend fun` 一次性请求 | `sendAsynchronousRequest completionHandler` | `async/await` |
| `Flow<T>` 持续订阅 | Combine Publisher / RxSwift Observable / KVO | `@FetchRequest`（SwiftData / CoreData 自动监听） |

---

## 6. 如果要升级到严格 DDD

仅在业务复杂度上升、多团队协作时才考虑。升级路径：

1. **拆分子域**：按 `movie` / `favorite` / `search` 等限界上下文分子包
   ```
   domain/movie/{model,service,event}/
   domain/favorite/{model,service,event}/
   ```

2. **引入聚合根**：例如 `Movie` 作为 Aggregate Root，`Cast` 作为其内部实体，外部只能通过 `Movie.addCast(...)` 操作

3. **区分 Entity 与 Value Object**：
   - Entity：有唯一标识 + 生命周期（`Movie` 有 `id`）
   - VO：无标识、不可变（如 `ImageUrl(value: String)`、`Rating(value: Double)`）

4. **充血模型**：把 `Movie.favorite()` 这类业务方法搬进 `Movie` 本身，而非 Repository

5. **领域事件**：定义 `MovieFavoritedEvent`，由领域层发布，Data 层订阅后写库 + 推送 Flow

6. **Domain Service**：跨实体的业务逻辑（如「计算电影相似度」）放入独立 `service/` 子包

> ⚠️ **权衡提示**：移动端盲目套用 DDD 通常会过度设计。本项目当前规模下，Clean Architecture + 贫血模型 + Repository 模式已足够，**不建议在没有明确业务驱动下升级到完整 DDD**。

---

## 7. 相关文件速查

| 想了解 | 看这个文件 |
|---|---|
| Domain 层模型字段 | [shared/.../domain/model/Cast.kt](../../shared/src/commonMain/kotlin/com/moviely/common/domain/model/Cast.kt)、[Movie.kt](../../shared/src/commonMain/kotlin/com/moviely/common/domain/model/Movie.kt)、[MovieDetail.kt](../../shared/src/commonMain/kotlin/com/moviely/common/domain/model/MovieDetail.kt) |
| 仓储接口（异步 API 设计） | [shared/.../domain/repository/MovieRepository.kt](../../shared/src/commonMain/kotlin/com/moviely/common/domain/repository/MovieRepository.kt) |
| Result 封装（ADT 三态） | [shared/.../domain/Result.kt](../../shared/src/commonMain/kotlin/com/moviely/common/domain/Result.kt) |
| 仓储实现（Data 层） | [shared/.../data/repository/MovieRepositoryImpl.kt](../../shared/src/commonMain/kotlin/com/moviely/data/repository/MovieRepositoryImpl.kt) |
| shared 模块全景 | [shared/AGENTS.md](../../shared/AGENTS.md) |
