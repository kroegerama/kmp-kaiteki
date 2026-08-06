package com.kroegerama.kmp.kaiteki.paging.pagingsource

/**
 * behavior of a paging source when an item id reappears on a different page
 */
public enum class DuplicateStrategy {

    /**
     * restart the whole list via `LoadResult.Invalid`; for deterministic backends, where a duplicate
     * means all loaded pages are stale. intra-page duplicates surface as [DuplicateIdException] instead
     */
    INVALIDATE,

    /**
     * drop the already-seen items and keep paging; for non-deterministic backends (e.g. random
     * order). pages dropped via `PagingConfig.maxSize` still count as delivered, so a shifted
     * item may stay hidden until its original page reloads or the source refreshes
     */
    FILTER,
}
