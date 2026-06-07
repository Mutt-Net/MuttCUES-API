import os
import subprocess

DEFAULT_BINARY = os.environ.get("UPSCAYL_BINARY", "realesrgan-ncnn-vulkan")
DEFAULT_MODELS_DIR = os.environ.get("MODELS_DIR", "/app/models")
DEFAULT_TIMEOUT = int(os.environ.get("UPSCALE_TIMEOUT", "300"))
DEFAULT_MODEL = "realesrgan-x4plus"
DEFAULT_SCALE = 4


def build_command(input_path, output_path, model, scale, gpu=True,
                  models_dir=DEFAULT_MODELS_DIR, binary=DEFAULT_BINARY):
    return [
        binary,
        "-i", input_path,
        "-o", output_path,
        "-n", model,
        "-s", str(scale),
        "-m", models_dir,
        "-g", "0" if gpu else "-1",
    ]


def list_models(models_dir=DEFAULT_MODELS_DIR):
    if not os.path.isdir(models_dir):
        return []
    return sorted(
        os.path.splitext(f)[0]
        for f in os.listdir(models_dir)
        if f.endswith(".param")
    )


def run_upscale(input_path, output_path, model=DEFAULT_MODEL, scale=DEFAULT_SCALE,
                gpu=True, models_dir=DEFAULT_MODELS_DIR, binary=DEFAULT_BINARY,
                timeout=DEFAULT_TIMEOUT):
    if not os.path.exists(input_path):
        return {"status": "error", "error": f"input not found: {input_path}"}

    out_dir = os.path.dirname(output_path)
    if out_dir:
        os.makedirs(out_dir, exist_ok=True)

    cmd = build_command(input_path, output_path, model, scale, gpu, models_dir, binary)
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    except subprocess.TimeoutExpired:
        return {"status": "error", "error": f"upscale timed out after {timeout}s"}
    except FileNotFoundError:
        return {"status": "error", "error": f"binary not found: {binary}"}

    if proc.returncode != 0:
        msg = (proc.stderr or proc.stdout or "non-zero exit").strip()
        return {"status": "error", "error": msg[:1000]}
    if not os.path.exists(output_path):
        return {"status": "error", "error": "binary reported success but produced no output file"}
    return {"status": "success", "output": output_path}
