package com.xxivek.tsdxxivek.dataDB

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Класс данных сущности представляет собой одну строку в базе данных.
 */
//Примечание: @Entity аннотация имеет несколько возможных аргументов.
// По умолчанию (без аргументов @Entity) имя таблицы будет таким же, как и класс.
// tableName Аргумент позволяет вам дать другое или более полезное имя таблицы.
// Этот аргумент для tableName необязателен, но настоятельно рекомендуется.
// Для простоты вы дадите то же имя, что и имя класса item.
// Есть несколько других аргументов @Entity, которые вы можете исследовать в документации.

@Entity(tableName = "item")
data class Item (
    //ColumnInfo Аннотация используется для настройки столбца, связанного с конкретным полем.
    // Например, при использовании name аргумента можно указать другое имя столбца для поля,
    // а не имя переменной

    // Анотируем id как первичный ключ. autoGenerate = true означает что id будет формироваться
    // автоматически.
//    @PrimaryKey(autoGenerate = true)
//    val id: Int = 0,
    @PrimaryKey(autoGenerate = false)
//    val id: Int,

    @ColumnInfo(name = "shtrihkod")
    val itemSh:String,

    @ColumnInfo(name = "shtrihtip")
    val itemShTip: Int,

    @ColumnInfo(name = "name")
    val itemName: String,

    @ColumnInfo(name = "price")
    val itemPrice: Double,

    @ColumnInfo(name = "quantityInStock")
    val itemQuantityInStock: Int,

    @ColumnInfo(name = "quantity")
    val itemQuantity: Int
)