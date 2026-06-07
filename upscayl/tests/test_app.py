import app as app_module


def make_client():
    app_module.app.config.update(TESTING=True)
    return app_module.app.test_client()


def test_upscale_requires_input_and_output():
    r = make_client().post("/api/upscale", json={"model": "m"})
    assert r.status_code == 200
    assert r.get_json()["status"] == "error"


def test_upscale_delegates_to_run_upscale(monkeypatch):
    captured = {}
    def fake_run(**kw):
        captured.update(kw)
        return {"status": "success", "output": kw["output_path"]}
    monkeypatch.setattr(app_module.upscaler, "run_upscale", fake_run)
    r = make_client().post("/api/upscale", json={
        "input": "/app/io/input/x.png", "output": "/app/io/output/x_4x.png",
        "model": "realesrgan-x4plus", "scale": 4, "gpu": True,
    })
    assert r.get_json() == {"status": "success", "output": "/app/io/output/x_4x.png"}
    assert captured["model"] == "realesrgan-x4plus" and captured["scale"] == 4


def test_models_endpoint(monkeypatch):
    monkeypatch.setattr(app_module.upscaler, "list_models", lambda *a, **k: ["realesrgan-x4plus"])
    r = make_client().get("/api/models")
    assert r.get_json() == {"models": ["realesrgan-x4plus"]}


def test_health_ok():
    r = make_client().get("/api/health")
    assert r.status_code == 200 and r.get_json()["status"] == "ok"
