package com.github.mrfapa.diffprompt.api

import com.github.mrfapa.diffprompt.interceptor.ApiKeyInterceptor
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClientFactory {

    companion object {
        fun createClient(baseUrl: String): Retrofit {
            val logger = LoggerFactory.getLogger("HttpLoggingInterceptor")
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                logger.info(message)
            }
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

            val client = OkHttpClient()
                .newBuilder()
                .addInterceptor(ApiKeyInterceptor())
                //.addInterceptor(loggingInterceptor)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build()

            val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build()
        }
    }

}