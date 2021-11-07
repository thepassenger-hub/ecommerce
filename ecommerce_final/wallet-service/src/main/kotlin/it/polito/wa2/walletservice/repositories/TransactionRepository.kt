package it.polito.wa2.walletservice.repositories

import it.polito.wa2.walletservice.common.TransactionDescription
import it.polito.wa2.walletservice.entities.Transaction
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
interface TransactionRepository : CoroutineCrudRepository<Transaction, ObjectId> {
    fun findAllByWalletIDAndTimestampBetween(walletID: ObjectId, from: Timestamp,
                                               to: Timestamp, pageable: Pageable): Flow<Transaction>

    fun findAllByWalletID(walletID: ObjectId, pageable: Pageable): Flow<Transaction>

    suspend fun findByReasonAndDescription(reason: ObjectId, description: TransactionDescription): Transaction?

    suspend fun findAllByReason(reason: String): Flow<Transaction>
}
