/* AEmulator Sunset addition, 2026-10-02. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.aemu.AppPrefs
import app.aemu.R
import app.aemu.core.ImageStore
import app.aemu.core.ProcessDeathWatch
import app.aemu.core.awaitVmProcessExit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Lives in the main app process, not :vm. No alarms or background relaunch needed. */
class VmRestartActivity : ComponentActivity() {
    private var failed by mutableStateOf(false)
    private var launched = false
    override fun attachBaseContext(base: Context) = super.attachBaseContext(AppPrefs.wrap(base))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(VmActivity.EXTRA_ID)
        val oldProcess = intent.extras?.getBinder(EXTRA_PROCESS)
        if (id == null || oldProcess == null || ImageStore.get(this, id) == null) { finish(); return }
        val recovery = intent.getBooleanExtra(VmActivity.EXTRA_RECOVERY, false)
        setContent {
            AemuTheme {
                BackHandler(enabled = !failed) { /* Finish the process handoff first. */ }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!failed) CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(if (failed) R.string.vm_restart_failed else R.string.vm_restarting))
                        if (failed) TextButton(onClick = { finish() }) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (!launched && !failed) {
                    try {
                        withTimeout(10_000) { awaitVmProcessExit(BinderDeathWatch(oldProcess)) }
                        VmActivity.start(this@VmRestartActivity, id, recovery, intent.getBooleanExtra(VmActivity.EXTRA_LOW_POWER, false))
                        launched = true
                        finish()
                    } catch (_: TimeoutCancellationException) { failed = true }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { failed = true }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PROCESS = "old_vm_process"
        fun handoff(context: Context, id: String, recovery: Boolean, oldProcess: IBinder, lowPower: Boolean = false) {
            context.startActivity(Intent(context, VmRestartActivity::class.java)
                .putExtra(VmActivity.EXTRA_ID, id).putExtra(VmActivity.EXTRA_RECOVERY, recovery)
                .putExtra(VmActivity.EXTRA_LOW_POWER, lowPower)
                .putExtras(Bundle().apply { putBinder(EXTRA_PROCESS, oldProcess) })
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private class BinderDeathWatch(private val binder: IBinder) : ProcessDeathWatch {
    private var recipient: IBinder.DeathRecipient? = null
    override val alive get() = binder.isBinderAlive
    override fun register(onDeath: () -> Unit) {
        val listener = IBinder.DeathRecipient { onDeath() }
        recipient = listener
        try { binder.linkToDeath(listener, 0) } catch (_: RemoteException) { onDeath() }
    }
    override fun unregister() {
        recipient?.let { runCatching { binder.unlinkToDeath(it, 0) } }
        recipient = null
    }
}
