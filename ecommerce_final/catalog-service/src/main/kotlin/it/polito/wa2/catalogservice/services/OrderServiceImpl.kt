package it.polito.wa2.catalogservice.services

import it.polito.wa2.catalogservice.dto.OrderDTO
import it.polito.wa2.catalogservice.dto.UserDetailsDTO
import it.polito.wa2.catalogservice.exceptions.*
import it.polito.wa2.catalogservice.repositories.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toSet
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
class OrderServiceImpl(
    @Qualifier("order-service-client") private val loadBalancedWebClientBuilder: WebClient.Builder,
    private val productService: ProductService,
    private val customerRepository: CustomerRepository
) : OrderService{
    val serviceURL = "http://order-service"

    val client: WebClient = loadBalancedWebClientBuilder
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_NDJSON_VALUE)
        .defaultUriVariables(Collections.singletonMap("url", serviceURL))
        .build()

    override suspend fun getOrders(page: Int?, size: Int?): Flow<OrderDTO> {
        val token = ReactiveSecurityContextHolder.getContext().awaitSingle().authentication.credentials as String
        val pageOpt = if ( page != null) Optional.of(page) else Optional.empty()
        val sizeOpt = if ( size != null) Optional.of(size) else Optional.empty()
        return client
            .get()
            .uri{
                it.host("order-service").path("/orders")
                    .queryParamIfPresent("page", pageOpt)
                    .queryParamIfPresent("size", sizeOpt)
                    .build()
            }
            .accept(MediaType.APPLICATION_NDJSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(Predicate { it == HttpStatus.UNAUTHORIZED }) { throw UnauthorizedException("Nice try") }
            .onStatus(Predicate { it == HttpStatus.INTERNAL_SERVER_ERROR }) { throw UnavailableServiceException("Something went wrong") }
            .bodyToFlow()
    }

    override suspend fun getOrder(orderID: ObjectId): OrderDTO {
        val token = ReactiveSecurityContextHolder.getContext().awaitSingle().authentication.credentials as String
        return client
            .get()
            .uri("$serviceURL/orders/$orderID")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(Predicate { it == HttpStatus.NOT_FOUND }) { throw NotFoundException("Order not found") }
            .onStatus(Predicate { it == HttpStatus.FORBIDDEN }) { throw UnauthorizedException("Nice try") }
            .onStatus(Predicate { it == HttpStatus.UNAUTHORIZED }) { throw UnauthorizedException("Nice try") }
            .onStatus(Predicate { it == HttpStatus.INTERNAL_SERVER_ERROR }) { throw UnavailableServiceException("Something went wrong") }
            .awaitBody()
    }

    override suspend fun newOrder(orderDTO: OrderDTO): OrderDTO {
        val auth = ReactiveSecurityContextHolder.getContext().awaitSingle().authentication
        val user = auth.principal as UserDetailsDTO
        val token = auth.credentials as String

         val productsInStock = productService.getProductsByIDS(orderDTO.products!!.map { it.id!! }).toSet()
        orderDTO.products.forEach { product ->
            val whProduct = productsInStock.find { it.id == product.id } ?: throw RuntimeException("error")
            product.price = whProduct.price
        }

        orderDTO.email = customerRepository.findByUser(user.id!!)?.email ?: throw NotFoundException("Email not found")

        return client
            .post()
            .uri("$serviceURL/orders/")
            .bodyValue(orderDTO)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(Predicate { it == HttpStatus.BAD_REQUEST }) { it.bodyToMono(WebClientBadRequestException::class.java)}
            .onStatus(Predicate { it == HttpStatus.UNAUTHORIZED }) { throw UnauthorizedException("Nice try") }
            .onStatus(Predicate { it == HttpStatus.INTERNAL_SERVER_ERROR }) { throw UnavailableServiceException("Something went wrong") }
            .awaitBody()
    }

    override suspend fun deleteOrder(orderID: ObjectId, orderDTO: OrderDTO) {
        val auth = ReactiveSecurityContextHolder.getContext().awaitSingle().authentication
        val user = auth.principal as UserDetailsDTO
        val token = auth.credentials as String

        if (user.roles?.contains("ADMIN") == false)
            orderDTO.email = customerRepository.findByUser(user.id!!)?.email ?: throw NotFoundException("Customer not found")

        client
            .method(HttpMethod.DELETE)
            .uri("$serviceURL/orders/$orderID")
            .bodyValue  (
                orderDTO
            )
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(Predicate { it == HttpStatus.UNAUTHORIZED }) { throw UnauthorizedException("Nice try") }
            .onStatus(Predicate { it == HttpStatus.FORBIDDEN }) { throw UnauthorizedException("Nice try") }
            .onStatus(Predicate { it == HttpStatus.CONFLICT }) { throw InvalidOperationException("You cannot cancel the order anymore") }
            .onStatus(Predicate { it == HttpStatus.BAD_REQUEST }) { it.bodyToMono(WebClientBadRequestException::class.java)}
            .onStatus(Predicate { it == HttpStatus.INTERNAL_SERVER_ERROR }) { throw UnavailableServiceException("Something went wrong") }
            .awaitBodilessEntity()
    }

    override suspend fun updateOrder(orderID: ObjectId, orderDTO: OrderDTO): OrderDTO {
        val token = ReactiveSecurityContextHolder.getContext().awaitSingle().authentication.credentials as String
        return client
            .patch()
            .uri("$serviceURL/orders/$orderID")
            .bodyValue(orderDTO)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(Predicate { it == HttpStatus.UNAUTHORIZED }) { throw UnauthorizedException("Nice try") }
            .onStatus(Predicate { it == HttpStatus.FORBIDDEN }) { throw UnauthorizedException("Nice try") }
            .onStatus(Predicate { it == HttpStatus.INTERNAL_SERVER_ERROR }) { throw UnavailableServiceException("Error in service") }
            .onStatus(Predicate { it == HttpStatus.BAD_REQUEST }) { it.bodyToMono(WebClientBadRequestException::class.java)}
            .awaitBody()
    }
}