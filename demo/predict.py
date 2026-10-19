# -*- coding: utf-8 -*-
"""
水果识别 demo —— 第一步：不训练，直接用别人训练好的模型，看它能不能认出水果。
用法：
    python predict.py 图片路径
    python predict.py            (不给路径就默认用 images/test.jpg)
    （或者把图片拖到「双击运行.bat」上松手）
作用：只输出前 5 个最可能的类别和概率。这一步的目的不是准确率，
     是让你亲眼看到「图片进去 → 结果出来」。
"""
import sys
import os

import torch
from torchvision.models import mobilenet_v3_small, MobileNet_V3_Small_Weights
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT = os.path.join(HERE, "images", "test.jpg")

# ImageNet 里和水果有关的类别，中文对照（其余的模型也给英文名）
ZH = {
    "Granny Smith": "青苹果",
    "orange": "橙子",
    "lemon": "柠檬",
    "banana": "香蕉",
    "strawberry": "草莓",
    "pineapple": "菠萝",
    "fig": "无花果",
    "pomegranate": "石榴",
    "jackfruit": "菠萝蜜",
    "custard apple": "释迦果",
    "pizza": "披萨",
}


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT
    if not os.path.exists(path):
        print(f"找不到图片：{path}")
        print("换个办法：把图片直接拖到「双击运行.bat」上松手，就能识别它")
        return

    # 下载/加载预训练模型（第一次跑会下 ~10MB 权重，之后就不再下了）
    weights = MobileNet_V3_Small_Weights.DEFAULT
    model = mobilenet_v3_small(weights=weights)
    model.eval()

    img = Image.open(path).convert("RGB")
    preprocess = weights.transforms()
    batch = preprocess(img).unsqueeze(0)

    with torch.no_grad():
        out = model(batch).squeeze(0)
        probs = out.softmax(0)

    print(f"图片：{path}")
    print("模型认为（前 5 个可能）：")
    for i in probs.topk(5).indices.tolist():
        name = weights.meta["categories"][i]
        print(f"    {probs[i]:6.1%}  {name}  {ZH.get(name, '')}")


if __name__ == "__main__":
    main()
