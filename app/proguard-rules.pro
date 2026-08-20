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
