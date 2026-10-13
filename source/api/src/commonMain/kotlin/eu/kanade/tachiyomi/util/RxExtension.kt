package eu.kanade.tachiyomi.util

import rx.Observable

expect suspend fun <T> Observable<T>.awaitSingle(): T

/**
 * Bridges a suspend [block] into a single-element [Observable]. Used by the deprecated
 * RxJava-facing source API so the suspend methods stay the single source of truth.
 */
expect fun <T> runAsObservable(block: suspend () -> T): Observable<T>
