package com.kroegerama.kmp.kaiteki.paging.pagingsource

/**
 * a single page delivered the same non-null [id] for more than one item —
 * broken backend data or id mapping rather than shifted pages
 */
public class DuplicateIdException internal constructor(
    public val id: Any
) : IllegalStateException("more than one item with id '$id' in a single page")
