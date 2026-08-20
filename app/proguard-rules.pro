# WorkManager instantiates Workers by reflection; keep this one explicitly since
# proguard-rules.pro otherwise has no custom rules for R8 to fall back on.
-keep class opb.myniceapp.dint.work.UpdateCheckWorker { *; }

# Room resolves its generated implementation by name at runtime in release builds.
# Keep the database surface and generated implementation so startup cannot fail
# after minification on signed APKs.
-keep class opb.myniceapp.dint.data.db.UnlockLogDatabase { *; }
-keep class opb.myniceapp.dint.data.db.UnlockLogDatabase_Impl { *; }
-keep interface opb.myniceapp.dint.data.db.UnlockLogDao { *; }
-keep class opb.myniceapp.dint.data.db.UnlockLogEntity { *; }

# androidx.lifecycle.compose.LocalLifecycleOwner (used internally by every
# collectAsStateWithLifecycle() call in this app) reflectively looks up
# androidx.compose.ui.platform.LocalLifecycleOwner for backward compatibility.
# lifecycle-runtime-compose only ships a *conditional* keep rule for this
# (-if ... -keep ...) that never fires here (confirmed: even a corrected,
# non-conditional single-method -keep wasn't enough -- R8's mapping file
# shows it fully INLINES these trivial top-level CompositionLocal property
# getters rather than just renaming them, so a method-signature keep can't
# protect something already eliminated). Keep the whole file's class
# unconditionally instead, confirmed by re-testing on an emulator.
-keep class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt { *; }
