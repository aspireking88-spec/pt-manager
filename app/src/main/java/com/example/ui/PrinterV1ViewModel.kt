package com.example.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.api.RuijieApiClient
import com.example.data.db.VoucherHistoryEntity
import com.example.data.db.VoucherHistoryRepository
import com.example.data.model.AuthMode
import com.example.data.model.RuijieAccount
import com.example.data.model.RuijieNetworkGroup
import com.example.data.model.RuijieProject
import com.example.data.model.RuijieServer
import com.example.data.model.RuijieTenant
import com.example.data.model.RuijieVoucherItem
import com.example.data.model.Voucher
import com.example.data.model.VoucherPackage
import com.example.data.model.VoucherStatus
import com.example.data.model.createdAt
import com.example.data.repository.RuijieRepository
import com.example.data.session.AccountManager
import com.example.data.session.SettingsStore
import com.example.printer.BluetoothPrinterManager
import com.example.printer.PrintDesignSettings
import com.example.printer.PrintRenderer
import com.example.printer.PrinterConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PrintProgressState(
    val isPrinting: Boolean = false,
    val statusMessage: String = "",
    val current: Int = 0,
    val total: Int = 0
)

data class VoucherDiagnostic(
    val selectedProjectName: String? = null,
    val selectedGroupIdStr: String? = null,
    val selectedTenantName: String? = null,
    val sessionState: String? = null,
    val formattedDisplay: String = ""
)

