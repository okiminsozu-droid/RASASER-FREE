#!/usr/bin/env python3
"""Read-only PDF text-layer preflight (offline); requires PyMuPDF.

Usage: python tools/pdf_text_preflight.py input.pdf --output report.json
Helps distinguish existing PDF text from an image-only PDF body.
Note: mixed and image-heavy documents still require page/block review.
"""
import argparse
import json
from pathlib import Path
from statistics import median

import fitz


def preflight(filename: Path) -> dict:
    counts = []
    pages_with_images = 0
    images = 0
    pages_with_body_text = 0
    samples = []
    with fitz.open(filename) as pdf:
        for number, page in enumerate(pdf, 1):
            words = page.get_text("words")
            image_blocks = sum(block["type"] == 1 for block in page.get_text("dict")["blocks"])
            counts.append(len(words))
            images += image_blocks
            pages_with_images += bool(image_blocks)
            pages_with_body_text += len(words) >= 70
            if number in {1,2,3,6,16,58,161,304}:
                samples.append({
                    "page": number,
                    "native_words": len(words),
                    "image_blocks": image_blocks,
                    "text_sample": page.get_text()[:145],
                })
        image_body = len(pdf)>0 and pages_with_images == len(pdf) and pages_with_body_text == 0
        return {
            "file":filename.name,
            "pages":len(pdf),
            "pages_with_images":int(pages_with_images),
            "image_blocks":images,
            "existing_native_words":sum(counts),
            "median_native_words_per_page":median(counts) if counts else 0,
            "pages_with_70_or_more_native_words":int(pages_with_body_text),
            "body_type_hint":"image-only body with small header text" if image_body else "mixed / selectable; inspect",
            "samples":samples,
        }


if __name__ == "__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("pdf",type=Path)
    parser.add_argument("--output",type=Path)
    args=parser.parse_args()
    report=json.dumps(preflight(args.pdf),ensure_ascii=False,indent=2)
    if args.output:args.output.write_text(report,encoding="utf-8")
    print(report)
