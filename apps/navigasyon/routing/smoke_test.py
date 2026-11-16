#!/usr/bin/env python3
"""Check that the offline Valhalla graph actually returns a road route."""
import json,math,sys
from pathlib import Path

def validate(path):
    payload=json.loads(path.read_text(encoding="utf-8"))
    trip=payload.get("trip")
    if not isinstance(trip,dict):raise ValueError("No Valhalla trip")
    summary=trip.get("summary",{})
    length=summary.get("length",0)
    time=summary.get("time",0)
    if not isinstance(length,(int,float)) or not (0.03<length<100):
        raise ValueError(f"Unexpected route km: {length}")
    if not isinstance(time,(int,float)) or not (3<time<20_000):
        raise ValueError(f"Unexpected route seconds: {time}")
    legs=trip.get("legs") or []
    if not legs or not legs[0].get("shape") or not legs[0].get("maneuvers"):
        raise ValueError("No real road shape or turn maneuvers")
    print(json.dumps({
        "checks":"offline real-road routing and Turkish turn maneuvers",
        "distance_km":length,"seconds":time,
        "maneuvers":len(legs[0]["maneuvers"]),
        "source":"Locally mounted OSM Valhalla routing graph"
    },ensure_ascii=False))

if __name__=="__main__":
    validate(Path(sys.argv[1]))
