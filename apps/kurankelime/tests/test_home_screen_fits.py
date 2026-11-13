"""Regression: compact phone home shows Favorites and RaSaSeR stays on one line.

All original navigation, Quran assets, search, and offline translation are preserved.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "app/src/main/java/com/rasaser/free/kurankelime/DiscoverHomeActivity.kt"

class HomeLayoutContract(unittest.TestCase):
    def test_favorites_menu_kept_and_buttons_compacted(self):
        s=HOME.read_text(encoding="utf-8")
        self.assertIn("HomeAction(Icons.Default.MenuBook, l.read)",s)
        self.assertIn("HomeAction(Icons.Default.Search, l.search)",s)
        self.assertIn("HomeAction(Icons.Default.StarBorder, l.favorites)",s)
        self.assertIn("Modifier.fillMaxWidth().height(56.dp)",s)
        self.assertIn("verticalArrangement=Arrangement.Top, horizontalAlignment=Alignment.CenterHorizontally",s)
        self.assertIn("val compactHome = LocalConfiguration.current.screenHeightDp < 900",s)
        self.assertIn("modifier=Modifier.size(if(compactHome) 104.dp else 154.dp)",s)

    def test_brand_text_never_breaks_into_two_lines(self):
        s=HOME.read_text(encoding="utf-8")
        self.assertIn('Text("RaSaSeR"',s)
        self.assertIn("maxLines=1, softWrap=false",s)
        self.assertIn('Text("APPS & TECHNOLOGY"',s)
        self.assertIn('Text("ÜCRETSİZDİR · HİÇBİR MADDİ AMACIMIZ YOKTUR"',s)

    def test_existing_actions_still_open_same_destinations(self):
        s=HOME.read_text(encoding="utf-8")
        self.assertIn("HomeAction(Icons.Default.MenuBook, l.read) { openBookPage(context) }",s)
        self.assertIn('HomeAction(Icons.Default.Search, l.search) { screen="search" }',s)
        self.assertIn('HomeAction(Icons.Default.StarBorder, l.favorites) { updated++; screen="favorites" }',s)
        self.assertIn('Uri.parse("https://www.okiminsozu.com")',s)
        self.assertIn("bundledReady -> bundledIndex.search(query)",s)

    def test_existing_package_identity_is_retained_for_update(self):
        s=(ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.3.13"',s)
        self.assertIn("versionCode = 24",s)
        self.assertIn('applicationIdSuffix = ".ui312"',s)

if __name__=="__main__":
    unittest.main()
