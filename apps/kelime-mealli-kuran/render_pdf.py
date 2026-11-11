#!/usr/bin/env python3
"""Standalone RaSaSeR Quran PDF compositor. No runtime API, no account, no paid service.

Input: locally vendored Tanzil Arabic JSON, kept verbatim in data/.
Translation input: independent original / expressly redistributable word-by-word data ONLY.
Arabic full edition will work without a Turkish data file; it MUST NOT be labelled
'a complete word-by-word Turkish translation' without all 6236 verse mappings.
"""
import argparse
import hashlib
import html
import json
import re
from pathlib import Path
from weasyprint import HTML

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "data" / "quran-uthmani-tanzil-v1.1.json"
AR_STOP = {"ۛ", "ۚ", "ۗ", "ۙ", "ۖ", "ۜ", "ۘ", "۞", "۩", "۝"}
PALETTE = ["#157e76", "#a66b31", "#456db3", "#98659a", "#4f8260", "#a55650"]

# Original pedagogical sample only; independently authored editorial glosses.
# These are NOT a certified Quran translation and require scholarly proofreading.
FATIHA = {
    1: ["Adıyla", "Allah'ın", "Rahmân", "Rahîm"],
    2: ["Övgü", "Allah'a", "Rabbi", "âlemlerin"],
    3: ["Rahmân", "Rahîm"],
    4: ["Mâliki", "gününün", "hesap"],
    5: ["Yalnız sana", "kulluk ederiz", "yalnız senden", "yardım isteriz"],
    6: ["Bizi ilet", "yola", "dosdoğru"],
    7: ["Yoluna", "onların", "nimet verdiklerinin", "kendilerine",
        "değil", "gazaba uğrayanların", "onlara", "ve değil", "sapmışların"]
}

CSS = """
@page {
  size: A4; margin: 17mm 16mm 20mm 16mm;
  @bottom-center {
    content: "RaSaSeR • Tanzil 1.1 • " counter(page);
    font-family: "DejaVu Sans"; font-size: 8pt; color: #817258;
  }
}
html { color: #2d322e; }
body { font-family: "DejaVu Sans"; font-size: 9.5pt; }
h1,h2,h3,p { margin: 0; }
.cover { page-break-after: always; text-align:center; padding-top: 68mm; }
.cover h1 { color: #95723d; font-size: 29pt; letter-spacing: .2pt; }
.cover h2 { color: #21695f; margin-top:13mm; font-size:18pt; }
.cover .sub { color:#7c7464; font-size:12pt; line-height:2; margin-top:12mm; }
.notice { margin-top: 16mm; border-top: 1px solid #ccb78a; padding-top:6mm; color:#766a5a; font-size:10pt; line-height:1.6; }
.surah { page-break-before:always; }
.surah-head { background: #f3ebdd; border:1px solid #cbb482; border-radius:7px; padding:11px 14px; margin-bottom:14px; }
.surah-head h2 { color:#796139; font-weight:700; font-size:18pt; }
.surah-head span { font-size:10pt; color:#5e6357; }
.verse { border-bottom:1px solid #e7e1d6; margin-bottom:8px; padding-bottom:7px; }
.verse-name { font-family:"DejaVu Sans"; color:#826c45; font-size:9pt; font-weight:bold; margin:4px 0 5px; }
.tokens { direction:rtl; text-align:right; line-height:1.45; }
.word { display:inline-block; vertical-align:top; text-align:center;
        min-width:95px; max-width:156px; margin:3px 3px 5px;
        padding:5px 4px 3px; border-radius:6px; background:#faf7f0;
        border: 1px solid #e8dfcf; break-inside:avoid; }
.ar { direction:rtl; unicode-bidi:isolate; font-family:"Amiri", "Noto Naskh Arabic",serif;
      font-size:21pt; line-height:2.2; min-height:65px; }
.tr { direction:ltr; font-family:"DejaVu Sans"; font-size:9pt; line-height:1.2;
      color:#524b40; min-height:16px; margin-top:7px; }
.ornament {font-size:8pt;color:#a19076;line-height:1.1}
.word.pending .tr { color:#b3a693; font-size:7.5pt; }
.word.pending { background:#fffdfa; }
.arabic-inline {font-family:"Amiri", "Noto Naskh Arabic",serif; font-size:20pt; line-height:1.8; color:#18776b; direction:rtl; text-align:right; unicode-bidi:isolate;}\n.arabic-inline span {display:inline; padding: 0 1.8px;}\n.verse.arabic-full {margin-bottom:6px; padding-bottom:4px;}\n.legal { page-break-before:always; }
.legal h2 { color:#9b7839; font-size:17pt; margin-bottom:9mm; }
.legal p {font-size:10pt;line-height:1.6;margin:5mm 0;}
"""

def clean_tokens(text):
    # All Arabic characters remain verbatim: standalone pause signs get their own tile.
    return [x for x in re.split(r"\s+", text.strip()) if x]

def verify(source):
    if len(source.get("surahs", [])) != 114:
        raise ValueError("Not 114 surahs")
    numbered = [(s["number"], a["number"]) for s in source["surahs"]
                for a in s["ayahs"] if a["number"] > 0]
    if len(numbered) != 6236 or len(set(numbered)) != 6236:
        raise ValueError("Missing, duplicated or extra numbered verses")
    for i, s in enumerate(source["surahs"], 1):
        if s["number"] != i:
            raise ValueError(f"Surah order changed near {i}")
    if "Tanzil" not in source["metadata"].get("source", ""):
        raise ValueError("Source attribution missing")
    return numbered

