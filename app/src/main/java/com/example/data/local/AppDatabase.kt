package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.local.dao.IFintechDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.GoalContributionEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.PasswordResetTokenEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.SubCategoryEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionTagCrossRef
import com.example.data.local.entity.UserEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.TransactionType

class RoomConverters {
    @TypeConverter fun fromAccountType(value: AccountType): String = value.name
    @TypeConverter fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter fun fromCategoryType(value: CategoryType): String = value.name
    @TypeConverter fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter fun fromFrequency(value: RecurrenceFrequency): String = value.name
    @TypeConverter fun toFrequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)
}

@Database(
    entities = [
        UserEntity::class,
        PasswordResetTokenEntity::class,
        SettingEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        SubCategoryEntity::class,
        RecurringTransactionEntity::class,
        TransactionEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        AttachmentEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): IFintechDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ifintech.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
