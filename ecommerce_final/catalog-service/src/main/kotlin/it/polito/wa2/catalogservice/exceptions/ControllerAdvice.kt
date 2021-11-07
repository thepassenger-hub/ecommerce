package it.polito.wa2.catalogservice.exceptions

import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.sql.Timestamp
import javax.validation.ValidationException

@ControllerAdvice
class ControllerAdvice {

    @ExceptionHandler(value = [ValidationException::class,
        NotFoundException::class,
        IllegalArgumentException::class,
        OptimisticLockingFailureException::class,
        UnauthorizedException::class,
        InvalidOperationException::class,
        UnavailableServiceException::class,
        WebClientBadRequestException::class,
        WebExchangeBindException::class,
        WebClientResponseException::class,
        WebClientRequestException::class,
        DuplicateKeyException::class
    ])
    fun genericExceptionHandler(e: Exception): ResponseEntity<ErrorDTO> {
        val errorDTO = ErrorDTO(
            timestamp = Timestamp(System.currentTimeMillis()),
            error = e.message!!
        )

        var status = HttpStatus.BAD_REQUEST

        when (e) {
            is ValidationException -> {
                errorDTO.status = 422
                status = HttpStatus.UNPROCESSABLE_ENTITY
            }
            is InvalidOperationException -> {
                errorDTO.status = 409
                status = HttpStatus.CONFLICT
            }
            is UnauthorizedException -> {
                errorDTO.status = 403
                status = HttpStatus.FORBIDDEN
            }
            is OptimisticLockingFailureException -> {
                errorDTO.status = 500
                errorDTO.error = "Database concurrency error"
                status = HttpStatus.INTERNAL_SERVER_ERROR
            }
            is NotFoundException -> {
                errorDTO.status = 404
                status = HttpStatus.NOT_FOUND
            }
            is IllegalArgumentException -> {
                status = HttpStatus.BAD_REQUEST
            }
            is UnavailableServiceException -> {
                errorDTO.status = 500
                status = HttpStatus.INTERNAL_SERVER_ERROR
            }
            is WebClientBadRequestException -> {
                errorDTO.status = e.status
                status = HttpStatus.BAD_REQUEST
                errorDTO.error = e.message
            }
            is WebClientResponseException -> {
                errorDTO.status = 500
                status = HttpStatus.INTERNAL_SERVER_ERROR
                errorDTO.error = "Could not reach service"
            }
            is WebClientRequestException -> {
                errorDTO.status = 500
                status = HttpStatus.INTERNAL_SERVER_ERROR
                errorDTO.error = "Could not reach service"
            }
            is WebExchangeBindException -> {
                errorDTO.error = e.fieldErrors.map { it.defaultMessage.toString() }.reduce{acc, elem -> "$acc, $elem" }
            }
            is DuplicateKeyException -> {
                errorDTO.error = "Duplicate key: ${e.message!!.substringAfter("dup key:").substringBefore(";")}"
            }
        }

        return ResponseEntity(errorDTO, status)
    }
}