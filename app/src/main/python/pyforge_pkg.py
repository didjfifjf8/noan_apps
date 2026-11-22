import importlib
import importlib.metadata
import importlib.util
import io
import json
import os
import platform
import pydoc
import re
import shutil
import sys
import urllib.error
import urllib.request
import zipfile

_FORBIDDEN_BINARY_EXTENSIONS = (".so", ".dll", ".pyd", ".dylib", ".dex", ".jar", ".apk", ".exe")

_STDLIB_DESCRIPTIONS = {
    "math": "Mathematical functions (trig, log, sqrt, constants pi & e)",
    "random": "Generate pseudo-random numbers, choices, and shuffles",
    "datetime": "Basic date and time types and arithmetic",
    "os": "Miscellaneous operating system interfaces and file paths",
    "sys": "System-specific parameters, argv, path, and interpreter info",
    "json": "JSON encoder and decoder",
    "re": "Regular expression matching operations",
    "collections": "Container datatypes (deque, Counter, defaultdict, namedtuple)",
    "itertools": "Functions creating iterators for efficient looping",
    "statistics": "Mathematical statistics functions (mean, median, stdev)",
    "functools": "Higher-order functions and operations on callable objects",
    "pathlib": "Object-oriented filesystem paths",
    "sqlite3": "DB-API 2.0 interface for SQLite databases",
    "csv": "CSV file reading and writing",
    "time": "Time access and conversions",
    "hashlib": "Secure hashes and message digests (SHA256, MD5, etc.)",
    "urllib": "URL handling modules and HTTP client utilities",
    "unittest": "Unit testing framework",
    "threading": "Thread-based parallelism",
    "asyncio": "Asynchronous I/O, event loops, and coroutines",
    "decimal": "Decimal fixed point and floating point arithmetic",
    "fractions": "Rational number arithmetic",
    "typing": "Support for type hints",
    "dataclasses": "Data Classes decorator and helpers",
    "enum": "Support for enumerations",
    "io": "Core tools for working with streams",
    "string": "Common string operations and constants",
    "copy": "Shallow and deep copy operations",
    "heapq": "Heap queue algorithm (priority queue)",
    "bisect": "Array bisection algorithm (binary search)",
}

_ANDROID_INCOMPATIBLE_CATALOG = [
    {
        "name": "tkinter",
        "summary": "Tk GUI toolkit",
        "reason": "Requires desktop X11/Wayland/Win32/Cocoa display server; not available on Android.",
    },
    {
        "name": "pygame",
        "summary": "2D game development library",
        "reason": "Requires native SDL2 C bindings compiled for Android NDK; not bundled in runtime.",
    },
    {
        "name": "torch",
        "summary": "PyTorch deep learning framework",
        "reason": "Requires large native C++/CUDA .so binaries not bundled for Android ARM runtime.",
    },
    {
        "name": "tensorflow",
        "summary": "End-to-end machine learning platform",
        "reason": "Requires native C++ .so runtime binaries not bundled in APK.",
    },
    {
        "name": "opencv-python",
        "summary": "OpenCV computer vision bindings",
        "reason": "Requires native C++ libcv2 .so binaries compiled for Android.",
    },
    {
        "name": "scipy",
        "summary": "Scientific computing & Fortran/C extensions",
        "reason": "Requires native Fortran/C LAPACK/BLAS .so binaries not bundled in APK.",
    },
    {
        "name": "playwright",
        "summary": "Browser automation framework",
        "reason": "Requires desktop Chromium/Firefox/WebKit subprocess binaries.",
    },
    {
        "name": "lxml",
        "summary": "XML and HTML processing with C libxml2",
        "reason": "Requires compiled native libxml2/libxslt C extensions (use pure-Python beautifulsoup4 or xml.etree instead).",
    },
]

_POPULAR_PURE_PYTHON_CATALOG = [
    ("sympy", "Computer algebra system (symbolic math)"),
    ("beautifulsoup4", "Screen-scraping and HTML/XML parser"),
    ("tabulate", "Pretty-print tabular data in Python"),
    ("pyyaml", "YAML parser and emitter for Python"),
    ("rich", "Rich text and formatting in the terminal"),
    ("tqdm", "Fast, extensible progress bar for loops"),
    ("networkx", "Creation and study of complex networks & graphs"),
    ("jinja2", "Fast, expressive templating engine"),
    ("click", "Composable command line interface toolkit"),
    ("markdown", "Python implementation of Markdown"),
    ("colorama", "Cross-platform colored terminal text"),
    ("python-dateutil", "Powerful extensions to standard datetime"),
]


