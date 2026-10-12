#!/usr/bin/env python3
"""planning/issues/*.md の原本を GitHub issue として一括起票する。

    python3 planning/create_issues.py --dry-run          # 検査と起票計画の表示だけ（GitHub に触れない）
    python3 planning/create_issues.py                    # 起票（gh CLI でログイン済みのリポジトリへ）
    python3 planning/create_issues.py --only E0,DEC      # グループを絞って起票

やること：
  1. 原本を読み、依存の参照先が存在するか・循環がないかを検査する
  2. ラベルを作る（既にあれば何もしない）
  3. Epic（E0〜E8）を起票し、子 issue を依存順に起票する
  4. 全件起票後、本文中の ID（E2-03・DEC-01 など）を「E2-03 (#123)」に置き換える
  5. 子 issue を Epic の sub-issue として紐付ける（GitHub の sub-issues API）
  6. ID と issue 番号の対応を planning/issue_map.json に保存する（再実行時は起票済みを飛ばす）

起票が済んだら planning/issues/ の原本は削除してよい（GitHub 側が正になる。履歴は git に残る）。
"""
import argparse
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ISSUE_DIR = os.path.join(HERE, "issues")
MAP_PATH = os.path.join(HERE, "issue_map.json")
FILES = ["DEC_VER.md", "E0_E1.md", "E2.md", "E3_E4.md", "E5_E6_E7.md", "E8.md"]  # 決定 issue を先に起票する
ID_RE = r"(?:E\d+-\d{2}|DEC-\d{2}|VER-\d{2})"
LABEL_COLORS = {"type:impl": "0E8A16", "type:decision": "D93F0B", "type:verify": "FBCA04",
                "epic": "5319E7", "design-change": "B60205"}


def parse():
    epics, issues = {}, []
    for fname in FILES:
        text = open(os.path.join(ISSUE_DIR, fname), encoding="utf-8").read()
        for block in re.split(r"(?m)^# (?=\S)", text)[1:]:
            head, _, rest = block.partition("\n")
            group = head.split(":")[0].strip()
            parts = re.split(rf"(?m)^## ({ID_RE}) (.+)$", rest)
            epics[group] = {"title": head.strip(), "body": parts[0].strip().strip("-").strip(), "file": fname}
            for i in range(1, len(parts), 3):
                iid, title, body = parts[i], parts[i + 1].strip(), parts[i + 2]
                meta = dict(re.findall(r"(?m)^- (labels|depends|refs): (.*)$", body))
                body = re.sub(r"(?m)^- (labels|depends|refs): .*\n", "", body, count=3).strip().strip("-").strip()
                deps = [] if meta.get("depends", "なし").strip() == "なし" else re.findall(ID_RE, meta["depends"])
                issues.append({"id": iid, "group": group, "title": f"{iid} {title}", "file": fname,
                               "labels": [l.strip() for l in meta.get("labels", "").split(",") if l.strip()],
                               "depends": deps, "refs": meta.get("refs", "").strip(), "body": body})
    return epics, issues


def check(issues):
    ids = [i["id"] for i in issues]
    dup = {x for x in ids if ids.count(x) > 1}
    errors = [f"ID が重複: {sorted(dup)}"] if dup else []
    known = set(ids)
    for i in issues:
        for d in i["depends"]:
            if d not in known:
                errors.append(f"{i['id']} の依存 {d} が存在しない")
        if not i["labels"]:
            errors.append(f"{i['id']} に labels がない")
        for ref in set(re.findall(ID_RE, i["body"])) - known:
            errors.append(f"{i['id']} の本文が未定義の {ref} を参照")
    order, state = [], {}
    by_id = {i["id"]: i for i in issues}

    def visit(n, path):
        if state.get(n) == 1:
            errors.append("循環: " + " → ".join(path + [n]))
            return
        if state.get(n) == 2 or n not in by_id:
            return
        state[n] = 1
        for d in by_id[n]["depends"]:
            visit(d, path + [n])
        state[n] = 2
        order.append(by_id[n])

    for i in issues:
        visit(i["id"], [])
    return errors, order


def render(issue):
    deps = "\n".join(f"- {d}" for d in issue["depends"]) or "なし"
    body = re.sub(r"(?m)^### ", "## ", issue["body"])
    return (f"## 参照設計書\n{issue['refs']}\n\n## 依存\n{deps}\n\n{body}\n\n---\n"
            f"原本: `planning/issues/{issue['file']}` の {issue['id']}。作業規約は `AGENTS.md`。")


