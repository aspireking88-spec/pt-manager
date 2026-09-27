package com.example.data.api

import com.example.data.model.RuijieCreateVoucherRequest
import com.example.data.model.RuijieTokenRequestBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface RuijieApiService {

    /**
     * Ruijie Cloud Stage 1 Auth Policy API:
     * POST /service/api/intl/auth/v2/policy/
     */
    @POST("service/api/intl/auth/v2/policy/")
    suspend fun authPolicy(
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Ruijie Cloud International Tenant Auth API:
     * POST /service/api/intl/tenant/auth/
     */
    @POST("service/api/intl/tenant/auth/")
    suspend fun authTenant(
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Ruijie Cloud Stage 2 Global Login API:
     * POST /service/api/intl/auth/v2/global/
     */
    @POST("service/api/intl/auth/v2/global/")
    suspend fun authGlobal(
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Ruijie Cloud User Info:
     * GET /service/api/user/info/
     */
    @GET("service/api/user/info/")
    suspend fun getUserInfo(
        @retrofit2.http.Header("Authorization") authorization: String? = null
    ): Response<ResponseBody>

    /**
     * Ruijie Cloud User Account Info (documented form):
     * GET /service/api/org/account/info?access_token={token}
     * Returns documented fields: account, userName, role, defaultTenant.
     */
    @GET("service/api/org/account/info")
    suspend fun getAccountInfoByToken(
        @Query("access_token") accessToken: String
    ): Response<ResponseBody>

    /**
     * Ruijie Cloud User Account Info (legacy header form, kept for fallback):
     * GET /service/api/org/account/info
     */
    @GET("service/api/org/account/info")
    suspend fun getAccountInfo(
        @retrofit2.http.Header("Authorization") authorization: String? = null
    ): Response<ResponseBody>

    /**
     * Ruijie Cloud User Account Login (POST JSON):
     * POST /service/api/login
     */
    @POST("service/api/login")
    suspend fun userLogin(
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Ruijie Cloud User Account Login (GET query parameters):
     * GET /service/api/login?account={account}&password={password}
     */
    @GET("service/api/login")
    suspend fun userLoginGet(
        @Query("account") account: String,
        @Query("password") password: String
    ): Response<ResponseBody>

    /**
     * Ruijie Cloud Auth Login endpoint:
     * POST /service/api/auth/login
     */
    @POST("service/api/auth/login")
    suspend fun authLogin(
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Ruijie Cloud OAuth2.0 Client Access Token endpoint (Section 2.1.1 "Get an Access Token"):
     * POST https://{cloudserver}/service/api/oauth20/client/access_token?token=d63dss0a81e4415a889ac5b78fsc904a
     * Fixed constant token query parameter: d63dss0a81e4415a889ac5b78fsc904a
     * Header: Content-Type: application/json
     * Body: {"appid": "...", "secret": "..."}
     */
    @retrofit2.http.Headers("Content-Type: application/json")
    @POST("service/api/oauth20/client/access_token?token=d63dss0a81e4415a889ac5b78fsc904a")
    suspend fun getClientAccessToken(
        @Body body: RuijieTokenRequestBody
    ): Response<ResponseBody>

    /**
     * Ruijie Cloud Token Refresh endpoint:
     * GET /service/api/token/refresh?appid={appid}&secret={secret}&access_token={accessToken}
     */
    @GET("service/api/token/refresh")
    suspend fun refreshToken(
        @Query("appid") appid: String,
        @Query("secret") secret: String,
        @Query("access_token") accessToken: String
    ): Response<ResponseBody>

    /**
     * Documented Project/Network Group Tree API
     * GET /service/api/group/single/tree?depth=BUILDING&access_token=ACCESS_TOKEN
     */
    @GET("service/api/group/single/tree")
    suspend fun getGroupSingleTree(
        @Query("depth") depth: String = "BUILDING",
        @Query("access_token") accessToken: String
    ): Response<ResponseBody>

    /**
     * Primary Voucher List API from Ruijie Cloud Document Center:
     * GET /service/api/open/auth/voucher/getList/{groupId}?access_token={}&start={}&pageSize={}
     */
    @GET("service/api/open/auth/voucher/getList/{groupId}")
    suspend fun getVoucherListOpen(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Query("start") start: Int = 0,
        @Query("pageSize") pageSize: Int = 200
    ): Response<ResponseBody>

    /**
     * Regional Tenant Voucher List API:
     * GET /service/api/intlSamVoucher/getList/{tenantName}/{groupId}?access_token={}&start={}&pageSize={}
     */
    @GET("service/api/intlSamVoucher/getList/{tenantName}/{groupId}")
    suspend fun getVoucherListTenant(
        @Path("tenantName") tenantName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Query("start") start: Int = 0,
        @Query("pageSize") pageSize: Int = 200,
        @Query("tenantId") tenantId: String? = null
    ): Response<ResponseBody>

    /**
     * Retrieve Cloud Voucher Packages (Primary Open API):
     * GET /service/api/open/auth/package/getList/{groupId}?access_token={}
     */
    @GET("service/api/open/auth/package/getList/{groupId}")
    suspend fun getPackageListOpen(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String
    ): Response<ResponseBody>

    /**
     * Retrieve Cloud Voucher Packages (Alternative Open API):
     * GET /service/api/open/auth/package/list/{groupId}?access_token={}
     */
    @GET("service/api/open/auth/package/list/{groupId}")
    suspend fun getPackageListOpenAlt(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String
    ): Response<ResponseBody>

    /**
     * Retrieve Cloud Voucher Packages (Tenant API):
     * GET /service/api/intlSamVoucher/getPackageList/{tenantName}/{groupId}?access_token={}
     */
    @GET("service/api/intlSamVoucher/getPackageList/{tenantName}/{groupId}")
    suspend fun getPackageListTenant(
        @Path("tenantName") tenantName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String
    ): Response<ResponseBody>

    /**
     * User Group List API:
     * GET /service/api/intl/usergroup/list/{groupId}?access_token={}&pageIndex=1&pageSize=100
     */
    @GET("service/api/intl/usergroup/list/{groupId}")
    suspend fun getUserGroupListIntl(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Query("pageIndex") pageIndex: Int = 1,
        @Query("pageSize") pageSize: Int = 100
    ): Response<ResponseBody>

    /**
     * Generate / Create Vouchers matching official Ruijie Web UI (confirmed by working reference):
     * POST /service/api/intlSamVoucher/create/{tenantName}/{userName}/{groupId}?access_token={}&group_id={}&lang=en
     */
    @POST("service/api/intlSamVoucher/create/{tenantName}/{userName}/{groupId}")
    suspend fun createVoucherWeb(
        @Path("tenantName") tenantName: String,
        @Path("userName") userName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Query("group_id") queryGroupId: Long,
        @Query("lang") lang: String = "en",
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Generate / Create Vouchers (Primary Open API):
     * POST /service/api/open/auth/voucher/create/{groupId}?access_token={}
     */
    @POST("service/api/open/auth/voucher/create/{groupId}")
    suspend fun createVoucherOpen(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Generate / Create Vouchers (Tenant API):
     * POST /service/api/intlSamVoucher/create/{tenantName}/{groupId}?access_token={}
     */
    @POST("service/api/intlSamVoucher/create/{tenantName}/{groupId}")
    suspend fun createVoucherTenant(
        @Path("tenantName") tenantName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Delete Vouchers (Primary Open API del):
     * POST /service/api/open/auth/voucher/del/{groupId}?access_token={}
     */
    @POST("service/api/open/auth/voucher/del/{groupId}")
    suspend fun deleteVoucherOpenDel(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Delete Vouchers (Alternative Open API delete):
     * POST /service/api/open/auth/voucher/delete/{groupId}?access_token={}
     */
    @POST("service/api/open/auth/voucher/delete/{groupId}")
    suspend fun deleteVoucherOpen(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Batch Delete Vouchers (Open API batchDel):
     * POST /service/api/open/auth/voucher/batchDel/{groupId}?access_token={}
     */
    @POST("service/api/open/auth/voucher/batchDel/{groupId}")
    suspend fun batchDelVoucherOpen(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Batch Delete Vouchers (Open API batchDelete):
     * POST /service/api/open/auth/voucher/batchDelete/{groupId}?access_token={}
     */
    @POST("service/api/open/auth/voucher/batchDelete/{groupId}")
    suspend fun batchDeleteVoucherOpen(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Delete Vouchers matching official Ruijie Web SAM UI:
     * POST /service/api/intlSamVoucher/del/{tenantName}/{userName}/{groupId}?access_token={}&group_id={}&lang=en
     */
    @POST("service/api/intlSamVoucher/del/{tenantName}/{userName}/{groupId}")
    suspend fun deleteVoucherWeb(
        @Path("tenantName") tenantName: String,
        @Path("userName") userName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Query("group_id") queryGroupId: Long,
        @Query("lang") lang: String = "en",
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Delete Vouchers matching official Ruijie Web SAM UI (action 'delete'):
     * POST /service/api/intlSamVoucher/delete/{tenantName}/{userName}/{groupId}?access_token={}&group_id={}&lang=en
     */
    @POST("service/api/intlSamVoucher/delete/{tenantName}/{userName}/{groupId}")
    suspend fun deleteVoucherWebAction(
        @Path("tenantName") tenantName: String,
        @Path("userName") userName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Query("group_id") queryGroupId: Long,
        @Query("lang") lang: String = "en",
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Batch Delete Vouchers matching official Ruijie Web SAM UI:
     * POST /service/api/intlSamVoucher/batchDel/{tenantName}/{userName}/{groupId}?access_token={}&group_id={}&lang=en
     */
    @POST("service/api/intlSamVoucher/batchDel/{tenantName}/{userName}/{groupId}")
    suspend fun batchDeleteVoucherWeb(
        @Path("tenantName") tenantName: String,
        @Path("userName") userName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Query("group_id") queryGroupId: Long,
        @Query("lang") lang: String = "en",
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Batch Delete Vouchers matching official Ruijie Web SAM UI (action 'batchDelete'):
     * POST /service/api/intlSamVoucher/batchDelete/{tenantName}/{userName}/{groupId}?access_token={}&group_id={}&lang=en
     */
    @POST("service/api/intlSamVoucher/batchDelete/{tenantName}/{userName}/{groupId}")
    suspend fun batchDeleteVoucherWebAction(
        @Path("tenantName") tenantName: String,
        @Path("userName") userName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Query("group_id") queryGroupId: Long,
        @Query("lang") lang: String = "en",
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Delete Vouchers (Regional Tenant API del):
     * POST /service/api/intlSamVoucher/del/{tenantName}/{groupId}?access_token={}
     */
    @POST("service/api/intlSamVoucher/del/{tenantName}/{groupId}")
    suspend fun deleteVoucherTenant(
        @Path("tenantName") tenantName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Delete Vouchers (Regional Tenant API delete):
     * POST /service/api/intlSamVoucher/delete/{tenantName}/{groupId}?access_token={}
     */
    @POST("service/api/intlSamVoucher/delete/{tenantName}/{groupId}")
    suspend fun deleteVoucherTenantAction(
        @Path("tenantName") tenantName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Unbind Voucher (Open API unbind):
     * POST /service/api/open/auth/voucher/unbind/{groupId}?access_token={}
     */
    @POST("service/api/open/auth/voucher/unbind/{groupId}")
    suspend fun unbindVoucherOpen(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Unbind Voucher MAC (Open API unbindMac):
     * POST /service/api/open/auth/voucher/unbindMac/{groupId}?access_token={}
     */
    @POST("service/api/open/auth/voucher/unbindMac/{groupId}")
    suspend fun unbindVoucherMacOpen(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Unbind Voucher MAC matching Ruijie Web SAM UI:
     * POST /service/api/intlSamVoucher/unbindMac/{tenantName}/{userName}/{groupId}?access_token={}&group_id={}&lang=en
     */
    @POST("service/api/intlSamVoucher/unbindMac/{tenantName}/{userName}/{groupId}")
    suspend fun unbindVoucherMacWeb(
        @Path("tenantName") tenantName: String,
        @Path("userName") userName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Query("group_id") queryGroupId: Long,
        @Query("lang") lang: String = "en",
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Unbind Voucher matching Ruijie Web SAM UI:
     * POST /service/api/intlSamVoucher/unbind/{tenantName}/{userName}/{groupId}?access_token={}&group_id={}&lang=en
     */
    @POST("service/api/intlSamVoucher/unbind/{tenantName}/{userName}/{groupId}")
    suspend fun unbindVoucherWeb(
        @Path("tenantName") tenantName: String,
        @Path("userName") userName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Query("group_id") queryGroupId: Long,
        @Query("lang") lang: String = "en",
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Unbind Voucher MAC (Tenant API):
     * POST /service/api/intlSamVoucher/unbindMac/{tenantName}/{groupId}?access_token={}
     */
    @POST("service/api/intlSamVoucher/unbindMac/{tenantName}/{groupId}")
    suspend fun unbindVoucherMacTenant(
        @Path("tenantName") tenantName: String,
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Unbind Terminal / Kick off active auth session:
     * POST /service/api/open/auth/terminal/unbind/{groupId}?access_token={}
     */
    @POST("service/api/open/auth/terminal/unbind/{groupId}")
    suspend fun unbindTerminalOpen(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Kick off active subscriber session:
     * POST /service/api/open/auth/user/kickoff/{groupId}?access_token={}
     */
    @POST("service/api/open/auth/user/kickoff/{groupId}")
    suspend fun kickoffUserOpen(
        @Path("groupId") groupId: Long,
        @Query("access_token") accessToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * Generic GET endpoint for custom query or diagnostics
     */
    @GET
    suspend fun getGeneric(
        @Url url: String
    ): Response<ResponseBody>

    /**
     * Generic POST endpoint for custom query or diagnostics
     */
    @POST
    suspend fun postGeneric(
        @Url url: String,
        @Body body: RequestBody
    ): Response<ResponseBody>
}
