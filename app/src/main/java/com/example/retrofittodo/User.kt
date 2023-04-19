package com.example.retrofittodo

data class User(
    val id: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}
