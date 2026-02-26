### Modules
Outify composes of these modules:
- [app](https://github.com/iTomKo/Outify/tree/master/docs/modules/app.md): the UI itself, written in Kotlin.
- [rust/librespot-ffi](https://github.com/iTomKo/Outify/tree/master/docs/modules/ffi.md): Rust backend utilizing JNI to bridge librespot with the UI
- [rust/librespot](https://github.com/iTomKo/Outify/tree/master/docs/modules/librespot.md): fork of [librespot-org/librespot](https://github.com/librespot-org/librespot), serves as the main backend

For more information about the modules take a look at their docs.

### Building from source
Prerequisites:
- JDK17 in `$JAVA_HOME`
- Gradle
- Maven
- Cargo (for librespot-ffi, librespot)
- Linux/WSL - for `./buildLibrespot.sh`

When building from source, please clone the repository with submodules.
```agsl
git clone --recurse-submodules https://github.com/iTomKo/Outify
```

Make sure you have JDK17 in `$JAVA_HOME`.

#### Building Rust backend
Run `./buildLibrespot.sh` (a bash script) from the repository root.
Note that this can take a while when running for the first time.
This script automatically builds the `.so` library files and moves them to the appropiate place.
> [!NOTE]
> Without built Rust backend the app **will not work**!

#### Building the app
- from Android Studio
- using `./gradlew build`