def link_ids(text, number_of):
    """本文中の ID に issue 番号を添える。コード表記（`…`）の中と末尾の原本表記は書き換えない。"""
    body, sep, footer = text.rpartition("\n---\n原本:")
    if not sep:
        body, footer = text, ""
    sub = lambda m: f"{m.group(0)} (#{number_of[m.group(0)]})" if m.group(0) in number_of else m.group(0)
    parts = body.split("`")
    parts = [re.sub(ID_RE, sub, p) if k % 2 == 0 else p for k, p in enumerate(parts)]
    return "`".join(parts) + (sep + footer if sep else "")


def gh(*args, input_text=None):
    r = subprocess.run(["gh", *args], input=input_text, capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError(f"gh {' '.join(args[:3])} … failed: {r.stderr.strip()}")
    return r.stdout.strip()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--only", default="", help="起票するグループ（例：E0,E1,DEC）")
    a = ap.parse_args()

    epics, issues = parse()
    errors, order = check(issues)
    if errors:
        print("原本に問題があります：\n  " + "\n  ".join(errors))
        sys.exit(1)
    only = {g.strip() for g in a.only.split(",") if g.strip()}
    order = [i for i in order if not only or i["group"] in only]
    labels = sorted({l for i in issues for l in i["labels"]} | {"epic"})

    if a.dry_run:
        groups = {}
        for i in issues:
            groups.setdefault(i["group"], []).append(i["id"])
        print(f"検査 OK：{len(issues)} 件（" + "、".join(f"{g} {len(v)}" for g, v in groups.items()) + "）")
        print(f"ラベル {len(labels)} 個：{', '.join(labels)}")
        print("起票順（依存順）：")
        for n, i in enumerate(order, 1):
            dep = f"  ← {', '.join(i['depends'])}" if i["depends"] else ""
            print(f"  {n:2d}. {i['title']}{dep}")
        return

    mapping = json.load(open(MAP_PATH)) if os.path.exists(MAP_PATH) else {}
    for l in labels:
        try:
            gh("label", "create", l, "--color", LABEL_COLORS.get(l, "C5DEF5"), "--force")
        except RuntimeError as e:
            print(f"ラベル {l}: {e}")

    for g in sorted({i["group"] for i in order}):
        key = f"epic:{g}"
        if key in mapping or not g.startswith("E"):
            continue
        url = gh("issue", "create", "--title", f"[Epic] {epics[g]['title']}", "--label", "epic",
                 "--body", epics[g]["body"] + f"\n\n原本: `planning/issues/{epics[g]['file']}`")
        mapping[key] = int(url.rstrip("/").split("/")[-1])
        json.dump(mapping, open(MAP_PATH, "w"), indent=1)
        print(f"#{mapping[key]}  [Epic] {g}")

    for i in order:
        if i["id"] in mapping:
            continue
        args = ["issue", "create", "--title", i["title"], "--body", render(i)]
        for l in i["labels"]:
            args += ["--label", l]
        url = gh(*args)
        mapping[i["id"]] = int(url.rstrip("/").split("/")[-1])
        json.dump(mapping, open(MAP_PATH, "w"), indent=1)
        print(f"#{mapping[i['id']]}  {i['title']}")

    number_of = {k: v for k, v in mapping.items() if not k.startswith("epic:")}
    for i in order:
        gh("issue", "edit", str(mapping[i["id"]]), "--body", link_ids(render(i), number_of))

    repo = gh("repo", "view", "--json", "nameWithOwner", "--jq", ".nameWithOwner")
    for i in order:
        epic = mapping.get(f"epic:{i['group']}")
        if not epic:
            continue
        try:
            sub_id = gh("api", f"repos/{repo}/issues/{mapping[i['id']]}", "--jq", ".id")
            gh("api", "--method", "POST", f"repos/{repo}/issues/{epic}/sub_issues", "-F", f"sub_issue_id={sub_id}")
        except RuntimeError as e:
            print(f"sub-issue の紐付けに失敗（{i['id']} → #{epic}）。GitHub の画面で手動で紐付けてください：{e}")
    print(f"完了。対応表：{os.path.relpath(MAP_PATH)}")


if __name__ == "__main__":
    main()
