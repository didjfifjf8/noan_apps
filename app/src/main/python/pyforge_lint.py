import ast
import builtins
import importlib
import json
import keyword
import pkgutil
import re
import sys

_SNIPPETS = [
    ("def ", "def name():\n    pass", "snippet"),
    ("class ", "class Name:\n    def __init__(self):\n        pass", "snippet"),
    ("if __name__", 'if __name__ == "__main__":\n    ', "snippet"),
    ("for ", "for item in items:\n    ", "snippet"),
    ("while ", "while condition:\n    ", "snippet"),
    ("try/except", "try:\n    pass\nexcept Exception as e:\n    print(e)", "snippet"),
    ("import ", "import ", "keyword"),
    ("from ", "from ", "keyword"),
    ("print()", "print()", "builtin"),
    ("input()", "input()", "builtin"),
    ("len()", "len()", "builtin"),
    ("range()", "range()", "builtin"),
    ("enumerate()", "enumerate()", "builtin"),
]

_COMMON_MODULES = sorted(
    {
        "math",
        "random",
        "datetime",
        "os",
        "sys",
        "json",
        "re",
        "collections",
        "itertools",
        "statistics",
        "functools",
        "pathlib",
        "time",
        "hashlib",
        "csv",
        "sqlite3",
        "unittest",
        "threading",
        "asyncio",
        "typing",
        "dataclasses",
        "enum",
        "decimal",
        "fractions",
        "numpy",
        "pandas",
        "matplotlib",
        "matplotlib.pyplot",
        "requests",
        "PIL",
        "PIL.Image",
    }
)


def check_syntax(code, filename="<editor>"):
    if not code or not code.strip():
        return json.dumps({"valid": True})
    try:
        ast.parse(code, filename=filename)
        return json.dumps({"valid": True})
    except SyntaxError as exc:
        return json.dumps(
            {
                "valid": False,
                "line": exc.lineno or 1,
                "offset": exc.offset or 1,
                "message": exc.msg or "SyntaxError",
                "text": (exc.text or "").rstrip("\n"),
            }
        )
    except Exception as exc:
        return json.dumps(
            {
                "valid": False,
                "line": 1,
                "offset": 1,
                "message": str(exc),
                "text": "",
            }
        )


def _extract_imports_and_symbols(code):
    alias_map = {}
    symbols = set()

    for match in re.finditer(r"^\s*import\s+([a-zA-Z0-9_.]+)(?:\s+as\s+([a-zA-Z0-9_]+))?", code, re.MULTILINE):
        mod = match.group(1)
        alias = match.group(2) or mod.split(".")[0]
        alias_map[alias] = mod
        symbols.add(alias)

    for match in re.finditer(r"^\s*from\s+([a-zA-Z0-9_.]+)\s+import\s+([^\n#]+)", code, re.MULTILINE):
        names = match.group(2).split(",")
        for part in names:
            part = part.strip()
            if not part or part == "*":
                continue
            if " as " in part:
                symbols.add(part.split(" as ")[1].strip())
            else:
                symbols.add(part)

    try:
        tree = ast.parse(code)
        for node in ast.walk(tree):
            if isinstance(node, ast.FunctionDef):
                symbols.add(node.name)
                for arg in node.args.args:
                    symbols.add(arg.arg)
            elif isinstance(node, ast.ClassDef):
                symbols.add(node.name)
            elif isinstance(node, ast.Name) and isinstance(node.ctx, ast.Store):
                symbols.add(node.id)
    except Exception:
        for match in re.finditer(r"^\s*(?:def|class)\s+([a-zA-Z_][a-zA-Z0-9_]*)", code, re.MULTILINE):
            symbols.add(match.group(1))
        for match in re.finditer(r"^\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*=", code, re.MULTILINE):
            symbols.add(match.group(1))

    return alias_map, symbols


def get_completions(code, cursor_pos):
    if cursor_pos < 0:
        cursor_pos = 0
    if cursor_pos > len(code):
        cursor_pos = len(code)

    before = code[:cursor_pos]
    line_start = before.rfind("\n") + 1
    current_line = before[line_start:]

    alias_map, local_symbols = _extract_imports_and_symbols(code)
    results = []
    seen = set()

    def add(label, insert_text, kind):
        if label not in seen:
            seen.add(label)
            results.append({"label": label, "insert": insert_text, "kind": kind})

    # Attribute completion e.g. math.sq or np.ar
    attr_match = re.search(r"([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z0-9_]*)$", current_line)
    if attr_match:
        obj_name = attr_match.group(1)
        attr_prefix = attr_match.group(2)
        mod_name = alias_map.get(obj_name, obj_name)
        mod = sys.modules.get(mod_name)
        if mod is None and (mod_name in sys.stdlib_module_names or mod_name in alias_map.values()):
            try:
                mod = importlib.import_module(mod_name)
            except Exception:
                mod = None
        if mod is not None:
            for attr in sorted(dir(mod)):
                if attr.startswith("_") and not attr_prefix.startswith("_"):
                    continue
                if attr.lower().startswith(attr_prefix.lower()):
                    try:
                        val = getattr(mod, attr)
                        is_callable = callable(val)
                    except Exception:
                        is_callable = False
                    insert = f"{attr}()" if is_callable else attr
                    add(attr, insert, "func" if is_callable else "attr")
                    if len(results) >= 24:
                        break
        return json.dumps(results[:24])

    # Import module completion
    imp_match = re.search(r"^\s*(?:import|from)\s+([a-zA-Z0-9_.]*)$", current_line)
    if imp_match:
        mod_prefix = imp_match.group(1)
        candidates = sorted(set(_COMMON_MODULES) | set(getattr(sys, "stdlib_module_names", set())))
        for m in candidates:
            if m.startswith(mod_prefix):
                add(m, m, "module")
                if len(results) >= 24:
                    break
        return json.dumps(results[:24])

    # General identifier completion
    word_match = re.search(r"([a-zA-Z_][a-zA-Z0-9_]*)$", current_line)
    prefix = word_match.group(1) if word_match else ""

    if not prefix:
        for label, ins, kind in _SNIPPETS[:8]:
            add(label, ins, kind)
        return json.dumps(results)

    p_low = prefix.lower()

    for sym in sorted(local_symbols):
        if sym.lower().startswith(p_low) and sym != prefix:
            add(sym, sym, "var")

    for label, ins, kind in _SNIPPETS:
        if label.lower().startswith(p_low):
            add(label, ins, kind)

    for kw in keyword.kwlist:
        if kw.lower().startswith(p_low):
            add(kw, kw + " ", "keyword")

    builtin_names = [b for b in dir(builtins) if not b.startswith("_")]
    for b in builtin_names:
        if b.lower().startswith(p_low):
            val = getattr(builtins, b, None)
            ins = f"{b}()" if callable(val) else b
            add(b, ins, "builtin")

    for m in _COMMON_MODULES:
        if m.lower().startswith(p_low):
            add(m, m, "module")

    return json.dumps(results[:24])