def ensure_site_packages(site_packages_dir):
    if site_packages_dir:
        os.makedirs(site_packages_dir, exist_ok=True)
        if site_packages_dir not in sys.path:
            sys.path.insert(0, site_packages_dir)
    importlib.invalidate_caches()


def _normalize_name(name):
    return re.sub(r"[-_.]+", "-", name).lower().strip()


def _is_in_site_packages(dist, site_packages_dir):
    if not site_packages_dir:
        return False
    try:
        sp_norm = os.path.normpath(os.path.abspath(site_packages_dir))
        dist_path = getattr(dist, "_path", None)
        if dist_path:
            dp_norm = os.path.normpath(os.path.abspath(str(dist_path)))
            return dp_norm.startswith(sp_norm)
        loc = dist.locate_file("")
        if loc:
            return os.path.normpath(os.path.abspath(str(loc))).startswith(sp_norm)
    except Exception:
        pass
    return False


def get_runtime_info(site_packages_dir):
    ensure_site_packages(site_packages_dir)
    dists = list(importlib.metadata.distributions())
    stdlib_count = len(getattr(sys, "stdlib_module_names", set()))
    return json.dumps(
        {
            "python_version": platform.python_version(),
            "implementation": platform.python_implementation(),
            "architecture": platform.machine() or "android",
            "platform": sys.platform,
            "site_packages_dir": site_packages_dir,
            "installed_packages_count": len(dists),
            "stdlib_modules_count": stdlib_count,
        }
    )


def list_libraries(site_packages_dir):
    ensure_site_packages(site_packages_dir)

    installed_list = []
    seen_norm = set()

    for dist in importlib.metadata.distributions():
        try:
            meta = dist.metadata
            name = meta["Name"] or "unknown"
            norm = _normalize_name(name)
            if norm in seen_norm:
                continue
            seen_norm.add(norm)
            version = dist.version or "0.0.0"
            summary = meta["Summary"] or "Installed Python package"
            home_page = meta["Home-page"] or ""
            requires = list(dist.requires or [])
            is_pip_runtime = _is_in_site_packages(dist, site_packages_dir)

            # Determine top-level import name
            import_name = name.replace("-", "_")
            if norm == "pillow":
                import_name = "PIL"
            elif norm == "beautifulsoup4":
                import_name = "bs4"
            elif norm == "pyyaml":
                import_name = "yaml"
            elif norm == "python-dateutil":
                import_name = "dateutil"
            else:
                try:
                    top_txt = dist.read_text("top_level.txt")
                    if top_txt:
                        first_line = top_txt.strip().splitlines()[0].strip()
                        if first_line:
                            import_name = first_line
                except Exception:
                    pass

            installed_list.append(
                {
                    "name": name,
                    "import_name": import_name,
                    "version": version,
                    "summary": summary,
                    "homepage": home_page,
                    "category": "pip_runtime" if is_pip_runtime else "bundled_native",
                    "installed": True,
                    "removable": is_pip_runtime,
                    "compatible": True,
                    "requires": requires[:12],
                }
            )
        except Exception:
            continue

    installed_list.sort(key=lambda x: x["name"].lower())

    # Standard library modules verified against live sys.stdlib_module_names
    stdlib_names = getattr(sys, "stdlib_module_names", set())
    featured_stdlib = [
        "math", "random", "datetime", "os", "sys", "json", "re",
        "collections", "itertools", "statistics", "functools", "pathlib",
        "sqlite3", "csv", "time", "hashlib", "urllib", "unittest",
        "threading", "asyncio", "decimal", "fractions", "typing",
        "dataclasses", "enum", "io", "string", "copy", "heapq", "bisect",
    ]
    stdlib_list = []
    seen_std = set()
    for mod in featured_stdlib:
        if mod in stdlib_names and mod not in seen_std:
            seen_std.add(mod)
            stdlib_list.append(
                {
                    "name": mod,
                    "import_name": mod,
                    "version": platform.python_version(),
                    "summary": _STDLIB_DESCRIPTIONS.get(mod, "Python standard library module"),
                    "homepage": f"https://docs.python.org/3/library/{mod}.html",
                    "category": "stdlib",
                    "installed": True,
                    "removable": False,
                    "compatible": True,
                    "requires": [],
                }
            )

    for mod in sorted(stdlib_names):
        if mod.startswith("_") or mod in seen_std:
            continue
        if mod == "tkinter":
            continue
        seen_std.add(mod)
        stdlib_list.append(
            {
                "name": mod,
                "import_name": mod,
                "version": platform.python_version(),
                "summary": _STDLIB_DESCRIPTIONS.get(mod, "Built-in Python 3.11 standard library module"),
                "homepage": f"https://docs.python.org/3/library/{mod}.html",
                "category": "stdlib",
                "installed": True,
                "removable": False,
                "compatible": True,
                "requires": [],
            }
        )

    # Catalog suggestions (pure-python pip installable that are NOT yet installed)
    available_pure = []
    for pkg_name, desc in _POPULAR_PURE_PYTHON_CATALOG:
        if _normalize_name(pkg_name) not in seen_norm:
            available_pure.append(
                {
                    "name": pkg_name,
                    "import_name": pkg_name.replace("-", "_"),
                    "version": "PyPI latest",
                    "summary": desc,
                    "homepage": f"https://pypi.org/project/{pkg_name}/",
                    "category": "pip_available",
                    "installed": False,
                    "removable": False,
                    "compatible": True,
                    "requires": [],
                }
            )

    # Incompatible packages (only mark incompatible if NOT actually installed)
    incompatible_list = []
    for entry in _ANDROID_INCOMPATIBLE_CATALOG:
        if _normalize_name(entry["name"]) not in seen_norm:
            incompatible_list.append(
                {
                    "name": entry["name"],
                    "import_name": entry["name"],
                    "version": "Unsupported",
                    "summary": entry["summary"],
                    "homepage": "",
                    "category": "incompatible",
                    "installed": False,
                    "removable": False,
                    "compatible": False,
                    "incompatibility_reason": entry["reason"],
                    "requires": [],
                }
            )

    return json.dumps(
        {
            "installed": installed_list,
            "stdlib": stdlib_list,
            "available": available_pure,
            "incompatible": incompatible_list,
        }
    )


