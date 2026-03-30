package com.example.touchlesstester

import java.io.Serializable


data class ResponseFingersData(
    val imageName: String,
    val grayscalePath: String,
    val nfiqScore: Int,
    val wsqPath: String,
    val binaryPath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val imageDp: Int
): Serializable



