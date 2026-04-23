package com.example.proyecto.domain.model

enum class ProductType { TOOL, SEED, CHEMICAL, FERTILIZER, VEGETABLE, OTHER }

data class InventoryItem(
    val id: String,
    val name: String,
    val type: ProductType,
    val quantity: String,
    val description: String?
)

data class Producto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val stock: Int,
    val icon: String
)