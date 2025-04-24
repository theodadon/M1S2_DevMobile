/** util/Ext */

package com.example.cnireader.util

import android.app.Activity

/** Exécute [block] sur le thread UI depuis une coroutine IO */
fun Activity.onUi(block: () -> Unit) = runOnUiThread(block)
