#!/usr/bin/env python3
"""Trusted, SHA256-verified multipart transfer from GitHub Releases to R2.
The Cloudflare Worker checks GitHub OIDC JWT signature and branch before writing.
No S3 keys, tokens or bucket secrets are persisted in this repository.
"""
import hashlib, json, os, pathlib, time, urllib.request, urllib.error, urllib.parse

ENDPOINT = "https://rasaser-nav-r2-import-probe.okiminsozu.workers.dev"
AUDIENCE = "rasaser-navigasyon-r2-transfer-v1"
BASE = "https://github.com/okiminsozu-droid/RASASER-FREE/releases/download"
SOURCES = {
    "rasaser-turkiye-shortbread.zip": ("rasaser-turkiye-maps-v0.1",605992151,"c7d8b277a916ffd4ec7839c7c0420104c19b88fcfcea456be7078346003bacfc"),
    "valhalla_tiles.tar": ("rasaser-turkiye-routing-v0.1",1148661760,"bdfd9a67d8fdd0d5856013e889c6e48cd8a14560934cc5d2b6f3c16a8f6a554f"),
    "turkey-places.zip": ("rasaser-turkiye-search-v0.1",66446365,"048f474b1ff427ac2310b5201a283d027d87ec769ca7e808254ffbbdf6b603f0"),
    "valhalla-turkiye-manifest.json": ("rasaser-turkiye-routing-v0.1",657,"ce663429563e556ac78e729e8d919b72993f587a2b8426e5d556007022924f26"),
    "valhalla.json": ("rasaser-turkiye-routing-v0.1",9598,"bc0bb95856c09c51b859bb2f18423b43a0353504bb8e26529f3aad71bbf98fc3"),
    "turkey.manifest.json": ("rasaser-turkiye-maps-v0.1",1048,"b588343ca2ad3b51dbd76f36e57009e58d8712e9fcb454c5e6f7b4673ce8cd46"),
    "turkey-places-manifest.json": ("rasaser-turkiye-search-v0.1",382,"1415b1c76a898e46da40691ededec05db3e1f75dfa870c8557fb662a1e49da01"),
}
DATA_DIR=pathlib.Path(os.environ.get("RUNNER_TEMP","/tmp")) / "rasaser-verified"
DATA_DIR.mkdir(parents=True,exist_ok=True)
cached_token,token_time="",0.0
def get_token():
    global cached_token,token_time
    if cached_token and time.time()-token_time < 45:
        return cached_token
    addr=os.environ["ACTIONS_ID_TOKEN_REQUEST_URL"] + "&audience="+urllib.parse.quote(AUDIENCE)
    request=urllib.request.Request(addr,headers={"Authorization":"Bearer "+os.environ["ACTIONS_ID_TOKEN_REQUEST_TOKEN"]})
    with urllib.request.urlopen(request,timeout=30) as response:
        payload=json.load(response)
    cached_token=payload["value"]
    token_time=time.time()
    return cached_token

def api(method,route,file,data=None,extra=None,attempts=4):
    url=ENDPOINT+route+"?file="+urllib.parse.quote(file)
    headers={"Authorization":"Bearer "+get_token(),"Accept":"application/json","User-Agent":"curl/8.12.1"}
    if extra:headers.update(extra)
    if isinstance(data,(dict,list)):
        data=json.dumps(data,separators=(",",":")).encode()
        headers["Content-Type"]="application/json"
    for n in range(attempts):
        try:
            request=urllib.request.Request(url,data=data,headers=headers,method=method)
            with urllib.request.urlopen(request,timeout=180) as res:
                return json.load(res)
        except (urllib.error.HTTPError,urllib.error.URLError,TimeoutError) as exc:
            if isinstance(exc,urllib.error.HTTPError):
                diagnostic=exc.read(400).decode("utf-8","replace")
                if exc.code in (400,401,403,404,413,422):
                    raise RuntimeError(f"{method} {route} {file}: HTTP {exc.code}, {diagnostic}") from exc
            if n+1==attempts:raise RuntimeError(f"{method} {route} {file} failed after retries: {exc}") from exc
            time.sleep(min(4*(n+1),15))
            headers["Authorization"]="Bearer "+get_token()

def download_and_verify(filename):
    tag,expected_size,expected_sha=SOURCES[filename]
    file=DATA_DIR / filename
    if not file.exists() or file.stat().st_size!=expected_size:
        url=BASE+"/"+tag+"/"+filename
        print(f"Downloading verified release: {filename}",flush=True)
        for n in range(4):
            try:
                req=urllib.request.Request(url,headers={"User-Agent":"RaSaSeR-GitHub-Release-R2-Transfer"})
                with urllib.request.urlopen(req,timeout=90) as src, file.open("wb") as dst:
                    while True:
                        chunk=src.read(4*1024*1024)
                        if not chunk: break
                        dst.write(chunk)
                break
            except Exception as exc:
                file.unlink(missing_ok=True)
                if n==3:raise
                print(f"Transient download error: {type(exc).__name__}, retrying",flush=True)
                time.sleep(5)
    if file.stat().st_size!=expected_size:raise RuntimeError(f"File size mismatch: {filename}")
    sha=hashlib.sha256()
    with file.open("rb") as f:
        for piece in iter(lambda:f.read(8*1024*1024),b""):
            sha.update(piece)
    if sha.hexdigest()!=expected_sha:raise RuntimeError(f"SHA256 verification FAILED: {filename}")
    print(f"Verified source {filename}: {expected_size} bytes; sha256={expected_sha}",flush=True)
    return file

def copy_file(filename):
    file=download_and_verify(filename)
    state=api("POST","/init",filename,data={})
    if state.get("skip"):
        print(f"Already verified in R2: {filename}",flush=True)
        return
    upload_id=state["uploadId"]
    part_bytes=int(state["partBytes"])
    pieces=[]
    with file.open("rb") as stream:
        number=0
        while True:
            chunk=stream.read(part_bytes)
            if not chunk:break
            number+=1
            sha=hashlib.sha256(chunk).hexdigest()
            query="&uploadId="+urllib.parse.quote(upload_id)+"&partNumber="+str(number)
            info=api("PUT","/part",filename,data=chunk,
                     extra={"X-RASASER-PART-SHA256":sha,
                            "Content-Type":"application/octet-stream",
                            "Content-Length":str(len(chunk))},
                     attempts=4)
            if info.get("partNumber")!=number or not info.get("etag"):
                raise RuntimeError(f"Invalid R2 response for {filename} part {number}")
            pieces.append(info)
            print(f"Uploaded {filename} part {number}: {len(chunk)} bytes",flush=True)
    completed=api("POST","/complete",filename,data={"uploadId":upload_id,"parts":pieces})
    if completed.get("size")!=SOURCES[filename][1]:
        raise RuntimeError("Post-commit size mismatch for "+filename)
    print(f"R2 stored and verified: {filename}",flush=True)
    file.unlink()
    state=api("POST","/init",filename,data={})
    if not state.get("skip"):
        raise RuntimeError("Final R2 metadata verification failed: "+filename)

def main():
    req=urllib.request.Request(ENDPOINT+"/health",headers={"User-Agent":"curl/8.12.1"})
    with urllib.request.urlopen(req,timeout=25) as res:
        print("Worker health:",res.read(400).decode(),flush=True)
    for filename in SOURCES:
        copy_file(filename)
    print("ALL SEVEN VERIFIED TURKEY NAVIGATION FILES UPLOADED TO CLOUDFLARE R2.",flush=True)
if __name__=="__main__":main()
