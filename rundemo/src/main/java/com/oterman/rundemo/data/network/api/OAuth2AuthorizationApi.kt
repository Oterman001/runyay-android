package com.oterman.rundemo.data.network.api

import com.oterman.rundemo.data.network.dto.request.BaseRequest
import com.oterman.rundemo.data.network.dto.request.OAuth2AuthorizationListRequestDto
import com.oterman.rundemo.data.network.dto.request.OAuth2RevokeAuthorizationRequestDto
import com.oterman.rundemo.data.network.dto.response.BaseResponse
import com.oterman.rundemo.data.network.dto.response.OAuth2AuthorizationResponseData
import retrofit2.http.Body
import retrofit2.http.POST

interface OAuth2AuthorizationApi {
    @POST("oauth2/user/authorizations")
    suspend fun fetchAuthorizations(
        @Body request: BaseRequest<OAuth2AuthorizationListRequestDto>
    ): BaseResponse<OAuth2AuthorizationResponseData>

    @POST("oauth2/user/revoke")
    suspend fun revokeAuthorization(
        @Body request: BaseRequest<OAuth2RevokeAuthorizationRequestDto>
    ): BaseResponse<OAuth2AuthorizationResponseData>
}
