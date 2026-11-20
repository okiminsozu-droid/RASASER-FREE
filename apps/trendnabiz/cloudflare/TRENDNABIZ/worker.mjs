/* TRENDNABIZ 0.3 | ephemeral, no archive, Cloudflare Workers + hibernating Durable Object.
   Never write news to D1, R2, KV, Cache or Durable Object storage.
   Public source metadata is factual; only source links are given. */
import { DurableObject } from "cloudflare:workers";

const TTL_MS = 180000;
const OUT_LIMIT = 40;
const headers = {
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store, max-age=0",
  "Pragma": "no-cache",
  "X-Robots-Tag": "noindex, nofollow",
  "X-Content-Type-Options": "nosniff"
};
const sources = {
  usgs: "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_hour.geojson",
  nasa: "https://eonet.gsfc.nasa.gov/api/v3/events?days=1&status=all&limit=100",
  noaa: "https://api.weather.gov/alerts/active",
  gdacs: "https://www.gdacs.org/xml/rss.xml",
  eu: "https://ec.europa.eu/commission/presscorner/api/rss?language=en"
};
const isoTime = raw => {
  if (typeof raw === "number") return raw > 1e11 ? raw : raw * 1000;
  const time = Date.parse(String(raw || ""));
  return Number.isFinite(time) ? time : 0;
};
const validRecent = (t, now) => Number.isFinite(t) && t > now - TTL_MS && t <= now + 30000;
const clip = (s, n=170) => String(s ?? "").replace(/\s+/g, " ").trim().slice(0,n);
const isHttps = url => {
  try { const u = new URL(url); return u.protocol === "https:" && !u.username && !u.password; }
  catch { return false; }
};
const news = (src, id, title, summary, url, issued, kind, priority) => ({
  id: src + ":" + clip(id,120), title:clip(title,200), summary:clip(summary,340),
  source:src, sourceUrl:url, issuedAt:issued, category:kind, priority, expiresAt:issued + TTL_MS
});
async function sourceJson(url, timeout=11000) {
  const result = await fetch(url, {
    headers: {"Accept":"application/json,application/geo+json","User-Agent":"TRENDNABIZ/0.3"},
    redirect:"error", signal: AbortSignal.timeout(timeout)
  });
  if (!result.ok) throw Error("source_unavailable");
  const length = Number(result.headers.get("content-length") || "0");
  if (length > 2000000) throw Error("source_too_large");
  const txt = await result.text();
  if (txt.length > 2000000) throw Error("source_too_large");
  return JSON.parse(txt);
}
function usgsParse(doc,now) {
  return (Array.isArray(doc.features) ? doc.features : []).slice(0,100).flatMap(f=>{
    const p=f.properties || {}, mag=Number(p.mag), t=isoTime(p.time);
    if (!validRecent(t,now) || !Number.isFinite(mag) || !String(f.id || "")) return [];
    const url=String(p.url || "");
    if (!url.startsWith("https://earthquake.usgs.gov/")) return [];
    const place=clip(p.place || "unreported location");
    return [news("USGS",f.id, "Earthquake M"+mag.toFixed(1)+": "+place,
      "USGS reported an earthquake near "+place+". Data may be revised; refer to local authorities.",
      url,t,"earthquake",mag>=6.5?"critical":mag>=5.5?"important":"normal")];
  });
}
function nasaParse(doc,now) {
  return (Array.isArray(doc.events)?doc.events:[]).slice(0,100).flatMap(x=>{
    const t=Math.max(0,...(Array.isArray(x.geometry)?x.geometry:[]).map(g=>isoTime(g.date)));
    if (!validRecent(t,now) || !x.id) return [];
    const cat=clip(x.categories?.[0]?.title || "natural event",45);
    const original=String(x.link || "");
    const url=original.startsWith("https://eonet.gsfc.nasa.gov/")?original:"https://eonet.gsfc.nasa.gov/";
    return [news("NASA EONET",x.id,clip(x.title),
      "NASA EONET lists a "+cat.toLowerCase()+" event. Check the official source for updates.",
      url,t,"natural_event","normal")];
  });
}
function noaaParse(doc,now) {
  return (Array.isArray(doc.features)?doc.features:[]).slice(0,200).flatMap(f=>{
    const p=f.properties||{}, t=isoTime(p.sent||p.effective), id=String(p.id||f.id||"");
    if (!validRecent(t,now) || !id || String(p.status||"Actual").toLowerCase()!=="actual") return [];
    const event=clip(p.event || "weather alert",90), area=clip(p.areaDesc||"US",90);
    const original=String(p["@id"]||p.id||"");
    const url=original.startsWith("https://api.weather.gov/")?original:"https://www.weather.gov/";
    const severity=String(p.severity||"").toLowerCase();
    return [news("NOAA/NWS",id,event+": "+area,
      "US National Weather Service issued a "+event+" for "+area+". Consult the issuing agency.",
      url,t,"weather",["extreme","severe"].includes(severity)?"critical":"important")];
  });
}

