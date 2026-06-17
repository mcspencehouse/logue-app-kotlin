package com.spencehouse.logue.service

object Config {
    const val IDENTITY_HOST = "https://identity.services.honda.com"
    const val WSC_HOST = "https://wsc.hondaweb.com"
    const val MQTT_HOST = "am7ptks1rwalc-ats.iot.us-east-2.amazonaws.com"
    const val MQTT_AUTHORIZER_NAME = "CPSD-IOT-CustAuthorizer-prod"

    const val CLIENT_ID = "AcuraEVAndroidAppPrOd0083"
    const val CLIENT_SECRET = "q4w5hzeqkFVMPQaeKuil"

    val COMMON_HEADERS = mapOf(
        "hondaHeaderType.country_code" to "US",
        "hondaHeaderType.language_code" to "en",
        "hondaHeaderType.businessId" to "ACURA EV",
        "User-Agent" to "okhttp/4.12.0",
    )

    val DASHBOARD_FILTERS = listOf(
        "DigitalTwin",
        "EV BATTERY LEVEL",
        "EV CHARGE STATE",
        "EV PLUG STATE",
        "EV PLUG VOLTAGE",
        "GET COMMUTE SCHEDULE",
        "HIGH VOLTAGE BATTERY PRECONDITIONING STATUS",
        "VEHICLE RANGE",
        "odometer",
        "tireStatus",
        "HV BATTERY CHARGE COMPLETE TIME",
        "TARGET CHARGE LEVEL SETTINGS",
        "GET CHARGE MODE",
        "CABIN PRECONDITIONING TEMP CUSTOM SETTING",
        "CHARGER POWER LEVEL",
        "HANDS FREE CALLING",
        "ENERGY EFFICIENCY",
    )
}
