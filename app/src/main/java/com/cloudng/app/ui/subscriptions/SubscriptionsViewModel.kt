package com.cloudng.app.ui.subscriptions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.cloudng.app.data.model.Subscription
import com.cloudng.app.data.model.UpdateInterval
import com.cloudng.app.data.repository.SubscriptionRepository
import com.cloudng.app.work.SubscriptionUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionsUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val refreshingId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed class SubscriptionsEvent {
    data class AddSubscription(val subscription: Subscription) : SubscriptionsEvent()
    data class DeleteSubscription(val subscription: Subscription) : SubscriptionsEvent()
    data class RefreshSubscription(val subscription: Subscription) : SubscriptionsEvent()
    data class UpdateSubscription(val subscription: Subscription) : SubscriptionsEvent()
    object DismissMessages : SubscriptionsEvent()
}

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            subscriptionRepository.observeAll().collect { subs ->
                _uiState.update { it.copy(subscriptions = subs) }
            }
        }
    }

    fun onEvent(event: SubscriptionsEvent) {
        when (event) {
            is SubscriptionsEvent.AddSubscription -> viewModelScope.launch {
                subscriptionRepository.save(event.subscription)
                if (event.subscription.updateInterval != UpdateInterval.MANUAL) {
                    SubscriptionUpdateWorker.scheduleAll(
                        WorkManager.getInstance(context),
                        event.subscription.id,
                        event.subscription.updateInterval
                    )
                }
            }
            is SubscriptionsEvent.DeleteSubscription -> viewModelScope.launch {
                SubscriptionUpdateWorker.cancel(
                    WorkManager.getInstance(context), event.subscription.id
                )
                subscriptionRepository.delete(event.subscription)
            }
            is SubscriptionsEvent.RefreshSubscription -> viewModelScope.launch {
                _uiState.update { it.copy(refreshingId = event.subscription.id) }
                val result = subscriptionRepository.refresh(event.subscription)
                _uiState.update {
                    it.copy(
                        refreshingId = null,
                        successMessage = result.getOrNull()?.let { count -> "Updated: $count profiles" },
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            }
            is SubscriptionsEvent.UpdateSubscription -> viewModelScope.launch {
                subscriptionRepository.save(event.subscription)
            }
            SubscriptionsEvent.DismissMessages -> _uiState.update {
                it.copy(errorMessage = null, successMessage = null)
            }
        }
    }
}
