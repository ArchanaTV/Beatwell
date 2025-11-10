# Assets Directory Structure

This directory contains all the static assets for the BeatWell Android application.

## Directory Structure

```
assets/
├── images/
│   ├── icons/           # App icons, button icons, etc.
│   ├── backgrounds/     # Background images and patterns
│   ├── logos/           # App logos and branding images
│   └── ui_elements/     # UI components, buttons, cards, etc.
├── fonts/               # Custom fonts (.ttf, .otf files)
├── sounds/              # Audio files (.mp3, .wav, .ogg)
└── data/                # JSON files, configuration files, etc.
```

## Usage Guidelines

### Images
- **Icons**: Store app icons, navigation icons, and small UI elements
- **Backgrounds**: Store background images, patterns, and large visual elements
- **Logos**: Store app logos, branding materials, and company images
- **UI Elements**: Store custom UI components, buttons, cards, and interface elements

### Fonts
- Place custom font files (.ttf, .otf) in the fonts directory
- Use descriptive names for font files
- Consider including font licenses if required

### Sounds
- Store audio files for notifications, sound effects, and music
- Use compressed formats like .mp3 or .ogg for better performance
- Keep file sizes reasonable for mobile app performance

### Data
- Store JSON configuration files
- Place static data files that don't change frequently
- Include any offline data or cache files

## Best Practices

1. **File Naming**: Use lowercase with underscores (e.g., `app_logo.png`, `button_background.xml`)
2. **File Formats**: 
   - Images: PNG for transparency, JPG for photos, SVG for scalable graphics
   - Audio: MP3 for music, OGG for sound effects
   - Fonts: TTF or OTF formats
3. **File Sizes**: Optimize images for mobile devices to reduce app size
4. **Organization**: Keep related assets in appropriate subdirectories
5. **Version Control**: Consider using Git LFS for large binary files

## Accessing Assets in Code

```kotlin
// Load image from assets
val inputStream = assets.open("images/logos/app_logo.png")
val bitmap = BitmapFactory.decodeStream(inputStream)

// Load font from assets
val typeface = Typeface.createFromAsset(assets, "fonts/custom_font.ttf")

// Load JSON data from assets
val jsonString = assets.open("data/config.json").bufferedReader().use { it.readText() }
```
