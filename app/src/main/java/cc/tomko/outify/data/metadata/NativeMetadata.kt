package cc.tomko.outify.data.metadata

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeMetadata @Inject constructor() {
    external fun getNativeMetadata(uri: String): String
}