def words_html(text, glosses=None, verse_id=""):
    tokens = clean_tokens(text)
    lexical = [w for w in tokens if w not in AR_STOP]
    if glosses is not None and len(glosses) != len(lexical):
        raise ValueError(f"Translation/token mismatch at {verse_id}: {len(glosses)} vs {len(lexical)}")
    i = 0
    out = []
    for pos, token in enumerate(tokens):
        is_stop = token in AR_STOP
        gl = None if is_stop or glosses is None else glosses[i]
        if not is_stop: i += 1
        color = PALETTE[(i-1 if not is_stop else i) % len(PALETTE)]
        arabic = html.escape(token)
        tr = html.escape(gl) if gl else ("" if is_stop else "—")
        cls = "word pending" if gl is None else "word"
        out.append(
            f'<div class="{cls}"><div class="ar" lang="ar" style="color:{color}">{arabic}</div>'
            f'<div class="tr">{tr}</div></div>'
        )
    return "".join(out)

def build_html(src, sample=False):
    edition = "FÂTİHA: KELİME MEALİ ÖRNEĞİ" if sample else "TAM KUR’AN: RENKLİ ARAPÇA METİN"
    status = ("İlk 7 ayet için bağımsız Türkçe kelime karşılığı. Dinî ilimler kontrolü bekleyen örnek."
              if sample else
              "114 sure / 6.236 ayet Arapça tam metin. Türkçe kelime meali henüz tamamlanmadı; bu kitap son meal baskısı değildir.")
    chunks = ['<html lang="tr"><head><meta charset="utf-8"><style>',CSS,
              '</style></head><body><section class="cover">',
              '<h1>RaSaSeR</h1><h2>',html.escape(edition),'</h2>',
              '<div class="sub">Çevrimdışı • Bilgisayar dizgisi • Kelime tabanlı<br>',
              'Siyah - Altın • Göz yormayan renk paleti</div>',
              '<p class="notice">',html.escape(status),'<br>',
              'Arapça kaynak: Tanzil Quran Text (Uthmani v1.1), CC BY 3.0 — tanzil.net</p>',
              '</section>']
    selections = src["surahs"][:1] if sample else src["surahs"]
    for surah in selections:
        n=surah["number"]
        all_verses = [v for v in surah["ayahs"] if v["number"]>=0]
        chunks.append(f'<section class="surah"><div class="surah-head"><h2>{n:03} · {html.escape(surah["name"])}</h2>'
                      f'<span>Sure {n} — {sum(v["number"]>0 for v in all_verses)} ayet</span></div>')
        for verse in all_verses:
            vn=verse["number"]
            gloss = FATIHA.get(vn) if n==1 and vn>0 else None
            title = "Besmele" if vn == 0 else f"{n}:{vn}"
            if sample:
                chunks.append(f'<div class="verse"><div class="verse-name">{title}</div>'
                              f'<div class="tokens">{words_html(verse["text"],gloss,f"{n}:{vn}")}</div></div>')
            else:
                # Full original Arabic edition is lean and reliably printable.
                # Turkish meanings are NOT fabricated for the other 6,229 verses.
                parts = [
                    f'<span style="color:{PALETTE[k % len(PALETTE)]}">{html.escape(word)}</span>'
                    for k, word in enumerate(clean_tokens(verse["text"]))
                ]
                chunks.append(f'<div class="verse arabic-full"><div class="verse-name">{title}</div>'
                              f'<div class="arabic-inline" lang="ar">{" ".join(parts)}</div></div>')
        chunks.append('</section>')
    chunks += ['<section class="legal"><h2>Kaynak ve yayın notu</h2>',
               '<p><b>Arapça metin:</b> Tanzil Quran Text, Uthmani Version 1.1, '
               'Copyright (C) 2007-2021 Tanzil Project, CC BY 3.0. '
               'https://tanzil.net — https://tanzil.net/docs/Text_License</p>',
               '<p>Permission is granted to copy and distribute verbatim copies of this text, '
               'but CHANGING IT IS NOT ALLOWED. This Quran text can be used in any website or '
               'application, provided that its source (Tanzil Project) is clearly indicated, '
               'and a link is made to tanzil.net.</p>',
               '<p><b>Metod:</b> Arapça ayet verisi kaynak JSON içinde aynen korunmuştur. '
               'Sayfa üzerinde kelimeler ayrı kutucuklara dizilmiş, renk yalnız görsel gösterimdir. '
               'Türkçe çeviri işi ve ilmî kontrol henüz tamamlanmamıştır.</p>',
               '<p>Bu PDF, başka şirketin canlı API hizmetine bağlı değildir. '
               'Kaynak lisans atıfları bağımsız üretime engel değildir ve korunacaktır.</p>',
               '</section></body></html>']
    return "".join(chunks)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--mode", choices=["full-arabic","sample-fatiha"],default="sample-fatiha")
    parser.add_argument("--out", required=True)
    args=parser.parse_args()
    raw=SOURCE.read_bytes()
    src=json.loads(raw)
    count=verify(src)
    print(f"Verified {len(src['surahs'])} surahs, {len(count)} verses; SHA256 {hashlib.sha256(raw).hexdigest()}",flush=True)
    doc=build_html(src,sample=args.mode=="sample-fatiha")
    out=Path(args.out);out.parent.mkdir(parents=True,exist_ok=True)
    HTML(string=doc,base_url=str(ROOT)).write_pdf(str(out))
    print(f"Written {out}: {out.stat().st_size} bytes",flush=True)

if __name__=="__main__":
    main()
