"""Rewrites build/unsigned.apk with forward-slash entry names (aapt2 on Windows writes assets/www\\x) and adds classes.dex.
resources.arsc stays uncompressed, as Android 11+ requires; zipalign runs afterwards."""
import os, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
src = os.path.join(HERE, "build", "unsigned.apk")
dst = os.path.join(HERE, "build", "repacked.apk")
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w") as zout:
    for info in zin.infolist():
        name = info.filename.replace("\\", "/")
        data = zin.read(info.filename)
        stored = name == "resources.arsc" or name.endswith(".png")
        zout.writestr(zipfile.ZipInfo(name, date_time=(2026, 1, 1, 0, 0, 0)), data,
                      compress_type=zipfile.ZIP_STORED if stored else zipfile.ZIP_DEFLATED)
    zout.write(os.path.join(HERE, "build", "classes.dex"), "classes.dex", compress_type=zipfile.ZIP_DEFLATED)
print("repacked", dst)
