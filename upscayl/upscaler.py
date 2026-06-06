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
