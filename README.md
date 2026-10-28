# Header/Source Split (Rider plugin)

Opening a C/C++ header (`.h .hpp .hh .hxx .h++`) automatically opens its source file
(`.cpp .cc .cxx .c .c++`) in a split next to it, and vice versa. Other file types are ignored.
If there's no matching file, nothing happens.

## Where it looks for the matching file
1. Same folder, same base name.
2. Mirrored folders: `Public` ↔ `Private` (Unreal), `Classes` → `Private`, `include`/`inc` ↔ `src`/`source`,
   keeping the same sub-path (`Public/Foo/Bar.h` ↔ `Private/Foo/Bar.cpp`).
3. Any same-named file in the project index, picking the one closest on disk.

## Settings
**Settings → Tools → Header/Source Split**
- Enable or disable the plugin.
- Layout: side by side (left | right) or stacked (top / bottom).
- Order: header first (left/top) or source first.

## Build
Needs the local Rider install at `D:/jetbrains_ides/Rider`. To use a different path, change `riderPath` and
`org.gradle.java.installations.paths` in `gradle.properties`.

```
gradlew.bat buildPlugin
```
The output is `build/distributions/HeaderSourceSplit-<version>.zip`.

## Install
In Rider: **Settings → Plugins → ⚙ → Install Plugin from Disk…**, pick the zip, then restart Rider.

## Note
The plugin uses editor-window APIs (`FileEditorManagerEx`, `EditorWindow.split`) that JetBrains doesn't
guarantee stay stable. A future Rider update may need a rebuild or a small fix.
