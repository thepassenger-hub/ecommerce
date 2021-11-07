package it.polito.wa2.warehouseservice.exceptions

import java.sql.Timestamp

data class ErrorDTO (
        val timestamp: Timestamp,
        var status: Int = 400,
        var error: String
)