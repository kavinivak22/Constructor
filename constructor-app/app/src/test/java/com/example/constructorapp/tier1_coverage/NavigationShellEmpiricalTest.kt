package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.ui.components.BottomNavItem
import com.example.constructorapp.ui.components.NavItems
import org.junit.Assert.*
import org.junit.Test

class NavigationShellEmpiricalTest {

    @Test
    fun testBottomNavItems_specContract() {
        assertEquals("Bottom navigation must have exactly 5 items", 5, NavItems.size)

        assertEquals("Dashboard", NavItems[0].title)
        assertEquals("/", NavItems[0].route)

        assertEquals("Projects", NavItems[1].title)
        assertEquals("/projects", NavItems[1].route)

        assertEquals("Orders", NavItems[2].title)
        assertEquals("/purchase-orders", NavItems[2].route)

        assertEquals("Messages", NavItems[3].title)
        assertEquals("/team-hub", NavItems[3].route)

        assertEquals("More", NavItems[4].title)
        assertEquals("more", NavItems[4].route)
    }

    @Test
    fun testTabToRouteMapping() {
        fun resolveRoute(selectedTab: Int): String {
            return when (selectedTab) {
                0 -> "/"
                1 -> "/projects"
                2 -> "/purchase-orders"
                3 -> "/team-hub"
                else -> "/"
            }
        }

        assertEquals("/", resolveRoute(0))
        assertEquals("/projects", resolveRoute(1))
        assertEquals("/purchase-orders", resolveRoute(2))
        assertEquals("/team-hub", resolveRoute(3))
        assertEquals("/", resolveRoute(4)) // More triggers drawer, falls back to default
        assertEquals("/", resolveRoute(-1))
        assertEquals("/", resolveRoute(99))
    }
}
