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

        binding.btnScan.setOnClickListener {
            binding.tvLog.text = "👉 Approchez la CNIe…"
            binding.tvRawData.text = ""
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collectLatest { state ->
                    when (state) {
                        ScannerState.Idle -> {}

                        ScannerState.Scanning -> {
                            binding.tvLog.text = "📡 Lecture en cours…"
                            binding.tvRawData.text = ""
                            binding.ivPhoto.setImageBitmap(null)
                            binding.ivPhoto.alpha = 0f
                            binding.tvEmoji.text = ""
                        }

                        is ScannerState.Success -> {
                            binding.tvLog.text = "✅ ${state.lastName} ${state.firstNames}, né·e ${state.birthDate}"
                            binding.ivPhoto.setImageBitmap(state.photo)
                            binding.ivPhoto.alpha = 1f
                            binding.tvEmoji.text = state.emoji
                            binding.tvRawData.text = ""
                        }

                        is ScannerState.Error -> {
                            binding.tvLog.text = "❌ Erreur de lecture"
                            binding.tvRawData.text = state.message
                        }
                    }
                }
            }
        }

        nfc = NfcAdapter.getDefaultAdapter(this)
        pi = PendingIntent.getActivity(
            this, 0,
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
            binding.tvLog.text = "❌ Tag NFC manquant"
            return
        }
        val can = binding.etCan.text.toString().trim()
        if (can.length != 6) {
            binding.tvLog.text = "Le CAN doit contenir exactement 6 chiffres."
            return
        }
        vm.scan(tag, can)
    }
}
