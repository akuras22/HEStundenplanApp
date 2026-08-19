# WorkManager and Glance both instantiate our classes by reflection (WorkManager looks up
# LectureReminderWorker by class name from its persisted work DB; Android instantiates
# NextLectureWidgetReceiver from the manifest, same as any BroadcastReceiver). Both libraries ship
# their own consumer rules that should already cover this, but keeping it explicit here means this
# doesn't silently break if that ever changes upstream.
-keep class de.hsesslingen.stundenplan.data.LectureReminderWorker { public <init>(...); }
-keep class de.hsesslingen.stundenplan.widget.NextLectureWidgetReceiver { public <init>(...); }

# TimetableEvent/Studiengang are only ever (de)serialized by hand (explicit JSONObject.put/get
# calls in TimetableCache/IcsExporter/SettingsStore — no reflection-based JSON library), so their
# field names are never looked up by string and are safe to let R8 rename/shrink normally. No keep
# rule needed for them.
