package uz.shushop.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoeFilterTest {
    private fun shoe(
        id: Long, name: String, brand: String, category: String, price: Long, old: Long? = null,
        rating: Double = 0.0, reviews: Int = 0, created: Long = id, sizes: Map<Int, Int> = mapOf(42 to 3),
    ) = ShoeWithVariants(
        ShoeModel(id, name, brand, "Sneakers", category, "", price, old, "court", null, rating, reviews, created),
        sizes.entries.mapIndexed { i, (size, stock) -> ShoeVariant(id * 100 + i, id, size, "Black", "#1c1c1e", stock) },
    )

    private val all = listOf(
        shoe(1, "AirFlex Sneakers", "Kora", "unisex", 690_000, rating = 5.0, reviews = 3),
        shoe(2, "Swift Run", "Velo", "women", 640_000, rating = 4.2, reviews = 5, sizes = mapOf(38 to 2, 39 to 0)),
        shoe(3, "Urban Step", "Nomad", "men", 720_000, old = 890_000, rating = 4.7, reviews = 3, sizes = mapOf(41 to 0, 42 to 4)),
        shoe(4, "Mini Kicks", "Lume", "kids", 320_000, sizes = mapOf(30 to 5)),
        shoe(5, "Trail Master", "Atlas", "men", 960_000, rating = 3.7, reviews = 3, sizes = mapOf(44 to 1)),
    )

    private fun ids(f: ShoeFilter) = f.apply(all).map { it.shoe.id }

    @Test fun defaultFilterShowsEverything() {
        assertEquals(5, ids(ShoeFilter()).size)
        assertTrue(ShoeFilter().isDefault)
        assertEquals(0, ShoeFilter().activeCount)
    }

    @Test fun searchLooksAtNameBrandAndType() {
        assertEquals(listOf(3L), ids(ShoeFilter(query = "urban")))
        assertEquals(listOf(2L), ids(ShoeFilter(query = "  VELO ")))
        assertEquals(5, ids(ShoeFilter(query = "sneakers")).size)          // every type is "Sneakers"
        assertEquals(emptyList<Long>(), ids(ShoeFilter(query = "sandals")))
    }

    @Test fun menAndWomenIncludeUnisex() {
        assertEquals(setOf(1L, 3L, 5L), ids(ShoeFilter(category = ShoeFilter.MEN)).toSet())
        assertEquals(setOf(1L, 2L), ids(ShoeFilter(category = ShoeFilter.WOMEN)).toSet())
    }

    @Test fun kidsIsOnlyKids() {
        assertEquals(listOf(4L), ids(ShoeFilter(category = ShoeFilter.KIDS)))
    }

    @Test fun saleIsAnythingWithAnOldPrice() {
        assertEquals(listOf(3L), ids(ShoeFilter(category = ShoeFilter.SALE)))
    }

    @Test fun sizeFilterOnlyCountsSizesThatAreInStock() {
        assertEquals(setOf(1L, 3L), ids(ShoeFilter(size = 42)).toSet())
        assertEquals(emptyList<Long>(), ids(ShoeFilter(size = 41)))             // shoe 3 has a 41 but it is sold out
        assertEquals(emptyList<Long>(), ids(ShoeFilter(size = 39)))
    }

    @Test fun brandFilterIgnoresCase() {
        assertEquals(setOf(3L), ids(ShoeFilter(brand = "nomad")).toSet())
        assertEquals(setOf(3L), ids(ShoeFilter(brand = "NOMAD")).toSet())
    }

    @Test fun filtersCombine() {
        assertEquals(listOf(3L), ids(ShoeFilter(query = "step", category = ShoeFilter.MEN, size = 42)))
        assertEquals(emptyList<Long>(), ids(ShoeFilter(category = ShoeFilter.KIDS, size = 42)))
    }

    @Test fun sortByPrice() {
        assertEquals(listOf(4L, 2L, 1L, 3L, 5L), ids(ShoeFilter(sort = ShoeFilter.PRICE_ASC)))
        assertEquals(listOf(5L, 3L, 1L, 2L, 4L), ids(ShoeFilter(sort = ShoeFilter.PRICE_DESC)))
    }

    @Test fun sortNewestAndRating() {
        assertEquals(listOf(5L, 4L, 3L, 2L, 1L), ids(ShoeFilter(sort = ShoeFilter.NEWEST)))
        assertEquals(listOf(1L, 3L, 2L, 5L, 4L), ids(ShoeFilter(sort = ShoeFilter.RATING)))
    }

    @Test fun popularUsesReviewsThenRating() {
        assertEquals(listOf(2L, 1L, 3L, 5L, 4L), ids(ShoeFilter()))
    }

    @Test fun activeCountTellsHowManyFiltersAreOn() {
        assertEquals(3, ShoeFilter(size = 42, brand = "Kora", sort = ShoeFilter.PRICE_ASC).activeCount)
    }
}
