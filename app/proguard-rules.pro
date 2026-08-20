# WorkManager instantiates Workers by reflection; keep this one explicitly since
# proguard-rules.pro otherwise has no custom rules for R8 to fall back on.
-keep class opb.myniceapp.dint.work.UpdateCheckWorker { *; }
