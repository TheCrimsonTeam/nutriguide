import io

import pytest
from PIL import Image
from app.main import normalize_image

def sample_image():
    buffer = io.BytesIO()
    Image.new("RGB", (240, 120), color=(220, 220, 220)).save(buffer, format="JPEG")
    return buffer.getvalue()

def test_valid_jpeg_is_normalized():
    result = normalize_image(sample_image(), "image/jpeg")
    with Image.open(io.BytesIO(result)) as image:
        assert image.format == "JPEG"
        assert image.size == (240, 120)

def test_unsupported_mime_is_rejected():
    with pytest.raises(ValueError):
        normalize_image(sample_image(), "image/gif")

def test_corrupt_image_is_rejected():
    with pytest.raises(ValueError):
        normalize_image(b"not a jpeg", "image/jpeg")