def verify_import(import_name, site_packages_dir=None):
    ensure_site_packages(site_packages_dir)
    try:
        mod = importlib.import_module(import_name)
        ver = getattr(mod, "__version__", None)
        if ver is None:
            try:
                ver = importlib.metadata.version(import_name)
            except Exception:
                ver = platform.python_version()
        loc = getattr(mod, "__file__", "<built-in>")
        return json.dumps(
            {
                "ok": True,
                "module": import_name,
                "version": str(ver),
                "location": str(loc),
            }
        )
    except Exception as exc:
        return json.dumps(
            {
                "ok": False,
                "module": import_name,
                "error": f"{type(exc).__name__}: {exc}",
            }
        )


def get_module_docs(module_name, site_packages_dir=None):
    ensure_site_packages(site_packages_dir)
    try:
        doc_text = pydoc.render_doc(module_name, title="Python Documentation: %s", renderer=pydoc.plaintext)
        if len(doc_text) > 16000:
            doc_text = doc_text[:16000] + "\n\n... (truncated for fast display)"
        return json.dumps({"ok": True, "name": module_name, "doc": doc_text})
    except Exception as exc:
        return json.dumps({"ok": False, "name": module_name, "error": str(exc)})


def _fetch_pypi_json(package_name):
    url = f"https://pypi.org/pypi/{urllib.request.quote(package_name.strip())}/json"
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "PyForge-Android-IDE/1.0", "Accept": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _find_pure_wheel(urls):
    for item in urls or []:
        fn = item.get("filename", "")
        if fn.endswith("-py3-none-any.whl") or fn.endswith("-py2.py3-none-any.whl"):
            return item
    return None


