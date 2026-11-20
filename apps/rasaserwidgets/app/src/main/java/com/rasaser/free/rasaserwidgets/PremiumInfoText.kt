package com.rasaser.free.rasaserwidgets

import android.content.Context

/**
 * Clean annual Premium plan information. Does not claim an active purchase
 * flow: the only action in the UI is Close until a verified Google Play
 * Billing subscription is available. Future pricing must come from Play.
 */
object PremiumInfoText {
    private val fields = listOf(
        "headline", "price", "billingTitle", "billingBody",
        "accessTitle", "accessBody", "expirationTitle", "expirationBody", "close"
    )
    private val localized = mapOf(
        "tr" to listOf(
            "VARYOK Premium", "99 TL / yıl", "Yıllık Premium",
            "Sınırsız kullanım için yıllık Premium planı Google Play üzerinden sunulacaktır. Türkiye için belirlenen hedef fiyat 99 TL'dir.",
            "Sınırsız özgürlük",
            "Premium abonelik etkinleştiğinde farklı uygulamalar için sınırsız widget oluşturabilirsiniz.",
            "Widget'ların güvende",
            "Abonelik süresi dolduğunda mevcut widget'larınız çalışmaya devam eder.",
            "Kapat"
        ),
        "en" to listOf(
            "VARYOK Premium", "99 TRY / year", "Annual Premium",
            "An annual Premium plan for unlimited use will be offered through Google Play. The target price for Türkiye is 99 TRY.",
            "Unlimited freedom",
            "With an active Premium subscription you can create unlimited widgets for different apps.",
            "Your widgets stay",
            "Previously created widgets continue working after the subscription ends.",
            "Close"
        ),
        "de" to listOf(
            "VARYOK Premium", "99 TRY / Jahr", "Jährliches Premium",
            "Ein jährlicher Premium-Tarif für unbegrenzte Nutzung wird über Google Play angeboten. Der geplante Preis in der Türkei beträgt 99 TRY.",
            "Unbegrenzt gestalten",
            "Mit einem aktiven Premium-Abo können Sie unbegrenzt Widgets für unterschiedliche Apps erstellen.",
            "Ihre Widgets bleiben",
            "Vorhandene Widgets funktionieren nach Abo-Ende weiter.",
            "Schließen"
        ),
        "es" to listOf(
            "VARYOK Premium", "99 TRY / año", "Premium anual",
            "El plan anual Premium para uso ilimitado se ofrecerá a través de Google Play. El precio previsto en Turquía es de 99 TRY.",
            "Libertad sin límites",
            "Con una suscripción Premium activa podrás crear widgets ilimitados para distintas aplicaciones.",
            "Tus widgets permanecen",
            "Los widgets ya creados seguirán funcionando al terminar la suscripción.",
            "Cerrar"
        ),
        "ja" to listOf(
            "VARYOK Premium", "年間 99 トルコリラ", "年間Premium",
            "無制限に使える年間PremiumプランはGoogle Playで提供予定です。トルコでの予定価格は99トルコリラです。",
            "無制限に作成",
            "Premium契約が有効になると、さまざまなアプリのウィジェットを無制限に作成できます。",
            "ウィジェットはそのまま",
            "契約終了後も作成済みのウィジェットは動作し続けます。",
            "閉じる"
        ),
        "ar" to listOf(
            "VARYOK Premium", "99 ليرة تركية / سنة", "Premium السنوي",
            "سيُتاح الاشتراك السنوي لاستخدام غير محدود عبر Google Play. السعر المستهدف في تركيا هو 99 ليرة تركية.",
            "حرية بلا حدود",
            "عند تفعيل الاشتراك يمكنك إنشاء أدوات غير محدودة لتطبيقات مختلفة.",
            "أدواتك تبقى",
            "تظل الأدوات التي أنشأتها تعمل بعد انتهاء الاشتراك.",
            "إغلاق"
        )
    )

    fun t(context: Context, key: String): String {
        val index = fields.indexOf(key)
        if (index < 0) return key
        return localized[VaryokText.language(context)]?.get(index)
            ?: localized["tr"]!![index]
    }
}