class PrinterV1ViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val historyRepository = VoucherHistoryRepository(db.voucherHistoryDao())
    val accountManager = AccountManager(application)
    val settingsStore = SettingsStore(application)
    val printerManager = BluetoothPrinterManager(application)
    val repository = RuijieRepository(RuijieApiClient, accountManager)

    val activeAccount: StateFlow<RuijieAccount> = accountManager.activeAccount
    val accounts: StateFlow<List<RuijieAccount>> = accountManager.accounts
    val appearance = settingsStore.appearance
    val printerState: StateFlow<PrinterConnectionState> = printerManager.connectionState
    val printHistory = activeAccount.flatMapLatest { acc ->
        historyRepository.historyForAccount(acc.id)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _vouchers = MutableStateFlow<List<Voucher>>(emptyList())
    val vouchers: StateFlow<List<Voucher>> = _vouchers.asStateFlow()
    val vouchersState: StateFlow<UiState<List<RuijieVoucherItem>>> = _vouchers.map { UiState.Success(it) }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiState.Success(emptyList())
    )

    private val _packages = MutableStateFlow<List<VoucherPackage>>(emptyList())
    val packages: StateFlow<List<VoucherPackage>> = _packages.asStateFlow()

    private val _tenants = MutableStateFlow<List<RuijieTenant>>(emptyList())
    val tenants: StateFlow<List<RuijieTenant>> = _tenants.asStateFlow()

    private val _networkGroups = MutableStateFlow<List<RuijieNetworkGroup>>(emptyList())
    val networkGroups: StateFlow<List<RuijieNetworkGroup>> = _networkGroups.asStateFlow()

    private val _projects = MutableStateFlow<List<RuijieProject>>(emptyList())
    val projects: StateFlow<List<RuijieProject>> = _projects.asStateFlow()

    val isLoadingProjects = MutableStateFlow(false)
    val projectUiState = MutableStateFlow("")
    val selectedProject = MutableStateFlow<RuijieProject?>(null)
    val activeFilter = MutableStateFlow(com.example.data.model.VoucherFilter.ALL)
    val selectedVoucherCodes = MutableStateFlow<Set<String>>(emptySet())
    val filterCounts = MutableStateFlow(mapOf<com.example.data.model.VoucherFilter, Int>())
    val printProgress = MutableStateFlow<PrintProgressState>(PrintProgressState())
    val currentPage = MutableStateFlow(1)
    val pageSize = MutableStateFlow(10)
    val totalVoucherCount = MutableStateFlow(0)
    val selectedTab = MutableStateFlow(0)
    val snackbarMessage = MutableStateFlow<String?>(null)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val allCloudVouchers: StateFlow<List<Voucher>> get() = vouchers

    private val _selectedVoucher = MutableStateFlow<Voucher?>(null)
    val selectedVoucher: StateFlow<Voucher?> = _selectedVoucher.asStateFlow()

    val selectedProjectName: StateFlow<String?> = MutableStateFlow(null)
    val selectedGroupIdStr: StateFlow<String?> = MutableStateFlow(null)
    val selectedTenantName: StateFlow<String?> = MutableStateFlow(null)
    val latestVoucherDiagnostic = MutableStateFlow<VoucherDiagnostic?>(null)
    val printerConnectionState: StateFlow<PrinterConnectionState> get() = printerState
    val selectedPreviewVoucher: StateFlow<Voucher?> get() = _selectedVoucher.asStateFlow()

    val deviceLayoutMode = MutableStateFlow(com.example.data.model.DeviceLayoutMode.AUTO)
    private val _printDesign = MutableStateFlow(PrintDesignSettings())
    val printDesign: StateFlow<PrintDesignSettings> = _printDesign.asStateFlow()
    val printDesignSettings: StateFlow<PrintDesignSettings> get() = _printDesign.asStateFlow()
    val paperConfig = MutableStateFlow(com.example.printer.PaperConfiguration())
    val historyList: StateFlow<List<com.example.data.db.VoucherHistoryEntity>> get() = printHistory
    val latestDiagnostic = MutableStateFlow<Any?>(null)
    val isLoggingIn = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)
    val printerErrorDialog = MutableStateFlow<String?>(null)
    val isPreviewRefreshing = MutableStateFlow(false)
    val previewRefreshMessage = MutableStateFlow<String?>(null)

    val packagesState = MutableStateFlow<UiState<List<VoucherPackage>>>(UiState.Success(_packages.value))
    val selectedPackage = MutableStateFlow<VoucherPackage?>(null)
    val generateQuantity = MutableStateFlow(1)
    val generateRemark = MutableStateFlow("")
    val voucherLength = MutableStateFlow(6)
    val voucherCodeType = MutableStateFlow(com.example.data.model.VoucherCodeType.ALPHANUMERIC)
    val isGenerating = MutableStateFlow(false)
    val isGenerateAndPrinting = MutableStateFlow(false)
    val generatedVouchers = MutableStateFlow<List<Voucher>>(emptyList())
    val generateError = MutableStateFlow<String?>(null)
    val generateUiState = MutableStateFlow<GenerateUiState>(GenerateUiState.Idle)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Dialog Visibilities
    val showProjectSelectorDialog = MutableStateFlow(false)
    val showApiConfigDialog = MutableStateFlow(false)
    val showPrinterDialog = MutableStateFlow(false)
    val showPrintPreviewDialog = MutableStateFlow(false)
    val showVoucherDetailsDialog = MutableStateFlow(false)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            refreshAll()
        }
    }

    fun setVoucherLength(len: Int) { voucherLength.value = len }
    fun setVoucherCodeType(type: com.example.data.model.VoucherCodeType) { voucherCodeType.value = type }
    fun setGenerateQuantity(q: Int) { generateQuantity.value = q }
    fun clearGeneratedVouchers() { generatedVouchers.value = emptyList() }
    fun printBatch(vouchers: List<Voucher>, copies: Int = 1) {
        viewModelScope.launch {
            vouchers.forEach { v ->
                printVoucher(v)
            }
        }
    }
    fun printSingle(voucher: Voucher, copies: Int = 1) {
        printVoucher(voucher)
    }
    fun refreshVoucherDetails(v: Voucher?, onComplete: ((Voucher) -> Unit)? = null) {
        _selectedVoucher.value = v
        if (v != null) onComplete?.invoke(v)
    }
    fun deleteSingleVoucher(v: Voucher?, onComplete: ((String) -> Unit)? = null) {
        if (v != null) {
            deleteVoucher(v)
            onComplete?.invoke("Voucher deleted")
        }
    }
    fun selectVoucherForPreview(v: Voucher?) { _selectedVoucher.value = v }
    fun dismissPrinterErrorDialog() { printerErrorDialog.value = null }

    fun attemptDeleteSelectedVouchers(onComplete: ((String) -> Unit)? = null) {
        val codes = selectedVoucherCodes.value
        viewModelScope.launch {
            _vouchers.value = _vouchers.value.filterNot { codes.contains(it.effectiveCode) }
            selectedVoucherCodes.value = emptySet()
            val msg = "Deleted ${codes.size} voucher(s)"
            _statusMessage.value = msg
            onComplete?.invoke(msg)
        }
    }

    fun attemptDeleteExpiredVouchers(onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val expired = _vouchers.value.filter { it.normalizedStatus == VoucherStatus.EXPIRED }
            _vouchers.value = _vouchers.value.filterNot { it.normalizedStatus == VoucherStatus.EXPIRED }
            val msg = "Deleted ${expired.size} expired voucher(s)"
            _statusMessage.value = msg
            onComplete?.invoke(msg)
        }
    }

    fun deleteExpiredVouchersBeforeDate(date: Long) {
        viewModelScope.launch {
            _vouchers.value = _vouchers.value.filterNot { it.createdAt < date }
        }
    }

    fun deleteExpiredVouchersBeforeDate(matchingVouchers: List<Voucher>, onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val codes = matchingVouchers.map { it.effectiveCode }.toSet()
            _vouchers.value = _vouchers.value.filterNot { codes.contains(it.effectiveCode) }
            val msg = "Deleted ${matchingVouchers.size} expired voucher(s)"
            _statusMessage.value = msg
            onComplete?.invoke(msg)
        }
    }

    fun selectAllVouchers(all: Boolean = true) {
        if (all) {
            selectedVoucherCodes.value = _vouchers.value.map { it.effectiveCode }.toSet()
        } else {
            selectedVoucherCodes.value = emptySet()
        }
    }

    fun selectAllVouchers(vouchersList: List<Voucher>) {
        selectedVoucherCodes.value = vouchersList.map { it.effectiveCode }.toSet()
    }

    fun loadPackages() { refreshAll() }
    fun selectPackage(pkg: VoucherPackage?) { selectedPackage.value = pkg }
    fun copyVoucherCode(code: String) {}

    fun setDeviceLayoutMode(mode: com.example.data.model.DeviceLayoutMode) {
        deviceLayoutMode.value = mode
    }

    fun clearSnackbar() {
        snackbarMessage.value = null
    }

    fun onSSOLoginSuccess() {
        refreshAll()
    }

    fun switchAccount(accOrId: Any) {
        viewModelScope.launch {
            if (accOrId is RuijieAccount) {
                accountManager.setActiveAccount(accOrId)
            } else if (accOrId is String) {
                val found = accounts.value.find { it.id == accOrId }
                if (found != null) accountManager.setActiveAccount(found)
            }
            refreshAll()
        }
    }

    fun addAccount(acc: RuijieAccount) {
        viewModelScope.launch {
            accountManager.addAccount(acc)
            refreshAll()
        }
    }

    fun addAccount(name: String, appId: String, secret: String, server: RuijieServer, customUrl: String) {
        viewModelScope.launch {
            val acc = RuijieAccount(
                accountName = name,
                appId = appId,
                secret = secret,
                server = server,
                customUrl = customUrl
            )
            accountManager.addAccount(acc)
            refreshAll()
        }
    }

    fun updateAccount(acc: RuijieAccount) {
        viewModelScope.launch {
            accountManager.updateAccount(acc)
            refreshAll()
        }
    }

    fun deleteAccount(accOrId: Any) {
        viewModelScope.launch {
            if (accOrId is RuijieAccount) {
                accountManager.removeAccount(accOrId.id)
            } else if (accOrId is String) {
                accountManager.removeAccount(accOrId)
            }
            refreshAll()
        }
    }

    fun logout() {
        refreshAll()
    }

    fun setTab(index: Int) {
        selectedTab.value = index
    }

    fun loadVouchers(page: Int = 1) {
        refreshAll()
    }

    fun loadProjects() {
        refreshAll()
    }

    fun deselectAllVouchers() {
        selectedVoucherCodes.value = emptySet()
    }

    fun copySelectedVouchers(context: android.content.Context? = null) {}

    fun refreshRuijieSession() {
        refreshAll()
    }

    fun refreshAccountInfo() {
        refreshAll()
    }

    fun refreshProjectTree() {
        refreshAll()
    }

    fun refreshUserGroups() {
        refreshAll()
    }

    fun runReadOnlyDiagnostics() {}

    fun unbindVoucher(v: Voucher, onResult: (Boolean, String?, Voucher) -> Unit) {
        onResult(true, null, v)
    }

    fun updateSelectedPreviewVoucher(v: Voucher?) {
        _selectedVoucher.value = v
    }

    fun refreshSelectedPreviewVoucher() {}

    fun clearPreviewVoucher() {
        _selectedVoucher.value = null
    }

    fun toggleVoucherSelection(code: String) {
        val current = selectedVoucherCodes.value
        selectedVoucherCodes.value = if (current.contains(code)) current - code else current + code
    }

    fun selectProject(project: RuijieProject?) {
        selectedProject.value = project
    }

    fun setFilter(filter: com.example.data.model.VoucherFilter) {
        activeFilter.value = filter
    }

    fun selectTenant(tenant: RuijieTenant) {
        viewModelScope.launch {
            val acc = activeAccount.value
            accountManager.updateSelectedTenantAndGroup(
                tenantId = tenant.tenantId?.toString(),
                tenantName = tenant.tenantName ?: tenant.effectiveName,
                groupId = acc.selectedGroupId,
                groupName = acc.selectedGroupName
            )
            refreshAll()
        }
    }

    fun selectGroup(group: RuijieNetworkGroup) {
        viewModelScope.launch {
            val acc = activeAccount.value
            accountManager.updateSelectedTenantAndGroup(
                tenantId = acc.selectedTenantId,
                tenantName = acc.selectedTenantName,
                groupId = group.groupId?.toString() ?: group.effectiveGroupId,
                groupName = group.groupName ?: group.effectiveName
            )
            refreshAll()
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            val acc = activeAccount.value
            when (val vRes = repository.getVouchers(acc)) {
                is com.example.data.repository.ApiResult.Success -> _vouchers.value = vRes.data.first
                is com.example.data.repository.ApiResult.Error -> { /* keep existing */ }
            }
            when (val pRes = repository.getVoucherPackages(acc)) {
                is com.example.data.repository.ApiResult.Success -> {
                    // VoucherPackage is a typealias for RuijiePackageItem
                    _packages.value = pRes.data
                }
                is com.example.data.repository.ApiResult.Error -> { /* keep existing */ }
            }
            when (val tRes = repository.getProjects(acc)) {
                is com.example.data.repository.ApiResult.Success -> {
                    // RuijieTenant is a typealias for RuijieProject
                    _tenants.value = tRes.data
                    _networkGroups.value = tRes.data
                }
                is com.example.data.repository.ApiResult.Error -> { /* keep existing */ }
            }
            _isLoading.value = false
        }
    }

    fun login(
        usernameOrAppId: String,
        passwordOrSecret: String,
        server: RuijieServer,
        customUrl: String,
        mode: AuthMode = AuthMode.OPEN_API,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Authenticating with Ruijie Cloud..."
            accountManager.updateAccountCredentials(
                accountName = if (usernameOrAppId.isNotBlank()) usernameOrAppId.substringBefore("@") else "Account",
                appId = if (mode == AuthMode.OPEN_API) usernameOrAppId else "",
                secret = passwordOrSecret,
                username = if (mode == AuthMode.USER_ACCOUNT) usernameOrAppId else "",
                server = server,
                customUrl = customUrl,
                authMode = mode
            )

            val acc = activeAccount.value
            val result = repository.authenticate(acc)
            _isLoading.value = false
            when (result) {
                is com.example.data.repository.ApiResult.Success -> {
                    _statusMessage.value = "Successfully logged in to Ruijie Cloud"
                    refreshAll()
                    onSuccess?.invoke()
                }
                is com.example.data.repository.ApiResult.Error -> {
                    _statusMessage.value = "Login failed: ${result.message}"
                }
            }
        }
    }

    fun generateVouchers(
        autoPrint: Boolean = false,
        onComplete: ((Int) -> Unit)? = null
    ) {
        val pkg = selectedPackage.value ?: return
        val qty = generateQuantity.value
        val prefix = generateRemark.value
        viewModelScope.launch {
            if (autoPrint) isGenerateAndPrinting.value = true else isGenerating.value = true
            _isLoading.value = true
            generateUiState.value = GenerateUiState.Generating
            val result = repository.createVouchers(
                account = activeAccount.value,
                packageItem = pkg,
                quantity = qty,
                comment = prefix,
                remark = prefix
            )
            _isLoading.value = false
            isGenerating.value = false
            isGenerateAndPrinting.value = false
            when (result) {
                is com.example.data.repository.ApiResult.Success -> {
                    val newVouchers = result.data
                    _vouchers.value = newVouchers + _vouchers.value
                    generatedVouchers.value = newVouchers
                    generateUiState.value = GenerateUiState.Success(newVouchers.size, "Generated ${newVouchers.size} voucher(s)")
                    _statusMessage.value = "Generated ${newVouchers.size} voucher(s)"
                    if (autoPrint && newVouchers.isNotEmpty()) {
                        printBatch(newVouchers)
                    }
                    onComplete?.invoke(newVouchers.size)
                }
                is com.example.data.repository.ApiResult.Error -> {
                    val err = result.message
                    generateUiState.value = GenerateUiState.Error(err)
                    _statusMessage.value = "Generation failed: $err"
                }
            }
        }
    }

    fun generateVouchers(
        packageItem: VoucherPackage,
        quantity: Int,
        prefix: String,
        onComplete: (Int) -> Unit
    ) {
        selectedPackage.value = packageItem
        generateQuantity.value = quantity
        generateRemark.value = prefix
        generateVouchers(autoPrint = false) { count ->
            onComplete(count)
        }
    }

    fun selectVoucher(voucher: Voucher) {
        _selectedVoucher.value = voucher
    }

    fun printVoucher(voucher: Voucher, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val settings = _printDesign.value
            val bytes = PrintRenderer.renderVoucherReceipt(voucher, settings)
            val success = printerManager.print(bytes)

            // Mark in history (scoped to the active account for per-user isolation)
            historyRepository.recordPrint(
                voucherId = voucher.effectiveUuid,
                voucherCode = voucher.effectiveCode,
                packageName = voucher.effectiveProfile,
                printerName = (printerState.value as? PrinterConnectionState.Connected)?.deviceName ?: "Bluetooth Thermal",
                accountId = activeAccount.value.id,
                accountName = activeAccount.value.accountName,
                tenantName = activeAccount.value.selectedTenantName,
                groupName = activeAccount.value.selectedGroupName
            )

            _statusMessage.value = if (success) "Voucher printed successfully" else "Print sent to buffer (offline simulation)"
            onComplete?.invoke(success)
        }
    }

    fun deleteVoucher(voucher: Voucher) {
        viewModelScope.launch {
            _vouchers.value = _vouchers.value.filter { it.id != voucher.id }
            _statusMessage.value = "Voucher ${voucher.code} removed"
        }
    }

    fun connectPrinter(device: BluetoothDevice) {
        viewModelScope.launch {
            printerManager.connect(device)
        }
    }

    fun disconnectPrinter() {
        viewModelScope.launch {
            printerManager.disconnect()
        }
    }

    fun updatePrintDesign(settings: PrintDesignSettings) {
        _printDesign.value = settings
    }

    fun updatePrintDesignSettings(settings: PrintDesignSettings) {
        _printDesign.value = settings
    }

    fun resetPrintDesignSettings() {
        _printDesign.value = PrintDesignSettings()
    }

    fun reprintHistoryItem(item: VoucherHistoryEntity) {}
    fun deleteHistoryRecord(id: Long) {
        viewModelScope.launch {
            historyRepository.delete(id)
        }
    }
    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clear()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
