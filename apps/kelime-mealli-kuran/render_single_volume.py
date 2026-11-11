#!/usr/bin/env python3
"""Build one consolidated Quran PDF. No translation notes or explanations in its pages.
The full Turkish original Elmalili verse translation is distinct from original
editorial *word* glosses where independently created. Never fake word alignment.
All data is local, runtime internet connection is not required.
"""
import argparse
import concurrent.futures as cf
import hashlib
import html
import json
import os
from pathlib import Path
import re
from weasyprint import HTML
from pypdf import PdfReader, PdfWriter

ROOT=Path(__file__).resolve().parent
DATA=ROOT/"data"
AR=DATA/"quran-uthmani-tanzil-v1.1.json"
TR=DATA/"elmalili-original-public-domain.json"
GLOSS=DATA/"turkish-word-meanings-editorial-draft.json"
OUTPUT="RaSaSeR-KURAN-114-SURE-ARAPCA-TURKCE-TEK-CILT.pdf"
COLORS=("#177e79","#926b36","#476db7","#965c8e","#3e845c","#aa5349")
FATIHA={
 1:["Adıyla","Allah'ın","Rahmân","Rahîm"],
 2:["Övgü","Allah'a","Rabbi","âlemlerin"],
 3:["Rahmân","Rahîm"],
 4:["Mâliki","gününün","hesap"],
 5:["Yalnız sana","kulluk ederiz","yalnız senden","yardım isteriz"],
 6:["Bizi ilet","yola","dosdoğru"],
 7:["Yoluna","onların","nimet verdiklerinin","kendilerine","değil",
    "gazaba uğrayanların","onlara","ve değil","sapmışların"],
}
GLOSS_BASMALA=["Adıyla","Allah'ın","Rahmân","Rahîm"]
CSS="""
@page {
 size:A4; margin:15mm 14mm 17mm 14mm;
 @bottom-center{content:"Tanzil Project · tanzil.net   •   Elmalılı Hamdi Yazır";
 color:#8b7b65; font-size:7pt; font-family:"DejaVu Sans";}
}
html, body {margin:0;padding:0}
body {font-family:"DejaVu Sans";color:#30362f}
.heading {background:#f5efe2;border-bottom:2px solid #bd9655;padding:9px 12px;margin:0 0 14px;break-after:avoid;}
.heading h1 {font-weight:700;font-size:17pt;color:#96703c;margin:0 0 2px;}
.heading .arname {font-family:Amiri; font-size:21pt;color:#176f68;direction:rtl;float:right;line-height:1.1}
.heading .sub {font-size:8pt;color:#786f61}
.verse {margin-bottom:5px; padding:4px 1px 6px; border-bottom:1px solid #e8e3d9;break-inside:avoid;}
.vnum{color:#9b7745;font-size:8.3pt;font-weight:bold;margin-bottom:2px;break-after:avoid;}
.arline{direction:rtl;text-align:right; unicode-bidi:isolate;font-family:Amiri;line-height:1.9;}
.arline span{font-size:19pt;margin-right:4px}
.wordgrid{direction:rtl;text-align:right;line-height:1.3;}
.pair {display:inline-block;vertical-align:top;margin:2px 2px 3px;padding:1px 3px 2px;
min-width:44px;max-width:135px;text-align:center;border-radius:4px;}
.pair .ar {display:block;font-family:Amiri;font-size:20pt;line-height:1.8;direction:rtl;unicode-bidi:isolate;}
.pair .tr {display:block;direction:ltr;font-family:"DejaVu Sans";font-size:8pt;line-height:1.25;font-weight:bold;margin-top:2px;}
.translation {direction:ltr;text-align:left;margin:5px 0 1px;
font-size:9.8pt;line-height:1.48;color:#3c403a;}
.surahEnd {margin:7px auto;color:#bca16e;text-align:center;font-size:11pt;}
"""
def word_tokens(verse_text):
 return [s for s in re.split(r"\s+",verse_text.strip()) if s]
def verify_and_prepare():
 a=json.loads(AR.read_text(encoding="utf-8"))
 t=json.loads(TR.read_text(encoding="utf-8"))["quran"]
 g=json.loads(GLOSS.read_text(encoding="utf-8"))["word_glosses"]
 if len(a["surahs"])!=114 or len(t)!=6236: raise RuntimeError("Arabic/Turkish Quran incomplete")
 trmap={(int(x["chapter"]),int(x["verse"])):x["text"] for x in t}
 if len(trmap)!=6236:raise RuntimeError("Duplicated Turkish verses")
 verses=[(s["number"],v["number"]) for s in a["surahs"] for v in s["ayahs"] if v["number"]>0]
 if len(verses)!=6236 or set(verses)!=set(trmap):
  raise RuntimeError("Arabic/Turkish verse ID mismatch")
 if len(set(verses))!=6236:raise RuntimeError("Duplicated Arabic verses")
 return a["surahs"],trmap,g
