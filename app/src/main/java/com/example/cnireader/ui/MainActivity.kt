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
    private val vm: ScannerViewModel by viewModels()

    @Inject lateinit var emojiApi: EmojiApiService
    @Inject lateinit var accessKey: String

    private var nfc: NfcAdapter? = null
    private lateinit var pi: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NFC
        binding.btnScan.setOnClickListener {
            binding.tvLog.text = "👉 Approchez la CNIe…"
            binding.tvRawData.text = ""
        }

        // Test valide
        binding.btnTestValid.setOnClickListener {
            lifecycleScope.launch {
                val list = try {
                    emojiApi.getAllEmojis(accessKey)
                } catch (e: Exception) {
                    binding.tvLog.text = "API KO"
                    binding.tvRawData.text = e.stackTraceToString()
                    return@launch
                }
                val em = list.random().character
                binding.tvLog.text = "✅ TEST VALIDE – Dupont Jean, 92-05-14\nEmoji : $em"
                binding.ivPhoto.setImageResource(android.R.drawable.ic_menu_camera)
                binding.ivPhoto.alpha = 1f
                binding.tvEmoji.text = em
                binding.tvRawData.text = list.take(5).toString()
            }
        }

        // Test invalide
        binding.btnTestInvalid.setOnClickListener {
            binding.tvLog.text = "❌ TEST PAS VALIDE"
            binding.ivPhoto.setImageBitmap(null)
            binding.ivPhoto.alpha = 0f
            binding.tvEmoji.text = ""
            binding.tvRawData.text = ""
        }

        // Test brut API
        binding.btnTestApi.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val all = emojiApi.getAllEmojis(accessKey)
                    binding.tvLog.text = "API OK : ${all.size} emojis"
                    binding.tvEmoji.text = all.firstOrNull()?.character.orEmpty()
                    binding.tvRawData.text = all.take(3).toString()
                } catch (e: Exception) {
                    binding.tvLog.text = "API KO"
                    binding.tvRawData.text = e.stackTraceToString()
                }
            }
        }

        // Observateur
        lifecycleScope.launchWhenStarted {
            vm.state.collectLatest { state ->
                when (state) {
                    ScannerState.Idle -> Unit
                    ScannerState.Scanning -> {
                        binding.tvLog.text = "Lecture en cours…"
                        binding.ivPhoto.setImageBitmap(null)
                        binding.ivPhoto.alpha = 0f
                        binding.tvEmoji.text = ""
                        binding.tvRawData.text = ""
                    }
                    is ScannerState.Success -> {
                        binding.tvLog.text = "✅ ${state.lastName} ${state.firstNames}, né·e ${state.birthDate}"
                        binding.ivPhoto.setImageBitmap(state.photo)
                        binding.ivPhoto.alpha = 1f
                        binding.tvEmoji.text = state.emoji
                        binding.tvRawData.text = "Emoji reçu : ${state.emoji}"
                    }
                    is ScannerState.Error -> {
                        binding.tvLog.text = "❌ Erreur scan"
                        binding.tvRawData.text = state.message
                    }
                }
            }
        }

        // Setup NFC intent
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
        try {
            if (intent.action != NfcAdapter.ACTION_TECH_DISCOVERED) return
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                ?: throw IllegalStateException("Tag manquant")
            val can = binding.etCan.text.toString().trim()
            require(can.length == 6) { "Le CAN doit contenir exactement 6 chiffres." }
            vm.scan(tag, can)
        } catch (e: Exception) {
            Log.e("MainActivity", "NFC error", e)
            binding.tvLog.text = "❌ Erreur NFC"
            binding.tvRawData.text = e.stackTraceToString()
        }
    }
}
