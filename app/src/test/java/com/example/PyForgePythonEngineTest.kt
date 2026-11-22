package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Base64

class PyForgePythonEngineTest {

    private fun pythonDir(): File {
        val candidates = listOf(
            File("src/main/python"),
            File("app/src/main/python"),
        )
        return candidates.first { it.exists() }
    }

    private fun pyStr(value: String): String {
        val b64 = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
        return "__import__('base64').b64decode('$b64').decode('utf-8')"
    }

    private fun runHostPython(harnessCode: String): String {
        val pyDir = pythonDir().absolutePath
        val tempScript = Files.createTempFile("pyforge_test_", ".py").toFile()
        tempScript.writeText(
            """
import sys
sys.path.insert(0, ${pyStr(pyDir)})
$harnessCode
"""
        )
        try {
            val pb = ProcessBuilder("python3", tempScript.absolutePath)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val code = proc.waitFor()
            assertEquals("Python test script failed with output:\n$output", 0, code)
            return output.trim()
        } finally {
            tempScript.delete()
        }
    }

    @Test
    fun `test loops functions recursion lists dicts input and stdlib imports`() {
        val workDir = Files.createTempDirectory("pyforge_work_").toFile()
        val siteDir = Files.createTempDirectory("pyforge_site_").toFile()
        val plotDir = Files.createTempDirectory("pyforge_plot_").toFile()
        try {
            val userCode = """
import math, random, datetime, os, sys, json, re, collections, itertools, statistics

name = input("Name: ")
print("Hello", name)

def fact(n):
    if n <= 1:
        return 1
    return n * fact(n - 1)

nums = [x * 2 for x in range(1, 6)]
freq = collections.Counter("abracadabra")
meta = {"fact5": fact(5), "sqrt16": int(math.sqrt(16)), "mean": statistics.mean(nums)}
print(json.dumps(meta, sort_keys=True))
print("Pairs:", list(itertools.islice(itertools.combinations([1, 2, 3], 2), 2)))
print("A_count:", freq["a"])
"""
            val out = runHostPython(
                """
import pyforge_runner

stdout_buf = []
stderr_buf = []

class InputStub:
    def __init__(self, answers):
        self._answers = list(answers)
    def requestInput(self, prompt):
        return self._answers.pop(0) if self._answers else ""

class CallbackStub:
    def onStarted(self, tid): pass
    def onStdout(self, s): stdout_buf.append(s)
    def onStderr(self, s): stderr_buf.append(s)
    def onPlot(self, p): pass

code = ${pyStr(userCode)}
rc = pyforge_runner.execute_script(
    "test.py",
    code,
    ${pyStr(workDir.absolutePath)},
    ${pyStr(siteDir.absolutePath)},
    ${pyStr(plotDir.absolutePath)},
    InputStub(["Ada"]),
    CallbackStub(),
)
assert rc == 0, "".join(stderr_buf)
joined = "".join(stdout_buf)
assert "Hello Ada" in joined, joined
assert '{"fact5": 120, "mean": 6, "sqrt16": 4}' in joined, joined
assert "Pairs: [(1, 2), (1, 3)]" in joined, joined
assert "A_count: 5" in joined, joined
print("OK")
"""
            )
            assertTrue(out.endsWith("OK"))
        } finally {
            workDir.deleteRecursively()
            siteDir.deleteRecursively()
            plotDir.deleteRecursively()
        }
    }

    @Test
    fun `test syntax linter and autocomplete`() {
        val out = runHostPython(
            """
import json, pyforge_lint

bad = json.loads(pyforge_lint.check_syntax("def foo(:\n    pass"))
assert bad["valid"] is False
assert bad["line"] == 1

good = json.loads(pyforge_lint.check_syntax("def foo(x):\n    return x * 2"))
assert good["valid"] is True

comps = json.loads(pyforge_lint.get_completions("import math\nmath.sq", len("import math\nmath.sq")))
labels = [c["label"] for c in comps]
assert "sqrt" in labels, labels
print("LINT_OK")
"""
        )
        assertEquals("LINT_OK", out)
    }

    @Test
    fun `test package manager stdlib inspection wheel install and uninstall offline`() {
        val siteDir = Files.createTempDirectory("pyforge_pkg_site_").toFile()
        try {
            val out = runHostPython(
                """
import io, json, os, zipfile
import pyforge_pkg

site_dir = ${pyStr(siteDir.absolutePath)}
catalog = json.loads(pyforge_pkg.list_libraries(site_dir))
stdlib_names = {item["name"] for item in catalog["stdlib"]}
for required in ["math", "random", "datetime", "os", "sys", "json", "re", "collections", "itertools", "statistics"]:
    assert required in stdlib_names, f"Missing stdlib {required}"

incompat = {item["name"] for item in catalog["incompatible"]}
assert "tkinter" in incompat
assert "torch" in incompat

# Simulate installing a pure-python wheel offline and verifying import + uninstall via RECORD
buf = io.BytesIO()
with zipfile.ZipFile(buf, "w") as zf:
    zf.writestr("demo_pure/__init__.py", "__version__ = '1.2.3'\ndef ping():\n    return 'pong'\n")
    zf.writestr("demo_pure-1.2.3.dist-info/METADATA", "Metadata-Version: 2.1\nName: demo-pure\nVersion: 1.2.3\nSummary: Pure demo\n")
    zf.writestr("demo_pure-1.2.3.dist-info/RECORD", "demo_pure/__init__.py,,\ndemo_pure-1.2.3.dist-info/METADATA,,\ndemo_pure-1.2.3.dist-info/RECORD,,\n")

buf.seek(0)
with zipfile.ZipFile(buf, "r") as zf:
    zf.extractall(site_dir)

verify = json.loads(pyforge_pkg.verify_import("demo_pure", site_dir))
assert verify["ok"] is True, verify
assert verify["version"] == "1.2.3", verify

catalog2 = json.loads(pyforge_pkg.list_libraries(site_dir))
installed_map = {item["name"]: item for item in catalog2["installed"]}
assert "demo-pure" in installed_map
assert installed_map["demo-pure"]["removable"] is True

uninstalled = json.loads(pyforge_pkg.uninstall_package("demo-pure", site_dir))
assert uninstalled["ok"] is True, uninstalled
assert not os.path.exists(os.path.join(site_dir, "demo_pure"))
print("PKG_OK")
"""
            )
            assertEquals("PKG_OK", out)
        } finally {
            siteDir.deleteRecursively()
        }
    }
}
