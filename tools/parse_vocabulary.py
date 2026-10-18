#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
解析《高频词汇总.txt》 → app/src/main/assets/words.json + 词库解析报告。

容错策略（与 D:\app-word_dictionary 计划一致）：
- 制表符分隔英文与中文释义；无制表符/无法切分的行标记为 raw 原样保留，不丢词条
- 提取词性标注：v./n./a./ad./prep./conj./pron./num./art./vt./vi./adj./adv. 及组合（n./v.）
- 兼容中文词性标注：(介词)（动词）等，映射为英文缩写
- 提取 → 连接的派生词链为 wordForms（本版源文件无 → 链，逻辑保留供后续词库复用）
- 多义词 ①②③ 标注原样保留在释义中，不拆分
- 每条记录保留 raw 原始行，便于追溯

用法:
    python tools/parse_vocabulary.py [输入.txt] [输出.json] [报告.md]
默认输入: 项目根目录 高频词汇总.txt
"""

import datetime
import json
import re
import sys
from pathlib import Path

# ---------------------------------------------------------------------------
# 词性标注规则
# ---------------------------------------------------------------------------

# 英文词性前缀（可组合，如 v./n.、n./v./a.）
POS_EN_PREFIX_RE = re.compile(r"^((?:[a-zA-Z]+\.)(?:/[a-zA-Z]+\.)*)")
# 释义中任意位置出现的英文词性标记（用于 "①n.替代物②adj.可替代的" 这类无前缀写法）
POS_ANY_EN_RE = re.compile(r"([a-zA-Z]+\.)")

# 中文词性标注 → 英文缩写
POS_ZH = {
    "名词": "n.",
    "动词": "v.",
    "形容词": "a.",
    "副词": "ad.",
    "介词": "prep.",
    "连词": "conj.",
    "代词": "pron.",
    "数词": "num.",
    "冠词": "art.",
    "感叹词": "int.",
    "及物动词": "vt.",
    "不及物动词": "vi.",
    "助动词": "aux.",
    "限定词": "det.",
    "缩写": "abbr.",
}
POS_ZH_RE = re.compile(r"[（(]([^（）()]{1,8})[）)]")


def extract_zh_pos(text: str):
    """提取中文字符串中括号内的中文词性标注；返回 (英文缩写, span) 或 (None, None)。"""
    for m in POS_ZH_RE.finditer(text):
        inner = m.group(1)
        for key, val in POS_ZH.items():
            if key in inner:
                return val, m.span()
    return None, None


def parse_pos_and_meaning(seg: str):
    """解析单个释义段：返回 (pos, meaning)。"""
    seg = seg.strip()
    pos = ""
    # 1) 段首中文词性，如 "(介词) 多亏/由于"
    zh_pos, span = extract_zh_pos(seg)
    if zh_pos is not None and span[0] == 0:
        pos = zh_pos
        seg = (seg[: span[0]] + seg[span[1]:]).strip()
    # 2) 英文词性前缀，如 "v.放弃"、"n./v.①进步②前进"
    m = POS_EN_PREFIX_RE.match(seg)
    if m:
        pos = m.group(1)
        seg = seg[m.end():].strip()
    return pos, seg


def parse_line(line: str):
    """解析单行词条；返回 (entry_dict, structured_ok)。"""
    raw = line
    parts = line.split("\t", 1)
    if len(parts) < 2:
        # 无法切分英文/中文 → raw 原样保留
        return {"word": line.strip(), "pos": "", "meaning": "",
                "wordForms": [], "raw": raw}, False

    english = parts[0].strip()
    chinese = parts[1].strip()
    if not english or not chinese:
        return {"word": line.strip(), "pos": "", "meaning": "",
                "wordForms": [], "raw": raw}, False

    word = english
    pos = ""
    # 英文侧携带中文词性标注，如 "an approach to(介词)"
    zh_pos, span = extract_zh_pos(word)
    if zh_pos is not None:
        pos = zh_pos
        word = (word[: span[0]] + word[span[1]:]).strip()

    # → 派生词链：首段为主释义，其余为词性变化列表
    segments = [s.strip() for s in chinese.split("→")]
    primary = segments[0]
    forms = [s for s in segments[1:] if s]

    seg_pos, meaning = parse_pos_and_meaning(primary)
    if not pos and seg_pos:
        pos = seg_pos
    # 无前缀词性时，取释义中出现的第一个英文词性标记（如 "①n.替代物②adj.可替代的"）
    if not pos:
        m = POS_ANY_EN_RE.search(meaning)
        if m:
            pos = m.group(1)

    return {"word": word, "pos": pos, "meaning": meaning,
            "wordForms": forms, "raw": raw}, True


# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------

def main():
    root = Path(__file__).resolve().parent.parent
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else root / "高频词汇总.txt"
    out_json = Path(sys.argv[2]) if len(sys.argv) > 2 else root / "app/src/main/assets/words.json"
    out_report = Path(sys.argv[3]) if len(sys.argv) > 3 else root / "docs/词库解析报告.md"

    text = src.read_text(encoding="utf-8-sig")
    all_lines = text.splitlines()

    # 定位分隔线（==== 之后为词条数据）
    sep_idx = next((i for i, ln in enumerate(all_lines) if re.fullmatch(r"=+\s*", ln)), -1)
    if sep_idx < 0:
        print("ERROR: 未找到数据分隔线", file=sys.stderr)
        sys.exit(1)

    data_lines = [ln for ln in all_lines[sep_idx + 1:] if ln.strip() != ""]

    entries, raw_lines = [], []
    for ln in data_lines:
        entry, ok = parse_line(ln)
        entries.append(entry)
        if not ok:
            raw_lines.append(ln)

    total = len(entries)
    structured = total - len(raw_lines)

    # 词性统计
    pos_count: dict[str, int] = {}
    for e in entries:
        if e["pos"]:
            pos_count[e["pos"]] = pos_count.get(e["pos"], 0) + 1

    # 重复词检查（源文件已去重，这里仅校验）
    words_seen = {}
    dupes = []
    for i, e in enumerate(entries):
        if e["word"] in words_seen:
            dupes.append((e["word"], words_seen[e["word"]], i))
        else:
            words_seen[e["word"]] = i

    # 输出 JSON（kotlinx-serialization 可解析的紧凑结构）
    book = {
        "total": total,
        "generatedAt": datetime.datetime.now().strftime("%Y-%m-%d"),
        "source": src.name,
        "words": entries,
    }
    out_json.parent.mkdir(parents=True, exist_ok=True)
    out_json.write_text(
        json.dumps(book, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )

    # 回读校验
    reread = json.loads(out_json.read_text(encoding="utf-8"))
    assert reread["total"] == total, "JSON 回读 total 不一致"
    assert len(reread["words"]) == total, "JSON 回读词条数不一致"

    # 解析报告
    lines = [
        "# 词库解析报告",
        "",
        f"- 输入文件：`{src}`",
        f"- 输出文件：`{out_json}`",
        f"- 生成时间：{book['generatedAt']}",
        "",
        "## 统计",
        "",
        f"| 指标 | 数值 |",
        f"|------|------|",
        f"| 源文件声明词条数 | 1269 |",
        f"| 实际解析词条数 | {total} |",
        f"| 结构化成功 | {structured} |",
        f"| raw 原样保留（无法结构化） | {len(raw_lines)} |",
        f"| 含词性标注 | {sum(1 for e in entries if e['pos'])} |",
        f"| 含派生词链（wordForms） | {sum(1 for e in entries if e['wordForms'])} |",
        f"| 重复词条 | {len(dupes)} |",
        "",
        "## 词性分布（Top 15）",
        "",
        "| 词性 | 数量 |",
        "|------|------|",
    ]
    for pos, cnt in sorted(pos_count.items(), key=lambda kv: -kv[1])[:15]:
        lines.append(f"| {pos} | {cnt} |")

    if raw_lines:
        lines += ["", "## raw 保留行清单", ""]
        for ln in raw_lines:
            lines.append(f"- `{ln}`")
    if dupes:
        lines += ["", "## 重复词条清单（word, 首次下标, 重复下标）", ""]
        for w, i1, i2 in dupes:
            lines.append(f"- {w}: {i1} / {i2}")

    lines += [
        "",
        "## 解析规则说明",
        "",
        "- 英文与中文释义以制表符分隔；无分隔或任一侧为空的行整体保留为 raw",
        "- 词性标注：`v.`/`n.`/`a.`/`ad.`/`prep.`/`conj.`/`pron.`/`num.`/`art.` 及组合（如 `n./v.`），"
        "兼容 `adj.`/`adv.`/`vt.`/`vi.` 等变体与中文标注（介词）等",
        "- 多义词 ①②③ 标注原样保留在释义中；`→` 连接的派生词链拆入 wordForms",
        "- 每条记录保留 `raw` 原始行，保证可追溯、不丢数据",
        "",
    ]
    out_report.parent.mkdir(parents=True, exist_ok=True)
    out_report.write_text("\n".join(lines), encoding="utf-8")

    print(f"OK total={total} structured={structured} raw={len(raw_lines)} dupes={len(dupes)}")
    print(f"  json: {out_json}")
    print(f"  report: {out_report}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
