package science.aist.neo4j.imdb

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestClient

class OmdbClient(
    private val apiKey: String = System.getenv("OMDB_API_KEY") ?: throw RuntimeException("OMDB_API_KEY not set")
) {

    fun omdbRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl("http://omdbapi.com")
            .defaultHeader("Accept", "application/json")
            .messageConverters { converters ->
                converters.add(0, MappingJackson2HttpMessageConverter(jacksonObjectMapper()))
            }
            .build()
    }

    fun getMovieByTitle(title: String): MovieResponse {
        return omdbRestClient().get()
            .uri { uriBuilder ->
                uriBuilder
                    .queryParam("apikey", apiKey)
                    .queryParam("t", title)
                    .build()
            }
            .retrieve()
            .body(MovieResponse::class.java)
            ?: throw RuntimeException("Failed to fetch movie data")
    }

    fun getMovieById(imdb: String): MovieResponse {
        return omdbRestClient().get()
            .uri { uriBuilder ->
                uriBuilder
                    .queryParam("apikey", apiKey)
                    .queryParam("i", imdb)
                    .build()
            }
            .retrieve()
            .body(MovieResponse::class.java)
            ?: throw RuntimeException("Failed to fetch movie data")
    }

}

data class MovieResponse(
    val Title: String,
    val Year: String,
    val Rated: String,
    val Released: String,
    val Runtime: String,
    val Genre: String,
    val Director: String,
    val Writer: String,
    val Actors: String,
    val Plot: String,
    val Language: String,
    val Country: String,
    val Awards: String,
    val Poster: String,
    val Ratings: List<Rating>,
    val Metascore: String,
    val imdbRating: String,
    val imdbVotes: String,
    val imdbID: String,
    val Type: String,
    val DVD: String?,
    val BoxOffice: String?,
    val Production: String?,
    val Website: String?,
    val Response: String
)

data class Rating(
    val Source: String,
    val Value: String
)