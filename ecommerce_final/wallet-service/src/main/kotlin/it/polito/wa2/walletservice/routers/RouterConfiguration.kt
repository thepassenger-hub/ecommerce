package it.polito.wa2.walletservice.routers

import it.polito.wa2.walletservice.exceptions.InvalidOperationException
import it.polito.wa2.walletservice.exceptions.NotFoundException
import it.polito.wa2.walletservice.exceptions.UnauthorizedException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter
import java.util.logging.Logger
import javax.validation.ValidationException

@Configuration
class RouterConfiguration(
    private val logger: Logger
) {
    /**
     * router is similar to a controller but it reduces bootstrap time since less annotation are needed.
     * coRouter ('co' since we are in reactive kotlin)
     * Here more on RouterFunctionDsl:
     * https://docs.spring.io/spring-framework/docs/current/kdoc-api/spring-framework/org.springframework.web.reactive.function.server/-router-function-dsl/index.html
     * see Functional endpoints (1.5 paragraph) from here:
     * https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html
     */

    @Bean
    fun walletRoutes(walletHandler: WalletHandler) = coRouter {

        "/wallets".nest {
            GET("/{walletID}/transactions", walletHandler::getAllTransactions)
            GET("/{walletID}", walletHandler::getWallet)
            GET("/{walletID}/transactions/{transactionID}", walletHandler::getTransaction)

            accept(MediaType.APPLICATION_JSON).nest {
                POST("", contentType(MediaType.APPLICATION_JSON), walletHandler::createWallet)
                POST("/{walletID}/transactions", walletHandler::createRechargeTransaction)
            }
        }
        /**
         * 	Declaring the 'before' and 'after' filters at the bottom,
         * 	the logs of request/response are applied to all routes.
         */
        /**
         * 	Declaring the 'before' and 'after' filters at the bottom,
         * 	the logs of request/response are applied to all routes.
         */
        before { serverRequest ->
            logger.info("Doing : ${serverRequest.path()}")
            serverRequest
        }
        after { serverRequest, serverResponse ->
            logger.info("Ended: $serverRequest with ${serverResponse.statusCode()}")
            serverResponse
        }

        /**
         * The onError is applied to all previous route also.
         * It's similar to @ExceptionHandler and @ControllerAdvice
         */
        onError<NotFoundException> { e, _ ->  status(HttpStatus.NOT_FOUND).bodyValueAndAwait(e.localizedMessage)}
        onError<ValidationException> { e, _ ->  status(HttpStatus.UNPROCESSABLE_ENTITY).bodyValueAndAwait(e.localizedMessage)}
        onError<IllegalArgumentException> {e, _ ->  status(HttpStatus.BAD_REQUEST).bodyValueAndAwait(e.localizedMessage)}
        onError<OptimisticLockingFailureException> { _, _ ->  status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValueAndAwait("Database concurrency error")}
        onError<InvalidOperationException> { e, _ ->  status(HttpStatus.CONFLICT).bodyValueAndAwait(e.localizedMessage)}
        onError<UnauthorizedException> { e, _ ->  status(HttpStatus.UNAUTHORIZED).bodyValueAndAwait(e.localizedMessage)}
        onError<DuplicateKeyException> {e, _ ->  status(HttpStatus.BAD_REQUEST).bodyValueAndAwait("Duplicate key: ${e.message!!.substringAfter("dup key:").substringBefore(";")}")}
        onError<Exception> {e, _ ->  status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValueAndAwait(e.localizedMessage)}
    }
}
