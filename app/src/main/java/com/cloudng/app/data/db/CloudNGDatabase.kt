package com.cloudng.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cloudng.app.data.model.LogEvent
import com.cloudng.app.data.model.Profile
import com.cloudng.app.data.model.RoutingRule
import com.cloudng.app.data.model.Subscription

@Database(
    entities = [Profile::class, Subscription::class, RoutingRule::class, LogEvent::class],
    version = 1,
    exportSchema = true
)
abstract class CloudNGDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun routingRuleDao(): RoutingRuleDao
    abstract fun logEventDao(): LogEventDao
}
