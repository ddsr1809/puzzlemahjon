"""Genera el icono de la app a partir de unos pocos números.

Escribe el icono adaptable (XML vectorial, Android 8+), los PNG para Android 7 y el icono
de 512 px que pide la ficha de Google Play. Hace falta Python 3 con Pillow:

    pip install Pillow
    python3 tools/make_icon.py app/src/main/res store

Se ejecuta desde la carpeta android/. Solo hace falta volver a lanzarlo si cambias el
diseño o los colores de aquí abajo.
"""
import math, os, sys
from PIL import Image, ImageDraw

RES = sys.argv[1]      # .../app/src/main/res
STORE = sys.argv[2]    # carpeta para el icono de la tienda

COBALT = "#1F4E9C"; IVORY = "#FFF8EA"; OCHRE = "#E3A21A"
C = 54.0               # centro del lienzo de 108 dp
RING_R, RING_W = 30.5, 2.5
STAR_OUT, STAR_IN = 25.0, 12.5
DOT_R = 6.0

def star(cx, cy, outer, inner, n=8):
    pts = []
    for i in range(n * 2):
        r = outer if i % 2 == 0 else inner
        a = math.radians(-90 + 180.0 * i / n)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts

def fmt(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s == "-0" else s

def circle_path(cx, cy, r):
    return f"M{fmt(cx - r)},{fmt(cy)}a{fmt(r)},{fmt(r)} 0 1,0 {fmt(2 * r)},0a{fmt(r)},{fmt(r)} 0 1,0 {fmt(-2 * r)},0z"

def poly_path(pts):
    return "M" + " L".join(f"{fmt(x)},{fmt(y)}" for x, y in pts) + " Z"

HEAD = '''<?xml version="1.0" encoding="utf-8"?>
<!-- {comment} Generado por tools/make_icon.py: no lo edites a mano. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
'''

def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)

ring = circle_path(C, C, RING_R)
starp = poly_path(star(C, C, STAR_OUT, STAR_IN))
dot = circle_path(C, C, DOT_R)

write(f"{RES}/drawable/ic_launcher_foreground.xml", HEAD.format(comment="Primer plano del icono: estrella de azulejo dentro de un aro.") + f'''    <path
        android:pathData="{ring}"
        android:strokeColor="{IVORY}"
        android:strokeWidth="{fmt(RING_W)}" />
    <path
        android:fillColor="{IVORY}"
        android:pathData="{starp}" />
    <path
        android:fillColor="{OCHRE}"
        android:pathData="{dot}" />
</vector>
''')

write(f"{RES}/drawable/ic_launcher_monochrome.xml", HEAD.format(comment="Icono de un solo color para los iconos temáticos de Android 13+.") + f'''    <path
        android:pathData="{ring}"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="{fmt(RING_W)}" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="{starp}" />
</vector>
''')

write(f"{RES}/mipmap-anydpi-v26/ic_launcher.xml", '''<?xml version="1.0" encoding="utf-8"?>
<!-- Icono adaptable (Android 8+). Android 7 usa los PNG de las carpetas mipmap-*dpi. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
''')

write(f"{RES}/values/colors.xml", f'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">{COBALT}</color>
</resources>
''')

def render(size, scale, plate):
    """Dibuja el icono en un lienzo de size px. scale = px por dp del diseño; plate = (margen, radio) en px o None."""
    ss = 4
    img = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    S = size * ss
    if plate is None:
        d.rectangle([0, 0, S, S], fill=COBALT)
    else:
        m, r = plate
        d.rounded_rectangle([m * ss, m * ss, S - m * ss - 1, S - m * ss - 1], radius=r * ss, fill=COBALT)
    c = S / 2; k = scale * ss
    d.ellipse([c - (RING_R + RING_W / 2) * k, c - (RING_R + RING_W / 2) * k, c + (RING_R + RING_W / 2) * k, c + (RING_R + RING_W / 2) * k], fill=IVORY)
    d.ellipse([c - (RING_R - RING_W / 2) * k, c - (RING_R - RING_W / 2) * k, c + (RING_R - RING_W / 2) * k, c + (RING_R - RING_W / 2) * k], fill=COBALT)
    d.polygon([(c + (x - C) * k, c + (y - C) * k) for x, y in star(C, C, STAR_OUT, STAR_IN)], fill=IVORY)
    d.ellipse([c - DOT_R * k, c - DOT_R * k, c + DOT_R * k, c + DOT_R * k], fill=OCHRE)
    return img.resize((size, size), Image.LANCZOS)

# Android 7: cuadrado redondeado de 44 dp dentro de un lienzo de 48 dp.
for name, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    dp = px / 48.0
    os.makedirs(f"{RES}/mipmap-{name}", exist_ok=True)
    render(px, 44.0 / 72.0 * dp, (2 * dp, 9 * dp)).save(f"{RES}/mipmap-{name}/ic_launcher.png", optimize=True)

# Google Play: 512 px, cuadrado completo (Play aplica su propia máscara).
os.makedirs(STORE, exist_ok=True)
render(512, 512 / 72.0, None).convert("RGB").save(f"{STORE}/play-icon-512.png", optimize=True)
print("ok")
