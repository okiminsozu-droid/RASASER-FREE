"""Source adapters. Only public factual data and explicitly approved feeds may be published."""
from __future__ import annotations
import json
import re
import ssl
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from urllib.parse import urlparse
from .events import Event, short, stable_id, timestamp

USER_AGENT = "TRENDNABIZ/0.2 (contact: webmaster@okiminsozu.com; source attribution included)"
MAX_BYTES = 4_000_000

def fetch_bytes(url: str, max_bytes: int = MAX_BYTES) -> bytes:
    parsed = urlparse(url)
    if parsed.scheme != "https" or not parsed.hostname:
        raise ValueError("Source URL must be HTTPS")
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/json, application/geo+json, application/xml, text/xml"})
    with urllib.request.urlopen(req, timeout=18, context=ssl.create_default_context()) as response:
        if urlparse(response.url).scheme != "https":
            raise ValueError("Unencrypted redirect rejected")
        content = response.read(max_bytes + 1)
        if len(content) > max_bytes:
            raise ValueError("Source payload exceeds configured byte limit")
        return content

def fetch_json(url: str) -> dict:
    obj = json.loads(fetch_bytes(url).decode("utf-8"))
    if not isinstance(obj, dict):
        raise ValueError("Expected JSON object")
    return obj

def usgs_events(doc: dict, now: int) -> list[Event]:
    result = []
    for feature in doc.get("features", [])[:100]:
        props = feature.get("properties") or {}
        raw_id = str(feature.get("id") or "")
        mag = props.get("mag")
        if not raw_id or not isinstance(mag, (float, int)) or isinstance(mag, bool):
            continue
        title = short(props.get("place") or "Unknown location", 160)
        link = props.get("url") or ""
        published = timestamp(props.get("time"))
        if not link.startswith("https://earthquake.usgs.gov/") or published <= 0:
            continue
        if published > now + 300 or published < now - 2 * 86400:
            continue
        severity = "critical" if mag >= 6.5 else "important" if mag >= 5.5 else "normal"
        result.append(Event(
            id=stable_id("usgs", raw_id),
            title=f"Earthquake M{mag:.1f}: {title}",
            summary=f"USGS reported magnitude {mag:.1f} near {title}. This record can be revised. Consult local authorities for safety instructions.",
            source="USGS", url=link, published_at=published,
            category="earthquake", severity=severity, region=title
        ))
    return result

def eonet_events(doc: dict, now: int) -> list[Event]:
    result = []
    for event in doc.get("events", [])[:150]:
        raw_id = str(event.get("id") or "")
        if not raw_id:
            continue
        geometry = event.get("geometry") or []
        event_time = max((timestamp(g.get("date")) for g in geometry if isinstance(g, dict)), default=0)
        if not event_time or event_time > now + 300 or event_time < now - 7 * 86400:
            continue
        cats = [c.get("id", "") for c in event.get("categories", []) if isinstance(c, dict)]
        category = str(cats[0]) if cats else "natural_event"
        title = short(event.get("title") or category, 180)
        # NASA curated metadata: preserve its factual event label, not linked publishers' text/media.
        url = event.get("link", "")
        if not isinstance(url, str) or not url.startswith("https://eonet.gsfc.nasa.gov/"):
            url = "https://eonet.gsfc.nasa.gov/"
        result.append(Event(
            id=stable_id("nasa", raw_id),
            title=title,
            summary=f"NASA EONET lists this {category} event. Event status and location may change; see the original NASA record.",
            source="NASA EONET", url=url, published_at=event_time,
            category=short(category, 45), severity="normal"
        ))
    return result

def noaa_events(doc: dict, now: int) -> list[Event]:
    result = []
    for feature in doc.get("features", [])[:250]:
        p = feature.get("properties") or {}
        if str(p.get("status") or "Actual").lower() != "actual":
            continue
        raw_id = str(p.get("id") or feature.get("id") or "")
        published = timestamp(p.get("sent") or p.get("effective") or p.get("onset"))
        if not raw_id or not published or published > now + 300 or published < now - 3 * 86400:
            continue
        event_type = short(p.get("event") or "Weather alert", 100)
        area = short(p.get("areaDesc") or "US", 125)
        severity_label = str(p.get("severity") or "").lower()
        severity = "critical" if severity_label in ("extreme", "severe") else "important"
        url = str(p.get("@id") or p.get("id") or "")
        if not url.startswith("https://api.weather.gov/"):
            url = "https://www.weather.gov/"
        result.append(Event(
            id=stable_id("noaa", raw_id), title=f"{event_type}: {area}",
            summary=f"US National Weather Service issued a {event_type.lower()} alert for {area}. Check the issuing authority for instructions.",
            source="NOAA/NWS", url=url, published_at=published,
            category="weather", severity=severity, region=area
        ))
    return result

def approved_feed_events(xml: bytes, entry: dict, now: int) -> list[Event]:
    """No unapproved articles: both source-specific license flags must be explicit."""
    if not (entry.get("enabled") is True
        and entry.get("permission_recorded") is True
        and entry.get("title_and_summary_reuse_allowed") is True):
        return []
    name = short(entry.get("name") or "", 80)
    if not name:
        return []
    root = ET.fromstring(xml)
    entries = root.findall("./channel/item")  # RSS 2.0
    if not entries:
        entries = root.findall("{http://www.w3.org/2005/Atom}entry")  # Atom
    result = []
    for item in entries[:50]:
        def field(rss_tag: str, atom_tag: str) -> str:
            node = item.find(rss_tag)
            if node is None: node = item.find("{http://www.w3.org/2005/Atom}"+atom_tag)
            return short(node.text if node is not None else "", 400)
        title = field("title", "title")
        link = field("link", "link")
        if not link:
            atom_link = item.find("{http://www.w3.org/2005/Atom}link")
            if atom_link is not None:
                link = atom_link.attrib.get("href") or ""
        identifier = field("guid", "id") or link
        date_text = field("pubDate", "updated") or field("published", "published")
        time = timestamp(date_text)
        if not time and date_text:
            try: time = int(parsedate_to_datetime(date_text).timestamp())
            except (TypeError, ValueError, OverflowError): pass
        if not title or not identifier or not link.startswith("https://") or not time:
            continue
        if time > now + 300 or time < now - 2 * 86400:
            continue
        # External link/domain cannot carry tokens into user's device.
        result.append(Event(
            id=stable_id("rss"+re.sub("[^a-z0-9]", "", name.lower())[:18], identifier),
            title=title[:280],
            summary=short(field("description", "summary"), 500) or f"{name} published this update.",
            source=name, url=link, published_at=time,
            category=short(entry.get("category") or "world", 45),
            severity="normal"
        ))
    return result

def provider_specs() -> dict:
    return {
        "usgs": ("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson", usgs_events),
        "nasa": ("https://eonet.gsfc.nasa.gov/api/v3/events?days=7&status=all&limit=100", eonet_events),
        "noaa": ("https://api.weather.gov/alerts/active", noaa_events),
    }
