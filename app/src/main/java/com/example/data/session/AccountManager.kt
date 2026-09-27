package com.example.data.session

import android.content.Context
import com.example.data.model.AuthMode
import com.example.data.model.RuijieAccount
import com.example.data.model.RuijieServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

typealias DeviceLayoutMode = com.example.data.model.DeviceLayoutMode

class AccountManager(context: Context) {
    private val prefs = context.getSharedPreferences("ruijie_accounts_prefs", Context.MODE_PRIVATE)

    private val _accounts = MutableStateFlow<List<RuijieAccount>>(emptyList())
    val accounts: StateFlow<List<RuijieAccount>> = _accounts.asStateFlow()

    private val _activeAccount = MutableStateFlow(RuijieAccount())
    val activeAccount: StateFlow<RuijieAccount> = _activeAccount.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        val jsonStr = prefs.getString("accounts_list", null)
        val activeId = prefs.getString("active_account_id", null)

        val list = mutableListOf<RuijieAccount>()
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val authModeName = obj.optString("authMode", AuthMode.OPEN_API.name)
                    val authMode = try { AuthMode.valueOf(authModeName) } catch (_: Exception) { AuthMode.OPEN_API }
                    val serverName = obj.optString("server", RuijieServer.GLOBAL.name)
                    val server = try { RuijieServer.valueOf(serverName) } catch (_: Exception) { RuijieServer.GLOBAL }

                    list.add(
                        RuijieAccount(
                            id = obj.getString("id"),
                            accountName = obj.optString("accountName", "Account ${i + 1}"),
                            appId = obj.optString("appId", ""),
                            secret = obj.optString("secret", ""),
                            username = obj.optString("username", ""),
                            server = server,
                            customUrl = obj.optString("customUrl", ""),
                            authMode = authMode,
                            token = if (obj.has("token") && !obj.isNull("token")) obj.getString("token") else null,
                            accessToken = if (obj.has("accessToken") && !obj.isNull("accessToken")) obj.getString("accessToken") else null,
                            refreshToken = if (obj.has("refreshToken") && !obj.isNull("refreshToken")) obj.getString("refreshToken") else null,
                            tokenExpiresAt = obj.optLong("tokenExpiresAt", 0L),
                            userRole = if (obj.has("userRole") && !obj.isNull("userRole")) obj.getString("userRole") else null,
                            nickname = if (obj.has("nickname") && !obj.isNull("nickname")) obj.getString("nickname") else null,
                            defaultTenant = if (obj.has("defaultTenant") && !obj.isNull("defaultTenant")) obj.getString("defaultTenant") else null,
                            selectedTenantId = if (obj.has("selectedTenantId") && !obj.isNull("selectedTenantId")) obj.getString("selectedTenantId") else null,
                            selectedTenantName = if (obj.has("selectedTenantName") && !obj.isNull("selectedTenantName")) obj.getString("selectedTenantName") else null,
                            selectedGroupId = if (obj.has("selectedGroupId") && !obj.isNull("selectedGroupId")) obj.getString("selectedGroupId") else null,
                            selectedGroupName = if (obj.has("selectedGroupName") && !obj.isNull("selectedGroupName")) obj.getString("selectedGroupName") else null
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        if (list.isEmpty()) {
            val defaultAcc = RuijieAccount()
            list.add(defaultAcc)
        }

        _accounts.value = list
        val current = list.firstOrNull { it.id == activeId } ?: list.first()
        _activeAccount.value = current
    }

    private fun persistAccounts(list: List<RuijieAccount>, activeId: String) {
        val array = JSONArray()
        for (acc in list) {
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("accountName", acc.accountName)
                put("appId", acc.appId)
                put("secret", acc.secret)
                put("username", acc.username)
                put("server", acc.server.name)
                put("customUrl", acc.customUrl)
                put("authMode", acc.authMode.name)
                put("token", acc.token)
                put("accessToken", acc.accessToken)
                put("refreshToken", acc.refreshToken)
                put("tokenExpiresAt", acc.tokenExpiresAt)
                put("userRole", acc.userRole)
                put("nickname", acc.nickname)
                put("defaultTenant", acc.defaultTenant)
                put("selectedTenantId", acc.selectedTenantId)
                put("selectedTenantName", acc.selectedTenantName)
                put("selectedGroupId", acc.selectedGroupId)
                put("selectedGroupName", acc.selectedGroupName)
            }
            array.put(obj)
        }
        prefs.edit()
            .putString("accounts_list", array.toString())
            .putString("active_account_id", activeId)
            .apply()
    }

