package app.rasoi.android

import android.app.Activity
import android.app.Application
import android.os.Bundle

/** Firebase phone verification needs a foreground activity for its reCAPTCHA fallback; this tracks the resumed one. */
internal class CurrentActivity(application: Application) {
    var current: Activity? = null
        private set

    init {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    current = activity
                }

                override fun onActivityPaused(activity: Activity) {
                    if (current === activity) current = null
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }
}
