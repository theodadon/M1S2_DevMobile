package com.example.cnireader.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cnireader.databinding.ActivityMainBinding
import com.example.cnireader.network.EmojiApiService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ScannerViewModel by viewModels()

    // Injection de l'API et de la clé
    @Inject lateinit var emojiApi: EmojiApiService
    @Inject lateinit var accessKey: String

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bouton Scan NFC
        binding.btnScan.setOnClickListener {
            binding.tvLog.text = "👉 Mode NFC : Approchez la carte…"
        }

        // Test Valide / Invalid
        binding.btnTestValid.setOnClickListener {
            binding.tvLog.text = "✅ TEST VALIDE : Dupont Jean, né·e le 1992-05-14"
            binding.ivPhoto.setImageResource(android.R.drawable.ic_menu_camera)
            binding.ivPhoto.alpha = 1f
            binding.tvEmoji.text = "Emoji du test : 😀"
        }
        binding.btnTestInvalid.setOnClickListener {
            binding.tvLog.text = "❌ TEST PAS VALIDE : échec de lecture"
            binding.ivPhoto.setImageBitmap(null)
            binding.ivPhoto.alpha = 0f
            binding.tvEmoji.text = ""
        }

        // --- NOUVEAU : Test API Emoji direct ---
        binding.btnTestValid.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val all = emojiApi.getAllEmojis(accessKey)
                    val emojiChar = all.random().character
                    Log.d("MainActivity", "Emoji Test Valide tiré : $emojiChar")
                    binding.tvLog.text = "✅ TEST VALIDE : Dupont Jean, né·e le 1992-05-14"
                    binding.ivPhoto.setImageResource(android.R.drawable.ic_menu_camera)
                    binding.ivPhoto.alpha = 1f
                    binding.tvEmoji.text = "Emoji du test : $emojiChar"
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erreur API Test Valide : ${e.message}", e)
                    binding.tvLog.text = "❌ API Emoji KO pour test valide : ${e.message}"
                    binding.ivPhoto.setImageBitmap(null)
                    binding.ivPhoto.alpha = 0f
                    binding.tvEmoji.text = ""
                }
            }
        }

        // Observateur du flow de scan réel
        lifecycleScope.launchWhenStarted {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is ScannerState.Idle -> { /* rien */ }
                    ScannerState.Scanning -> {
                        binding.tvLog.text = "Lecture en cours…"
                        binding.ivPhoto.setImageBitmap(null)
                        binding.ivPhoto.alpha = 0f
                        binding.tvEmoji.text = ""
                    }
                    is ScannerState.Success -> {
                        binding.tvLog.text =
                            "✅ ${state.lastName} ${state.firstNames}, né·e le ${state.birthDate}"
                        binding.ivPhoto.setImageBitmap(state.photo)
                        binding.ivPhoto.alpha = 1f
                        binding.tvEmoji.text = "Emoji du jour : ${state.emoji}"
                    }
                    is ScannerState.Error -> {
                        binding.tvLog.text = "❌ ${state.message}"
                    }
                }
            }
        }

        // Initialisation NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)),
            arrayOf(arrayOf(IsoDep::class.java.name))
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != NfcAdapter.ACTION_TECH_DISCOVERED) return
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val can = binding.etCan.text.toString().trim()
        if (can.length != 6) {
            binding.tvLog.text = "Le CAN doit contenir exactement 6 chiffres."
            return
        }
        viewModel.scan(tag, can)
    }
}
