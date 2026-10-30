package com.example.model

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

/** Builds a Country from any two-letter ISO code (for example "TZ" -> Tanzania 🇹🇿). */
fun countryFromIso(code: String?): Country {
    val iso = code?.trim()?.uppercase(Locale.ROOT).orEmpty()
    if (iso.length != 2 || !iso.all { it in 'A'..'Z' }) return POPULAR_COUNTRIES[0] // Global
    POPULAR_COUNTRIES.firstOrNull { it.code == iso }?.let { return it }
    val name = Locale("", iso).getDisplayCountry(Locale.ENGLISH).ifBlank { iso }
    val flag = String(Character.toChars(0x1F1E6 + (iso[0] - 'A'))) +
        String(Character.toChars(0x1F1E6 + (iso[1] - 'A')))
    return Country(iso, name, flag)
}

/**
 * Best guess of the country the phone is in right now. Needs no permission.
 * Mobile network country first, then SIM country, then the phone's region setting.
 */
fun detectDeviceCountry(context: Context): Country {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val iso = listOf(tm?.networkCountryIso, tm?.simCountryIso, Locale.getDefault().country)
        .firstOrNull { !it.isNullOrBlank() }
    return countryFromIso(iso)
}
