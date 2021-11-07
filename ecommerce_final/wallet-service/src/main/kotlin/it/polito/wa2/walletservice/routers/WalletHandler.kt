package it.polito.wa2.walletservice.routers

import it.polito.wa2.walletservice.dto.TransactionDTO
import it.polito.wa2.walletservice.dto.WalletDTO
import it.polito.wa2.walletservice.services.WalletService
import kotlinx.coroutines.delay
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*


@Component
class WalletHandler(
    private val walletService: WalletService
) {

    suspend fun getWallet(request: ServerRequest): ServerResponse {
        val walletID = request.pathVariable("walletID")
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(walletService.getWallet(walletID))
    }

    suspend fun createWallet(request: ServerRequest): ServerResponse {
        val walletDTO = request.awaitBody(WalletDTO::class)
            return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(walletService.createWallet(walletDTO))
    }

    /**
     * Please notice that the wallet must have some money (balance)
     * So the customer has to **recharge** the wallet.
     * For doing so, the admin calls this end-point.
     * For *Payment* or *Refund* (compensative transaction) it will be used Kafka instead.
     */
    suspend fun createRechargeTransaction(request: ServerRequest): ServerResponse{
        val walletID = request.pathVariable("walletID")
        val transactionDTO = request.awaitBody(TransactionDTO::class)

        var counter = 5
        while (counter-- > 0) {
            try {
                return ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(walletService.createRechargeTransaction(walletID, transactionDTO))
            } catch (e: OptimisticLockingFailureException) {
                    delay(1000)
                }
        }
        throw OptimisticLockingFailureException("Error with concurrent write requests")
    }

    suspend fun getAllTransactions(request: ServerRequest): ServerResponse{
        val walletID = request.pathVariable("walletID")
        val from = request.queryParamOrNull("from")?.toLong()
        val to = request.queryParamOrNull("to")?.toLong()

        val page = request.queryParamOrNull("page")?.toInt()
        val size = request.queryParamOrNull("size")?.toInt()
        var pageable: Pageable = Pageable.unpaged()
        if(page != null && size != null)
            pageable = PageRequest.of(page, size)
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_NDJSON)
            .bodyAndAwait(walletService.getAllTransactions(walletID, from, to, pageable))
    }

    suspend fun getTransaction(request: ServerRequest): ServerResponse{
        val walletID = request.pathVariable("walletID")
        val transactionID = request.pathVariable("transactionID")
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(walletService.getTransaction(walletID, transactionID))
    }
}
