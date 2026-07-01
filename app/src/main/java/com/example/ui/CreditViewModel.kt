package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CreditEntity
import com.example.data.CreditRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreditViewModel : ViewModel() {

    private val repository = CreditRepository()

    val credits: StateFlow<List<CreditEntity>> =
        repository.observeCredits()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun addCredit(credit: CreditEntity) {

        viewModelScope.launch {

            repository.addCredit(credit)

        }
    }
}