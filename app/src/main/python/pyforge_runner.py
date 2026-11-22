import builtins
import ctypes
import io
import os
import sys
import threading
import time
import traceback


def ensure_site_packages(site_packages_dir):
    if site_packages_dir:
        os.makedirs(site_packages_dir, exist_ok=True)
        if site_packages_dir not in sys.path:
            sys.path.insert(0, site_packages_dir)


class _StreamProxy(io.TextIOBase):
    def __init__(self, callback, is_error=False):
        self._callback = callback
        self._is_error = is_error
        self._closed = False

    def writable(self):
        return True

    def write(self, s):
        if not s:
            return 0
        text = str(s)
        if self._is_error:
            self._callback.onStderr(text)
        else:
            self._callback.onStdout(text)
        return len(text)

    def writelines(self, lines):
        for line in lines:
            self.write(line)

    def flush(self):
        pass

    def isatty(self):
        return False

    @property
    def encoding(self):
        return "utf-8"


class _StdinProxy(io.TextIOBase):
    def __init__(self, input_provider, stdout_proxy):
        self._input_provider = input_provider
        self._stdout = stdout_proxy

    def readable(self):
        return True

    def readline(self, size=-1):
        val = self._input_provider.requestInput("")
        if val is None:
            raise KeyboardInterrupt("Execution stopped while waiting for input()")
        line = str(val) + "\n"
        if size is not None and size > 0:
            return line[:size]
        return line


def _configure_matplotlib(plot_output_dir, output_callback):
    os.environ["MPLBACKEND"] = "Agg"
    try:
        import matplotlib
        matplotlib.use("Agg", force=True)
        import matplotlib.pyplot as plt

        if not getattr(plt, "_pyforge_patched", False):
            def _pyforge_show(*args, **kwargs):
                os.makedirs(plot_output_dir, exist_ok=True)
                fignums = plt.get_fignums()
                for idx, num in enumerate(fignums):
                    fig = plt.figure(num)
                    ts = int(time.time() * 1000)
                    out_path = os.path.join(plot_output_dir, f"plot_{ts}_{num}_{idx}.png")
                    fig.savefig(out_path, dpi=150, bbox_inches="tight", facecolor="#0D1117", edgecolor="none")
                    output_callback.onPlot(out_path)
                plt.close("all")

            plt.show = _pyforge_show
            plt._pyforge_patched = True
    except Exception:
        pass


def interrupt_thread(thread_id):
    if not thread_id:
        return False
    res = ctypes.pythonapi.PyThreadState_SetAsyncExc(
        ctypes.c_ulong(int(thread_id)),
        ctypes.py_object(KeyboardInterrupt),
    )
    if res > 1:
        ctypes.pythonapi.PyThreadState_SetAsyncExc(ctypes.c_ulong(int(thread_id)), None)
        return False
    return res == 1


def execute_script(
    file_path,
    code,
    working_dir,
    site_packages_dir,
    plot_output_dir,
    input_provider,
    output_callback,
):
    ensure_site_packages(site_packages_dir)

    if working_dir:
        os.makedirs(working_dir, exist_ok=True)
        if working_dir not in sys.path:
            sys.path.insert(0, working_dir)

    prev_cwd = os.getcwd()
    prev_stdout = sys.stdout
    prev_stderr = sys.stderr
    prev_stdin = sys.stdin
    prev_input = builtins.input

    stdout_proxy = _StreamProxy(output_callback, is_error=False)
    stderr_proxy = _StreamProxy(output_callback, is_error=True)
    stdin_proxy = _StdinProxy(input_provider, stdout_proxy)

    def _custom_input(prompt=""):
        if prompt:
            stdout_proxy.write(str(prompt))
        val = input_provider.requestInput(str(prompt) if prompt else "")
        if val is None:
            raise KeyboardInterrupt("Execution stopped by user")
        result = str(val)
        stdout_proxy.write(result + "\n")
        return result

    thread_id = threading.get_ident()
    output_callback.onStarted(thread_id)

    exit_code = 0
    try:
        if working_dir:
            os.chdir(working_dir)

        _configure_matplotlib(plot_output_dir, output_callback)

        sys.stdout = stdout_proxy
        sys.stderr = stderr_proxy
        sys.stdin = stdin_proxy
        builtins.input = _custom_input

        script_name = file_path if file_path else "<script.py>"
        exec_globals = {
            "__name__": "__main__",
            "__file__": script_name,
            "__package__": None,
            "__builtins__": builtins,
        }
        compiled = compile(code, script_name, "exec")
        exec(compiled, exec_globals)
    except SystemExit as exc:
        code_val = exc.code
        if code_val is None:
            exit_code = 0
        elif isinstance(code_val, int):
            exit_code = code_val
        else:
            stderr_proxy.write(str(code_val) + "\n")
            exit_code = 1
    except KeyboardInterrupt as exc:
        msg = str(exc) if str(exc) else "KeyboardInterrupt: Execution interrupted"
        stderr_proxy.write(f"\n[Interrupted] {msg}\n")
        exit_code = 130
    except Exception:
        stderr_proxy.write(traceback.format_exc())
        exit_code = 1
    finally:
        sys.stdout = prev_stdout
        sys.stderr = prev_stderr
        sys.stdin = prev_stdin
        builtins.input = prev_input
        try:
            os.chdir(prev_cwd)
        except Exception:
            pass

    return exit_code
