import os
from flask import Flask, request, jsonify
import upscaler

app = Flask(__name__)

INPUT_ROOT = os.environ.get("INPUT_ROOT", "/app/io/input")
OUTPUT_ROOT = os.environ.get("OUTPUT_ROOT", "/app/io/output")


def _within(path, root):
    resolved = os.path.realpath(path)
    root_real = os.path.realpath(root)
    try:
        return os.path.commonpath([resolved, root_real]) == root_real
    except ValueError:
        return False


@app.post("/api/upscale")
def upscale():
    body = request.get_json(force=True, silent=True) or {}
    input_path = body.get("input")
    output_path = body.get("output")
    if not input_path or not output_path:
        return jsonify({"status": "error", "error": "input and output are required"}), 200
    if not _within(input_path, INPUT_ROOT):
        return jsonify({"status": "error", "error": "input path is outside the allowed input root"}), 200
    if not _within(output_path, OUTPUT_ROOT):
        return jsonify({"status": "error", "error": "output path is outside the allowed output root"}), 200
    model = body.get("model") or upscaler.DEFAULT_MODEL
    if model not in set(upscaler.list_models()):
        return jsonify({"status": "error", "error": f"unknown model: {model}"}), 200
    result = upscaler.run_upscale(
        input_path=input_path,
        output_path=output_path,
        model=model,
        scale=int(body.get("scale") or upscaler.DEFAULT_SCALE),
        gpu=bool(body.get("gpu", True)),
    )
    return jsonify(result), 200


@app.get("/api/models")
def models():
    return jsonify({"models": upscaler.list_models()}), 200


@app.get("/api/health")
def health():
    return jsonify({"status": "ok"}), 200
