package cc.tomko.outify.ui.viewmodel.settings

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import cc.tomko.outify.core.AuthCallbackServer
import cc.tomko.outify.core.SpClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    val spClient: SpClient,
) : ViewModel() {
    private var server: AuthCallbackServer? = null

    fun startAccountAuth(context: Context) {
        server = AuthCallbackServer(onCodeReceived = {code, state ->
            spClient.completeOAuthFlow(code)
            println("handling code: $code")
        })

        server?.start()

        val url = spClient.startOAuthFlow()

        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
        )
    }
}