def search_pypi(package_name, site_packages_dir=None):
    ensure_site_packages(site_packages_dir)
    q = package_name.strip()
    if not q:
        return json.dumps({"ok": False, "error": "Empty package name"})

    norm_q = _normalize_name(q)
    for item in _ANDROID_INCOMPATIBLE_CATALOG:
        if _normalize_name(item["name"]) == norm_q:
            return json.dumps(
                {
                    "ok": True,
                    "name": item["name"],
                    "version": "Unsupported",
                    "summary": item["summary"],
                    "description": item["reason"],
                    "compatible": False,
                    "incompatibility_reason": item["reason"],
                    "installed": False,
                    "installed_version": None,
                    "removable": False,
                }
            )

    installed_ver = None
    is_removable = False
    for dist in importlib.metadata.distributions():
        try:
            if _normalize_name(dist.metadata["Name"]) == norm_q:
                installed_ver = dist.version
                is_removable = _is_in_site_packages(dist, site_packages_dir)
                break
        except Exception:
            continue

    try:
        data = _fetch_pypi_json(q)
        info = data.get("info", {})
        urls = data.get("urls", [])
        pure_whl = _find_pure_wheel(urls)
        compatible = (pure_whl is not None) or (installed_ver is not None and not is_removable)
        reason = ""
        if not compatible:
            reason = (
                "Only platform-specific native/source distributions were found on PyPI. "
                "Runtime pip on Android supports pure-Python wheels (*-py3-none-any.whl) only."
            )
        return json.dumps(
            {
                "ok": True,
                "name": info.get("name", q),
                "version": info.get("version", ""),
                "summary": info.get("summary") or "Python package on PyPI",
                "description": (info.get("description") or "")[:4000],
                "homepage": info.get("home_page") or info.get("project_url") or "",
                "compatible": compatible,
                "incompatibility_reason": reason,
                "installed": installed_ver is not None,
                "installed_version": installed_ver,
                "removable": is_removable,
                "wheel_filename": pure_whl.get("filename") if pure_whl else "",
                "wheel_size": pure_whl.get("size", 0) if pure_whl else 0,
            }
        )
    except urllib.error.HTTPError as exc:
        if exc.code == 404:
            return json.dumps({"ok": False, "error": f"Package '{q}' not found on PyPI (404)."})
        return json.dumps({"ok": False, "error": f"PyPI HTTP error {exc.code}: {exc.reason}"})
    except Exception as exc:
        return json.dumps({"ok": False, "error": f"Network or lookup error: {exc}"})


def _parse_req_name(req_str):
    if ";" in req_str:
        pkg_part, marker = req_str.split(";", 1)
        marker_low = marker.lower()
        if "extra ==" in marker_low or 'extra == "' in marker_low or "extra=='" in marker_low:
            return None
        if 'sys_platform == "win32"' in marker_low or "sys_platform == 'win32'" in marker_low:
            return None
        if 'sys_platform == "darwin"' in marker_low or "sys_platform == 'darwin'" in marker_low:
            return None
    else:
        pkg_part = req_str
    m = re.match(r"^\s*([A-Za-z0-9][A-Za-z0-9._-]*)", pkg_part)
    return m.group(1) if m else None