const decodeXml = text => clip(String(text||"")
  .replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g,"$1")
  .replace(/<[^>]*>/g," ")
  .replace(/&amp;/g,"&").replace(/&quot;/g,'"')
  .replace(/&apos;/g,"'").replace(/&lt;/g,"<").replace(/&gt;/g,">"),220);
function xmlField(fragment, tag) {
  const re=new RegExp("<"+tag+"(?:\\s[^>]*)?>([\\s\\S]*?)<\\/"+tag+">","i");
  return decodeXml(fragment.match(re)?.[1]||"");
}
async function sourceXml(url) {
  const result=await fetch(url,{headers:{"Accept":"application/rss+xml,application/xml,text/xml","User-Agent":"TRENDNABIZ/0.4"},redirect:"error",signal:AbortSignal.timeout(11000)});
  if(!result.ok) throw Error("rss_unavailable");
  const body=await result.text();
  if(body.length > 650000) throw Error("rss_too_large");
  return body;
}
function officialRss(xml, now, sourceName, allowedHosts, category) {
  if(!xml.includes("<rss") && !xml.includes("<channel")) return [];
  const parts=[...xml.matchAll(/<item\b[^>]*>([\s\S]*?)<\/item>/gi)].slice(0,80);
  return parts.flatMap((match)=>{
    const item=match[1], title=xmlField(item,"title"), link=xmlField(item,"link");
    const issued=isoTime(xmlField(item,"pubDate")||xmlField(item,"dc:date")||xmlField(item,"updated"));
    if(!validRecent(issued,now)||!title||!isHttps(link))return [];
    const host=new URL(link).hostname.toLowerCase();
    if(!allowedHosts.some(s=>host===s||host.endsWith("."+s)))return [];
    const id=xmlField(item,"guid")||link;
    const critical=sourceName==="GDACS" && /^red\b/i.test(title);
    return [news(sourceName,id,title,
      sourceName+" published a new "+category+" notice. Refer to the original source for details.",
      link,issued,category,critical?"critical":"important")];
  });
}

