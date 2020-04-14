package com.mlreef.rest.api

import com.mlreef.rest.Account
import com.mlreef.rest.AccountToken
import com.mlreef.rest.DataProjectRepository
import com.mlreef.rest.ExperimentRepository
import com.mlreef.rest.Person
import com.mlreef.rest.api.v1.dto.LoginRequest
import com.mlreef.rest.api.v1.dto.RegisterRequest
import com.mlreef.rest.api.v1.dto.UserDto
import com.mlreef.rest.exceptions.ErrorCode
import com.mlreef.rest.exceptions.GitlabAuthenticationFailedException
import com.mlreef.rest.external_api.gitlab.dto.OAuthToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.Rollback
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID.randomUUID
import javax.transaction.Transactional

class AuthApiTest : RestApiTest() {

    val authUrl = "/api/v1/auth"

    @Autowired
    private lateinit var experimentRepository: ExperimentRepository

    @Autowired
    private lateinit var dataProjectRepository: DataProjectRepository

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    @BeforeEach
    @AfterEach
    fun clearRepo() {
        experimentRepository.deleteAll()
        dataProjectRepository.deleteAll()
        accountTokenRepository.deleteAll()
        accountRepository.deleteAll()
        personRepository.deleteAll()
    }

    @Transactional
    @Rollback
    @Test
    fun `Can register with new user`() {
        Mockito.`when`(restClient.userLoginOAuthToGitlab(
            Mockito.anyString(), Mockito.anyString()
        )).thenReturn(
            OAuthToken("accesstoken12345", "refreshtoken1234567", "bearer", "api", 1585910424)
        )

        val email = "email@example.org"
        val registerRequest = RegisterRequest("username", email, "a password", "name")

        val returnedResult: UserDto = this.mockMvc.perform(
            post("$authUrl/register")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk)
            .andDo(document(
                "register-success",
                requestFields(registerRequestFields()),
                responseFields(userDtoResponseFields())))
            .andReturn().let {
                objectMapper.readValue(it.response.contentAsByteArray, UserDto::class.java)
            }

        with(accountRepository.findOneByEmail(email)!!) {
            assertThat(id).isEqualTo(returnedResult.id)
        }
    }

    @Transactional
    @Rollback
    @Test
    fun `Cannot register with existing user`() {
        val existingUser = createMockUser()
        val registerRequest = RegisterRequest(existingUser.username, existingUser.email, "a password", "name")

        this.mockMvc.perform(
            post("$authUrl/register")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().is4xxClientError)
            .andDo(document(
                "register-fail",
                responseFields(errorResponseFields())))

    }

    @Transactional
    @Rollback
    @Test
    fun `Can login with existing user`() {
        Mockito.`when`(restClient.userLoginOAuthToGitlab(
            Mockito.anyString(), Mockito.anyString()
        )).thenReturn(
            OAuthToken("accesstoken12345", "refreshtoken1234567", "bearer", "api", 1585910424)
        )

        val plainPassword = "password"
        val existingUser = createMockUser(plainPassword, "0000")
        val loginRequest = LoginRequest(existingUser.username, existingUser.email, plainPassword)

        val returnedResult: UserDto = this.mockMvc.perform(
            post("$authUrl/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk)
            .andDo(document(
                "login-success",
                requestFields(loginRequestFields()),
                responseFields(userDtoResponseFields())))
            .andReturn().let {
                objectMapper.readValue(it.response.contentAsByteArray, UserDto::class.java).censor()
            }
        assertThat(returnedResult).isNotNull
    }

    @Transactional
    @Rollback
    @Test
    fun `Cannot login with Gitlab is rejected credentials`() {
        Mockito.`when`(restClient.userLoginOAuthToGitlab(
            Mockito.anyString(), Mockito.anyString()
        )).then {
            throw GitlabAuthenticationFailedException(403, "Incorrect user or password", ErrorCode.ValidationFailed, "Bad credentials")
        }

        val plainPassword = "password"
        val existingUser = createMockUser(plainPassword, "0000")
        val loginRequest = LoginRequest(existingUser.username, existingUser.email, plainPassword)

        this.mockMvc.perform(
            post("$authUrl/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().is4xxClientError)
            .andDo(document(
                "login-fail",
                responseFields(errorResponseFields())))
    }

    private fun userDtoResponseFields(): List<FieldDescriptor> {
        return listOf(
            fieldWithPath("id").type(JsonFieldType.STRING).description("UUID"),
            fieldWithPath("username").type(JsonFieldType.STRING).description("An unique username"),
            fieldWithPath("email").type(JsonFieldType.STRING).description("An valid email"),
            fieldWithPath("token").type(JsonFieldType.STRING).description("The permanent (with long-lifetime) token to authenticate in gitlab and mlreef. Can be used in PRIVATE-TOKEN"),
            fieldWithPath("access_token").type(JsonFieldType.STRING).description("The OAuth (with short-lifetime) access token to authenticate in gitlab and mlreef. Can be used in PRIVATE-TOKEN"),
            fieldWithPath("refresh_token").type(JsonFieldType.STRING).description("The OAuth refresh token to authenticate in gitlab and mlreef. an be used in PRIVATE-TOKEN")
        )
    }

    private fun registerRequestFields(): List<FieldDescriptor> {
        return listOf(
            fieldWithPath("password").type(JsonFieldType.STRING).description("A plain text password"),
            fieldWithPath("username").type(JsonFieldType.STRING).description("A valid, not-yet-existing username"),
            fieldWithPath("email").type(JsonFieldType.STRING).description("A valid email"),
            fieldWithPath("name").type(JsonFieldType.STRING).description("The fullname of the user")
        )
    }

    private fun loginRequestFields(): List<FieldDescriptor> {
        return listOf(
            fieldWithPath("password").type(JsonFieldType.STRING).description("The plain text password"),
            fieldWithPath("username").type(JsonFieldType.STRING).optional().description("At least username or email has to be provided"),
            fieldWithPath("email").type(JsonFieldType.STRING).optional().description("At least username or email has to be provided")
        )
    }

    @Transactional
    fun createMockUser(plainPassword: String = "password", userOverrideSuffix: String? = null): Account {
        val accountId = randomUUID()
        val passwordEncrypted = passwordEncoder.encode(plainPassword)
        val person = Person(randomUUID(), "person_slug", "user name", 1L)
        val token = AccountToken(randomUUID(), accountId, "secret_token", 0)
        val account = Account(accountId, "username", "email@example.com", passwordEncrypted, person, mutableListOf(token))

        personRepository.save(person)
        accountRepository.save(account)
//        accountTokenRepository.save(token)
        return account
    }
}
