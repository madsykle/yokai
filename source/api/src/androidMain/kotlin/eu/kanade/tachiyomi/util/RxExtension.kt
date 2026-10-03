package eu.kanade.tachiyomi.util

import rx.Observable
import yokai.util.lang.awaitSingle
import yokai.util.lang.runAsObservable as runAsObservableImpl

actual suspend fun <T> Observable<T>.awaitSingle(): T = awaitSingle()

actual fun <T> runAsObservable(block: suspend () -> T): Observable<T> = runAsObservableImpl(block = block)
