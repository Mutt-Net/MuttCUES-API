from flask import Flask, request, jsonify
import upscaler

app = Flask(__name__)


@app.post("/api/upscale")
def upscale():
    body = request.get_json(force=True, silent=True) or {}
    input_path = body.get("input")
    output_path = body.get("output")
    if not input_path or not output_path:
        return jsonify({"status": "error", "error": "input and output are required"}), 200
    result = upscaler.run_upscale(
        input_path=input_path,
        output_path=output_path,
        model=body.get("model") or upscaler.DEFAULT_MODEL,
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
