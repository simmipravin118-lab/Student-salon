package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CustomerTicketInfo
import com.example.data.model.DailySummary
import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicket
import com.example.data.model.SalonService
import com.example.data.model.ShopConfig
import com.example.data.repository.SalonRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab {
    CUSTOMER,
    LIVE_BOARD,
    OWNER,
    SALON_INFO
}

data class SalonUiState(
    val shopConfig: ShopConfig = ShopConfig(),
    val allTickets: List<QueueTicket> = emptyList(),
    val waitingTickets: List<QueueTicket> = emptyList(),
    val servingTicket: QueueTicket? = null,
    val skippedTickets: List<QueueTicket> = emptyList(),
    val myTicketInfo: CustomerTicketInfo? = null,
    val myTicketsInfoList: List<CustomerTicketInfo> = emptyList(),
    val selectedTicketId: String? = null,
    val dailySummary: DailySummary = DailySummary(),
    val isOwnerAuthenticated: Boolean = false,
    val currentTab: AppTab = AppTab.CUSTOMER,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val snackbarMessage: String? = null,
    val nextCustomerInLine: QueueTicket? = null,
    val syncStatus: com.example.data.model.SyncStatus = com.example.data.model.SyncStatus()
)

class SalonViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = SalonRepository(
        queueTicketDao = database.queueTicketDao(),
        shopConfigDao = database.shopConfigDao(),
        context = application
    )

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    private val _isOwnerAuthenticated = MutableStateFlow(repository.isOwnerAuthenticated())
    private val _currentTab = MutableStateFlow(AppTab.CUSTOMER)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _selectedTicketId = MutableStateFlow<String?>(null)
    val selectedTicketId: StateFlow<String?> = _selectedTicketId.asStateFlow()

    // Form inputs state
    val customerNameInput = MutableStateFlow("")
    val customerPhoneInput = MutableStateFlow("")
    val selectedService = MutableStateFlow(SalonService.HAIRCUT)
    val customerNotesInput = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.ensureDefaultShopConfig()
        }

        // Live 1-second ticker for accurate countdowns & remaining service time
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _currentTime.value = System.currentTimeMillis()
            }
        }
    }

    val uiState: StateFlow<SalonUiState> = combine(
        repository.getShopConfig(),
        repository.getTodayTickets(),
        repository.syncStatus,
        _currentTime,
        _isOwnerAuthenticated,
        _currentTab,
        _selectedTicketId
    ) { args: Array<Any?> ->
        val shopConfig = args[0] as ShopConfig
        @Suppress("UNCHECKED_CAST")
        val tickets = args[1] as List<QueueTicket>
        val syncStatus = args[2] as com.example.data.model.SyncStatus
        val now = args[3] as Long
        val isOwner = args[4] as Boolean
        val tab = args[5] as AppTab
        val selectedId = args[6] as? String

        val waiting = tickets
            .filter { it.status == QueueStatus.WAITING }
            .sortedWith(
                compareByDescending<QueueTicket> { it.isRejoinedPriority }
                    .thenBy { it.queueNumber }
            )
        val serving = tickets.firstOrNull { it.status == QueueStatus.SERVING }
        val skipped = tickets.filter { it.status == QueueStatus.SKIPPED }.sortedBy { it.queueNumber }

        val savedTicketIds = repository.getMySavedTicketIds()
        // Active tickets only (excluding CANCELLED and COMPLETED)
        val myActiveTickets = tickets.filter { ticket ->
            ticket.id in savedTicketIds &&
            ticket.status != QueueStatus.CANCELLED &&
            ticket.status != QueueStatus.COMPLETED
        }

        val myTicketsInfoList = myActiveTickets.map { ticket ->
            repository.calculateTicketInfo(ticket, tickets, now)
        }.sortedWith(
            compareByDescending<CustomerTicketInfo> { it.ticket.status == QueueStatus.SERVING }
                .thenByDescending { it.ticket.status == QueueStatus.WAITING }
                .thenBy { it.ticket.queueNumber }
        )

        val activeSelectedId = if (selectedId != null && myTicketsInfoList.any { it.ticket.id == selectedId }) {
            selectedId
        } else {
            myTicketsInfoList.firstOrNull()?.ticket?.id
        }

        val myTicketInfo = myTicketsInfoList.firstOrNull { it.ticket.id == activeSelectedId } ?: myTicketsInfoList.firstOrNull()

        val summary = repository.calculateDailySummary(tickets)
        val nextCustomer = waiting.firstOrNull()

        SalonUiState(
            shopConfig = shopConfig,
            allTickets = tickets,
            waitingTickets = waiting,
            servingTicket = serving,
            skippedTickets = skipped,
            myTicketInfo = myTicketInfo,
            myTicketsInfoList = myTicketsInfoList,
            selectedTicketId = activeSelectedId,
            dailySummary = summary,
            isOwnerAuthenticated = isOwner,
            currentTab = tab,
            currentTimeMillis = now,
            snackbarMessage = _snackbarMessage.value,
            nextCustomerInLine = nextCustomer,
            syncStatus = syncStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalonUiState()
    )

    fun switchTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectTicket(ticketId: String) {
        _selectedTicketId.value = ticketId
    }

    fun joinQueue(onSuccess: (QueueTicket) -> Unit = {}) {
        val name = customerNameInput.value.trim()
        if (name.isEmpty()) {
            showSnackbar("Please enter your name to join queue")
            return
        }

        if (!uiState.value.shopConfig.isOpen) {
            showSnackbar("Salon is currently CLOSED. Please join when open.")
            return
        }

        viewModelScope.launch {
            val ticket = repository.joinQueue(
                customerName = name,
                customerPhone = customerPhoneInput.value,
                service = selectedService.value,
                notes = customerNotesInput.value
            )
            _selectedTicketId.value = ticket.id
            customerNameInput.value = ""
            customerPhoneInput.value = ""
            customerNotesInput.value = ""
            showSnackbar("Queue Ticket #${ticket.queueNumber} issued successfully!")
            onSuccess(ticket)
        }
    }

    fun cancelMyTicket(ticketId: String) {
        viewModelScope.launch {
            repository.cancelTicket(ticketId)
            showSnackbar("Ticket cancelled.")
        }
    }

    fun clearMySavedTicket(ticketId: String? = null) {
        viewModelScope.launch {
            if (ticketId != null) {
                repository.removeMyTicketId(ticketId)
            } else {
                repository.clearAllMySavedTickets()
            }
            showSnackbar("Ticket cleared.")
        }
    }

    fun toggleShopStatus(isOpen: Boolean) {
        viewModelScope.launch {
            repository.setShopOpen(isOpen)
            showSnackbar(if (isOpen) "Salon is now OPEN 🟢" else "Salon is now CLOSED 🔴")
        }
    }

    fun startNextCustomer() {
        val next = uiState.value.waitingTickets.firstOrNull()
        if (next == null) {
            showSnackbar("No waiting customers in queue.")
            return
        }
        viewModelScope.launch {
            repository.startCustomer(next.id)
            showSnackbar("Now serving #${next.queueNumber} - ${next.customerName} 🟢")
        }
    }

    fun startCustomer(ticketId: String) {
        viewModelScope.launch {
            repository.startCustomer(ticketId)
            showSnackbar("Customer seated for service 🟢")
        }
    }

    fun completeService(ticketId: String) {
        viewModelScope.launch {
            repository.completeService(ticketId)
            showSnackbar("Service completed ✅")
        }
    }

    fun skipCustomer(ticketId: String) {
        viewModelScope.launch {
            repository.skipCustomer(ticketId)
            showSnackbar("Customer skipped ⏭️")
        }
    }

    fun rejoinCustomer(ticketId: String) {
        viewModelScope.launch {
            repository.rejoinCustomer(ticketId)
            showSnackbar("Customer rejoined with NEXT turn priority ↩️")
        }
    }

    fun resetTodayQueue() {
        viewModelScope.launch {
            repository.clearTodayQueue()
            showSnackbar("Queue reset for today. Numbering restarts at #1.")
        }
    }

    fun seedDemoQueue() {
        viewModelScope.launch {
            repository.seedSampleData()
            showSnackbar("Sample queue loaded for demonstration!")
        }
    }

    fun authenticateOwner(pin: String): Boolean {
        return if (pin == "1234" || pin == uiState.value.shopConfig.ownerPin) {
            _isOwnerAuthenticated.value = true
            repository.setOwnerAuthenticated(true)
            showSnackbar("Welcome, Owner! Dashboard unlocked 🔐")
            true
        } else {
            showSnackbar("Incorrect PIN. Please try again.")
            false
        }
    }

    fun logoutOwner() {
        _isOwnerAuthenticated.value = false
        repository.setOwnerAuthenticated(false)
        showSnackbar("Owner locked.")
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }
}
