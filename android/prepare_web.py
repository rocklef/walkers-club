"""Copies ../index.html into assets/www and makes it work offline: Google Fonts and the Motion library are downloaded and bundled."""
import os, re, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
WWW = os.path.join(HERE, "assets", "www")
FONTS = os.path.join(WWW, "fonts")
os.makedirs(FONTS, exist_ok=True)
UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()

html = open(os.path.join(HERE, "..", "index.html"), encoding="utf-8").read()

# fonts
font_link = re.search(r'<link rel="stylesheet" href="(https://fonts\.googleapis\.com/css2[^"]+)">', html)
css = get(font_link.group(1).replace("&amp;", "&")).decode("utf-8")
n = 0
def localise(m):
    global n
    url = m.group(1)
    name = f"f{n:02d}.woff2"; n += 1
    path = os.path.join(FONTS, name)
    if not os.path.exists(path):
        open(path, "wb").write(get(url))
    return f"url(fonts/{name})"
css = re.sub(r"url\((https://fonts\.gstatic\.com/[^)]+)\)", localise, css)
open(os.path.join(WWW, "fonts.css"), "w", encoding="utf-8").write(css)
html = html.replace(font_link.group(0), '<link rel="stylesheet" href="fonts.css">')
html = re.sub(r'<link rel="preconnect"[^>]*>\n?', "", html)

# app feel: no pinch / double-tap zoom
html = html.replace('content="width=device-width, initial-scale=1, viewport-fit=cover"', 'content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover"', 1)
html = html.replace("<style>", "<style>\nhtml{touch-action:manipulation; -webkit-tap-highlight-color:transparent}", 1)

# motion
motion = re.search(r'<script src="(https://cdn\.jsdelivr\.net/npm/motion@[^"]+)"></script>', html)
mpath = os.path.join(WWW, "motion.js")
if not os.path.exists(mpath):
    open(mpath, "wb").write(get(motion.group(1)))
html = html.replace(motion.group(0), '<script src="motion.js"></script>')

open(os.path.join(WWW, "index.html"), "w", encoding="utf-8").write("<!doctype html>\n<html lang=\"en\">\n" + html + "\n</html>\n")
print(f"web bundle ok: {n} font files, motion.js, index.html")
