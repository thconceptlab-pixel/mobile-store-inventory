package com.mobilestore.inventory.data.local

import androidx.room.TypeConverter
import com.mobilestore.inventory.data.local.entity.PaymentMethod
import com.mobilestore.inventory.data.local.entity.PhoneStatus
import com.mobilestore.inventory.data.local.entity.TransactionType

class Converters {
    @TypeConverter
    fun fromPhoneStatus(value: PhoneStatus): String = value.name
    @TypeConverter
    fun toPhoneStatus(value: String): PhoneStatus = PhoneStatus.valueOf(value)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name
    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
