import gzip
import json
import os
from pathlib import Path

from google.protobuf import json_format

import index_pb2

OWNER = "saefulrohman"
REPO = "junji-ito-mihon-extension"
ROOT = Path(__file__).resolve().parents[1]
INFO = ROOT / "release-files" / "keiyoushi-source-info.json"
FINGERPRINT = os.environ["SIGNING_FINGERPRINT"].replace(":", "").replace(" ", "").lower()
TAG = os.environ["RELEASE_TAG"]
APK_NAME = Path(os.environ["APK_NAME"]).name
JAR_NAME = Path(os.environ["JAR_NAME"]).name

if len(FINGERPRINT) != 64:
    raise SystemExit("Expected a 64-character SHA-256 signing certificate fingerprint")

metadata = json.loads(INFO.read_text(encoding="utf-8"))
sources = [
    index_pb2.Source(
        id=int(source["id"]),
        name=source["name"],
        language=source["lang"],
        homeUrl=source["baseUrl"],
        mirrorUrls=source.get("mirrorUrls", []),
    )
    for source in metadata["sources"]
]

extension = index_pb2.Extension(
    name=metadata["name"],
    packageName=metadata["packageName"],
    resources=index_pb2.Resources(
        apkUrl=f"https://github.com/{OWNER}/{REPO}/releases/download/{TAG}/{APK_NAME}",
        jarUrl=f"https://github.com/{OWNER}/{REPO}/releases/download/{TAG}/{JAR_NAME}",
        iconUrl="https://junjiitomangaindonesia.blogspot.com/favicon.ico",
    ),
    extensionLib=metadata["extensionLib"],
    versionCode=int(metadata["versionCode"]),
    versionName=metadata["versionName"],
    contentWarning=int(metadata["contentWarning"]),
    sources=sources,
)

index = index_pb2.Index(
    name="Saeful's Mihon Extensions",
    badgeLabel="SAEFUL",
    signingKey=FINGERPRINT,
    contact=index_pb2.Contact(website=f"https://github.com/{OWNER}/{REPO}"),
    extensionList=index_pb2.ExtensionList(extensions=[extension]),
)

(ROOT / "index.json").write_text(
    json_format.MessageToJson(
        index,
        always_print_fields_with_no_presence=True,
        preserving_proto_field_name=True,
    ) + "\n",
    encoding="utf-8",
)
(ROOT / "index.pb").write_bytes(
    gzip.compress(index.SerializeToString(deterministic=True), mtime=0)
)
(ROOT / "repo.json").write_text(
    json.dumps(
        {
            "index_v2": f"https://raw.githubusercontent.com/{OWNER}/{REPO}/main/index.pb",
            "meta": {
                "name": "Saeful's Mihon Extensions",
                "website": f"https://github.com/{OWNER}/{REPO}",
                "signingKeyFingerprint": FINGERPRINT,
            },
        },
        indent=2,
    ) + "\n",
    encoding="utf-8",
)
print(f"Generated Mihon index for {metadata['name']} ({metadata['packageName']})")
