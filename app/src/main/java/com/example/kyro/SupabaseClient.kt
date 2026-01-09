package com.example.kyro

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
// Conexion con Supabase mediante nuestro cliente
object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://ixrjuqrwybkgububbdia.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml4cmp1cXJ3eWJrZ3VidWJiZGlhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjYzMDY0ODcsImV4cCI6MjA4MTg4MjQ4N30.2JHiF5Mztko2NKHdJPEoot7BzJ_fr3fJMbfadIrKQqM"
    ) {
        install(Auth)
        install(Postgrest)
        defaultSerializer = KotlinXSerializer()
    }
}