package app.teadroid

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import app.teadroid.data.GiteaApi
import app.teadroid.data.Graph
import app.teadroid.data.SessionStore

class TeaDroidApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        val session = SessionStore(this)
        Graph.session = session
        Graph.api = GiteaApi(session)
    }

    // Images (avatars, README pictures, raw repo files) go through the authenticated OkHttp client,
    // so private-repo content loads too.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { Graph.api.http }))
                add(SvgDecoder.Factory())
            }
            .build()
}
