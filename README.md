# Shushop - Android app

The Android app of **Shushop**, an online shoe store. It is built with Kotlin, Jetpack Compose (Material 3), Room and MVVM, and it talks to the same server and database as the website:

| Part | Repository |
| --- | --- |
| Website + REST API + database scripts | `kubrobe/shoestore` |
| Android app (this repository) | `kubrobe/shoestore_android` |

```
 phone (Room copy) ◄──►  Shushop server (Node, Render)  ◄──►  PostgreSQL (Supabase)  ◄──► website
```

Whatever happens on the phone (cart, orders, saved shoes, alerts) is saved on the server, so the website shows it too, and the other way round. All shoes, brands and people in the demo data are made up. Prices are in so'm.

## Screens

| Screen | What it does |
| --- | --- |
| Login / Register | Validated forms (email, password 8-72 characters, name, phone). The session is restored when the app is reopened; *Log out* is in the profile. A **Server** button lets you point the app at another server. |
| Shop | Search box, chips *All Shoes / Men / Women / Kids / Sale*, filter sheet (size in stock, brand, sort order), two-column grid with hearts, pull to refresh |
| Shoe detail | Picture, colour dots, size circles (sold-out sizes are disabled), quantity stepper, *Add to Cart*, stock alert, reviews, write a review after a delivered order |
| Checkout | Items with steppers, delivery address, payment method, coupon code, cost breakdown, *Confirm Order* |
| Orders / Order detail | History, status timeline, cancel while the order is new or being packed |
| Notifications | Dark screen with chips *All / Price Drops / Restocks / Orders*, blue dots for unread, mark all as read |
| Wishlist | Saved shoes |
| Profile | Avatar with initials, stats (orders, saved pairs, shoe size), edit profile, change password, addresses, log out |

The shoe pictures are drawn on a Compose `Canvas` from the same geometry as the website, so the app needs no image files. If the owner sets a photo URL for a shoe, the photo is shown instead.

## How it works

```
Compose screens ─► ShopViewModel ─► ShopRepository ─┬─► Room DAOs (offline copy, the screens read from here as Flow)
                                                     └─► ApiClient (HTTP + JSON to the server)
```

- **The server is the boss.** A change (add to cart, place order...) is sent to the server first. Only when the server accepts it, the answer is saved in Room in one `@Transaction`. If the server says no ("only 1 left in size 42"), the screen shows that message and nothing local changes.
- **The phone keeps working without internet** for browsing: the shop, your orders and notifications come from Room, which is refreshed from `GET /api/mobile/sync`. Buying needs a connection.
- **Login** returns a token. It is stored encrypted (AES-GCM, key in the Android Keystore) and sent as `Authorization: Bearer ...`. A `401` answer logs the user out and clears the local copy.
- **Prices and rules** (shipping fee, free-shipping limit, max quantity, payment methods) come from the server (`/api/meta`), so the app and the website cannot disagree. The cart preview uses the same formula as the server.

More detail, with the database diagram and the course checklist, is in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Run it

1. Install **Android Studio** (current stable). Open this folder (`File → Open`). Let Gradle sync; it downloads what it needs.
2. Start an emulator or plug in a phone and press **Run**.
3. Demo login: `alex@demo.uz` / `demo12345`.

The app talks to `https://shushop.onrender.com` by default. To use another server:

- **Without rebuilding**: on the login screen tap **Server** and type the address.
- **At build time**: add `shushopApiUrl=https://your-service.onrender.com` to `gradle.properties` (or `~/.gradle/gradle.properties`).

Testing against a server on your own computer (`npm start` in the website project, port 3000):

| Where the app runs | Address to use |
| --- | --- |
| Android emulator | `http://10.0.2.2:3000` |
| Real phone on the same Wi-Fi | `http://<your computer's IP>:3000` |

Plain `http://` is allowed only in the **debug** build (`app/src/debug/AndroidManifest.xml`); the release build accepts `https://` only.

If the free Render server was asleep, the first request can take up to a minute. Pull to refresh once more.

## Tests

```bash
./gradlew testDebugUnitTest
```

85 unit tests, no phone needed: money formatting, order pricing, validators, ISO dates, server-address cleaning, colours and shoe drawing geometry, order statuses, the shop filter, the JSON parsers (using real answers from the server saved in `app/src/test/resources/fixtures`), the Room entity classes, and `ApiClient` against a small local HTTP server (login, errors, `401`, the `PATCH`-as-`POST` header).

## Build a signed APK

1. `Build → Generate Signed App Bundle / APK... → APK → Next`.
2. *Create new...* a keystore (a `.jks` file) and pick strong passwords. **Back the keystore up and never commit it** - `.gitignore` already skips `*.jks` and `*.keystore`. Without it you can never publish an update of the same app.
3. Choose the **release** variant and finish. The APK is in `app/release/`.

For a quick test on a real phone, `./gradlew assembleRelease` also works: that APK is signed with the debug key, so it installs anywhere, but it is not for publishing.

## Project layout

```
app/src/main/java/uz/shushop/app/
  data/     Room entities, DAOs, AppDatabase, SyncDao, JSON parsers, ShopFilter, SessionStore, ShopRepository
  remote/   ApiClient (HttpURLConnection + org.json)
  util/     money/date formatting, validators, pricing, order statuses, shoe drawing geometry, token encryption
  ui/       AppRoot (navigation), ShopViewModel, theme, components, screens
app/src/test/   unit tests and server-answer fixtures
```

## Status and honesty note

The data, network and utility code is compiled and covered by the 85 unit tests above. The Compose screens, Room's generated code and the Gradle build were written without being able to run Android Studio in the environment this project was created in, so **the first sync and build on your computer is the real compile check**. If Android Studio reports an error, it is most likely a small one (an import or a parameter name) and quick to fix.
