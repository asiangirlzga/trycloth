# Virtual Try-On (on-device, synthetic avatars)

Android app (Kotlin + Jetpack Compose) that fits lingerie garments onto built-in
synthetic 3D female avatars. Everything runs on the phone: no server, no GPU,
no AI model, no internet permission. Target device: Samsung Galaxy F06 5G.

## How it works

1. **Pick a model** - the avatar grid is read from `assets/catalog.json`.
2. **Tap garments** - one top and one bottom at a time (tap again to remove).
3. **Fit** - each avatar has named body anchors (`bustL`, `bustR`, `hipL`, `hipR`).
   Each garment has two matching points on its own image. `Fitter.kt` computes the
   scale + rotation + translation that pins the garment points to the avatar anchors.
4. **Fine-tune** - drag, pinch and twist on the preview to adjust the selected garment.
5. **Save / Share** - exports a PNG to `Pictures/TryOn` (no storage permission needed).

## Open and run

1. Open the `TryOnApp` folder in Android Studio (Koala or newer). It downloads
   Gradle 8.7 and the SDK pieces on first sync.
2. Enable USB debugging on the F06 and press Run.

## Replace the placeholder art with your own

Everything lives in `app/src/main/assets/`.

**Avatars** (`avatars/*.png`)
- Use renders of your own synthetic / 3D-rendered models: front-facing, standing,
  full body. Suggested size 600 x 1200 or similar (keep under about 1500 px tall).
- Add an entry to `catalog.json` and mark the anchors as fractions (0..1) of the
  image width and height:
  - `bustL`, `bustR` - centre of each side of the chest area
  - `hipL`, `hipR` - the hip points where waistbands sit
- Tip: open the image in any editor, read the pixel position, then divide by the
  image width (x) and height (y).

**Garments** (`garments/*.png`)
- Transparent PNG, front view, background removed.
- Add an entry to `catalog.json`:
  - `category`: `top` or `bottom`
  - `targets`: the two avatar anchor names to pin to, e.g. `["bustL","bustR"]`
  - `points`: the two matching points on the garment image (fractions 0..1)
- Keep garment PNGs under about 800 px wide to keep memory low.

## Responsible use

Avatars should be synthetic or 3D-rendered models, or people who have agreed to
be shown. The app deliberately has no "upload a photo of a person" mode.

## Project layout

```
app/src/main/
  assets/catalog.json        avatars, garments, anchor data
  assets/avatars, garments   images
  java/com/example/tryon/
    MainActivity.kt          navigation (picker -> try-on)
    data/Catalog.kt          models + JSON loader
    fit/Fitter.kt            anchor fitting + renderer
    ui/Screens.kt            Compose screens, gestures
    util/ImageIo.kt          save to gallery, share
```

## Notes

- I could not compile or run the project in my build environment (no Android SDK
  and no network), so it is untested on a device. If Android Studio reports a
  build error on first sync, send me the message and I'll fix it.
- The placeholders are simple flat mannequins and shapes, only meant to show the flow.
