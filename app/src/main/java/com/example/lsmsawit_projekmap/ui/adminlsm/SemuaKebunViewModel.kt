package com.example.lsmsawit_projekmap.ui.adminlsm

import androidx.lifecycle.*
import com.example.lsmsawit_projekmap.model.KebunAdminViewData
import com.example.lsmsawit_projekmap.repository.KebunRepository
import kotlinx.coroutines.launch

class SemuaKebunViewModel : ViewModel() {

    private val repository = KebunRepository()

    private val _kebunList = MutableLiveData<List<KebunAdminViewData>>()
    val kebunList: LiveData<List<KebunAdminViewData>> = _kebunList

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var fullList: List<KebunAdminViewData> = emptyList()

    fun loadKebun() {
        viewModelScope.launch {
            try {
                _loading.value = true
                fullList = repository.getAllKebunWithOwner()
                _kebunList.value = fullList
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun filter(query: String) {
        _kebunList.value =
            if (query.isBlank()) fullList
            else fullList.filter {
                it.kebun.namaKebun.contains(query, true) ||
                        it.namaPemilik.contains(query, true) ||
                        it.kebun.status.contains(query, true)
            }
    }
}
