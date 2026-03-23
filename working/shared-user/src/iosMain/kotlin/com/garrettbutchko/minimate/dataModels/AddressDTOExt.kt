package com.garrettbutchko.minimate.dataModels

import kotlinx.cinterop.ExperimentalForeignApi
import platform.MapKit.MKAddress
import com.garrettbutchko.minimate.dataModels.mapModels.AddressDTO

@OptIn(ExperimentalForeignApi::class)
fun AddressDTO.toMKAddress(): MKAddress {
    return MKAddress(fullAddress = this.fullAddress, shortAddress = this.shortAddress)
}