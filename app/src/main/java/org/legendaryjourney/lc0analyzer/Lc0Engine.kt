package org.legendaryjourney.lc0analyzer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.xmlpull.v1.XmlPullParser
import java.io.File

data class EngineDescriptor(val name: String, val path: String)

object Lc0Engine {
    private const val PROVIDER_ACTION = "intent.chess.provider.ENGINE"
    private const val AUTHORITY_KEY = "chess.provider.engine.authority"

    fun find(context: Context): EngineDescriptor? {
        val packageManager = context.packageManager
        val providers = packageManager.queryIntentActivities(
            Intent(PROVIDER_ACTION),
            PackageManager.GET_META_DATA,
        )
        for (provider in providers) {
            val packageName = provider.activityInfo.packageName
            if (provider.activityInfo.metaData?.getString(AUTHORITY_KEY) == null) continue
            val resources = packageManager.getResourcesForApplication(provider.activityInfo.applicationInfo)
            val listId = resources.getIdentifier("enginelist", "xml", packageName)
            if (listId == 0) continue

            resources.getXml(listId).use { parser ->
                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG &&
                        parser.name.equals("engine", ignoreCase = true)
                    ) {
                        val name = parser.getAttributeValue(null, "name") ?: "Chess engine"
                        val fileName = parser.getAttributeValue(null, "filename")
                        if (fileName == null) {
                            parser.next()
                            continue
                        }
                        val targets = parser.getAttributeValue(null, "target")
                            ?.split('|')
                            .orEmpty()
                        if (Build.SUPPORTED_ABIS.none { it in targets }) {
                            parser.next()
                            continue
                        }

                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val engineFile = File(appInfo.nativeLibraryDir, fileName)
                        if (engineFile.canExecute()) return EngineDescriptor(name, engineFile.absolutePath)
                    }
                    parser.next()
                }
            }
        }
        return null
    }
}
