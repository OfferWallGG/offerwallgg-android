# Offerwall.GG Android SDK

The Offerwall.GG wall inside your Android app, in one line.

* No dependencies: it uses the system WebView.
* Android 5.0 (API 21) and up, Kotlin or Java.
* Starting an offer opens the Play Store or the browser. The wall's own pages,
  including Traffic Tasks, stay in your app.

## 1. Add it

**Gradle (JitPack):** add the JitPack repository in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

and the SDK in your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.offerwallgg:offerwallgg-android:1.0.0")
}
```

**Prebuilt:** copy `dist/offerwallgg-1.0.0.aar` to `app/libs/` and add:

```kotlin
dependencies {
    implementation(files("libs/offerwallgg-1.0.0.aar"))
}
```

**From source:** copy the `offerwallgg` folder into your project, add
`include(":offerwallgg")` to `settings.gradle.kts` and
`implementation(project(":offerwallgg"))` to your app.

## 2. Show the wall

```kotlin
import gg.offerwall.sdk.OfferwallGG
import gg.offerwall.sdk.OfferwallOptions

OfferwallGG.show(this, OfferwallOptions(
    publicKey = "YOUR_PUBLIC_KEY",
    userId = currentUser.id,
    title = "Earn coins",
))
```

Java:

```java
OfferwallGG.show(this, new OfferwallOptions("YOUR_PUBLIC_KEY", user.getId()));
```

Inside your own screen instead of full screen:

```kotlin
val wall = OfferwallView(context)
wall.load(OfferwallOptions("YOUR_PUBLIC_KEY", currentUser.id))
// in your back handling:  if (!wall.onBackPressedInWall()) finish()
// in onDestroy:           wall.release()
```

## 3. Credit rewards on your server

Rewards are **not** sent to the app. Offerwall.GG sends a signed postback to
your server when the advertiser confirms a completion, and your server credits
the user. Set the postback URL on your placement at offerwall.gg — see
https://offerwall.gg/developers/postbacks.

## Signed links (optional)

If your placement requires signed links, your SERVER computes the signature
and the app passes it in:

```
signature = hex( HMAC-SHA256( "appId=<publicKey>&userId=<userId>", <secretKey> ) )
```

```kotlin
OfferwallGG.show(this, OfferwallOptions("YOUR_PUBLIC_KEY", user.id, signature = signatureFromYourServer))
```

**Never put the secret key in the app.** Anyone can read it out of an APK.

## Listener (optional)

```kotlin
OfferwallGG.listener = object : OfferwallListener {
    override fun onOpened() {}
    override fun onClosed() { refreshBalanceFromYourServer() }
    override fun onOfferStarted(url: String) {}
    override fun onError(description: String) {}
}
```

## Build and test

```
./gradlew :offerwallgg:testDebugUnitTest :offerwallgg:assembleRelease
```
