# Severus Finance

Severus Finance is a modern, production-grade personal finance management web application designed for accurate single-user wealth, expense, loan amortization, and ledger tracking.

---

## 🌟 Key Features

* **Single-User JWT Authentication**: Secure password hashing with BCrypt cost 12, short-lived JWT access tokens in memory, and HttpOnly, Secure, SameSite=None refresh token cookies with rotation and revocation.
* **Financial Calculations Engine**:
  * Accurate Salary Cycle calculation (credit day to day before credit day next month).
  * `Remaining Salary Today`: Monthly Salary minus loan EMIs due so far, active fixed expenses due so far, and variable expenses recorded.
  * `Salary Surplus After Scheduled Commitments`: Monthly Salary minus all cycle EMIs, fixed expenses, and variable expenses.
* **Loan Amortization & Early Repayment**:
  * Reducing balance loan schedule generation with `BigDecimal` precision.
  * EMI tracking, payment logging, and early prepayment calculation.
* **Lend & Borrow Ledger**:
  * Track money lent (receivable) vs borrowed (payable).
  * Support for partial repayments, overpayment safeguards, and automatic settlement.
* **Expense Management**:
  * Separate tracking for fixed recurring monthly bills and variable daily expenses.
* **Interactive Dashboard**:
  * Built with React 19, Tailwind CSS, and Recharts.
  * Expense category breakdown donut chart and 6-cycle trend visualization.
  * Upcoming 30-day timeline & overdue items alerts.

---

## 🛠️ Architecture & Tech Stack

* **Monorepo Structure**:
  * `/backend`: Quarkus 3.15.1, Java 21, Hibernate Panache, SmallRye JWT, Flyway, PostgreSQL.
  * `/frontend`: React 19, TypeScript, Vite, Tailwind CSS 4, shadcn/ui primitives, Recharts, TanStack Query.

---

## 🚀 Development Quickstart

### Prerequisites
* Java 21+
* Node.js 20+
* Docker & Docker Compose

### Terminal 1 (Project Root):
```bash
docker compose up -d
```

### Terminal 2 (Backend):
```bash
cd backend
chmod +x run-dev.sh status.sh
./run-dev.sh
```

### Terminal 3 (Frontend):
```bash
cd frontend
npm run dev
```

### Status Check:
```bash
cd backend
./status.sh
```

---

## 🔒 Default Credentials
* **Email**: `severus.technology@gmail.com`
* **Password**: `Severus@Finance2026!`

*(Credentials can be changed in the Settings page or via `SINGLE_USER_EMAIL` / `SINGLE_USER_PASSWORD` env vars)*.