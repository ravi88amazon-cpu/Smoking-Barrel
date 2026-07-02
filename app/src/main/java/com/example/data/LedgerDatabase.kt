package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "credits")
data class CreditEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val cloudId: String = "",

    val date: String = "",
    val vendor: String = "",
    val productName: String = "",
    val productType: String = "",

    val numberOfProduct: Int = 0,

    val costPerProduct: Double = 0.0,
    val costOfProduct: Double = 0.0,
    val salesPrice: Double = 0.0,
    val totalPrice: Double = 0.0,

    val paymentStatus: String = "",

    val isSynced: Boolean = false
)
@Entity(tableName = "debits_account")
data class DebitAccountEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val cloudId: String = "",

    val date: String = "",
    val source: String = "",
    val productName: String = "",
    val productType: String = "",

    val numberOfProduct: Int = 0,

    val costPerProduct: Double = 0.0,
    val totalPrice: Double = 0.0,

    val paymentStatus: String = "",

    val isSynced: Boolean = false
)
@Entity(tableName = "debits_hand")
data class DebitHandEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val spentBy: String,
    val vendor: String,
    val description: String,
    val qty: Double,
    val rate: Double,
    val amount: Double,
    val status: String,
    val isSynced: Boolean = false
)

@Dao
interface LedgerDao {
    @Query("SELECT * FROM credits ORDER BY id DESC")
    fun getAllCredits(): Flow<List<CreditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredit(credit: CreditEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredits(credits: List<CreditEntity>)

    @Query("DELETE FROM credits WHERE isSynced = 1")
    suspend fun clearCredits()

    @Query("DELETE FROM credits WHERE id = :id")
    suspend fun deleteCredit(id: Int)

    @Query("SELECT * FROM debits_account ORDER BY id DESC")
    fun getAllDebitsAccount(): Flow<List<DebitAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebitAccount(debit: DebitAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebitsAccount(debits: List<DebitAccountEntity>)

    @Query("DELETE FROM debits_account WHERE isSynced = 1")
    suspend fun clearDebitsAccount()

    @Query("DELETE FROM debits_account WHERE id = :id")
    suspend fun deleteDebitAccount(id: Int)

    @Query("SELECT * FROM debits_hand ORDER BY id DESC")
    fun getAllDebitsHand(): Flow<List<DebitHandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebitHand(debit: DebitHandEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebitsHand(debits: List<DebitHandEntity>)

    @Query("DELETE FROM debits_hand WHERE isSynced = 1")
    suspend fun clearDebitsHand()

    @Query("DELETE FROM debits_hand WHERE id = :id")
    suspend fun deleteDebitHand(id: Int)

    @Query("UPDATE credits SET isSynced = 1 WHERE id = :id")
    suspend fun markCreditSynced(id: Int)

    @Query("UPDATE debits_account SET isSynced = 1 WHERE id = :id")
    suspend fun markDebitAccountSynced(id: Int)

    @Query("UPDATE debits_hand SET isSynced = 1 WHERE id = :id")
    suspend fun markDebitHandSynced(id: Int)
}

@Database(
    entities = [CreditEntity::class, DebitAccountEntity::class, DebitHandEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var INSTANCE: LedgerDatabase? = null

        fun getDatabase(context: Context): LedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "ledger_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
