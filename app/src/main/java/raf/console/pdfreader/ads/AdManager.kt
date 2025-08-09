package raf.console.pdfreader.ads


import android.app.Activity
import android.content.Context
import android.util.Log
import com.yandex.mobile.ads.rewarded.Reward

/**
 * Singleton object to hold and manage ad manager instances.
 */
object AdManagerHolder {
    private const val TAG = "AdManagerHolder"

    // Менеджеры по каждому adUnitId
    private val interstitialMap = mutableMapOf<String, InterstitialAdManager>()

    fun initialize(context: Context) {
        // Можно ничего не делать тут, или прогреть часто используемые ID
        // preloadInterstitialAd(context, "R-M-16660854-6")
        // preloadInterstitialAd(context, "R-M-16660854-4")
    }

    private fun managerFor(context: Context, adUnitId: String): InterstitialAdManager {
        return interstitialMap.getOrPut(adUnitId) {
            InterstitialAdManager(context.applicationContext)
        }
    }

    fun preloadInterstitialAd(context: Context, adUnitId: String) {
        val mgr = managerFor(context, adUnitId)
        mgr.loadAd(
            adUnitId = adUnitId,
            onLoaded = { Log.i(TAG, "Preloaded interstitial: $adUnitId") },
            onError  = { e -> Log.e(TAG, "Preload failed $adUnitId: ${e.description}") }
        )
    }

    /**
     * Покажи объявление, а если его нет — загрузи и покажи, когда загрузится.
     * Есть защитный таймаут (например, 1500 мс) — если не успело, идем дальше.
     */
    fun showInterstitialAd(
        activity: Activity,
        adUnitId: String,
        timeoutMs: Long = 1500,
        onShown: () -> Unit = {},
        onDismissed: () -> Unit = {}
    ) {
        val mgr = managerFor(activity, adUnitId)

        var finished = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val finishIfNotShown = Runnable {
            if (!finished) {
                finished = true
                onDismissed()  // не успели показать — продолжаем UX
            }
        }
        handler.postDelayed(finishIfNotShown, timeoutMs)

        // Попробуем показать сразу
        mgr.show(
            activity = activity,
            onShown = {
                if (!finished) {
                    finished = true
                    handler.removeCallbacks(finishIfNotShown)
                    onShown()
                }
            },
            onDismissed = {
                if (!finished) {
                    finished = true
                    handler.removeCallbacks(finishIfNotShown)
                    onDismissed()
                }
            }
        )

        // Если не было готового креатива — подгружаем и показываем по готовности
        mgr.loadAd(
            adUnitId = adUnitId,
            onLoaded = {
                // Если к этому моменту уже не “finished”, пробуем показать
                mgr.show(
                    activity = activity,
                    onShown = {
                        if (!finished) {
                            finished = true
                            handler.removeCallbacks(finishIfNotShown)
                            onShown()
                        }
                    },
                    onDismissed = {
                        if (!finished) {
                            finished = true
                            handler.removeCallbacks(finishIfNotShown)
                            onDismissed()
                        }
                    }
                )
            },
            onError = {
                // Если загрузка не удалась — по таймауту уйдем дальше
                Log.e(TAG, "Load failed for $adUnitId: ${it.description}")
            }
        )
    }

    fun destroy() {
        interstitialMap.values.forEach { it.destroy() }
        interstitialMap.clear()
    }
}
