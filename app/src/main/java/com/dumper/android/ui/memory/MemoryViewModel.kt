package com.dumper.android.ui.memory

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MemoryViewModel : ViewModel() {

    val selectedApps = MutableLiveData<String>()
    val packageName = MutableLiveData<String>()
    val libName = MutableLiveData<String>()

}