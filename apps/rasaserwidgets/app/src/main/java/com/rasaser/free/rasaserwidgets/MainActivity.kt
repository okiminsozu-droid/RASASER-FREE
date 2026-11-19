package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }
    override fun onResume() {
        super.onResume()
        render()
    }
    private fun render() {
        val root = UiKit.page(this)
        UiKit.label(this, root, "RaSaSeR", 32f, UiKit.GOLD, true)
        UiKit.label(this, root, "WIDGET STUDIO", 23f, UiKit.WHITE, true)
        UiKit.label(this, root, "Ana ekranını kendi tarzına dönüştür.", 15f, UiKit.MUTED)
        val hero = UiKit.panel(this, root)
        UiKit.label(this, hero, "✦  SINIRSIZ • ÇEVRİMDIŞI • REKLAMSIZ", 13f, UiKit.GOLD, true)
        UiKit.label(this, hero, "Fotoğraflı, görünmez veya yazılı tek dokunuş kısayolları oluştur.", 18f, UiKit.WHITE)
        UiKit.button(this, hero, "+  Ana ekrana widget ekle") { addWidget() }

        UiKit.label(this, root, "KISAYOL TİPLERİ", 15f, UiKit.GOLD, true)
        val info = UiKit.panel(this, root)
        listOf(
            "◈  Fotoğraf ve şeffaf simge",
            "◈  Uygulama ve internet bağlantısı",
            "◈  Arama ekranı ve kişisel not",
            "◈  Wi-Fi, Bluetooth ve sistem ayarları",
            "◈  İsteğe bağlı el feneri",
            "◈  Bütün widget'lar için ayrı görünürlük"
        ).forEach { UiKit.label(this, info, it, 15f) }

        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, ActionWidgetProvider::class.java))
        UiKit.label(this, root, "EKLENEN WIDGET'LAR (${ids.size})", 15f, UiKit.GOLD, true)
        val list = UiKit.panel(this, root)
        if (ids.isEmpty()) {
            UiKit.label(this, list, "Henüz eklenmiş bir widget yok. Ana ekranda Widget'lar bölümünden de ekleyebilirsin.", 14f, UiKit.MUTED)
        } else {
            ids.sorted().forEach { id ->
                val data = WidgetStore.load(this, id)
                UiKit.label(this, list,
                    "${data.caption.ifBlank { "Widget #$id" }}  •  ${data.kind}  •  %${data.opacity}",
                    15f, UiKit.WHITE, true)
                UiKit.button(this, list, "Düzenle • #$id", false) {
                    startActivity(Intent(this, WidgetEditorActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                }
            }
        }
        UiKit.label(this, root, "Gizlilik: İnternet izni yok. Fotoğraflar ve ayarlar yalnızca cihazında.", 12f, UiKit.MUTED)
        UiKit.label(this, root, "RaSaSeR • APPS & TECHNOLOGY • 0.1.0", 11f, UiKit.MUTED)
    }
    private fun addWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, ActionWidgetProvider::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            val accepted = manager.requestPinAppWidget(provider, null, null)
            if (!accepted) Toast.makeText(this, "Ana ekran isteği kabul edilmedi.", Toast.LENGTH_LONG).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Widget ekleme")
                .setMessage("Ana ekranda boş alana uzun bas → Widget'lar → RaSaSeR Widget Studio → Ekle.")
                .setPositiveButton("Anladım", null).show()
        }
    }
}
