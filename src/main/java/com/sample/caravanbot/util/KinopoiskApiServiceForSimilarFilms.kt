package com.sample.caravanbot.util

import com.truedev.kinoposk.api.model.Result
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchResult

//TODO (Можно всю логику скопировать и библиотеку удалить)
/**
 * Copy of KinopoiskApiService
 * @see com.truedev.kinoposk.api.service.KinopoiskApiService
 */
class KinopoiskApiServiceForSimilarFilms(timeoutMs: Int = 15000) {

    private val kpApiClientService: KPApiClientServiceForSimilarFilms =
        KPApiClientServiceForSimilarFilms(timeoutMs)

    fun searchByKeyword(genre: String): Result<SearchResult> {
        return kpApiClientService.request(
            KPApiClientServiceForSimilarFilms.MAIN_API_URL_V2_1,
            "${KPApiClientServiceForSimilarFilms.GET_FILM}${KPApiClientServiceForSimilarFilms.GET_SEARCH_BY_FILTERS}?genre=$genre",
            SearchResult::class.java
        )
    }

}