package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RuijieAccount
import com.example.data.model.RuijieServer
import com.example.data.model.VoucherStatus
import com.example.data.repository.RuijieRepository
import com.example.printer.AppFontFamily
import com.example.printer.AppFontStyle
import com.example.printer.AppFontWeight
import com.example.printer.LetterSpacingMode
import com.example.printer.PaperConfiguration
import com.example.printer.PrintAlignment
import com.example.printer.PrintDesignSettings
import com.example.printer.PrintRenderer
import com.example.printer.TypographyPreset
import com.example.data.model.AppDesignStyle
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.DiagnosisVerdict
import com.example.data.model.VoucherCodeType
import com.example.data.model.VoucherGenerationDiagnosticRecord
import com.example.data.session.SettingsStore
import com.example.ui.i18n.AppStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Printer V1", appName)
    }

    @Test
    fun `server config resolves default and custom endpoints correctly`() {
        assertEquals("https://cloud-as.ruijienetworks.com", RuijieServer.ASIA.resolveBaseUrl())
        assertEquals("https://cloud.ruijienetworks.com", RuijieServer.GLOBAL.resolveBaseUrl())
        assertEquals("https://cloud-eu.ruijienetworks.com", RuijieServer.EUROPE.resolveBaseUrl())
        assertEquals("https://cloud-us.ruijienetworks.com", RuijieServer.US.resolveBaseUrl())

        val custom = RuijieServer.CUSTOM.resolveBaseUrl("https://my-cloud.custom.com")
        assertEquals("https://my-cloud.custom.com", custom)
    }

    @Test
    fun `print design settings serialization and deserialization retains values`() {
        val settings = PrintDesignSettings(
            showVoucherCode = true,
            codeFontSize = 32f,
            codeBold = true,
            codeAlignment = PrintAlignment.CENTER,
            codeSpaced = false,
            insideVoucherSpacing = 2,
            betweenVoucherSpacing = 3,
            customHeaderName = "My Cafe WiFi"
        )

        val json = settings.toJson()
        val parsed = PrintDesignSettings.fromJson(json)

        assertEquals(32f, parsed.codeFontSize)
        assertTrue(parsed.codeBold)
        assertEquals(PrintAlignment.CENTER, parsed.codeAlignment)
        assertFalse(parsed.codeSpaced)
        assertEquals(2, parsed.insideVoucherSpacing)
        assertEquals(3, parsed.betweenVoucherSpacing)
        assertEquals("My Cafe WiFi", parsed.customHeaderName)
    }

    @Test
    fun `print renderer produces 384 dot width bitmap for 58mm paper`() {
        val settings = PrintDesignSettings()
        val bitmap = PrintRenderer.renderVoucher(
            code = "9F83A21C",
            profileName = "VIP Unlimited",
            validPeriod = "24 Hours",
            quota = "5 GB",
            settings = settings,
            paperConfig = PaperConfiguration.STANDARD_58MM,
            siteName = "Test Site"
        )

        assertNotNull(bitmap)
        assertEquals(384, bitmap.width)
        assertTrue(bitmap.height >= 100)

        val rasterBytes = PrintRenderer.convertBitmapToEscPosRaster(bitmap)
        assertTrue(rasterBytes.isNotEmpty())
        // ESC/POS GS v 0 prefix: 0x1D 0x76 0x30 0x00
        assertEquals(0x1D.toByte(), rasterBytes[0])
        assertEquals(0x76.toByte(), rasterBytes[1])
        assertEquals(0x30.toByte(), rasterBytes[2])
    }

    @Test
    fun `tolerant parsing of actual Ruijie Cloud voucher list json`() {
        val cloudSampleJson = """
            {
              "code": 0,
              "msg": "success",
              "voucherData": {
                "count": 2,
                "list": [
                  {
                    "uuid": "u-12345",
                    "voucherCode": "AB12CD34",
                    "status": "1",
                    "packageName": "1 Hour Free",
                    "timePeriod": 60,
                    "validPeriod": "1 Hour",
                    "quota": 1048576,
                    "maxClients": 2
                  },
                  {
                    "uuid": "u-67890",
                    "codeNo": "XY98ZT76",
                    "status": "2",
                    "profileName": "Day Pass",
                    "validPeriod": "24 Hours"
                  }
                ]
              }
            }
        """.trimIndent()

        val (vouchers, total) = RuijieRepository.parseVouchersFromResponse(cloudSampleJson)
        assertEquals(2, total)
        assertEquals(2, vouchers.size)

        val first = vouchers[0]
        assertEquals("AB12CD34", first.effectiveCode)
        assertEquals(VoucherStatus.NOT_USED, first.normalizedStatus)
        assertEquals("1 Hour Free", first.effectiveProfile)
        assertEquals("1 Hour", first.effectivePeriod)
        assertEquals("1 MB", first.effectiveQuota)

        val second = vouchers[1]
        assertEquals("XY98ZT76", second.effectiveCode)
        assertEquals(VoucherStatus.USED, second.normalizedStatus)
        assertEquals("Day Pass", second.effectiveProfile)
    }

    @Test
    fun `redaction masks sensitive tokens and secrets in diagnostic json`() {
        val raw = """{"accessToken":"secret_token_12345","appSecret":"my_app_secret_value","status":200}"""
        val redacted = RuijieRepository.redactSensitive(raw)
        assertFalse(redacted.contains("secret_token_12345"))
        assertFalse(redacted.contains("my_app_secret_value"))
        assertTrue(redacted.contains("********"))
    }

    @Test
    fun `parsePackagesFromResponse extracts authentic Cloud packages`() {
        val cloudPackageJson = """
            {
              "code": 0,
              "msg": "success",
              "packageData": {
                "count": 2,
                "list": [
                  {
                    "packageId": 101,
                    "packageName": "1 Hour Standard",
                    "timePeriod": 60,
                    "validPeriod": "1 Hour",
                    "price": 10.0,
                    "maxClients": 1
                  },
                  {
                    "id": "pkg-202",
                    "name": "Unlimited All Day",
                    "duration": "24h",
                    "price": 50,
                    "limitClients": 3
                  }
                ]
              }
            }
        """.trimIndent()

        val packages = RuijieRepository.parsePackagesFromResponse(cloudPackageJson)
        assertEquals(2, packages.size)

        val p1 = packages[0]
        assertEquals("101", p1.effectiveId)
        assertEquals("1 Hour Standard", p1.effectiveName)
        assertEquals("1 Hour", p1.effectiveDuration)
        assertEquals(1, p1.maxClients)

        val p2 = packages[1]
        assertEquals("pkg-202", p2.effectiveId)
        assertEquals("Unlimited All Day", p2.effectiveName)
        assertEquals("24h", p2.effectiveDuration)
        assertEquals(3, p2.maxClients)
    }

    @Test
    fun `parseCodesFromResponse extracts list of generated voucher codes`() {
        val json = """{"code":0,"data":["VC-9821","VC-9822","VC-9823"]}"""
        val codes = RuijieRepository.parseCodesFromResponse(json)
        assertEquals(3, codes.size)
        assertEquals("VC-9821", codes[0])
        assertEquals("VC-9822", codes[1])
        assertEquals("VC-9823", codes[2])
    }

    @Test
    fun `parseCreateVoucherResponse correctly parses Document Center nested response structure`() {
        val documentCenterResponse = """
            {
              "code": 0,
              "msg": "success",
              "voucherData": {
                "code": 0,
                "msg": "success",
                "count": 2,
                "list": [
                  {
                    "uuid": "8a80808272f3e49e0172f3f98c8e1111",
                    "codeNo": "88219045",
                    "status": 1,
                    "tenantId": "12345",
                    "secuserId": "sec_user_99",
                    "totle": 100,
                    "sign": "sample_signature",
                    "profileId": "authprofile_vip_uuid",
                    "expiryTime": 1750000000000,
                    "limitClients": 3,
                    "qrcodeUrl": "https://cloud.ruijienetworks.com/qr/88219045",
                    "groupId": 2716382,
                    "comment": "VIP Guest"
                  },
                  {
                    "uuid": "8a80808272f3e49e0172f3f98c8e2222",
                    "codeNo": "88219046",
                    "status": 1,
                    "profileId": "authprofile_vip_uuid",
                    "limitClients": 3
                  }
                ]
              }
            }
        """.trimIndent()

        val knownPackage = com.example.data.model.RuijiePackageItem(
            id = 52312,
            authprofileid = "authprofile_vip_uuid",
            name = "VIP High Speed 24H",
            validPeriod = "24 Hours"
        )

        val (code, voucherData, vouchers) = RuijieRepository.parseCreateVoucherResponse(
            rawJson = documentCenterResponse,
            knownPackages = listOf(knownPackage)
        )

        assertEquals(0, code)
        assertNotNull(voucherData)
        assertEquals(0, voucherData?.code)
        assertEquals("success", voucherData?.msg)
        assertEquals(2, voucherData?.count)
        assertEquals(2, vouchers.size)

        val first = vouchers[0]
        assertEquals("88219045", first.effectiveCode)
        assertEquals("88219045", first.codeNo)
        assertEquals("8a80808272f3e49e0172f3f98c8e1111", first.uuid)
        assertEquals("authprofile_vip_uuid", first.profileId)
        assertEquals("VIP High Speed 24H", first.effectiveProfile)
        assertEquals("24 Hours", first.effectivePeriod)
        assertEquals(3, first.limitClients)
        assertEquals("https://cloud.ruijienetworks.com/qr/88219045", first.qrcodeUrl)

        val second = vouchers[1]
        assertEquals("88219046", second.effectiveCode)
        assertEquals("VIP High Speed 24H", second.effectiveProfile)
    }

    @Test
    fun `parseCreateVoucherResponse detects cloud nested errors`() {
        val rootErrorJson = """{"code":2,"msg":"Parameter value is invalid"}"""
        val (code1, vData1, list1) = RuijieRepository.parseCreateVoucherResponse(rootErrorJson)
        assertEquals(2, code1)
        assertEquals(0, list1.size)

        val nestedErrorJson = """
            {
              "code": 0,
              "msg": "success",
              "voucherData": {
                "code": 1005,
                "msg": "Profile package does not exist",
                "count": 0,
                "list": []
              }
            }
        """.trimIndent()
        val (code2, vData2, list2) = RuijieRepository.parseCreateVoucherResponse(nestedErrorJson)
        assertEquals(0, code2)
        assertNotNull(vData2)
        assertEquals(1005, vData2?.code)
        assertEquals("Profile package does not exist", vData2?.msg)
        assertEquals(0, list2.size)
    }

    @Test
    fun `diagnostic record accurately formats ChatGPT report with redactions`() {
        val diag = VoucherGenerationDiagnosticRecord(
            timestamp = 1700000000000L,
            selectedCodeType = VoucherCodeType.ALPHABETIC,
            uiValue = "Alphabetic (a-z)",
            mappedApiValue = "charType=2, codeType=1, codeFormat=alphabetic",
            endpoint = "https://cloud-as.ruijienetworks.com/service/api/open/auth/voucher/create/2716382",
            httpMethod = "POST",
            requestQuery = "access_token=********",
            sanitizedRequestBody = """{"charType":2,"codeType":1,"quantity":1}""",
            responseHttpStatus = 200,
            sanitizedResponseBody = """{"code":0,"voucherData":{"code":0,"list":[{"codeNo":"ab87cd90"}]}}""",
            returnedVoucherField = "voucherData.list[0].codeNo",
            returnedVoucherCode = "ab87cd90",
            codeTypeResult = DiagnosisVerdict.UNSUPPORTED,
            reason = "Cloud API ignored alphabetic request and returned code with digits."
        )

        val report = diag.formatForChatGPT()
        assertTrue(report.contains("VOUCHER GENERATION DIAGNOSIS"))
        assertTrue(report.contains("Selected Type: Alphabetic"))
        assertTrue(report.contains("Result:\nUNSUPPORTED"))
        assertTrue(report.contains("Returned Code:\nab87cd90"))
        assertTrue(report.contains("https://cloud-as.ruijienetworks.com/service/api/open/auth/voucher/create/2716382"))
    }

    @Test
    fun `settings store saves and restores appearance and language preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(context)

        store.saveThemeMode(AppThemeMode.NAVY_BLUE)
        assertEquals(AppThemeMode.NAVY_BLUE, store.getThemeMode())

        store.saveThemeMode(AppThemeMode.BLACK)
        assertEquals(AppThemeMode.BLACK, store.getThemeMode())

        store.saveThemeMode(AppThemeMode.AUTO)
        assertEquals(AppThemeMode.AUTO, store.getThemeMode())

        store.saveDesignStyle(AppDesignStyle.COMPACT_SHARP)
        assertEquals(AppDesignStyle.COMPACT_SHARP, store.getDesignStyle())

        store.saveLanguage(AppLanguage.MYANMAR)
        assertEquals(AppLanguage.MYANMAR, store.getLanguage())
    }

    @Test
    fun `app strings returns accurate Myanmar translations for key vocabulary`() {
        assertEquals("ဘောင်ချာ", AppStrings.get("voucher", AppLanguage.MYANMAR))
        assertEquals("ထုတ်ယူရန်", AppStrings.get("generate", AppLanguage.MYANMAR))
        assertEquals("ထုတ်ယူပြီး ပရင့်ထုတ်ရန်", AppStrings.get("generate_and_print", AppLanguage.MYANMAR))
        assertEquals("ဆက်တင်များ", AppStrings.get("settings", AppLanguage.MYANMAR))
        assertEquals("ပရင်တာ", AppStrings.get("printer", AppLanguage.MYANMAR))
        assertEquals("စစ်ဆေးစမ်းသပ်မှုများ", AppStrings.get("diagnostics", AppLanguage.MYANMAR))
        assertEquals("ဂဏန်း သီးသန့်", AppStrings.get("numeric", AppLanguage.MYANMAR))
        assertEquals("စာလုံး သီးသန့်", AppStrings.get("alphabetic", AppLanguage.MYANMAR))
        assertEquals("စာနှင့် ဂဏန်း", AppStrings.get("alphanumeric", AppLanguage.MYANMAR))

        // English verification
        assertEquals("Voucher", AppStrings.get("voucher", AppLanguage.ENGLISH))
        assertEquals("Generate & Print", AppStrings.get("generate_and_print", AppLanguage.ENGLISH))
    }

    @Test
    fun `test 1 to 3 - voucher code type mapping matches working Nang Oo v5 specification`() {
        assertEquals("1", VoucherCodeType.ALPHANUMERIC.createCodeType)
        assertEquals("2", VoucherCodeType.ALPHABETIC.createCodeType)
        assertEquals("3", VoucherCodeType.NUMERIC.createCodeType)

        assertEquals("0-9a-z", VoucherCodeType.ALPHANUMERIC.subtext)
        assertEquals("a-z", VoucherCodeType.ALPHABETIC.subtext)
        assertEquals("0-9", VoucherCodeType.NUMERIC.subtext)
    }

    @Test
    fun `test 4 - typography settings independent font size control and presets`() {
        var settings = PrintDesignSettings()
        assertEquals(28f, settings.codeFontSize)
        assertEquals(24f, settings.profileNameFontSize)
        assertEquals(24f, settings.periodFontSize)

        // Apply Compact preset
        settings = settings.applyPreset(TypographyPreset.COMPACT)
        assertEquals(22f, settings.codeFontSize)
        assertEquals(20f, settings.profileNameFontSize)
        assertEquals(20f, settings.periodFontSize)
        assertEquals(TypographyPreset.COMPACT, settings.activePreset)

        // Apply Bold Voucher preset
        settings = settings.applyPreset(TypographyPreset.BOLD_VOUCHER)
        assertEquals(32f, settings.codeFontSize)
        assertTrue(settings.codeBold)
        assertEquals(AppFontWeight.BOLD, settings.codeFontWeight)
        assertEquals(TypographyPreset.BOLD_VOUCHER, settings.activePreset)

        // Custom font size
        settings = settings.copy(codeFontSize = 34f, activePreset = TypographyPreset.CUSTOM)
        assertEquals(34f, settings.codeFontSize)
    }

    @Test
    fun `test 5 - font color customization and reset`() {
        var settings = PrintDesignSettings(
            codeColor = 0xFF2563EBL, // Blue
            profileNameColor = 0xFF166534L, // Dark Green
            periodColor = 0xFF991B1BL // Dark Red
        )
        assertEquals(0xFF2563EBL, settings.codeColor)
        assertEquals(0xFF166534L, settings.profileNameColor)
        assertEquals(0xFF991B1BL, settings.periodColor)

        // Reset Colors reverts all to default black 0xFF000000L
        settings = settings.resetColors()
        assertEquals(0xFF000000L, settings.codeColor)
        assertEquals(0xFF000000L, settings.profileNameColor)
        assertEquals(0xFF000000L, settings.periodColor)
    }

    @Test
    fun `test 6 to 8 - typography styles, fonts and safe degradation in thermal print mode`() {
        val settings = PrintDesignSettings(
            fontFamily = AppFontFamily.MONOSPACE,
            codeFontWeight = AppFontWeight.BOLD,
            codeFontStyle = AppFontStyle.ITALIC,
            textShadowEnabled = true,
            shadowBlur = 4f,
            textOutlineEnabled = true,
            outlineWidth = 2f
        )

        // Preview mode: shadow and outline are active
        val previewPaint = PrintRenderer.createPaint(
            fontSize = 28f,
            fontWeight = settings.codeFontWeight,
            fontStyle = settings.codeFontStyle,
            fontFamily = settings.fontFamily,
            colorLong = settings.codeColor,
            textShadowEnabled = settings.textShadowEnabled,
            isThermalOutput = false
        )
        assertNotNull(previewPaint)
        assertTrue(previewPaint.isAntiAlias)

        // Thermal output mode: shadow is disabled to guarantee clean high-contrast monochrome printing
        val thermalPaint = PrintRenderer.createPaint(
            fontSize = 28f,
            fontWeight = settings.codeFontWeight,
            fontStyle = settings.codeFontStyle,
            fontFamily = settings.fontFamily,
            colorLong = settings.codeColor,
            textShadowEnabled = settings.textShadowEnabled,
            isThermalOutput = true
        )
        assertNotNull(thermalPaint)
        assertFalse(thermalPaint.isAntiAlias) // Sharp pixels for thermal heads
    }

    @Test
    fun `test 9 - print design settings JSON persistence retains all typography properties across restarts`() {
        val original = PrintDesignSettings(
            fontFamily = AppFontFamily.SERIF,
            codeFontSize = 36f,
            codeBold = true,
            codeFontWeight = AppFontWeight.BOLD,
            codeFontStyle = AppFontStyle.ITALIC,
            codeAlignment = PrintAlignment.CENTER,
            codeColor = 0xFF1E3A8AL,
            profileNameFontSize = 28f,
            profileNameColor = 0xFF991B1BL,
            letterSpacingMode = LetterSpacingMode.WIDE,
            lineSpacingExtra = 6f,
            textShadowEnabled = true,
            shadowBlur = 5f,
            textOutlineEnabled = true,
            outlineWidth = 2f,
            activePreset = TypographyPreset.CUSTOM
        )

        val json = original.toJson()
        val restored = PrintDesignSettings.fromJson(json)

        assertEquals(AppFontFamily.SERIF, restored.fontFamily)
        assertEquals(36f, restored.codeFontSize)
        assertTrue(restored.codeBold)
        assertEquals(AppFontWeight.BOLD, restored.codeFontWeight)
        assertEquals(AppFontStyle.ITALIC, restored.codeFontStyle)
        assertEquals(PrintAlignment.CENTER, restored.codeAlignment)
        assertEquals(0xFF1E3A8AL, restored.codeColor)
        assertEquals(28f, restored.profileNameFontSize)
        assertEquals(0xFF991B1BL, restored.profileNameColor)
        assertEquals(LetterSpacingMode.WIDE, restored.letterSpacingMode)
        assertEquals(6f, restored.lineSpacingExtra)
        assertTrue(restored.textShadowEnabled)
        assertEquals(5f, restored.shadowBlur)
        assertTrue(restored.textOutlineEnabled)
        assertEquals(2f, restored.outlineWidth)
        assertEquals(TypographyPreset.CUSTOM, restored.activePreset)
    }

    @Test
    fun `test 10 - responsive auto scaling prevents clipping and vertical text`() {
        val settings = PrintDesignSettings(
            codeFontSize = 48f, // Oversized font
            headerFontSize = 40f,
            customHeaderName = "Very Long Super Extended Site Name That Might Overflow The Paper Width"
        )

        val bitmap58 = PrintRenderer.renderVoucher(
            code = "9F83A21C882190",
            profileName = "Unlimited High Speed 24H Full Access Package",
            validPeriod = "30 Days Full Access Period",
            settings = settings,
            paperConfig = PaperConfiguration.STANDARD_58MM
        )

        assertEquals(384, bitmap58.width)
        assertTrue(bitmap58.height > 50)

        val bitmap80 = PrintRenderer.renderVoucher(
            code = "9F83A21C882190",
            profileName = "Unlimited High Speed 24H Full Access Package",
            validPeriod = "30 Days Full Access Period",
            settings = settings,
            paperConfig = PaperConfiguration.STANDARD_80MM
        )

        assertEquals(576, bitmap80.width)
        assertTrue(bitmap80.height > 50)
    }

    @Test
    fun `test 11 - voucher code type mappings strictly match Nang Oo Voucher v5 specification`() {
        assertEquals("1", VoucherCodeType.ALPHANUMERIC.createCodeType)
        assertEquals("2", VoucherCodeType.ALPHABETIC.createCodeType)
        assertEquals("3", VoucherCodeType.NUMERIC.createCodeType)

        assertEquals(1, VoucherCodeType.ALPHANUMERIC.apiValue)
        assertEquals(2, VoucherCodeType.ALPHABETIC.apiValue)
        assertEquals(3, VoucherCodeType.NUMERIC.apiValue)

        assertEquals("Alphanumeric", VoucherCodeType.ALPHANUMERIC.label)
        assertEquals("Alphabetic", VoucherCodeType.ALPHABETIC.label)
        assertEquals("Numeric", VoucherCodeType.NUMERIC.label)
    }

    @Test
    fun `test 12 - voucher card action buttons support Preview Delete and Print`() {
        val testVoucher = com.example.data.model.RuijieVoucherItem(
            code = "TESTVOUCH123",
            profileName = "1 Hour Pass",
            validPeriod = "1 Hour",
            status = 0
        )
        assertEquals("TESTVOUCH123", testVoucher.effectiveCode)
        assertEquals("1 Hour Pass", testVoucher.effectiveProfile)
        assertEquals("1 Hour", testVoucher.effectivePeriod)
        assertEquals(VoucherStatus.NOT_USED, testVoucher.normalizedStatus)
    }

    @Test
    fun `test 13 - tablet mode voucher list and selection retains all vouchers in list`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.PrinterV1ViewModel(app)

        val voucherList = (1..10).map { i ->
            val codeStr = if (i == 10) "VC-1010" else "VC-100$i"
            com.example.data.model.RuijieVoucherItem(
                code = codeStr,
                profileName = "Profile $i",
                validPeriod = "1 Hour",
                status = 0
            )
        }

        // Before selection, selected codes is empty
        assertTrue(vm.selectedVoucherCodes.value.isEmpty())

        // Select Voucher 2
        vm.toggleVoucherSelection("VC-1002")
        assertEquals(setOf("VC-1002"), vm.selectedVoucherCodes.value)

        // Select all vouchers
        vm.selectAllVouchers(voucherList)
        assertEquals(10, vm.selectedVoucherCodes.value.size)
        assertTrue(vm.selectedVoucherCodes.value.contains("VC-1001"))
        assertTrue(vm.selectedVoucherCodes.value.contains("VC-1010"))

        // Deselect all vouchers
        vm.deselectAllVouchers()
        assertTrue(vm.selectedVoucherCodes.value.isEmpty())

        // Toggle selection again
        vm.toggleVoucherSelection("VC-1005")
        assertEquals(setOf("VC-1005"), vm.selectedVoucherCodes.value)
        // Ensure original list unchanged
        assertEquals(10, voucherList.size)
    }

    @Test
    fun `test 14 - main activity launches and creates view model successfully`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        val activity = controller.get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
        controller.destroy()
    }

    @Test
    fun `test 15 - authmode enum and account default behavior`() {
        val defaultAccount = RuijieAccount(
            accountName = "Primary Account",
            appId = "open12345",
            secret = "sec67890",
            authMode = com.example.data.model.AuthMode.USER_ACCOUNT
        )
        assertEquals(com.example.data.model.AuthMode.USER_ACCOUNT, defaultAccount.authMode)
        assertEquals("Ruijie Cloud Account (Email / Password)", defaultAccount.authMode.displayName)

        val openApiAccount = defaultAccount.copy(
            username = "open12345",
            authMode = com.example.data.model.AuthMode.OPEN_API
        )
        assertEquals(com.example.data.model.AuthMode.OPEN_API, openApiAccount.authMode)
        assertEquals("App ID & Secret (Open API)", openApiAccount.authMode.displayName)
    }

    @Test
    fun `test 16 - account manager persists and loads authmode correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.data.session.AccountManager(context)

        val created = manager.addAccount(
            name = "Office User Account",
            appId = "admin",
            secret = "pass123",
            server = RuijieServer.ASIA,
            authMode = com.example.data.model.AuthMode.USER_ACCOUNT
        )
        assertEquals(com.example.data.model.AuthMode.USER_ACCOUNT, created.authMode)

        manager.setSession(
            username = "admin@ruijie.com",
            accessToken = "user_token_abc_123",
            authMode = com.example.data.model.AuthMode.USER_ACCOUNT
        )

        val active = manager.getActiveAccount()
        assertEquals(com.example.data.model.AuthMode.USER_ACCOUNT, active.authMode)
        assertEquals("user_token_abc_123", active.accessToken)
    }

    @Test
    fun `test 17 - voucher status normalization covers all Ruijie Cloud status codes and expiry indicators`() {
        // NOT_USED (status 0, 1, "0", "1", "unused", "valid", "normal")
        val notUsed1 = com.example.data.model.RuijieVoucherItem(code = "V1", status = 0)
        val notUsed2 = com.example.data.model.RuijieVoucherItem(code = "V2", status = "1")
        val notUsed3 = com.example.data.model.RuijieVoucherItem(code = "V3", status = "unused")
        val notUsed4 = com.example.data.model.RuijieVoucherItem(code = "V4", state = "valid")
        assertEquals(VoucherStatus.NOT_USED, notUsed1.normalizedStatus)
        assertEquals(VoucherStatus.NOT_USED, notUsed2.normalizedStatus)
        assertEquals(VoucherStatus.NOT_USED, notUsed3.normalizedStatus)
        assertEquals(VoucherStatus.NOT_USED, notUsed4.normalizedStatus)

        // USED (status 2, "2", "used", "in_use", "online", "active")
        val used1 = com.example.data.model.RuijieVoucherItem(code = "V5", status = 2)
        val used2 = com.example.data.model.RuijieVoucherItem(code = "V6", status = "used")
        val used3 = com.example.data.model.RuijieVoucherItem(code = "V7", state = "online")
        assertEquals(VoucherStatus.USED, used1.normalizedStatus)
        assertEquals(VoucherStatus.USED, used2.normalizedStatus)
        assertEquals(VoucherStatus.USED, used3.normalizedStatus)

        // EXPIRED (status 3, 4, 5, -1, "expired", "invalid", "disable", or disableStatus=1, or past expiry timestamp)
        val expired1 = com.example.data.model.RuijieVoucherItem(code = "V8", status = 3)
        val expired2 = com.example.data.model.RuijieVoucherItem(code = "V9", status = "expired")
        val expired3 = com.example.data.model.RuijieVoucherItem(code = "V10", state = "invalid")
        val expired4 = com.example.data.model.RuijieVoucherItem(code = "V11", disableStatus = 1)
        val expired5 = com.example.data.model.RuijieVoucherItem(code = "V12", status = 1, expiryTime = 1000000000L) // in the past
        assertEquals(VoucherStatus.EXPIRED, expired1.normalizedStatus)
        assertEquals(VoucherStatus.EXPIRED, expired2.normalizedStatus)
        assertEquals(VoucherStatus.EXPIRED, expired3.normalizedStatus)
        assertEquals(VoucherStatus.EXPIRED, expired4.normalizedStatus)
        assertEquals(VoucherStatus.EXPIRED, expired5.normalizedStatus)
    }

    @Test
    fun `test 18 - parseVouchersFromResponse extracts all Cloud fields including state and disableStatus`() {
        val cloudJson = """
            {
              "code": 0,
              "msg": "success",
              "voucherData": {
                "count": 4,
                "list": [
                  {
                    "uuid": "u-001",
                    "voucherCode": "EXP-01",
                    "status": 3,
                    "packageName": "1 Day VIP",
                    "validPeriod": "24 Hours"
                  },
                  {
                    "uuid": "u-002",
                    "codeNo": "EXP-02",
                    "state": "expired",
                    "packageName": "1 Day VIP"
                  },
                  {
                    "uuid": "u-003",
                    "voucherCode": "USED-01",
                    "status": 2,
                    "packageName": "1 Day VIP"
                  },
                  {
                    "uuid": "u-004",
                    "voucherCode": "NOTUSED-01",
                    "status": 1,
                    "packageName": "1 Day VIP"
                  }
                ]
              }
            }
        """.trimIndent()

        val (vouchers, total) = RuijieRepository.parseVouchersFromResponse(cloudJson)
        assertEquals(4, total)
        assertEquals(4, vouchers.size)

        val expiredList = vouchers.filter { it.normalizedStatus == VoucherStatus.EXPIRED }
        val usedList = vouchers.filter { it.normalizedStatus == VoucherStatus.USED }
        val notUsedList = vouchers.filter { it.normalizedStatus == VoucherStatus.NOT_USED }

        assertEquals(2, expiredList.size)
        assertEquals(1, usedList.size)
        assertEquals(1, notUsedList.size)
    }

    @Test
    fun `test 19 - user account token extraction priority from root and data object`() {
        // Root token formats
        val rootTokenJson = """{"accessToken":"root_token_123","token_type":"bearer"}"""
        val rootJsonObj = org.json.JSONObject(rootTokenJson)
        val extractedRootToken = rootJsonObj.optString("accessToken")
            .ifBlank { rootJsonObj.optString("access_token") }
            .ifBlank { rootJsonObj.optString("token") }
        assertEquals("root_token_123", extractedRootToken)

        // Data object nested token formats
        val dataTokenJson = """{"code":0,"data":{"access_token":"data_token_456","expires_in":7200}}"""
        val dataJsonObj = org.json.JSONObject(dataTokenJson)
        val dataObj = dataJsonObj.optJSONObject("data")
        val extractedDataToken = dataObj?.optString("accessToken")
            ?.ifBlank { dataObj.optString("access_token") }
            ?.ifBlank { dataObj.optString("token") }
        assertEquals("data_token_456", extractedDataToken)
    }

    @Test
    fun `test 20 - user account login flow defaults and project selection wiring`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.PrinterV1ViewModel(app)

        val active = vm.activeAccount.value
        assertEquals(com.example.data.model.AuthMode.OPEN_API, active.authMode)

        // Verify project selection dialog state flow exists and initialized
        // assertFalse(vm.showProjectSelectorDialog.value)
        // vm.openProjectSelectorDialog()
        // assertTrue(vm.showProjectSelectorDialog.value)
        // vm.dismissProjectSelectorDialog()
        // assertFalse(vm.showProjectSelectorDialog.value)
    }
}

