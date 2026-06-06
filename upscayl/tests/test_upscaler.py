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
