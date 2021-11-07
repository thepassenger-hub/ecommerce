package it.polito.wa2.catalogservice.controllers

import it.polito.wa2.catalogservice.dto.TransactionDTO
import it.polito.wa2.catalogservice.dto.WalletDTO
import it.polito.wa2.catalogservice.services.WalletService
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/wallets")
class WalletController(
    private val walletService: WalletService
) {
    //RETRIEVE INFO ABOUT A WALLET GIVEN ITS ID
    @GetMapping("/{walletID}")
    @ResponseStatus(HttpStatus.OK)
    suspend fun getWallet(@PathVariable walletID: ObjectId): WalletDTO {
        return walletService.getWallet(walletID)
    }

    //GET LIST OF TRANSACTION OF A GIVEN WALLET, OPTIONALLY IN A RANGE OF TIME
    @GetMapping("/{walletID}/transactions")
    @ResponseStatus(HttpStatus.OK)
    suspend fun getTransactions(@PathVariable walletID: ObjectId,
                                @RequestParam from: Long?,
                                @RequestParam to: Long?,
                                @RequestParam page: Int?,
                                @RequestParam size: Int?): Flow<TransactionDTO> {
        return walletService.getTransactions(walletID,from,to,page,size)
    }

    //RETRIEVE THE INFO OF A TRANSACTION GIVEN ITS ID
    @GetMapping("/{walletID}/transactions/{transactionID}")
    @ResponseStatus(HttpStatus.OK)
    suspend fun getTransaction(@PathVariable walletID: ObjectId,
                               @PathVariable transactionID: ObjectId): TransactionDTO {
        return walletService.getTransaction(walletID,transactionID)
    }

    //ADD A WALLET FOR A GIVEN CUSTOMER
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun newWallet(@RequestBody @Validated walletDTO: WalletDTO): WalletDTO {
        return walletService.newWallet(walletDTO)
    }

    //CREATE A TRANSACTION
    @PostMapping("/{walletID}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun newTransaction(@PathVariable walletID: ObjectId,
                               @RequestBody @Validated transactionDTO: TransactionDTO): TransactionDTO {
        return walletService.newTransaction(walletID,transactionDTO)
    }
}