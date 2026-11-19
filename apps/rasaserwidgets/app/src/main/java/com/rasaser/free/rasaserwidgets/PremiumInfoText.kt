package com.rasaser.free.rasaserwidgets

import android.content.Context

/**
 * Read-only Premium information. These strings must not imply that purchasing,
 * entitlement checks or subscription expiry enforcement are implemented.
 * When Billing is actually connected, product pricing should be supplied by
 * Google Play's ProductDetails for each user's region, not hard-coded here.
 */
object PremiumInfoText {
    private val fields = listOf("headline", "price", "billingTitle", "billingBody", "accessTitle", "accessBody", "expirationTitle", "expirationBody", "status", "close")
    private val localized = mapOf(
        "tr" to listOf("VARYOK Premium", "99 TL / yıl", "Yıllık Premium abonelik", "Google Play Billing üzerinden yıllık 99 TL ödeme ve otomatik yenileme planlanmaktadır.", "Premium hak kontrolü", "Aktif aboneliği olan kullanıcılar sınırsız sayıda farklı uygulama için widget oluşturabilecektir.", "Abonelik bitiş kontrolü", "Abonelik süresi dolduğunda yeni Premium widget oluşturma durdurulacak; daha önce oluşturulan widget'lar çalışmaya devam edecektir.", "Bilgilendirme: Premium satın alma ve abonelik kontrolü henüz etkin değildir. Bu ekran ödeme almaz.", "Kapat"),
        "en" to listOf("VARYOK Premium", "99 TRY / year", "Annual Premium subscription", "An annual 99 TRY payment through Google Play Billing with automatic renewal is planned.", "Premium entitlement check", "Users with an active subscription will be able to create widgets for unlimited different apps.", "Subscription expiration", "When the subscription expires, new Premium widgets will be restricted. Existing widgets will continue working.", "Information: Premium purchasing and subscription checks are not active yet. This screen cannot take payments.", "Close"),
        "de" to listOf("VARYOK Premium", "99 TRY / Jahr", "Jährliches Premium-Abo", "Geplant sind 99 TRY pro Jahr über Google Play Billing mit automatischer Verlängerung.", "Prüfung des Premium-Status", "Mit aktivem Abo lassen sich Widgets für unbegrenzt viele unterschiedliche Apps erstellen.", "Ablauf des Abonnements", "Nach Ablauf können keine neuen Premium-Widgets erstellt werden. Bereits erstellte Widgets funktionieren weiter.", "Hinweis: Premium-Käufe und Abo-Prüfung sind noch nicht aktiv. Hier werden keine Zahlungen angenommen.", "Schließen"),
        "es" to listOf("VARYOK Premium", "99 TRY / año", "Suscripción Premium anual", "Se prevé un pago anual de 99 TRY mediante Google Play Billing, con renovación automática.", "Verificación de Premium", "Con una suscripción activa se podrán crear widgets para un número ilimitado de aplicaciones diferentes.", "Fin de la suscripción", "Al vencer, se bloqueará la creación de nuevos widgets Premium. Los widgets ya creados seguirán funcionando.", "Información: las compras Premium y la verificación de suscripciones aún no están activas. Esta pantalla no cobra pagos.", "Cerrar"),
        "ja" to listOf("VARYOK Premium", "年間 99 トルコリラ", "年間Premiumサブスクリプション", "Google Play Billingによる年間99トルコリラの自動更新プランを予定しています。", "Premium利用権の確認", "有効なサブスクリプションがあれば、異なるアプリのウィジェットを無制限に作成できます。", "サブスクリプションの期限切れ", "期限切れ後は新しいPremiumウィジェットを作成できません。作成済みのウィジェットは引き続き動作します。", "ご案内：Premiumの購入と契約確認はまだ有効ではありません。この画面では支払いできません。", "閉じる"),
        "ar" to listOf("VARYOK Premium", "99 ليرة تركية / سنة", "اشتراك Premium السنوي", "من المخطط دفع 99 ليرة تركية سنويًا عبر Google Play Billing مع التجديد التلقائي.", "التحقق من صلاحية Premium", "يستطيع المشتركون النشطون إنشاء ويدجت لعدد غير محدود من التطبيقات المختلفة.", "انتهاء الاشتراك", "عند انتهاء الاشتراك، يتوقف إنشاء ويدجت Premium الجديدة، بينما تستمر الويدجت المنشأة سابقًا في العمل.", "ملاحظة: شراء Premium والتحقق من الاشتراك غير مفعلين بعد. لا يمكن الدفع من هذه الشاشة.", "إغلاق")
    )

    fun t(context: Context, key: String): String {
        val index = fields.indexOf(key)
        if (index < 0) return key
        return localized[VaryokText.language(context)]?.get(index)
            ?: localized["tr"]!![index]
    }
}