export class LiveHub extends DurableObject {
  constructor(ctx,env) { super(ctx,env); this.seen = new Map(); }
  async fetch(request) {
    const url = new URL(request.url);
    if(url.pathname === "/probe") return Response.json({hub:"ready",activeSockets:this.ctx.getWebSockets().length,archive:false},{headers:{"Cache-Control":"no-store"}});
    if(url.pathname === "/ws") {
      if(request.headers.get("Upgrade")?.toLowerCase() !== "websocket") return new Response("WebSocket required",{status:426,headers});
      const pair = new WebSocketPair(), client=pair[0], server=pair[1];
      this.ctx.acceptWebSocket(server);
      server.send(JSON.stringify({type:"hello",serverAt:Date.now(),history:false}));
      return new Response(null,{status:101,webSocket:client});
    }
    if(url.pathname === "/dispatch" && request.method==="POST") {
      const batch = await request.json();
      const now=Date.now(), sent=[];
      for(const [key,until] of this.seen) if(until<=now) this.seen.delete(key);
      for(const item of (Array.isArray(batch)?batch:[]).slice(0,OUT_LIMIT)) {
        if(!item || !validRecent(item.issuedAt,now) || !isHttps(item.sourceUrl)) continue;
        if(this.seen.has(item.id)) continue;
        this.seen.set(item.id,now+TTL_MS); // ID only, never store news text.
        const message=JSON.stringify({type:"event",...item});
        for(const socket of this.ctx.getWebSockets()) {
          try { socket.send(message); }
          catch { try{socket.close(1011,"delivery_error");}catch{} }
        }
        sent.push(item.id);
      }
      return Response.json({accepted:sent.length},{headers:{"Cache-Control":"no-store"}});
    }
    return new Response("Not found",{status:404,headers});
  }
  webSocketMessage(socket,message) {
    // No historic event retrieval or user-published news; a self-test echoes ONLY to requesting socket.
    if(message==="ping") { socket.send(JSON.stringify({type:"pong",at:Date.now()})); return; }
    if(message==="self_test") {
      const now=Date.now();
      if(!this.selfTests) this.selfTests=new WeakMap();
      if(now-(this.selfTests.get(socket)||0)<30000) return;
      this.selfTests.set(socket,now);
      socket.send(JSON.stringify({
        type:"event",id:"SELFTEST:"+now,title:"TRENDNABIZ • Cloudflare TEST",
        summary:"Bu gercek bir haber degildir. Cloudflare canli baglantisindan gonderilen test bildirimidir.",
        source:"TRENDNABIZ TEST",sourceUrl:"https://trendnabiz-engine.okiminsozu.workers.dev/healthz",
        issuedAt:now,category:"diagnostic",priority:"normal",expiresAt:now+TTL_MS
      }));
    }
  }
  webSocketClose() {}
  webSocketError() {}
}
export default {
  async fetch(request,env) {
    const url=new URL(request.url);
    if(request.method !== "GET") return new Response("Method not allowed",{status:405,headers});
    if(url.pathname === "/healthz") return new Response(JSON.stringify({
      app:"TRENDNABIZ",engine:"ephemeral-live",version:"0.4.0",ttlSeconds:180,
      archive:false,newsHistory:false,liveConnectionOnly:true,pushBackgroundConnected:false,
      deployedSources:["USGS","NASA EONET","NOAA/NWS","GDACS","European Commission"]
    }),{headers});
    if(url.pathname === "/v1/probe") {
      try {
        const response=await env.LIVE_HUB.get(env.LIVE_HUB.idFromName("world-live")).fetch("https://internal/probe");
        return Response.json({gateway:"active",hubReady:response.status===200,newsArchive:false},{status:response.status===200?200:503,headers:{"Cache-Control":"no-store"}});
      } catch(_) {
        return Response.json({gateway:"active",hubStatus:503,archive:false},{status:503,headers:{"Cache-Control":"no-store"}});
      }
    }
    if(url.pathname === "/v1/live") {
      if(request.headers.get("Upgrade")?.toLowerCase() !== "websocket") return new Response("WebSocket required",{status:426,headers});
      const id=env.LIVE_HUB.idFromName("world-live");
      return env.LIVE_HUB.get(id).fetch(new Request(new URL("/ws",url.origin),request));
    }
    return new Response("Not found",{status:404,headers});
  },
  async scheduled(event,env,ctx) {
    const time=Date.now(),minute=new Date(time).getUTCMinutes();
    const chosen=[["USGS",sources.usgs,usgsParse]];
    if(minute%3===0) chosen.push(["NOAA",sources.noaa,noaaParse]);
    if(minute%5===0) chosen.push(["NASA",sources.nasa,nasaParse]);
    const fetched=await Promise.allSettled(chosen.map(async ([label,url,fn])=>fn(await sourceJson(url),time)));
    if(minute%6===0) fetched.push(...await Promise.allSettled([sourceXml(sources.gdacs).then(xml=>officialRss(xml,time,"GDACS",["gdacs.org"],"disaster"))]));
    if(minute%5===0) fetched.push(...await Promise.allSettled([sourceXml(sources.eu).then(xml=>officialRss(xml,time,"European Commission",["europa.eu"],"world"))]));
    const batch=fetched.flatMap(x=>x.status==="fulfilled"?x.value:[]).slice(0,OUT_LIMIT);
    if(!batch.length) return;
    // News exists only in this request's local memory and websocket frames, not persistent storage.
    const id=env.LIVE_HUB.idFromName("world-live");
    await env.LIVE_HUB.get(id).fetch("https://internal/dispatch",{
      method:"POST",headers:{"Content-Type":"application/json"},
      body:JSON.stringify(batch)
    });
  }
};
