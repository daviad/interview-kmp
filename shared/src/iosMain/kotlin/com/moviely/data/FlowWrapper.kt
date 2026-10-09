package com.moviely.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Kotlin Flow → Swift 桥接器
 *
 * 【为什么需要这个？】
 * Kotlin Flow 是冷流，需要 CoroutineScope.collect 才能收到值。
 * Swift 没有 Flow 的概念，只有 Combine/async sequences。
 * KMP 导出 Flow 到 Swift 后，Swift 侧无法直接 collect。
 *
 * 这个 Wrapper 在 Kotlin 侧开一个 scope 订阅 Flow，
 * 每收到一个值就回调 Swift 闭包——相当于把 Flow 变成回调式 API。
 *
 * 🍎 UIKit 类比：NSFetchedResultsController 的 delegate 回调模式
 *   controllerDidChangeContent → onEach
 *   controllerDidPerformFetchError → onError
 *
 * 🍎 SwiftUI 类比：@FetchRequest 属性包装器自动驱动 View body 重算
 */
class FlowWrapper<T : Any>(private val flow: Flow<T>) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * 开始订阅（Main 线程回调，安全更新 UI）
     * @param onEach 每收到一个值时调用（主线程）
     * @param onError 发生异常时调用（主线程）
     */
    fun subscribe(onEach: (T) -> Unit, onError: (String) -> Unit) {
        scope.launch {
            flow.catch { e -> onError(e.message ?: e.toString()) }
                .collect { onEach(it) }
        }
    }

    /** 取消订阅（ViewModel deinit 时调用） */
    fun cancel() {
        scope.cancel()
    }
}

/** 便捷工厂：把 Flow 包成 FlowWrapper，Swift 侧调用更简洁 */
fun <T : Any> wrapFlow(flow: Flow<T>): FlowWrapper<T> = FlowWrapper(flow)
