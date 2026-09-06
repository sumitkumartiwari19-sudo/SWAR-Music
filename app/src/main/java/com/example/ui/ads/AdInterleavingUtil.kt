package com.example.ui.ads

/**
 * Shared interval constant for interleaving inline native 1:1 ads into content lists.
 * Configured to 15 items as requested (easily adjustable in one place).
 */
const val INLINE_AD_INTERVAL = 15

/**
 * Wrapper representing either a normal list item or an ad placeholder.
 */
sealed class ListItemWithAd<out T> {
    data class Content<T>(val item: T) : ListItemWithAd<T>()
    data class InlineAd(
        val adUnit: AdUnit.NativeBanner1x1 = AdUnit.NativeBanner1x1(),
        val adIndex: Int
    ) : ListItemWithAd<Nothing>()
}

/**
 * Interleaves inline 1:1 native banners into any list after every [interval] items.
 * Guaranteed never to place an ad as the very first item, and never to place two ads adjacent.
 */
fun <T> List<T>.interleaveWithAds(
    interval: Int = INLINE_AD_INTERVAL
): List<ListItemWithAd<T>> {
    if (isEmpty() || interval <= 0) {
        return map { ListItemWithAd.Content(it) }
    }

    val result = ArrayList<ListItemWithAd<T>>(size + (size / interval))
    var adCounter = 0

    forEachIndexed { index, item ->
        result.add(ListItemWithAd.Content(item))
        val currentItemCount = index + 1
        // Insert inline ad after every [interval] items
        if (currentItemCount % interval == 0 && currentItemCount < size) {
            result.add(
                ListItemWithAd.InlineAd(
                    adUnit = AdUnit.NativeBanner1x1(),
                    adIndex = adCounter++
                )
            )
        }
    }

    return result
}

/**
 * Determines whether a 300×250 footer ad can safely be displayed at the end of a list
 * without violating the anti-stacking rule (no two ad units directly adjacent).
 *
 * @param totalItems Count of content items in the list.
 * @param interval The inline ad interval.
 * @param minSpacing Minimum number of content items required after the last inline ad before a footer ad.
 */
fun canShowFooterAd(
    totalItems: Int,
    interval: Int = INLINE_AD_INTERVAL,
    minSpacing: Int = 3
): Boolean {
    if (totalItems <= 0) return false
    // If the list is shorter than the interval, there is no inline ad, so footer ad is safe
    if (totalItems < interval) return true
    // If the list reached or just passed an interval boundary, an inline ad is nearby
    val remainder = totalItems % interval
    // If remainder is 0, the list ended right on an interval boundary
    // If remainder is less than minSpacing, the last inline ad was too close
    return remainder >= minSpacing
}
