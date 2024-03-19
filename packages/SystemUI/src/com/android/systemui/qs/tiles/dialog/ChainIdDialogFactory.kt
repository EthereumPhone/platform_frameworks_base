/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.qs.tiles.dialog

import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.View
import com.android.internal.logging.UiEventLogger
import com.android.systemui.animation.DialogLaunchAnimator
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import java.util.concurrent.Executor
import javax.inject.Inject

private const val TAG = "ChainIdDialogFactory"
private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

/**
 * Factory to create [ChainId] objects.
 */
@SysUISingleton
class ChainIdDialogFactory @Inject constructor(
    @Main private val handler: Handler,
    @Background private val executor: Executor,
    private val context: Context,
    private val uiEventLogger: UiEventLogger,
    private val dialogLaunchAnimator: DialogLaunchAnimator
) {
    companion object {
        var internetDialog: ChainIdDialog? = null
    }

    /** Creates a [ChainIdDialog]. The dialog will be animated from [view] if it is not null. */
    fun create(
        aboveStatusBar: Boolean,
        view: View?
    ) {
        if (internetDialog != null) {
            Log.d(TAG, "ChainIdDialog is showing, do not create it twice.")

            return
        } else {
            println("ChainIdDialog: Creating dialog now.")
            internetDialog = ChainIdDialog(context, this)
            if (view != null) {
                println("ChainIdDialog: Showing dialog now. view not null")
                dialogLaunchAnimator.showFromView(internetDialog!!, view,
                    animateBackgroundBoundsChange = true)
            } else {
                println("ChainIdDialog: Showing dialog now. view is null")
                internetDialog?.show()
            }
        }
    }

    fun destroyDialog() {
        if (DEBUG) {
            Log.d(TAG, "destroyDialog")
        }
        internetDialog = null
    }
}
