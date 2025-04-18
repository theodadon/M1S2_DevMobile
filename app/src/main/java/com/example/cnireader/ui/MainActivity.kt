package com.example.cnireader.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.cnireader.R
import com.example.cnireader.nfc.PassportReader
import com.example.cnireader.util.onUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
//import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class MainActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pending: PendingIntent
    private val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
    private val techs   = arrayOf(arrayOf(IsoDep::class.java.name))

    private lateinit var etCan: EditText
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* SpongyCastle en position 1 pour ECDSA/ECDH */
        //if (Security.getProvider("SC") == null) {
        //    Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)}

        etCan = findViewById(R.id.et_can)
        tvLog = findViewById(R.id.tv_log)
        findViewById<Button>(R.id.btn_scan).setOnClickListener {
            tvLog.text = "Approchez la carte au dos du téléphone…"
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pending = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onResume() { super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pending, filters, techs) }

    override fun onPause()  { super.onPause()
        nfcAdapter.disableForegroundDispatch(this) }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != NfcAdapter.ACTION_TECH_DISCOVERED) return
        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return

        val can = etCan.text.toString().trim()
        if (can.length != 6) { tvLog.text = "Le CAN doit contenir exactement 6 chiffres."; return }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                /* Charge le certificat CSCA depuis assets */
                val cscaBytes = assets.open("csca_france.crt").readBytes()

                val data = PassportReader.read(tag, can, cscaBytes)
                onUi {
                    tvLog.text = """
                        ✅ Authenticité confirmée !
                        Nom : ${data.lastName}
                        Prénoms : ${data.firstNames}
                        Naissance : ${data.birthDate}
                    """.trimIndent()
                }
            } catch (e: Exception) {
                onUi { tvLog.text = "❌ Erreur : ${e.message}" }
                e.printStackTrace()
            }
        }
    }
}
