package com.kitchentwenty2.domain.model

data class MenuItemModel(
    val menuItemId: Long,
    val name: String,
    val category: String = "Main Course",
    val description: String? = null,
    val defaultPrice: Double
)

val SampleMenuItems = listOf(
    MenuItemModel(1, "Chicken Dum Biryani", "Biryani", "Fragrant basmati rice slow-cooked with spiced chicken", 260.0),
    MenuItemModel(2, "Mutton Dum Biryani", "Biryani", "Tender mutton cooked with saffron flavored long-grain rice", 340.0),
    MenuItemModel(3, "Veg Fried Rice", "Rice & Noodles", "Wok-tossed basmati rice with crunchy vegetables", 180.0),
    MenuItemModel(4, "Paneer Butter Masala", "Curries", "Fresh cottage cheese in creamy tomato-cashew gravy", 220.0),
    MenuItemModel(5, "Chicken Tikka Masala", "Curries", "Charcoal-grilled chicken chunks in aromatic spicy sauce", 280.0),
    MenuItemModel(6, "Special Raita", "Accompaniments", "Chilled yogurt mixed with roasted cumin and cucumber", 60.0),
    MenuItemModel(7, "Gulab Jamun (4 pcs)", "Desserts", "Melt-in-mouth milk solids soaked in cardamom syrup", 90.0)
)

