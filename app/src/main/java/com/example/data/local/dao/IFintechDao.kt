package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface IFintechDao {
    // Users & Auth
    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUser(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResetToken(token: PasswordResetTokenEntity)

    @Query("SELECT * FROM password_reset_tokens WHERE token = :token AND usedAt IS NULL LIMIT 1")
    suspend fun getValidResetToken(token: String): PasswordResetTokenEntity?

    @Query("UPDATE password_reset_tokens SET usedAt = :usedAt WHERE id = :id")
    suspend fun markResetTokenUsed(id: String, usedAt: Long)

    // Settings
    @Query("SELECT * FROM settings WHERE userId = :userId LIMIT 1")
    fun observeSetting(userId: String): Flow<SettingEntity?>

    @Query("SELECT * FROM settings WHERE userId = :userId LIMIT 1")
    suspend fun getSetting(userId: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: SettingEntity)

    // Accounts
    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY type ASC, name ASC")
    fun observeAccounts(userId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY type ASC, name ASC")
    suspend fun getAccounts(userId: String): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getAccount(userId: String, id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET balance = :balance, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateAccountBalance(id: String, balance: Double, updatedAt: Long)

    @Query("DELETE FROM accounts WHERE id = :id AND userId = :userId")
    suspend fun deleteAccount(userId: String, id: String)

    // Categories & SubCategories
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY type ASC, name ASC")
    fun observeCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY type ASC, name ASC")
    suspend fun getCategories(userId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getCategory(userId: String, id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND userId = :userId")
    suspend fun deleteCategory(userId: String, id: String)

    @Query(
        "SELECT s.* FROM subcategories s INNER JOIN categories c ON s.categoryId = c.id WHERE c.userId = :userId ORDER BY s.name ASC"
    )
    fun observeSubCategories(userId: String): Flow<List<SubCategoryEntity>>

    @Query(
        "SELECT s.* FROM subcategories s INNER JOIN categories c ON s.categoryId = c.id WHERE c.userId = :userId ORDER BY s.name ASC"
    )
    suspend fun getSubCategoriesForUser(userId: String): List<SubCategoryEntity>

    @Query("SELECT * FROM subcategories WHERE id = :id LIMIT 1")
    suspend fun getSubCategory(id: String): SubCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubCategory(subCategory: SubCategoryEntity)

    @Update
    suspend fun updateSubCategory(subCategory: SubCategoryEntity)

    @Query("DELETE FROM subcategories WHERE id = :id")
    suspend fun deleteSubCategory(id: String)

    // Transactions
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    fun observeTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    suspend fun getTransactions(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getTransaction(userId: String, id: String): TransactionEntity?

    @Query(
        "SELECT * FROM transactions WHERE userId = :userId AND (accountId IN (:accountIds) OR toAccountId IN (:accountIds))"
    )
    suspend fun getTransactionsForAccounts(userId: String, accountIds: List<String>): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE userId = :userId AND (accountId = :accountId OR toAccountId = :accountId)")
    suspend fun countTransactionsForAccount(userId: String, accountId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteTransaction(userId: String, id: String)

    // Tags
    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name ASC")
    fun observeTags(userId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name ASC")
    suspend fun getTags(userId: String): List<TagEntity>

    @Query("SELECT * FROM tags WHERE userId = :userId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTagByName(userId: String, name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Query(
        "SELECT tt.* FROM transaction_tags tt INNER JOIN transactions t ON tt.transactionId = t.id WHERE t.userId = :userId"
    )
    fun observeTransactionTags(userId: String): Flow<List<TransactionTagCrossRef>>

    @Query(
        "SELECT tt.* FROM transaction_tags tt INNER JOIN transactions t ON tt.transactionId = t.id WHERE t.userId = :userId"
    )
    suspend fun getTransactionTagsForUser(userId: String): List<TransactionTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionTag(crossRef: TransactionTagCrossRef)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun deleteTagsForTransaction(transactionId: String)

    // Attachments
    @Query("SELECT * FROM attachments WHERE userId = :userId")
    fun observeAttachments(userId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE userId = :userId")
    suspend fun getAttachments(userId: String): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id AND userId = :userId")
    suspend fun deleteAttachment(userId: String, id: String)

    // Budgets
    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY year DESC, month DESC")
    fun observeBudgets(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    suspend fun getBudgets(userId: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND year = :year AND month = :month")
    suspend fun getBudgetsForMonth(userId: String, year: Int, month: Int): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getBudget(userId: String, id: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId AND year = :year AND month = :month LIMIT 1")
    suspend fun getBudgetByCategoryMonth(userId: String, categoryId: String, year: Int, month: Int): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id AND userId = :userId")
    suspend fun deleteBudget(userId: String, id: String)

    // Goals & Contributions
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY isCompleted ASC, deadline ASC, createdAt DESC")
    fun observeGoals(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId")
    suspend fun getGoals(userId: String): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getGoal(userId: String, id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id AND userId = :userId")
    suspend fun deleteGoal(userId: String, id: String)

    @Query(
        "SELECT gc.* FROM goal_contributions gc INNER JOIN goals g ON gc.goalId = g.id WHERE g.userId = :userId ORDER BY gc.date DESC, gc.createdAt DESC"
    )
    fun observeGoalContributions(userId: String): Flow<List<GoalContributionEntity>>

    @Query(
        "SELECT gc.* FROM goal_contributions gc INNER JOIN goals g ON gc.goalId = g.id WHERE g.userId = :userId ORDER BY gc.date DESC, gc.createdAt DESC"
    )
    suspend fun getGoalContributionsForUser(userId: String): List<GoalContributionEntity>

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC, createdAt DESC")
    suspend fun getContributionsForGoal(goalId: String): List<GoalContributionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoalContribution(contribution: GoalContributionEntity)

    @Query("DELETE FROM goal_contributions WHERE id = :id AND goalId = :goalId")
    suspend fun deleteGoalContribution(goalId: String, id: String)

    // Recurring
    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY nextRunDate ASC")
    fun observeRecurringTransactions(userId: String): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY nextRunDate ASC")
    suspend fun getRecurringTransactions(userId: String): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getRecurringTransaction(userId: String, id: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND isActive = 1 AND nextRunDate <= :now ORDER BY nextRunDate ASC")
    suspend fun getDueRecurringTransactions(userId: String, now: Long): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity)

    @Update
    suspend fun updateRecurringTransaction(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteRecurringTransaction(userId: String, id: String)

    // Backup clear
    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun clearBudgets(userId: String)

    @Query("DELETE FROM goals WHERE userId = :userId")
    suspend fun clearGoals(userId: String)

    @Query("DELETE FROM recurring_transactions WHERE userId = :userId")
    suspend fun clearRecurring(userId: String)

    @Query("DELETE FROM attachments WHERE userId = :userId")
    suspend fun clearAttachments(userId: String)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun clearTransactions(userId: String)

    @Query("DELETE FROM tags WHERE userId = :userId")
    suspend fun clearTags(userId: String)

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun clearCategories(userId: String)

    @Query("DELETE FROM accounts WHERE userId = :userId")
    suspend fun clearAccounts(userId: String)
}
