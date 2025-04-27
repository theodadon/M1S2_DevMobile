package com.example.cnireader.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cnireader.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: ScannerViewModel by viewModels()

    private var nfc: NfcAdapter? = null
    private lateinit var pi: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bouton manuel pour réinitialiser l'affichage
        binding.btnScan.setOnClickListener {
            binding.tvLog.text     = "Approchez votre CNI..."
            binding.tvRawData.text = ""
            binding.ivPhoto.setImageBitmap(null)
            binding.ivPhoto.alpha = 0f
        }

        // Observateur des états du ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collectLatest { state ->
                    when (state) {
                        ScannerState.Idle -> {
                            binding.tvLog.text = "En attente..."
                        }
                        ScannerState.Scanning -> {
                            binding.tvLog.text = "Lecture en cours..."
                            binding.tvRawData.text = ""
                            binding.ivPhoto.setImageBitmap(null)
                            binding.ivPhoto.alpha = 0f
                        }
                        is ScannerState.Success -> {
                            binding.tvLog.text = "${state.lastName} ${state.firstNames}, né(e) ${state.birthDate}"
                            binding.ivPhoto.setImageBitmap(state.photo)
                            binding.ivPhoto.alpha = 1f
                            binding.tvRawData.text = ""
                        }
                        is ScannerState.Error -> {
                            binding.tvLog.text = "Erreur lors de la lecture"
                            binding.tvRawData.text = state.message
                            binding.ivPhoto.setImageBitmap(null)
                            binding.ivPhoto.alpha = 0f
                        }
                    }
                }
            }
        }

        // Initialisation NFC
        nfc = NfcAdapter.getDefaultAdapter(this)
        pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onResume() {
        super.onResume()
        nfc?.enableForegroundDispatch(
            this, pi,
            arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)),
            arrayOf(arrayOf(IsoDep::class.java.name))
        )
    }

    override fun onPause() {
        super.onPause()
        nfc?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != NfcAdapter.ACTION_TECH_DISCOVERED) return

        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag == null) {
            binding.tvLog.text = "Tag NFC non détecté"
            return
        }

        vm.scan(tag)
    }
}
