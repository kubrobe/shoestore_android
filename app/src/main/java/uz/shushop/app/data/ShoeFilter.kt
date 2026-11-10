package uz.shushop.app.data

/**
 * What the shop screen is showing: the search box, the chip (All shoes, Men, Women, Kids, Sale), the filter sheet.
 * [apply] is a pure function so it is easy to unit-test; the same rules the server uses for GET /api/products.
 */
data class ShoeFilter(
    val query: String = "",
    val category: String = ALL,
    val size: Int? = null,
    val brand: String? = null,
    val sort: String = POPULAR,
) {
    val isDefault: Boolean get() = size == null && brand == null && sort == POPULAR
    val activeCount: Int get() = listOf(size != null, brand != null, sort != POPULAR).count { it }

    fun apply(all: List<ShoeWithVariants>): List<ShoeWithVariants> {
        val q = query.trim().lowercase()
        val filtered = all.filter { item ->
            val s = item.shoe
            val matchesQuery = q.isEmpty() || s.name.lowercase().contains(q) || s.brand.lowercase().contains(q) || s.type.lowercase().contains(q)
            val matchesCategory = when (category) {
                MEN -> s.category == "men" || s.category == "unisex"
                WOMEN -> s.category == "women" || s.category == "unisex"
                KIDS -> s.category == "kids"
                SALE -> s.oldPrice != null
                else -> true
            }
            val matchesSize = size == null || item.variants.any { it.size == size && it.stock > 0 }
            val matchesBrand = brand == null || s.brand.equals(brand, ignoreCase = true)
            matchesQuery && matchesCategory && matchesSize && matchesBrand
        }
        return when (sort) {
            PRICE_ASC -> filtered.sortedWith(compareBy({ it.shoe.price }, { it.shoe.id }))
            PRICE_DESC -> filtered.sortedWith(compareBy<ShoeWithVariants>({ -it.shoe.price }, { it.shoe.id }))
            NEWEST -> filtered.sortedWith(compareBy<ShoeWithVariants>({ -it.shoe.createdAt }, { -it.shoe.id }))
            RATING -> filtered.sortedWith(compareBy<ShoeWithVariants>({ -it.shoe.avgRating }, { -it.shoe.reviewCount }, { it.shoe.id }))
            // the server ranks "popular" by pairs sold; the phone does not know other people's orders, so it uses reviews as the stand-in
            else -> filtered.sortedWith(compareBy<ShoeWithVariants>({ -it.shoe.reviewCount }, { -it.shoe.avgRating }, { it.shoe.id }))
        }
    }

    companion object {
        const val ALL = "all"
        const val MEN = "men"
        const val WOMEN = "women"
        const val KIDS = "kids"
        const val SALE = "sale"
        const val POPULAR = "popular"
        const val PRICE_ASC = "price_asc"
        const val PRICE_DESC = "price_desc"
        const val NEWEST = "newest"
        const val RATING = "rating"

        val CHIPS = listOf(ALL to "All Shoes", MEN to "Men", WOMEN to "Women", KIDS to "Kids", SALE to "Sale")
        val SORTS = listOf(POPULAR to "Popular", PRICE_ASC to "Price: low to high", PRICE_DESC to "Price: high to low", NEWEST to "Newest", RATING to "Top rated")
    }
}
