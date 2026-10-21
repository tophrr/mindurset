package com.mindurset.app

import android.app.Application
import com.mindurset.app.di.AppContainer

class MindUrSetApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
