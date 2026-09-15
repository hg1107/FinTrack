package com.fintrack.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fintrack.data.local.dao.BankAccountDao
import com.fintrack.data.local.dao.CreditCardDao
import com.fintrack.data.local.dao.KittyDao
import com.fintrack.data.local.dao.LedgerDao
import com.fintrack.data.local.entity.*
import java.time.LocalDate

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add unique composite index so upserts work correctly (one record per member per month/year)
        database.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_kitty_payments_memberId_month_year
            ON kitty_payments(memberId, month, year)
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // CreditCard: add billingDueDay and statementDay
        database.execSQL("ALTER TABLE credit_cards ADD COLUMN billingDueDay INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE credit_cards ADD COLUMN statementDay INTEGER NOT NULL DEFAULT 0")

        // Emi: add tenureMonths and startDate
        database.execSQL("ALTER TABLE emis ADD COLUMN tenureMonths INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE emis ADD COLUMN startDate TEXT")

        // New kitty_payouts table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS kitty_payouts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                kittyId INTEGER NOT NULL,
                month INTEGER NOT NULL,
                year INTEGER NOT NULL,
                hostMemberId INTEGER NOT NULL,
                amountReceived REAL NOT NULL DEFAULT 0.0,
                receivedDate TEXT,
                notes TEXT NOT NULL DEFAULT '',
                FOREIGN KEY (kittyId) REFERENCES kitties(id) ON DELETE CASCADE,
                FOREIGN KEY (hostMemberId) REFERENCES kitty_members(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_kitty_payouts_kittyId ON kitty_payouts(kittyId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_kitty_payouts_hostMemberId ON kitty_payouts(hostMemberId)")
        database.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_kitty_payouts_kittyId_month_year
            ON kitty_payouts(kittyId, month, year)
        """.trimIndent())
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // ── 1. Add note column to kitty_payments (existing rows get NULL — safe) ──
        database.execSQL("ALTER TABLE kitty_payments ADD COLUMN note TEXT")

        // ── 2. Create ledgers table ──
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS ledgers (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                colorHex TEXT NOT NULL DEFAULT '#2ECC71',
                iconId TEXT NOT NULL DEFAULT 'book',
                createdDate TEXT NOT NULL,
                isArchived INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // ── 3. Create ledger_entries table ──
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS ledger_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                ledgerId INTEGER NOT NULL,
                amount REAL NOT NULL,
                date TEXT NOT NULL,
                note TEXT,
                sourceType TEXT NOT NULL,
                sourceId INTEGER,
                isRecurring INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (ledgerId) REFERENCES ledgers(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_entries_ledgerId ON ledger_entries(ledgerId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_entries_sourceType_sourceId ON ledger_entries(sourceType, sourceId)")

        // ── 4. Seed the four default ledgers (INSERT OR IGNORE is safe on re-runs) ──
        val today = LocalDate.now().toString()
        database.execSQL("""
            INSERT OR IGNORE INTO ledgers (name, type, colorHex, iconId, createdDate, isArchived)
            VALUES
              ('Cash',       'AUTO',   '#2ECC71', 'payments',     '$today', 0),
              ('Raj Office', 'AUTO',   '#3498DB', 'business',     '$today', 0),
              ('Maid',       'MANUAL', '#9B59B6', 'cleaning',     '$today', 0),
              ('Boutique',   'MANUAL', '#E67E22', 'shopping_bag', '$today', 0)
        """.trimIndent())

        // ── 5. Rebuild kitty_payouts WITHOUT the (kittyId, month, year) unique constraint ──
        //    This allows multiple hosts in the same month.
        //    All existing rows are preserved via INSERT … SELECT.
        database.execSQL("""
            CREATE TABLE kitty_payouts_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                kittyId INTEGER NOT NULL,
                month INTEGER NOT NULL,
                year INTEGER NOT NULL,
                hostMemberId INTEGER NOT NULL,
                amountReceived REAL NOT NULL DEFAULT 0.0,
                receivedDate TEXT,
                notes TEXT NOT NULL DEFAULT '',
                FOREIGN KEY (kittyId) REFERENCES kitties(id) ON DELETE CASCADE,
                FOREIGN KEY (hostMemberId) REFERENCES kitty_members(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("""
            INSERT INTO kitty_payouts_new
              (id, kittyId, month, year, hostMemberId, amountReceived, receivedDate, notes)
            SELECT id, kittyId, month, year, hostMemberId, amountReceived, receivedDate, notes
            FROM kitty_payouts
        """.trimIndent())
        database.execSQL("DROP TABLE kitty_payouts")
        database.execSQL("ALTER TABLE kitty_payouts_new RENAME TO kitty_payouts")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_kitty_payouts_kittyId ON kitty_payouts(kittyId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_kitty_payouts_hostMemberId ON kitty_payouts(hostMemberId)")
    }
}

@Database(
    entities = [
        Kitty::class,
        KittyMember::class,
        KittyPayment::class,
        KittyPayout::class,
        CreditCard::class,
        CardTransaction::class,
        BankAccount::class,
        AccountTransaction::class,
        Emi::class,
        Ledger::class,
        LedgerEntry::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kittyDao(): KittyDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fintrack.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(object : Callback() {
                        // Seed ledgers on fresh installs (migration handles upgrades)
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            val today = LocalDate.now().toString()
                            db.execSQL("""
                                INSERT OR IGNORE INTO ledgers (name, type, colorHex, iconId, createdDate, isArchived)
                                VALUES
                                  ('Cash',       'AUTO',   '#2ECC71', 'payments',     '$today', 0),
                                  ('Raj Office', 'AUTO',   '#3498DB', 'business',     '$today', 0),
                                  ('Maid',       'MANUAL', '#9B59B6', 'cleaning',     '$today', 0),
                                  ('Boutique',   'MANUAL', '#E67E22', 'shopping_bag', '$today', 0)
                            """.trimIndent())
                        }
                    })
                    .build().also { INSTANCE = it }
            }
        }
    }
}
