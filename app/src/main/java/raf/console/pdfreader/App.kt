package raf.console.pdfreader

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.yandex.mobile.ads.common.MobileAds
import raf.console.pdfreader.ads.AdManagerHolder
import raf.console.pdfreader.ads.AppOpenAdManager

class App : Application() {

    private lateinit var appOpenManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()

        // Yandex Mobile Ads init (легкий, без App ID)
        MobileAds.initialize(this) {}

        // Межстраничная: создаём и сразу прелоадим
        AdManagerHolder.initialize(this)

        // App Open: создаём менеджер (он сам подпишется на ProcessLifecycleOwner и начнёт загрузку)
        appOpenManager = AppOpenAdManager(this)

        // Чтобы AppOpen знал текущую активити — регистрируем коллбэки активности
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                appOpenManager.onActivityResumed(activity)
            }
            override fun onActivityPaused(activity: Activity) {
                appOpenManager.onActivityPaused(activity)
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
}