def render_chapter(surah,trmap,glosses,temporary):
 n=surah["number"]
 name=html.escape(surah["name"])
 count=sum(v["number"]>0 for v in surah["ayahs"])
 out=['<html lang="tr"><head><meta charset="utf-8"><style>',CSS,'</style></head><body>',
      f'<div class="heading"><span class="arname">{name}</span><h1>{n}. SURE</h1>',
      f'<div class="sub">{count} Ayet</div></div>']
 gloss_count=0
 for verse in surah["ayahs"]:
  num=verse["number"]
  key=f"{n}:{num}"
  if num>0 and (n,num) not in trmap:raise RuntimeError("Missing Turkish verse "+key)
  a=word_tokens(verse["text"])
  if n==1 and num>0: gloss=FATIHA[num]
  elif num==0:gloss=GLOSS_BASMALA
  else: gloss=glosses.get(key)
  if gloss is not None and len(gloss)!=len(a):
   # The Arabic source treats a few pause markers differently; never misalign.
   gloss=None
  title=("Besmele" if num==0 else f"{n}:{num}")
  out.append(f'<div class="verse"><div class="vnum">{title}</div>')
  if gloss is not None:
   out.append('<div class="wordgrid">')
   for i,(aw,meaning) in enumerate(zip(a,gloss)):
    c=COLORS[i%len(COLORS)]
    out.append(f'<span class="pair"><span class="ar" lang="ar" style="color:{c}">{html.escape(aw)}</span>'
               f'<span class="tr" style="color:{c}">{html.escape(meaning)}</span></span>')
    gloss_count+=1
   out.append('</div>')
  else:
   out.append('<div class="arline" lang="ar">')
   for i,aw in enumerate(a):
    c=COLORS[i%len(COLORS)]
    out.append(f'<span style="color:{c}">{html.escape(aw)}</span> ')
   out.append('</div>')
  if num>0:
   out.append(f'<p class="translation">{html.escape(trmap[(n,num)])}</p>')
  out.append('</div>')
 out.append('<p class="surahEnd">❖</p></body></html>')
 path=temporary/f"surah-{n:03}.pdf"
 HTML(string="".join(out),base_url=str(ROOT)).write_pdf(str(path))
 return (n,str(path),gloss_count,len([v for v in surah["ayahs"] if v["number"]>0]))
def main():
 p=argparse.ArgumentParser()
 p.add_argument("--out",type=Path,required=True)
 p.add_argument("--jobs",type=int,default=2)
 args=p.parse_args()
 surahs,trmap,glosses=verify_and_prepare()
 args.out.parent.mkdir(parents=True,exist_ok=True)
 tmp=args.out.parent/"_quran_build_temp"
 tmp.mkdir(parents=True,exist_ok=True)
 results=[]
 with cf.ProcessPoolExecutor(max_workers=max(1,min(4,args.jobs))) as pool:
  futures=[pool.submit(render_chapter,s,trmap,glosses,tmp) for s in surahs]
  for f in cf.as_completed(futures):
   result=f.result()
   print("Rendered",result[0],flush=True)
   results.append(result)
 results.sort()
 writer=PdfWriter()
 for n,path,gcount,count in results:
  reader=PdfReader(path)
  if not reader.pages:raise RuntimeError(f"Empty PDF {n}")
  writer.append(reader)
 writer.add_metadata({"/Title":"Kur'an-ı Kerim — Arapça ve Türkçe Tek Cilt",
                      "/Subject":"Arapça: Tanzil Uthmani 1.1; Ayet meali: Elmalılı Hamdi Yazır (özgün)",
                      "/Creator":"RaSaSeR bağımsız PDF dizgi sistemi",
                      "/Keywords":"Kur'an, Arapça, Türkçe, Elmalılı, Tanzil"})
 with args.out.open("wb") as stream:writer.write(stream)
 pages=len(writer.pages)
 print(json.dumps({"pdf":str(args.out),"size":args.out.stat().st_size,
                   "chapters":len(results),"verses":sum(x[3] for x in results),
                   "pages":pages,"word_gloss_draft_count":sum(x[2] for x in results),
                   "sha256":hashlib.sha256(args.out.read_bytes()).hexdigest()},ensure_ascii=False),flush=True)
 for _,filename,_,_ in results:Path(filename).unlink()
 tmp.rmdir()
if __name__=="__main__":
 main()
