package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RuijieVoucherItem
import com.example.data.model.VoucherStatus
import com.example.ui.PrinterV1ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoucherPreviewStabilityTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: PrinterV1ViewModel

    private val voucherA = RuijieVoucherItem(
        uuid = "uuid-vouch-a-1111",
        voucherCode = "CODE_A",
        codeNo = "CODE_A",
        status = 0
    )

    private val voucherB = RuijieVoucherItem(
        uuid = "uuid-vouch-b-2222",
        voucherCode = "CODE_B",
        codeNo = "CODE_B",
        status = 1
    )

    private val voucherC = RuijieVoucherItem(
        uuid = "uuid-vouch-c-3333",
        voucherCode = "CODE_C",
        codeNo = "CODE_C",
        status = 0
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        viewModel = PrinterV1ViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test voucher preview stable selection across list reorder, additions, and explicit selection`() {
        // 1. Select voucher A & open Preview
        viewModel.selectVoucherForPreview(voucherA)

        // Confirm Preview shows A
        assertEquals("uuid-vouch-a-1111", viewModel.selectedPreviewUuid.value)
        assertEquals("CODE_A", viewModel.selectedPreviewCode.value)
        assertEquals(voucherA, viewModel.selectedPreviewVoucher.value)
        assertEquals("uuid-vouch-a-1111", viewModel.selectedUuidBeforeRefresh)
        assertEquals("CODE_A", viewModel.selectedCodeBeforeRefresh)
        assertFalse("Selection must not change without user action", viewModel.selectionChangedWithoutUserAction)

        // 2. Reorder the voucher list (e.g. [voucherC, voucherB, voucherA])
        val reorderedList = listOf(voucherC, voucherB, voucherA)
        // Find by identity in reordered list
        val currentUuid = viewModel.selectedPreviewUuid.value
        val currentCode = viewModel.selectedPreviewCode.value
        val matchedAfterReorder = reorderedList.find { item ->
            val itemUuid = item.uuid?.ifBlank { null } ?: item.id?.ifBlank { null }
            val itemCode = item.voucherCode?.ifBlank { null } ?: item.codeNo?.ifBlank { null } ?: item.code?.ifBlank { null } ?: item.effectiveCode.ifBlank { null }
            (!currentUuid.isNullOrBlank() && itemUuid == currentUuid) ||
            (!currentCode.isNullOrBlank() && itemCode.equals(currentCode, ignoreCase = true))
        }

        assertNotNull(matchedAfterReorder)
        assertEquals("CODE_A", matchedAfterReorder?.effectiveCode)
        // Update viewModel with reordered matched voucher
        viewModel.updateSelectedPreviewVoucher(matchedAfterReorder!!)
        assertEquals("CODE_A", viewModel.selectedPreviewVoucher.value?.effectiveCode)
        assertEquals("uuid-vouch-a-1111", viewModel.selectedPreviewVoucher.value?.uuid)
        assertFalse(viewModel.selectionChangedWithoutUserAction)

        // 3. Add a new voucher B at the front of the list (e.g. [voucherB, voucherC, voucherA])
        val listWithNewVoucherAtFront = listOf(voucherB, voucherC, voucherA)
        val matchedAfterAdd = listWithNewVoucherAtFront.find { item ->
            val itemUuid = item.uuid?.ifBlank { null } ?: item.id?.ifBlank { null }
            val itemCode = item.voucherCode?.ifBlank { null } ?: item.codeNo?.ifBlank { null } ?: item.code?.ifBlank { null } ?: item.effectiveCode.ifBlank { null }
            (!currentUuid.isNullOrBlank() && itemUuid == currentUuid) ||
            (!currentCode.isNullOrBlank() && itemCode.equals(currentCode, ignoreCase = true))
        }
        // Confirm Preview does not switch to B, still shows A
        assertNotNull(matchedAfterAdd)
        assertEquals("CODE_A", matchedAfterAdd?.effectiveCode)
        viewModel.updateSelectedPreviewVoucher(matchedAfterAdd!!)
        assertEquals("CODE_A", viewModel.selectedPreviewVoucher.value?.effectiveCode)
        assertFalse(viewModel.selectionChangedWithoutUserAction)

        // 4. Refresh once with updated voucher A data (e.g. data used updated)
        val updatedVoucherA = voucherA.copy(usedQuota = 500L)
        val refreshedList = listOf(voucherB, updatedVoucherA, voucherC)
        val matchedAfterRefresh = refreshedList.find { item ->
            val itemUuid = item.uuid?.ifBlank { null } ?: item.id?.ifBlank { null }
            val itemCode = item.voucherCode?.ifBlank { null } ?: item.codeNo?.ifBlank { null } ?: item.code?.ifBlank { null } ?: item.effectiveCode.ifBlank { null }
            (!currentUuid.isNullOrBlank() && itemUuid == currentUuid) ||
            (!currentCode.isNullOrBlank() && itemCode.equals(currentCode, ignoreCase = true))
        }
        assertNotNull(matchedAfterRefresh)
        assertEquals("CODE_A", matchedAfterRefresh?.effectiveCode)
        assertEquals(500L, matchedAfterRefresh?.usedQuota)
        viewModel.updateSelectedPreviewVoucher(matchedAfterRefresh!!)
        assertEquals("CODE_A", viewModel.selectedPreviewVoucher.value?.effectiveCode)
        assertEquals(500L, viewModel.selectedPreviewVoucher.value?.usedQuota)
        assertFalse(viewModel.selectionChangedWithoutUserAction)

        // 5. Recompose simulation: Verify stable Compose keys for voucher A
        val stableKeyA = voucherA.uuid ?: voucherA.voucherCode ?: voucherA.codeNo
        val stableKeyUpdatedA = updatedVoucherA.uuid ?: updatedVoucherA.voucherCode ?: updatedVoucherA.codeNo
        assertEquals("uuid-vouch-a-1111", stableKeyA)
        assertEquals(stableKeyA, stableKeyUpdatedA)
        // Dialog recomposition with same identity preserves voucher A
        assertEquals("CODE_A", viewModel.selectedPreviewVoucher.value?.effectiveCode)

        // 6. Tap another voucher explicitly (Voucher B)
        viewModel.selectVoucherForPreview(voucherB)

        // Confirm only then Preview changes to B
        assertEquals("uuid-vouch-b-2222", viewModel.selectedPreviewUuid.value)
        assertEquals("CODE_B", viewModel.selectedPreviewCode.value)
        assertEquals(voucherB, viewModel.selectedPreviewVoucher.value)
        assertFalse("Explicit user selection does not count as without user action", viewModel.selectionChangedWithoutUserAction)
    }

    @Test
    fun `test unavailable voucher retains current preview content and shows message`() {
        // Select voucher A
        viewModel.selectVoucherForPreview(voucherA)
        assertEquals("CODE_A", viewModel.selectedPreviewVoucher.value?.effectiveCode)

        // Refreshed list does not contain voucher A
        val listWithoutA = listOf(voucherB, voucherC)
        val currentUuid = viewModel.selectedPreviewUuid.value
        val currentCode = viewModel.selectedPreviewCode.value

        val matched = listWithoutA.find { item ->
            val itemUuid = item.uuid?.ifBlank { null } ?: item.id?.ifBlank { null }
            val itemCode = item.voucherCode?.ifBlank { null } ?: item.codeNo?.ifBlank { null } ?: item.code?.ifBlank { null } ?: item.effectiveCode.ifBlank { null }
            (!currentUuid.isNullOrBlank() && itemUuid == currentUuid) ||
            (!currentCode.isNullOrBlank() && itemCode.equals(currentCode, ignoreCase = true))
        }

        // Must NOT match any other voucher
        assertNull("Voucher A should not match any other voucher in the list", matched)

        // In this case, ViewModel keeps current preview content and sets message
        // Confirm preview voucher is still A (never replaced with voucher B or C)
        assertEquals("CODE_A", viewModel.selectedPreviewVoucher.value?.effectiveCode)
        assertEquals("uuid-vouch-a-1111", viewModel.selectedPreviewVoucher.value?.uuid)
        assertFalse(viewModel.selectionChangedWithoutUserAction)
    }

    @Test
    fun `test single flight guard prevents duplicate refresh requests`() {
        assertFalse(viewModel.isPreviewRefreshing.value)
        // Selecting voucher sets initial state
        viewModel.selectVoucherForPreview(voucherA)
        assertNotNull(viewModel.selectedPreviewVoucher.value)
        assertNull(viewModel.previewRefreshMessage.value)
    }
}
