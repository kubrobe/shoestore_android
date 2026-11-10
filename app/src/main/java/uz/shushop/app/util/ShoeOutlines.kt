// GENERATED from src/shoe-art.js of the shop server (the same drawing the website uses). The shapes are SVG path data.
package uz.shushop.app.util

/** The outline of one shoe style: upper, sole, where the dark outsole band starts, and the lace crossings. */
class ShoeOutline(val upper: String, val sole: String, val outsoleY: Int, val laces: List<Pair<Int, Int>>)

internal val SHOE_OUTLINES: Map<String, ShoeOutline> = mapOf(
    "court" to ShoeOutline(
        upper = "M84 380 C78 352 96 326 132 316 C170 306 214 300 250 286 L262 278 C286 266 306 246 322 226 L346 222 C352 244 376 262 404 262 C436 262 470 248 502 240 C522 238 534 268 531 316 L528 380 Z",
        sole = "M72 382 C72 400 86 414 108 414 L506 414 C528 414 542 402 542 382 L542 366 L72 366 Z",
        outsoleY = 400,
        laces = listOf(236 to 290, 216 to 296, 196 to 301, 176 to 306),
    ),
    "runner" to ShoeOutline(
        upper = "M82 380 C76 356 92 334 128 324 C168 316 214 308 252 290 L264 280 C288 268 308 248 324 232 L348 228 C354 248 378 264 406 264 C438 264 472 250 504 242 C524 240 536 270 533 318 L530 380 Z",
        sole = "M70 386 C70 402 84 414 106 414 L508 414 C532 414 544 398 544 374 L544 336 C470 350 300 360 72 372 Z",
        outsoleY = 400,
        laces = listOf(240 to 296, 220 to 301, 200 to 306, 180 to 311),
    ),
    "high" to ShoeOutline(
        upper = "M84 380 C78 352 96 326 132 316 C170 306 214 300 250 286 L262 278 C282 262 294 222 300 170 L332 160 C338 178 354 198 382 206 C412 212 442 198 474 180 C502 170 522 192 524 234 C528 272 530 310 528 380 Z",
        sole = "M72 382 C72 400 86 414 108 414 L506 414 C528 414 542 402 542 382 L542 366 L72 366 Z",
        outsoleY = 400,
        laces = listOf(262 to 262, 268 to 240, 274 to 218, 280 to 196, 236 to 292, 212 to 298, 188 to 304),
    ),
    "chunky" to ShoeOutline(
        upper = "M80 380 C72 346 94 318 134 310 C174 302 216 298 252 282 L264 274 C288 262 308 244 324 224 L350 220 C356 242 380 260 408 260 C440 260 474 246 506 238 C528 236 538 268 535 316 L532 380 Z",
        sole = "M66 388 C66 404 82 416 106 416 L512 416 C536 416 548 402 548 380 L548 350 C470 354 300 352 70 352 Z",
        outsoleY = 400,
        laces = listOf(240 to 288, 220 to 293, 200 to 298, 180 to 303),
    ),
    "slip" to ShoeOutline(
        upper = "M84 380 C78 356 98 334 134 326 C176 320 220 322 262 318 C290 316 316 300 340 290 C366 282 400 286 430 292 C466 296 500 292 520 286 C534 300 533 340 530 380 Z",
        sole = "M72 390 C72 404 86 414 108 414 L506 414 C528 414 540 404 540 388 L540 376 L72 376 Z",
        outsoleY = 404,
        laces = listOf(),
    ),
    "boot" to ShoeOutline(
        upper = "M86 380 C80 352 98 326 134 316 C172 306 214 300 248 288 L260 280 C292 262 318 200 330 118 L500 108 C524 112 534 170 532 250 C534 296 534 336 530 380 Z",
        sole = "M68 384 C68 404 84 416 108 416 L508 416 C532 416 546 404 546 384 L546 360 L68 360 Z",
        outsoleY = 398,
        laces = listOf(270 to 268, 278 to 244, 286 to 220, 294 to 196, 302 to 172, 310 to 148, 244 to 292, 214 to 300, 184 to 308),
    ),
)
