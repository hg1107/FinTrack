# FinTrack — Personal Finance Tracker

A fully offline Android app for tracking Kitty Parties, Credit Cards, and Bank Accounts.  
Built with **Kotlin + Jetpack Compose**, **Material 3 dark theme**, **Room database**, and **WorkManager** for EMI reminders.

---

## Features

| Module | Key Features |
|--------|-------------|
| **Kitty Parties** | Unlimited kitties & members, 12-month rolling view, per-member amounts, Paid/Unpaid status, Office/Online/Cash payment modes |
| **Credit Cards** | Unlimited cards, running balance per transaction, credit limit & available credit bar, add/edit/delete transactions |
| **Bank Accounts** | Unlimited accounts, running balance, Transactions + EMI Schedule tabs, reminder notifications 1–N days before due |

---

## Tech Stack

- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM (ViewModel + StateFlow + Repository)
- **Database**: Room 2.6 (local, fully offline)
- **DI**: Hilt 2.51
- **Background**: WorkManager 2.9 (daily EMI reminder check)
- **Theme**: Dark-only, Inter font, emerald green accent (#2ECC71)
- **Currency**: ₹ INR

---

## Project Structure

```
app/src/main/java/com/fintrack/
├── data/
│   ├── local/
│   │   ├── db/          ← AppDatabase, Converters
│   │   ├── entity/      ← Kitty, KittyMember, KittyPayment, CreditCard,
│   │   │                   CardTransaction, BankAccount, AccountTransaction, Emi
│   │   └── dao/         ← KittyDao, CreditCardDao, BankAccountDao
│   └── repository/      ← KittyRepository, CreditCardRepository, BankAccountRepository
├── di/
│   └── DatabaseModule.kt
├── ui/
│   ├── home/            ← HomeScreen, HomeViewModel
│   ├── kitty/           ← KittyListScreen, KittyDetailScreen, AddEditKittyScreen,
│   │                       PaymentEntrySheet, KittyViewModel
│   ├── cards/           ← CreditCardsScreen, CreditCardDetailScreen, AddEditCardScreen,
│   │                       AddEditTransactionSheet, CreditCardViewModel
│   ├── accounts/        ← BankAccountsScreen, BankAccountDetailScreen,
│   │                       AddEditBankAccountScreen, AddEditEmiSheet, BankAccountViewModel
│   ├── shared/          ← CommonComponents (SummaryCard, StatusChip, AmountInputField, etc.)
│   ├── navigation/      ← NavGraph, BottomNavBar
│   └── theme/           ← Color, Type, Theme
├── worker/
│   ├── EmiReminderWorker.kt
│   └── NotificationHelper.kt
├── FinTrackApplication.kt
└── MainActivity.kt
```

---

## Setup & Build

### Prerequisites

- **Android Studio Ladybug** (2024.2.1) or newer
- **JDK 17** or newer
- Android SDK with **compileSdk 35** and **minSdk 26**

### Steps

1. **Clone / copy** the `FinTrack/` folder into your Android Studio projects directory.

2. **Open** in Android Studio:
   ```
   File → Open → select the FinTrack/ folder
   ```

3. **Sync Gradle** — Android Studio will prompt automatically. Click **Sync Now**.

4. **Run** on a device or emulator:
   ```
   Run → Run 'app'   (or Shift+F10)
   ```
   Minimum API 26 (Android 8.0).

5. **Build debug APK** (optional):
   ```bash
   ./gradlew assembleDebug
   # Output: app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Data Structure (Room Entities)

### Kitty Module
| Entity | Key Fields |
|--------|-----------|
| `Kitty` | id, name, startMonth (1–12), startYear, createdDate |
| `KittyMember` | id, kittyId, name, hostMonth (1–12), amount (₹) |
| `KittyPayment` | id, memberId, month, year, isPaid, amountPaid, datePaid, paymentMode [OFFICE/ONLINE/CASH], onlineAccountName |

### Credit Card Module
| Entity | Key Fields |
|--------|-----------|
| `CreditCard` | id, name, lastFourDigits, creditLimit, createdDate |
| `CardTransaction` | id, cardId, date, amount, type [DEBIT/CREDIT], note |

### Bank Account Module
| Entity | Key Fields |
|--------|-----------|
| `BankAccount` | id, name, lastFourDigits, createdDate |
| `AccountTransaction` | id, accountId, date, amount, type [DEBIT/CREDIT], note |
| `Emi` | id, accountId, label, amount, dueDayOfMonth, isPaid, paidDate, reminderDaysBefore, isReminderEnabled |

---

## EMI Notifications

- WorkManager runs a **daily background check**.
- For each EMI where `isReminderEnabled = true` and `isPaid = false`:
  - If `daysUntilDue ≤ reminderDaysBefore`, a push notification is fired.
- On **Android 13+**, `POST_NOTIFICATIONS` permission is requested on first launch.

---

## Customization

- **Accent color**: Change `Primary` in `Color.kt` (currently `#2ECC71` emerald green).
- **Add more payment modes**: Extend the `PaymentMode` enum in `KittyPayment.kt`.
- **Multi-currency**: Replace `inrFormatter` in `CommonComponents.kt`.

---

## License

MIT — personal use.