    fun saveAccount(account: RuijieAccount, makeActive: Boolean = true) {
        val currentList = _accounts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == account.id }
        if (index >= 0) {
            currentList[index] = account
        } else {
            currentList.add(account)
        }
        val activeId = if (makeActive) account.id else _activeAccount.value.id
        _accounts.value = currentList
        if (makeActive) {
            _activeAccount.value = account
        }
        persistAccounts(currentList, activeId)
    }

    fun selectAccount(id: String) {
        val acc = _accounts.value.firstOrNull { it.id == id } ?: return
        _activeAccount.value = acc
        persistAccounts(_accounts.value, id)
    }

    fun updateActiveAccountToken(token: String?, expiresAt: Long = 0L) {
        val current = _activeAccount.value
        val updated = current.copy(token = token, tokenExpiresAt = expiresAt)
        saveAccount(updated, makeActive = true)
    }

    /** Update tokens on the active account after an OAuth refresh. */
    fun updateActiveToken(accessToken: String, refreshToken: String, expiresInSeconds: Long) {
        val current = _activeAccount.value
        val updated = current.copy(
            token = accessToken,
            accessToken = accessToken,
            refreshToken = refreshToken.ifBlank { current.refreshToken },
            tokenExpiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
        )
        saveAccount(updated, makeActive = true)
    }

    fun updateSelectedTenantAndGroup(
        tenantId: String?,
        tenantName: String?,
        groupId: String?,
        groupName: String?
    ) {
        val current = _activeAccount.value
        val updated = current.copy(
            selectedTenantId = tenantId,
            selectedTenantName = tenantName,
            selectedGroupId = groupId,
            selectedGroupName = groupName
        )
        saveAccount(updated, makeActive = true)
    }

    /** Update the active project/group selection on the active account. */
    fun updateActiveProject(groupId: Long, groupName: String?, tenantName: String?) {
        val current = _activeAccount.value
        val updated = current.copy(
            selectedGroupId = groupId.takeIf { it != 0L }?.toString() ?: current.selectedGroupId,
            selectedGroupName = groupName?.ifBlank { null } ?: current.selectedGroupName,
            selectedTenantName = tenantName?.ifBlank { null } ?: current.selectedTenantName
        )
        saveAccount(updated, makeActive = true)
    }

    fun updateAccountCredentials(
        accountName: String,
        appId: String,
        secret: String,
        username: String,
        server: RuijieServer,
        customUrl: String,
        authMode: AuthMode = AuthMode.OPEN_API
    ) {
        val current = _activeAccount.value
        val updated = current.copy(
            accountName = accountName,
            appId = appId,
            secret = secret,
            username = username,
            server = server,
            customUrl = customUrl,
            authMode = authMode
        )
        saveAccount(updated, makeActive = true)
    }

    fun deleteAccount(id: String) {
        val currentList = _accounts.value.toMutableList()
        currentList.removeAll { it.id == id }
        if (currentList.isEmpty()) {
            currentList.add(RuijieAccount())
        }
        val nextActive = currentList.first()
        _accounts.value = currentList
        _activeAccount.value = nextActive
        persistAccounts(currentList, nextActive.id)
    }

    fun setActiveAccount(acc: RuijieAccount) {
        saveAccount(acc, true)
    }

    /** Synchronous access to the currently active account. */
    fun getActiveAccount(): RuijieAccount = _activeAccount.value

    /**
     * Atomically stores a fresh login session on the active account.
     * Per-user isolation: every account keeps its own tokens/session, so up to
     * ~100 independent users can each manage their own Ruijie account/project.
     */
    fun setSession(
        username: String,
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        email: String,
        tenantName: String,
        tenantId: Long,
        userRole: String,
        nickname: String,
        defaultTenant: String? = null,
        server: RuijieServer,
        customUrl: String,
        authMode: AuthMode
    ) {
        val current = _activeAccount.value
        val updated = current.copy(
            username = username,
            email = email.ifBlank { current.email },
            token = accessToken,
            accessToken = accessToken,
            refreshToken = refreshToken.ifBlank { current.refreshToken },
            tokenExpiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L,
            tenantName = tenantName.ifBlank { current.tenantName },
            selectedTenantId = tenantId.takeIf { it != 0L }?.toString() ?: current.selectedTenantId,
            userRole = userRole.ifBlank { current.userRole },
            nickname = nickname.ifBlank { current.nickname },
            defaultTenant = defaultTenant ?: current.defaultTenant,
            server = server,
            customUrl = customUrl,
            authMode = authMode
        )
        saveAccount(updated, makeActive = true)
    }

    fun addAccount(acc: RuijieAccount) {
        saveAccount(acc, true)
    }

    fun updateAccount(acc: RuijieAccount) {
        saveAccount(acc, true)
    }

    fun removeAccount(id: String) {
        deleteAccount(id)
    }
}
