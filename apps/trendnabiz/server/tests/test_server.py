"""Offline deterministic tests for TRENDNABIZ ingestion and attribution."""
import json
import tempfile
import unittest
from pathlib import Path

from server.events import Event
from server.feeds import approved_feed_events, eonet_events, noaa_events, usgs_events
from server.store import Store

NOW = 1800000000

def example(event_id="a",category="earthquake",severity="critical"):
    return Event(event_id, "Important event", "Event from a public primary source.",
                 "Official source", "https://example.org/source", NOW-10,
                 category, severity)

class StoreTests(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.store = Store(str(Path(self.dir.name)/"test.sqlite3"))
    def tearDown(self):
        self.dir.cleanup()
    def test_first_sync_silent_subsequent_is_deduplicated(self):
        one = example("one")
        self.assertEqual(self.store.ingest("usgs",[one],NOW)["queued"],0)
        self.assertTrue(self.store.ingest("usgs",[one],NOW)["new"] == 0)
        two = example("two")
        self.assertEqual(self.store.ingest("usgs",[one,two],NOW)["queued"],1)
        self.assertEqual(len(self.store.pending(NOW)),1)
        self.store.mark_delivery(two.id,True,NOW)
        self.assertEqual(self.store.pending(NOW),[])
    def test_independent_source_baselines(self):
        self.store.ingest("nasa",[example("one")],NOW)
        self.assertTrue(self.store.ingest("usgs",[example("two")],NOW)["baseline"])
    def test_outdated_event_not_sent_as_fresh_push(self):
        self.store.ingest("nasa",[],NOW)
        old=Event("old","Old","Prior report","NASA","https://example.org/old",
                  NOW-86400,"wildfire","important")
        self.assertEqual(self.store.ingest("nasa",[old],NOW)["queued"],0)
    def test_retry_backoff(self):
        self.store.ingest("usgs",[],NOW)
        self.store.ingest("usgs",[example("x")],NOW)
        self.store.mark_delivery("x",False,NOW)
        self.assertEqual(self.store.pending(NOW),[])
        self.assertEqual(len(self.store.pending(NOW+45)),1)
    def test_created_since(self):
        self.store.ingest("usgs",[example("b")],NOW)
        self.assertEqual(len(self.store.created_since(NOW)),1)

class FeedTests(unittest.TestCase):
    def test_usgs(self):
        doc={"features":[{"id":"test","properties":{
             "mag":7.1,"place":"Test region", "time":NOW*1000,
             "url":"https://earthquake.usgs.gov/earthquakes/eventpage/test"}}]}
        events=usgs_events(doc,NOW)
        self.assertEqual(len(events),1)
        self.assertEqual(events[0].severity,"critical")
        self.assertEqual(events[0].source,"USGS")
    def test_eonet(self):
        doc={"events":[{"id":"test","title":"Wildfire","categories":[{"id":"wildfires"}],
             "geometry":[{"date":"2027-01-15T00:00:00Z"}],"link":"https://eonet.gsfc.nasa.gov/api/v3/events/test"}]}
        # Test with controlled time near published timestamp.
        from server.events import timestamp
        now=timestamp("2027-01-15T01:00:00Z")
        events=eonet_events(doc,now)
        self.assertEqual(events[0].category,"wildfires")
        self.assertEqual(events[0].severity,"normal")
    def test_noaa_official_status(self):
        doc={"features":[{"properties":{
           "status":"Actual","id":"https://api.weather.gov/alerts/test",
           "event":"Tornado Warning","areaDesc":"Test Area",
           "severity":"Extreme","sent":"2027-01-15T00:00:00Z"}}]}
        from server.events import timestamp
        events=noaa_events(doc,timestamp("2027-01-15T01:00:00Z"))
        self.assertEqual(events[0].severity,"critical")
    def test_rss_license_gate(self):
        xml=b"""<rss><channel><item><title>Permitted report</title>
        <link>https://example.org/article</link><guid>abc</guid>
        <pubDate>Fri, 15 Jan 2027 01:00:00 GMT</pubDate>
        <description>Authorized summary.</description></item></channel></rss>"""
        from server.events import timestamp
        now=timestamp("2027-01-15T01:02:00Z")
        conf={"name":"Authorized feed","enabled":True,"permission_recorded":True,
              "title_and_summary_reuse_allowed":True}
        self.assertEqual(approved_feed_events(xml,{**conf,"permission_recorded":False},now),[])
        events=approved_feed_events(xml,conf,now)
        self.assertEqual(len(events),1)
        self.assertEqual(events[0].title,"Permitted report")
    def test_event_validation(self):
        with self.assertRaises(ValueError):
            Event("x","Test","Desc","source","http://insecure.test",NOW,"world")
        with self.assertRaises(ValueError):
            Event("x","Test","Desc","source","https://example.org",NOW,"world","unverified")

if __name__=="__main__":
    unittest.main()