def install_package(package_name, site_packages_dir, progress_callback=None, upgrade=False):
    ensure_site_packages(site_packages_dir)
    logs = []
    visited = set()

    def emit(msg, pct):
        logs.append(msg)
        if progress_callback is not None:
            try:
                progress_callback.onProgress(msg, int(pct))
            except Exception:
                pass

    def _installed_map():
        out = {}
        for dist in importlib.metadata.distributions():
            try:
                out[_normalize_name(dist.metadata["Name"])] = dist
            except Exception:
                pass
        return out

    def _install_recursive(pkg, depth=0):
        norm = _normalize_name(pkg)
        if norm in visited:
            return
        visited.add(norm)

        installed_map = _installed_map()
        if norm in installed_map and not (upgrade and depth == 0):
            dist = installed_map[norm]
            if not _is_in_site_packages(dist, site_packages_dir):
                emit(f"Requirement already satisfied (bundled): {pkg}=={dist.version}", 20)
                return
            if not upgrade:
                emit(f"Requirement already satisfied: {pkg}=={dist.version}", 20)
                return

        emit(f"Fetching metadata for {pkg} from PyPI...", 15)
        data = _fetch_pypi_json(pkg)
        info = data.get("info", {})
        actual_name = info.get("name", pkg)
        version = info.get("version", "0.0.0")
        urls = data.get("urls", [])
        pure_whl = _find_pure_wheel(urls)

        if pure_whl is None:
            raise RuntimeError(
                f"'{actual_name}' has no pure-Python wheel (*-py3-none-any.whl) on PyPI. "
                "Packages requiring native C/C++/Rust extensions cannot be compiled or downloaded dynamically on Android."
            )

        if upgrade and norm in installed_map and _is_in_site_packages(installed_map[norm], site_packages_dir):
            emit(f"Removing old version of {actual_name}...", 25)
            _remove_dist(installed_map[norm], site_packages_dir)

        whl_url = pure_whl["url"]
        whl_name = pure_whl["filename"]
        emit(f"Downloading {whl_name}...", 40)

        req = urllib.request.Request(whl_url, headers={"User-Agent": "PyForge-Android-IDE/1.0"})
        with urllib.request.urlopen(req, timeout=30) as resp:
            whl_bytes = resp.read()

        emit(f"Verifying wheel integrity ({len(whl_bytes) // 1024} KB)...", 65)
        zf = zipfile.ZipFile(io.BytesIO(whl_bytes))
        for member in zf.namelist():
            if member.lower().endswith(_FORBIDDEN_BINARY_EXTENSIONS):
                raise RuntimeError(
                    f"Blocked '{whl_name}': contains native binary '{member}'. "
                    "Only pure-Python packages are allowed at runtime."
                )

        emit(f"Extracting {actual_name}=={version} into site-packages...", 80)
        zf.extractall(site_packages_dir)

        requires = info.get("requires_dist") or []
        for req_str in requires:
            dep_name = _parse_req_name(req_str)
            if dep_name:
                _install_recursive(dep_name, depth=depth + 1)

        emit(f"Installed {actual_name}=={version}", 95)

    try:
        _install_recursive(package_name.strip(), depth=0)
        importlib.invalidate_caches()
        emit("Package installation complete.", 100)
        return json.dumps({"ok": True, "logs": logs})
    except Exception as exc:
        err_msg = str(exc)
        emit(f"ERROR: {err_msg}", 100)
        return json.dumps({"ok": False, "error": err_msg, "logs": logs})


def _remove_dist(dist, site_packages_dir):
    sp_abs = os.path.abspath(site_packages_dir)
    record = dist.read_text("RECORD")
    removed = 0
    if record:
        for line in record.splitlines():
            rel = line.split(",")[0].strip()
            if not rel:
                continue
            target = os.path.abspath(os.path.join(sp_abs, rel))
            if target.startswith(sp_abs) and os.path.isfile(target):
                os.remove(target)
                removed += 1
    dist_path = getattr(dist, "_path", None)
    if dist_path:
        dp_abs = os.path.abspath(str(dist_path))
        if dp_abs.startswith(sp_abs) and os.path.isdir(dp_abs):
            shutil.rmtree(dp_abs, ignore_errors=True)
    # Clean __pycache__ and empty directories inside site_packages_dir
    for root, dirs, files in os.walk(sp_abs, topdown=False):
        if os.path.basename(root) == "__pycache__":
            shutil.rmtree(root, ignore_errors=True)
            continue
        remaining_dirs = [d for d in dirs if os.path.exists(os.path.join(root, d))]
        remaining_files = [f for f in files if os.path.exists(os.path.join(root, f))]
        if root != sp_abs and not remaining_dirs and not remaining_files:
            try:
                os.rmdir(root)
            except Exception:
                pass
    return removed


def uninstall_package(package_name, site_packages_dir):
    ensure_site_packages(site_packages_dir)
    norm = _normalize_name(package_name)
    for dist in importlib.metadata.distributions():
        try:
            if _normalize_name(dist.metadata["Name"]) == norm:
                if not _is_in_site_packages(dist, site_packages_dir):
                    return json.dumps(
                        {
                            "ok": False,
                            "error": f"'{package_name}' is a pre-bundled native APK package and cannot be uninstalled.",
                        }
                    )
                removed = _remove_dist(dist, site_packages_dir)
                importlib.invalidate_caches()
                return json.dumps(
                    {
                        "ok": True,
                        "message": f"Uninstalled {package_name} ({removed} files removed).",
                    }
                )
        except Exception as exc:
            return json.dumps({"ok": False, "error": str(exc)})
    return json.dumps({"ok": False, "error": f"Package '{package_name}' is not installed in site-packages."})
