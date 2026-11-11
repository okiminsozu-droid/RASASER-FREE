#!/usr/bin/env python3
"""Create a reproducible, offline Arabic-to-Turkish word translation workbook.

IMPORTANT: blank translation != approved translation. No automatic meanings invented.
Original Quran text source is kept byte-for-byte in the vendored JSON.
"""
import argparse
import csv
import hashlib
import json
import re
from collections import Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parent
SRC=ROOT/"data/quran-uthmani-tanzil-v1.1.json"
PAUSE={"ۛ","ۚ","ۗ","ۙ","ۖ","ۜ","ۘ","۞","۩","۝"}
FATIHA={
 1:["Adıyla","Allah'ın","Rahmân","Rahîm"],
 2:["Övgü","Allah'a","Rabbi","âlemlerin"],
 3:["Rahmân","Rahîm"],
 4:["Mâliki","gününün","hesap"],
 5:["Yalnız sana","kulluk ederiz","yalnız senden","yardım isteriz"],
 6:["Bizi ilet","yola","dosdoğru"],
 7:["Yoluna","onların","nimet verdiklerinin","kendilerine",
    "değil","gazaba uğrayanların","onlara","ve değil","sapmışların"]
}

def generate(output):
    raw=SRC.read_bytes()
    data=json.loads(raw)
    surahs=data.get("surahs",[])
    assert len(surahs)==114
    headers=["surah","ayah","position","kind","arabic_exact","turkish_meaning",
             "translation_status","reviewer","review_date","license","notes"]
    output.mkdir(parents=True,exist_ok=True)
    dest=output/"KURAN-114-SURE-KELIME-TR-CALISMA-TABLOSU.tsv"
    stats=Counter()
    numbered=set()
    with dest.open("w",encoding="utf-8-sig",newline="") as f:
        writer=csv.DictWriter(f,fieldnames=headers,delimiter="\t")
        writer.writeheader()
        for surah in surahs:
            sid=surah["number"]
            assert 1 <= sid <= 114
            for verse in surah["ayahs"]:
                aid=verse["number"]
                if aid>0:
                    key=(sid,aid)
                    assert key not in numbered
                    numbered.add(key)
                tokens=[w for w in re.split(r"\s+",verse["text"].strip()) if w]
                lexical=[w for w in tokens if w not in PAUSE]
                if sid==1 and aid in FATIHA and len(FATIHA[aid])!=len(lexical):
                    raise ValueError(f"Fatiha draft gloss count mismatch at {sid}:{aid}")
                index=0
                for token in tokens:
                    is_pause=token in PAUSE
                    if not is_pause:index+=1
                    tr=(FATIHA[aid][index-1] if sid==1 and aid in FATIHA and not is_pause else "")
                    status=("editorial_draft_needs_scholarly_review" if tr else
                            "not_applicable" if is_pause else "translation_missing")
                    writer.writerow(dict(surah=sid,ayah=aid,position=index if not is_pause else "",
                        kind="pause_marker" if is_pause else "arabic_word",
                        arabic_exact=token,turkish_meaning=tr,translation_status=status,
                        reviewer="",review_date="",license="",notes=""))
                    stats["total_entries"]+=1
                    stats["pause_markers" if is_pause else "word_entries"]+=1
                    if tr:stats["draft_meanings"]+=1
                    else:stats["empty_or_markers"]+=1
    assert len(numbered)==6236
    assert stats["draft_meanings"]==29
    result={
        "source":"Tanzil Uthmani v1.1 via dotquran/corpus",
        "source_sha256":hashlib.sha256(raw).hexdigest(),
        "word_table_sha256":hashlib.sha256(dest.read_bytes()).hexdigest(),
        "surahs":len(surahs),"numbered_verses":len(numbered),
        "status":"INCOMPLETE - no release of full word-meaning PDF until legal and scholarly reviews",
        **dict(stats)
    }
    (output/"TRANSLATION-QA-STATUS.json").write_text(
        json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(result,ensure_ascii=False,indent=2),flush=True)

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--out",type=Path,default=Path("output"))
    generate(p.parse_args().out)
