package com.sample.caravanbot.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.httpGet
import com.truedev.kinoposk.api.model.Result

//TODO (Можно всю логику скопировать и библиотеку удалить)
/**
 * Copy of KPApiClientService
 * @see com.truedev.kinoposk.api.service.KPApiClientService
 */
class KPApiClientServiceForSimilarFilms(private val timeout: Int) {
    private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    companion object {
        const val AUTH_HEADER = "X-API-KEY"
        const val MAIN_API_URL_V2_1 = "https://kinopoiskapiunofficial.tech/api/v2.1"
        const val GET_FILM = "/films"

        const val GET_SEARCH_BY_FILTERS = "/search-by-filters"
    }

    fun <T> request(url: String, path: String, clazz: Class<T>): Result<T> {
        val (request, response, result) = (url + path)
            .httpGet()
            .timeout(timeout)
            .timeoutRead(timeout)
            .header(mapOf(AUTH_HEADER to "6bb24419-0275-4f32-970d-f9ffca5f2959"))
            .responseString()

        return when (result) {
            is com.github.kittinunf.result.Result.Failure -> Result.Failure(
                httpStatus = response.statusCode,
                error = response.responseMessage
            )
            is com.github.kittinunf.result.Result.Success -> Result.Success(
                httpStatus = response.statusCode,
                result = mapper.readValue(result.get(), clazz)
            )
        }
    }

}