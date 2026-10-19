# -*- coding: utf-8 -*-
"""
水果识别 —— 供 Java 调用的版本（方案 D）

与 predict.py 的区别：**stdout 只输出一行 JSON**，方便 Java 用 ProcessBuilder 直接解析。
predict.py 保留不动，是给人看的版本；这个是给机器看的版本。

用法：
    python predict_json.py 图片路径

成功输出：
    {"success": true, "file": "...", "model": "mobilenet_v3_small",
     "cost_ms": 512, "results": [
        {"rank":1, "class_index":954, "name_en":"banana", "name_cn":"香蕉", "confidence":0.8734},
        ... 共 5 条 ...]}

失败输出：
    {"success": false, "error": "找不到图片：xxx"}
"""
import sys
import os
import io
import json
import time

# Windows 下 cmd 默认 GBK，强制 UTF-8，否则中文会乱码
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

import torch
from torchvision.models import mobilenet_v3_small, MobileNet_V3_Small_Weights
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT = os.path.join(HERE, "images", "test.jpg")
TOP_K = 5

# ImageNet 里与水果有关的类别 → 中文（其余保持英文）
ZH = {
    "Granny Smith": "青苹果", "orange": "橙子", "lemon": "柠檬",
    "banana": "香蕉", "strawberry": "草莓", "pineapple": "菠萝",
    "fig": "无花果", "pomegranate": "石榴", "jackfruit": "菠萝蜜",
    "custard apple": "释迦果",
}


def fail(msg):
    """失败时也必须输出合法 JSON，Java 端才不会解析崩溃"""
    print(json.dumps({"success": False, "error": msg}, ensure_ascii=False))
    sys.exit(0)


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT

    if not os.path.exists(path):
        fail(f"找不到图片：{path}")

    t0 = time.time()
    try:
        weights = MobileNet_V3_Small_Weights.DEFAULT
        model = mobilenet_v3_small(weights=weights)
        model.eval()

        img = Image.open(path).convert("RGB")
        batch = weights.transforms()(img).unsqueeze(0)

        with torch.no_grad():
            probs = model(batch).squeeze(0).softmax(0)

        cats = weights.meta["categories"]
        results = []
        for rank, idx in enumerate(probs.topk(TOP_K).indices.tolist(), start=1):
            name_en = cats[idx]
            results.append({
                "rank": rank,
                "class_index": int(idx),
                "name_en": name_en,
                "name_cn": ZH.get(name_en, ""),
                "confidence": round(float(probs[idx]), 4),
            })
    except Exception as e:
        fail(f"识别失败：{type(e).__name__}: {e}")
        return

    print(json.dumps({
        "success": True,
        "file": path,
        "model": "mobilenet_v3_small",
        "cost_ms": int((time.time() - t0) * 1000),
        "results": results,
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
