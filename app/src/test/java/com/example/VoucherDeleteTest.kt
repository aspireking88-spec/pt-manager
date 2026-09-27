package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.api.RuijieApiClient
import com.example.data.model.RuijieAccount
import com.example.data.model.RuijieServer
import com.example.data.model.RuijieVoucherItem
import com.example.data.repository.RuijieRepository
import com.example.data.session.AccountManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoucherDeleteTest {

    private val testAccount = RuijieAccount(
        id = "acc-1",
        accountName = "Test Account",
        username = "admin_user",
        tenantName = "test_tenant",
        tenantId = 12345L,
        appId = "test_app_id",
        secret = "test_secret",
        server = RuijieServer.GLOBAL,
        accessToken = "mock_access_token",
        selectedGroupId = 99999L
    )

    @Test
    fun testEffectiveUuidExtraction() {
        val voucherWithUuid = RuijieVoucherItem(
            uuid = "4028818f78e123456789abcdef012345",
            voucherCode = "83749201",
            codeNo = "83749201"
        )
        assertEquals("4028818f78e123456789abcdef012345", voucherWithUuid.effectiveUuid)
        assertEquals("83749201", voucherWithUuid.effectiveCode)

        val voucherWithoutUuid = RuijieVoucherItem(
            id = "id_98765",
            voucherCode = "11223344"
        )
        assertEquals("id_98765", voucherWithoutUuid.effectiveUuid)
        assertEquals("11223344", voucherWithoutUuid.effectiveCode)
    }

    @Test
    fun testSingleVoucherDeletePayload() {
        val voucher = RuijieVoucherItem(
            uuid = "uuid_single_001",
            voucherCode = "VOUCH123",
            codeNo = "VOUCH123"
        )

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val accountManager = AccountManager(context)
        val apiClient = RuijieApiClient(accountManager)
        val repo = RuijieRepository(
            apiClient = apiClient,
            accountManager = accountManager
        )

        val payload = repo.buildDeletePayload(listOf(voucher), 99999L, testAccount)

        assertNotNull(payload)
        assertEquals(99999L, payload.getLong("groupId"))
        assertEquals("test_tenant", payload.getString("tenantName"))

        // Verify UUID fields
        assertEquals("uuid_single_001", payload.getString("uuid"))
        val uuidsArray = payload.getJSONArray("uuids")
        assertEquals(1, uuidsArray.length())
        assertEquals("uuid_single_001", uuidsArray.getString(0))

        val uuidListArray = payload.getJSONArray("uuidList")
        assertEquals(1, uuidListArray.length())
        assertEquals("uuid_single_001", uuidListArray.getString(0))

        // Verify Code fields
        assertEquals("VOUCH123", payload.getString("code"))
        val codesArray = payload.getJSONArray("codes")
        assertEquals(1, codesArray.length())
        assertEquals("VOUCH123", codesArray.getString(0))
    }

    @Test
    fun testBatchVouchersDeletePayload() {
        val vouchers = listOf(
            RuijieVoucherItem(uuid = "uuid_batch_1", voucherCode = "CODE_A"),
            RuijieVoucherItem(uuid = "uuid_batch_2", voucherCode = "CODE_B"),
            RuijieVoucherItem(uuid = "uuid_batch_3", voucherCode = "CODE_C")
        )

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val accountManager = AccountManager(context)
        val apiClient = RuijieApiClient(accountManager)
        val repo = RuijieRepository(
            apiClient = apiClient,
            accountManager = accountManager
        )

        val payload = repo.buildDeletePayload(vouchers, 88888L, testAccount)

        assertEquals(88888L, payload.getLong("groupId"))

        val uuidsArray = payload.getJSONArray("uuids")
        assertEquals(3, uuidsArray.length())
        assertEquals("uuid_batch_1", uuidsArray.getString(0))
        assertEquals("uuid_batch_2", uuidsArray.getString(1))
        assertEquals("uuid_batch_3", uuidsArray.getString(2))

        val codesArray = payload.getJSONArray("codes")
        assertEquals(3, codesArray.length())
        assertEquals("CODE_A", codesArray.getString(0))
        assertEquals("CODE_B", codesArray.getString(1))
        assertEquals("CODE_C", codesArray.getString(2))
    }
}
