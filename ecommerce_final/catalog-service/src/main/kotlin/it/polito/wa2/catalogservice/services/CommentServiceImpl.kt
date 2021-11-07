package it.polito.wa2.catalogservice.services

import it.polito.wa2.catalogservice.dto.CommentDTO
import it.polito.wa2.catalogservice.exceptions.NotFoundException
import it.polito.wa2.catalogservice.exceptions.UnauthorizedException
import it.polito.wa2.catalogservice.exceptions.UnavailableServiceException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.awaitSingle
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToFlow
import java.util.*
import java.util.function.Predicate

@Service
class CommentServiceImpl(
    @Qualifier("warehouse-service-client") private val loadBalancedWebClientBuilder: WebClient.Builder
): CommentService {
    val serviceURL = "http://warehouse-service"
    //Create a WebClient instance
    //building a client by using the DefaultWebClientBuilder class, which allows full customization
    val client: WebClient = loadBalancedWebClientBuilder
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_NDJSON_VALUE)
        .defaultUriVariables(Collections.singletonMap("url", serviceURL))
        .build()

    override suspend fun getComments(productID: ObjectId): Flow<CommentDTO> {
        return client
            .get()
            .uri("$serviceURL/products/$productID/comments")
            .accept(MediaType.APPLICATION_NDJSON)
            .retrieve()
            .onStatus(Predicate { it == HttpStatus.INTERNAL_SERVER_ERROR }) { throw UnavailableServiceException("Something went wrong") }
            .onStatus(Predicate { it == HttpStatus.BAD_REQUEST }) { throw IllegalArgumentException("Product not found") }
            .bodyToFlow()
    }

    //RETRIEVE INFO ABOUT A COMMENT GIVEN ITS ID
    override suspend fun getComment(productID: ObjectId, commentID: ObjectId): CommentDTO {
        return client
            .get()
            .uri("$serviceURL/products/$productID/comments/$commentID")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(Predicate { it == HttpStatus.NOT_FOUND }) { throw NotFoundException("Comment not found") }
            .onStatus(Predicate { it == HttpStatus.FORBIDDEN }) { throw UnauthorizedException("Unauthorized") }
            .onStatus(Predicate { it == HttpStatus.UNAUTHORIZED }) { throw UnauthorizedException("Unauthorized") }
            .onStatus(Predicate { it == HttpStatus.INTERNAL_SERVER_ERROR }) { throw UnavailableServiceException("Something went wrong") }
            .awaitBody()
    }

    override suspend fun addComment(productID: ObjectId, commentDTO: CommentDTO): CommentDTO {
        val token = ReactiveSecurityContextHolder.getContext().awaitSingle().authentication.credentials as String
        return client
            .post()
            .uri("$serviceURL/products/$productID/comments")
            .bodyValue(commentDTO)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(Predicate { it == HttpStatus.BAD_REQUEST }) { throw IllegalArgumentException("Product not found") }
            .onStatus(Predicate { it == HttpStatus.UNAUTHORIZED }) { throw UnauthorizedException("Unauthorized") }
            .onStatus(Predicate { it == HttpStatus.FORBIDDEN }) { throw UnauthorizedException("Unauthorized") }
            .onStatus(Predicate { it == HttpStatus.INTERNAL_SERVER_ERROR }) { throw UnavailableServiceException("Something went wrong") }
            .awaitBody()
    }

    override suspend fun updateComment(productID: ObjectId, commentID: ObjectId, commentDTO: CommentDTO): CommentDTO {
        val token = ReactiveSecurityContextHolder.getContext().awaitSingle().authentication.credentials as String

        return client
            .put()
            .uri("$serviceURL/products/$productID/comments/$commentID")
            .bodyValue(commentDTO)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(Predicate { it == HttpStatus.UNAUTHORIZED }) { throw UnauthorizedException("Unauthorized") }
            .onStatus(Predicate { it == HttpStatus.INTERNAL_SERVER_ERROR }) { throw UnavailableServiceException("Something went wrong") }
            .onStatus(Predicate { it == HttpStatus.BAD_REQUEST }) { throw IllegalArgumentException("The comment does not exist") }
            .awaitBody()
    }

    override suspend fun deleteComment(productID: ObjectId, commentID: ObjectId) {
        val token = ReactiveSecurityContextHolder.getContext().awaitSingle().authentication.credentials as String
        client
            .method(HttpMethod.DELETE)
            .uri("$serviceURL/products/$productID/comments/$commentID")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(Predicate { it == HttpStatus.UNAUTHORIZED }) { throw UnauthorizedException("Unauthorized") }
            .onStatus(Predicate { it == HttpStatus.FORBIDDEN }) { throw UnauthorizedException("Unauthorized") }
            .onStatus(Predicate { it == HttpStatus.INTERNAL_SERVER_ERROR }) { throw UnavailableServiceException("Something went wrong") }
            .onStatus(Predicate { it == HttpStatus.BAD_REQUEST }) { throw IllegalArgumentException("The comment does not exist") }
            .awaitBodilessEntity()
    }
}