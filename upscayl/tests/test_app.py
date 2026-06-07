import app as app_module


def make_client():
    app_module.app.config.update(TESTING=True)
    return app_module.app.test_client()


def _roots(tmp_path, monkeypatch):
    in_root = tmp_path / "input"
    in_root.mkdir()
    out_root = tmp_path / "output"
    out_root.mkdir()
    monkeypatch.setattr(app_module, "INPUT_ROOT", str(in_root))
    monkeypatch.setattr(app_module, "OUTPUT_ROOT", str(out_root))
    monkeypatch.setattr(app_module.upscaler, "list_models", lambda *a, **k: ["realesrgan-x4plus"])
    return in_root, out_root


def test_upscale_requires_input_and_output():
    r = make_client().post("/api/upscale", json={"model": "m"})
    assert r.status_code == 200
    assert r.get_json()["status"] == "error"


def test_upscale_delegates_to_run_upscale(tmp_path, monkeypatch):
    in_root, out_root = _roots(tmp_path, monkeypatch)
    captured = {}

    def fake_run(**kw):
        captured.update(kw)
        return {"status": "success", "output": kw["output_path"]}

    monkeypatch.setattr(app_module.upscaler, "run_upscale", fake_run)
    in_path = str(in_root / "x.png")
    out_path = str(out_root / "x_4x.png")
    r = make_client().post("/api/upscale", json={
        "input": in_path, "output": out_path,
        "model": "realesrgan-x4plus", "scale": 4, "gpu": True,
    })
    assert r.get_json() == {"status": "success", "output": out_path}
    assert captured["model"] == "realesrgan-x4plus" and captured["scale"] == 4
    assert captured["input_path"] == in_path and captured["gpu"] is True


def test_upscale_rejects_input_outside_root(tmp_path, monkeypatch):
    in_root, out_root = _roots(tmp_path, monkeypatch)
    ran = {"called": False}
    monkeypatch.setattr(app_module.upscaler, "run_upscale",
                        lambda **kw: ran.__setitem__("called", True))
    r = make_client().post("/api/upscale", json={
        "input": str(tmp_path / "evil" / ".." / "etc" / "passwd"),
        "output": str(out_root / "x_4x.png"),
        "model": "realesrgan-x4plus",
    })
    body = r.get_json()
    assert body["status"] == "error" and "input" in body["error"]
    assert ran["called"] is False


def test_upscale_rejects_output_outside_root(tmp_path, monkeypatch):
    in_root, out_root = _roots(tmp_path, monkeypatch)
    monkeypatch.setattr(app_module.upscaler, "run_upscale", lambda **kw: {"status": "success"})
    r = make_client().post("/api/upscale", json={
        "input": str(in_root / "x.png"),
        "output": str(tmp_path / "elsewhere" / "x_4x.png"),
        "model": "realesrgan-x4plus",
    })
    body = r.get_json()
    assert body["status"] == "error" and "output" in body["error"]


def test_upscale_rejects_unknown_model(tmp_path, monkeypatch):
    in_root, out_root = _roots(tmp_path, monkeypatch)
    monkeypatch.setattr(app_module.upscaler, "run_upscale", lambda **kw: {"status": "success"})
    r = make_client().post("/api/upscale", json={
        "input": str(in_root / "x.png"), "output": str(out_root / "x_4x.png"),
        "model": "../../evil",
    })
    body = r.get_json()
    assert body["status"] == "error" and "unknown model" in body["error"]


def test_models_endpoint(monkeypatch):
    monkeypatch.setattr(app_module.upscaler, "list_models", lambda *a, **k: ["realesrgan-x4plus"])
    r = make_client().get("/api/models")
    assert r.get_json() == {"models": ["realesrgan-x4plus"]}


def test_health_ok():
    r = make_client().get("/api/health")
    assert r.status_code == 200 and r.get_json()["status"] == "ok"
