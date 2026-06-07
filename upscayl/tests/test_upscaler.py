import upscaler


def test_build_command_gpu_uses_device_zero():
    cmd = upscaler.build_command(
        "/in/a.png", "/out/a_4x.png", "realesrgan-x4plus", 4,
        gpu=True, models_dir="/app/models", binary="realesrgan-ncnn-vulkan",
    )
    assert cmd == [
        "realesrgan-ncnn-vulkan",
        "-i", "/in/a.png", "-o", "/out/a_4x.png",
        "-n", "realesrgan-x4plus", "-s", "4",
        "-m", "/app/models", "-g", "0",
    ]


def test_build_command_cpu_uses_device_minus_one():
    cmd = upscaler.build_command("/in/a.png", "/out/a.png", "m", 2, gpu=False)
    assert "-g" in cmd and cmd[cmd.index("-g") + 1] == "-1"
    assert cmd[cmd.index("-s") + 1] == "2"


def test_list_models_returns_param_stems_sorted(tmp_path):
    (tmp_path / "realesrgan-x4plus.param").write_text("x")
    (tmp_path / "realesrgan-x4plus.bin").write_text("x")
    (tmp_path / "4x_NMKD-Superscale-SP_178000_G.param").write_text("x")
    (tmp_path / "notes.txt").write_text("ignore")
    assert upscaler.list_models(str(tmp_path)) == [
        "4x_NMKD-Superscale-SP_178000_G", "realesrgan-x4plus",
    ]


def test_list_models_missing_dir_returns_empty():
    assert upscaler.list_models("/no/such/dir") == []


import subprocess
from types import SimpleNamespace
import os


def test_run_upscale_missing_input_returns_error():
    res = upscaler.run_upscale("/no/input.png", "/out/o.png")
    assert res["status"] == "error"
    assert "input not found" in res["error"]


def test_run_upscale_success(tmp_path, monkeypatch):
    inp = tmp_path / "in.png"; inp.write_text("img")
    out = tmp_path / "out" / "in_4x.png"
    def fake_run(cmd, **kw):
        os.makedirs(os.path.dirname(out), exist_ok=True)
        out.write_text("upscaled")
        return SimpleNamespace(returncode=0, stdout="done", stderr="")
    monkeypatch.setattr(subprocess, "run", fake_run)
    res = upscaler.run_upscale(str(inp), str(out), binary="fake")
    assert res == {"status": "success", "output": str(out)}


def test_run_upscale_nonzero_exit_returns_stderr(tmp_path, monkeypatch):
    inp = tmp_path / "in.png"; inp.write_text("img")
    monkeypatch.setattr(subprocess, "run",
                        lambda cmd, **kw: SimpleNamespace(returncode=1, stdout="", stderr="model not found"))
    res = upscaler.run_upscale(str(inp), str(tmp_path / "o.png"), binary="fake")
    assert res["status"] == "error" and "model not found" in res["error"]


def test_run_upscale_timeout_returns_error(tmp_path, monkeypatch):
    inp = tmp_path / "in.png"; inp.write_text("img")
    def boom(cmd, **kw):
        raise subprocess.TimeoutExpired(cmd, 300)
    monkeypatch.setattr(subprocess, "run", boom)
    res = upscaler.run_upscale(str(inp), str(tmp_path / "o.png"), binary="fake", timeout=300)
    assert res["status"] == "error" and "timed out" in res["error"]


def test_run_upscale_success_but_no_output_is_error(tmp_path, monkeypatch):
    inp = tmp_path / "in.png"; inp.write_text("img")
    monkeypatch.setattr(subprocess, "run",
                        lambda cmd, **kw: SimpleNamespace(returncode=0, stdout="", stderr=""))
    res = upscaler.run_upscale(str(inp), str(tmp_path / "missing.png"), binary="fake")
    assert res["status"] == "error" and "no output" in res["error"]
