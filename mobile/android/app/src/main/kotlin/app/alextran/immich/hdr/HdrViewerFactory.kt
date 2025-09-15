package app.alextran.immich.hdr

import android.content.Context
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

/**
 * Factory class that creates instances of HdrViewer for the platform view.
 * This factory is registered with Flutter's platform view registry.
 */
class HdrViewerFactory : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val creationParams = args as Map<String?, Any?>?
        return HdrViewer(context, viewId, creationParams)
    }
}
