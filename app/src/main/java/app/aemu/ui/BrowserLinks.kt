package app.aemu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import app.aemu.R
import app.aemu.catalog.CatalogUrls

/** Resolve a generic web URL first, avoiding vendor/app deep-link handlers for the actual URL. */
fun openBrowser(ctx: Context, url: String) {
    val safe = CatalogUrls.valid(url) ?: return
    try {
        val generic = Intent(Intent.ACTION_VIEW, Uri.parse("https://aemu-browser.invalid/"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val resolved = ctx.packageManager.resolveActivity(generic, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        val browsers = ctx.packageManager.queryIntentActivities(generic, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }.toSet()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safe)).addCategory(Intent.CATEGORY_BROWSABLE)
        val pkg = resolved?.activityInfo?.packageName
        if (pkg != null && pkg in browsers) intent.setPackage(pkg)
        else intent.selector = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
        ctx.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        Toast.makeText(ctx, R.string.browser_unavailable, Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(ctx, R.string.browser_unavailable, Toast.LENGTH_LONG).show()
    }
}
