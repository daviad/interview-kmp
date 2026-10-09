package com.moviely.common.domain

/**
 * 🎯 核心思想：用「代数数据类型 ADT」封装异步操作的三种状态
 *
 * 为什么用 sealed class？
 *   sealed class =「受限的继承体系」，子类必须定义在同一文件内（Kotlin 1.5+ 放宽为同模块同包）
 *   编译器知道所有可能的子类，所以 when 表达式可以穷举，不需要 else 分支
 *   🍎 UIKit 类比：没有原生对应，通常用 enum + objc Associated 关联值变通 / Delegate 回调
 *   🍎 SwiftUI 类比：相当于 Swift 的 enum with associated values（Swift 原生支持，Kotlin 的 sealed class 与之等价）
 *
 * out T（协变）的作用：
 *   Result<Dog> 可以赋值给 Result<Animal>，因为 Success<T> 生产者「产出」T
 *   所以 Error 和 Loading 可以用 Result<Nothing>（Nothing 是所有类型的子类，可被当成任何 Result<X>）
 *   🍎 Swift 类比：Result<Success, Failure> 的 Success 就是协变的
 */
sealed class Result<out T> {
    /** ✅ 成功状态：携带数据 T */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * ❌ 失败状态：携带异常和可选消息
     * 为什么是 Result<Nothing>？
     *   Error 不可能有 T 数据，用 Nothing（Kotlin 的空/底类型）表示「永远不会有值」
     *   Nothing 是所有类型的子类，所以 Result<Nothing> 能被当成 Result<任何东西>
     *   🍎 Swift 类比：enum case .failure(Error)，自动处理关联值的位置
     */
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()

    /**
     * ⏳ 加载状态：无数据
     * data object = 单例（Kotlin 1.7+ 正式语法），因为 Loading 不需要任何字段，是全局唯一的
     */
    data object Loading : Result<Nothing>()

    // ── 便捷状态判断属性（类似 UIKit 的 isHidden 这种计算属性）
    val isSuccess get() = this is Success
    val isError   get() = this is Error
    val isLoading get() = this is Loading

    /**
     * 取值：成功返回数据，否则 null
     * 🍎 UIKit 类比：dict[@"key"] ?: nil（可选解包）
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * 取值：成功返回数据，失败/加载就抛异常
     * 🍎 UIKit 类比：try! nsData 解包（强制 try，慎用）
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Result is still loading")
    }

    /**
     * 链式回调：成功时执行 action，返回 this 以便继续链式调用
     * inline：内联函数，避免创建 lambda 对象（性能开销为 0）
     * 🍎 UIKit 类比：completion block 里 if success 的写法
     * 🍎 Swift 类比：Combine 的 .sink(receiveValue: { })
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this  // 链式返回，支持 result.onSuccess{}.onError{}
    }

    /** 链式回调：失败时执行 action */
    inline fun onError(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }
}
