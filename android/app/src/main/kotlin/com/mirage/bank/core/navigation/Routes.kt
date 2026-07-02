package com.mirage.bank.core.navigation

/** Central registry of every nav route in the app. Feature packages reference these, not raw strings. */
object Routes {
    // Auth
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Home / shell
    const val HOME = "home"

    // Personal
    const val BALANCE = "balance"
    const val TRANSFER = "transfer"
    const val WITHDRAW = "withdraw"
    const val TRANSACTIONS = "transactions"
    const val PROFILE = "profile"
    const val CHANGE_PASSWORD = "change_password"

    // Youth/Guardian
    const val GUARDIAN_DASHBOARD = "guardian_dashboard"
    const val GUARDIAN_WARD_TRANSACTIONS = "guardian_ward_transactions"

    // Business
    const val BUSINESS_MEMBERS = "business_members"

    // Support
    const val SUPPORT_TICKETS = "support_tickets"
    const val SUPPORT_NEW_TICKET = "support_new_ticket"
    const val SUPPORT_TICKET_DETAIL = "support_ticket_detail/{ticketId}"
    fun supportTicketDetail(ticketId: String) = "support_ticket_detail/$ticketId"

    // Premium
    const val PREMIUM_UPSELL = "premium_upsell"
    const val PREMIUM_ANALYTICS = "premium_analytics"
    const val PREMIUM_SAVINGS_GOALS = "premium_savings_goals"
    const val PREMIUM_VIRTUAL_CARDS = "premium_virtual_cards"
    const val PREMIUM_CASHBACK = "premium_cashback"
    const val PREMIUM_ADVANCED_SECURITY = "premium_advanced_security"

    // Admin
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_TRANSACTIONS = "admin_transactions"
    const val ADMIN_TREASURY_LEDGER = "admin_treasury_ledger"
    const val ADMIN_AUDIT_LOG = "admin_audit_log"
    const val ADMIN_SUPPORT = "admin_support"
    const val ADMIN_SUPPORT_TICKET_DETAIL = "admin_support_ticket_detail/{ticketId}"
    fun adminSupportTicketDetail(ticketId: String) = "admin_support_ticket_detail/$ticketId"

    // Settings
    const val SETTINGS = "settings"
}
