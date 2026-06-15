"""CUDA Real-ESRGAN upscaler.

This host's containerized Vulkan is broken (NVIDIA driver-580 ICD won't
initialize in any container), but CUDA/compute works fine — so we run
Real-ESRGAN on the GPU via PyTorch + spandrel instead of the Vulkan-based
upscayl-ncnn binary. spandrel auto-detects the architecture from the .pth
(RealESRGAN/ESRGAN/NMKD/...), so arbitrary ESRGAN weights dropped into
MODELS_DIR work by filename stem.

Public contract is unchanged (app.py depends on it):
  DEFAULT_MODEL, DEFAULT_SCALE, list_models(models_dir),
  run_upscale(input_path, output_path, model, scale, gpu, models_dir) ->
    {"status": "success", "output": path} | {"status": "error", "error": msg}
"""
import os
import logging

import numpy as np
from PIL import Image
import torch
from spandrel import ModelLoader, ImageModelDescriptor

log = logging.getLogger(__name__)

DEFAULT_MODELS_DIR = os.environ.get("MODELS_DIR", "/app/models")
DEFAULT_MODEL = "realesrgan-x4plus"
DEFAULT_SCALE = 4
# Tile size for inference; keeps VRAM bounded on large textures. 0 = no tiling.
TILE = int(os.environ.get("UPSCALE_TILE", "512"))
TILE_PAD = int(os.environ.get("UPSCALE_TILE_PAD", "32"))

# Pipeline model name -> weight filename in MODELS_DIR. Any *.pth in MODELS_DIR
# is also selectable by its filename stem (so the NMKD normals model etc. work
# just by dropping the file into the models volume).
_ALIASES = {
    "realesrgan-x4plus": "RealESRGAN_x4plus.pth",
    "realesrgan-x2plus": "RealESRGAN_x2plus.pth",
    "realesr-general-x4v3": "realesr-general-x4v3.pth",
}

_cache: dict = {}  # (model, device) -> ImageModelDescriptor


def _weight_path(model, models_dir):
    fn = _ALIASES.get(model)
    if fn and os.path.exists(os.path.join(models_dir, fn)):
        return os.path.join(models_dir, fn)
    direct = os.path.join(models_dir, f"{model}.pth")
    return direct if os.path.exists(direct) else None


def list_models(models_dir=DEFAULT_MODELS_DIR):
    names = {name for name, fn in _ALIASES.items()
             if os.path.exists(os.path.join(models_dir, fn))}
    if os.path.isdir(models_dir):
        names.update(os.path.splitext(f)[0] for f in os.listdir(models_dir)
                     if f.endswith(".pth"))
    return sorted(names)


def _device(gpu):
    return "cuda" if (gpu and torch.cuda.is_available()) else "cpu"


def _load(model, models_dir, device):
    key = (model, device)
    if key in _cache:
        return _cache[key]
    path = _weight_path(model, models_dir)
    if path is None:
        raise FileNotFoundError(f"no .pth weights for model '{model}' in {models_dir}")
    desc = ModelLoader().load_from_file(path)
    if not isinstance(desc, ImageModelDescriptor):
        raise ValueError(f"'{model}' is not an image-to-image super-resolution model")
    desc.to(device).eval()
    _cache[key] = desc
    return desc


@torch.inference_mode()
def _infer(desc, t, tile):
    """t: (1,3,H,W) float[0,1] on the model's device. tile<=0 or >= image = one
    pass; otherwise tiled to bound VRAM."""
    scale = desc.scale
    _, _, h, w = t.shape
    if tile <= 0 or (h <= tile and w <= tile):
        return desc(t).clamp_(0, 1)

    out = torch.zeros((1, 3, h * scale, w * scale), device=t.device, dtype=t.dtype)
    for y in range(0, h, tile):
        for x in range(0, w, tile):
            y0, x0 = max(y - TILE_PAD, 0), max(x - TILE_PAD, 0)
            y1, x1 = min(y + tile + TILE_PAD, h), min(x + tile + TILE_PAD, w)
            up = desc(t[:, :, y0:y1, x0:x1]).clamp_(0, 1)
            # source region within this (padded) tile, mapped to output scale
            sy, sx = (y - y0) * scale, (x - x0) * scale
            oy0, ox0 = y * scale, x * scale
            oy1 = min((y + tile) * scale, h * scale)
            ox1 = min((x + tile) * scale, w * scale)
            out[:, :, oy0:oy1, ox0:ox1] = up[:, :, sy:sy + (oy1 - oy0), sx:sx + (ox1 - ox0)]
    return out


def _infer_resilient(desc, t, device):
    """Run inference, halving the tile on CUDA OOM down to a 64px floor. The GPU
    is shared, so the free slice varies job-to-job; shrinking the tile shrinks
    the allocation to fit whatever is currently free."""
    tile = TILE if TILE > 0 else max(int(t.shape[-2]), int(t.shape[-1]))
    while True:
        try:
            if device == "cuda":
                torch.cuda.empty_cache()
            return _infer(desc, t, tile)
        except RuntimeError as exc:
            if "out of memory" not in str(exc).lower() or tile <= 64:
                raise
            if device == "cuda":
                torch.cuda.empty_cache()
            tile = max(64, tile // 2)
            log.warning("CUDA OOM — retrying upscale with tile=%d", tile)


def run_upscale(input_path, output_path, model=DEFAULT_MODEL, scale=DEFAULT_SCALE,
                gpu=True, models_dir=DEFAULT_MODELS_DIR, **_):
    if not os.path.exists(input_path):
        return {"status": "error", "error": f"input not found: {input_path}"}
    out_dir = os.path.dirname(output_path)
    if out_dir:
        os.makedirs(out_dir, exist_ok=True)

    try:
        device = _device(gpu)
        desc = _load(model, models_dir, device)

        src = Image.open(input_path)
        has_alpha = src.mode in ("RGBA", "LA", "PA") or "transparency" in src.info
        rgb = src.convert("RGB")

        arr = np.asarray(rgb, dtype=np.float32) / 255.0          # H,W,3
        t = torch.from_numpy(arr).permute(2, 0, 1).unsqueeze(0).to(device)
        out_t = _infer_resilient(desc, t, device)
        out_np = (out_t.squeeze(0).permute(1, 2, 0).cpu().numpy() * 255.0).round().astype(np.uint8)
        result = Image.fromarray(out_np, mode="RGB")

        # Preserve alpha: the model is RGB-only, so scale alpha with Lanczos to match.
        if has_alpha:
            alpha = src.convert("RGBA").getchannel("A").resize(result.size, Image.LANCZOS)
            result = result.convert("RGBA")
            result.putalpha(alpha)

        result.save(output_path)
        if device == "cuda":
            torch.cuda.empty_cache()
    except FileNotFoundError as exc:
        return {"status": "error", "error": str(exc)}
    except Exception as exc:                       # report any engine error to the API
        log.exception("upscale failed")
        return {"status": "error", "error": f"{type(exc).__name__}: {exc}"}

    if not os.path.exists(output_path):
        return {"status": "error", "error": "engine reported success but produced no output file"}
    return {"status": "success", "output": output_path}
