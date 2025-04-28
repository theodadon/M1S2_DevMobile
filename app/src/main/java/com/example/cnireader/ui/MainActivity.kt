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
import com.example.cnireader.R
import com.example.cnireader.databinding.ActivityMainBinding
import com.example.cnireader.network.EmojiApiService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: ScannerViewModel by viewModels()

    @Inject lateinit var emojiApi: EmojiApiService
    @Inject lateinit var accessKey: String

    private var nfc: NfcAdapter? = null
    private lateinit var pi: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🛰️ SCAN NFC
        binding.btnScan.setOnClickListener {
            binding.tvLog.text = "Veuillez approcher la carte"
            binding.tvRawData.text = ""
            binding.ivPhoto.setImageBitmap(null)
            binding.ivPhoto.alpha = 0f
            binding.tvEmoji.text = ""
        }

        // 🎯 BOUTON TEST VALIDE
        binding.btnTestValid.setOnClickListener {
            testFakeScan(valid = true)
        }

        // 🎯 BOUTON TEST NON VALIDE
        binding.btnTestInvalid.setOnClickListener {
            testFakeScan(valid = false)
        }

        // 🌐 BOUTON TEST API SIMPLE
        binding.btnTestApi.setOnClickListener {
            testApiOnly()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collectLatest { state ->
                    when (state) {
                        ScannerState.Idle -> {}
                        ScannerState.Scanning -> {
                            binding.tvLog.text = "Lecture en cours..."
                            binding.tvRawData.text = ""
                        }
                        is ScannerState.Error -> {
                            binding.tvLog.text = "Erreur lors de la lecture"
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
            binding.tvLog.text = "Erreur : Tag NFC manquant"
            return
        }
        val can = binding.etCan.text.toString().trim()
        vm.scan(tag, can)
    }

    // 🔥 Fonction de test factice
    private fun testFakeScan(valid: Boolean) {
        binding.tvLog.text = "Test en cours..."
        binding.tvRawData.text = ""
        binding.ivPhoto.setImageBitmap(null)
        binding.ivPhoto.alpha = 0f
        binding.tvEmoji.text = ""

        lifecycleScope.launch {
            try {
                val emojis = emojiApi.getAllEmojis(accessKey)
                val emoji = emojis.random().character

                if (valid) {
                    binding.tvLog.text = "✅ John DOE, né·e 01/01/1990"
                } else {
                    binding.tvLog.text = "❌ Jane SMITH, né·e 31/12/1995"
                }
                binding.tvRawData.text = "Test avec données simulées"
                binding.ivPhoto.setImageResource(R.drawable.fake_photo) // ← ajoute une fausse photo
                binding.ivPhoto.alpha = 1f
                binding.tvEmoji.text = emoji
            } catch (e: Exception) {
                binding.tvLog.text = "Erreur API"
                binding.tvRawData.text = e.message ?: "Erreur inconnue"
            }
        }
    }

    // 🔥 Fonction de test API simple
    private fun testApiOnly() {
        binding.tvLog.text = "Test API en cours..."
        binding.tvRawData.text = ""
        binding.ivPhoto.setImageBitmap(null)
        binding.ivPhoto.alpha = 0f
        binding.tvEmoji.text = ""

        lifecycleScope.launch {
            try {
                val emojis = emojiApi.getAllEmojis(accessKey)
                val emoji = emojis.random().character

                binding.tvLog.text = "🌐 API OK"
                binding.tvRawData.text = "Emoji : $emoji"
                binding.tvEmoji.text = emoji
            } catch (e: Exception) {
                binding.tvLog.text = "Erreur API"
                binding.tvRawData.text = e.message ?: "Erreur inconnue"
            }
        }
    }
}
