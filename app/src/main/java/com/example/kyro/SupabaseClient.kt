package com.example.kyro

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
// Conexion con Supabase mediante nuestro cliente
object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://ixrjuqrwybkgububbdia.supabase.co",
        supabaseKey = "sb_secret_j2P96Sih-CojS4A9IxLKrw_huNPNogi"
    ) {
        install(Auth)
        install(Postgrest)
        defaultSerializer = KotlinXSerializer()
    }
}