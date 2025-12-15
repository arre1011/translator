# Face-to-Face Translator - Mobile App

React Native / Expo mobile app for real-time face-to-face translation.

## Requirements

- Node.js 18+
- Xcode 15+ (for iOS development)
- Android Studio (for Android development)
- Physical device or simulator with microphone access

## Quick Start

```bash
# Install dependencies
npm install

# Generate native projects (required for WebRTC)
npx expo prebuild

# Run on iOS
npx expo run:ios

# Run on Android
npx expo run:android
```

> **Important**: This app requires a **development build** and cannot run in Expo Go due to WebRTC native dependencies.

## Configuration

### Backend API URL

Edit `services/api/apiClient.ts` to set your backend URL:

```typescript
const getBaseUrl = (): string => {
  if (__DEV__) {
    // For iOS Simulator: localhost works
    // For Android Emulator: use 10.0.2.2
    // For physical device: use your computer's IP
    return 'http://192.168.1.XXX:8080/api';
  }
  return 'https://api.your-domain.com/api';
};
```

### App Settings

Edit `app.json` for:
- App name and slug
- Bundle identifier
- Permissions
- Icons and splash screen

## Project Structure

```
mobile-app/
├── app/                        # Expo Router screens
│   ├── _layout.tsx             # Root layout
│   └── (tabs)/
│       ├── _layout.tsx         # Tab navigation
│       └── index.tsx           # Main translator screen
├── components/
│   └── translator/             # Translation UI components
│       ├── SpeakerPanel.tsx    # Speaker container
│       ├── MicButton.tsx       # Microphone button
│       ├── LanguageSelector.tsx # Language picker
│       ├── TranscriptDisplay.tsx
│       ├── TranslationDisplay.tsx
│       └── PlayAudioButton.tsx
├── services/
│   ├── api/                    # Backend API client
│   │   ├── apiClient.ts        # Axios instance
│   │   ├── realtimeApi.ts      # API endpoints
│   │   └── types.ts            # TypeScript types
│   └── webrtc/
│       └── webrtcService.ts    # WebRTC management
├── hooks/
│   └── use-translation-session.ts  # Main translation hook
├── constants/
│   └── languages.ts            # Supported languages
└── assets/                     # Images, fonts, icons
```

## Key Components

| Component | Description |
|-----------|-------------|
| `SpeakerPanel` | Container for each speaker's UI (language, mic, text) |
| `MicButton` | Large microphone button with recording state |
| `LanguageSelector` | Modal for selecting source/target language |
| `TranscriptDisplay` | Shows the speech transcript |
| `TranslationDisplay` | Shows the translated text |
| `useTranslationSession` | Hook managing WebRTC and session state |

## Available Scripts

| Script | Description |
|--------|-------------|
| `npm start` | Start Expo development server |
| `npm run ios` | Run on iOS (requires prebuild) |
| `npm run android` | Run on Android (requires prebuild) |
| `npm run web` | Run web version |
| `npm run lint` | Run ESLint |
| `npm run prebuild` | Generate native projects |
| `npm run prebuild:clean` | Clean and regenerate native projects |

## Building for Production

### iOS

```bash
# Build for App Store
npx expo build:ios

# Or use EAS Build
eas build --platform ios
```

### Android

```bash
# Build APK/AAB
npx expo build:android

# Or use EAS Build
eas build --platform android
```

## Troubleshooting

### WebRTC not working

```bash
# Clean rebuild
npx expo prebuild --clean
npm install
npx expo run:ios
```

### "Cannot connect to backend"

1. Check backend is running on port 8080
2. For physical devices, use your computer's local IP (not localhost)
3. Ensure device and computer are on the same network

### iOS Pod installation fails

```bash
cd ios
pod deintegrate
pod install
cd ..
npx expo run:ios
```

### Android Gradle build fails

```bash
cd android
./gradlew clean
cd ..
npx expo run:android
```

### Microphone permission denied

1. iOS: Go to Settings > Privacy > Microphone
2. Android: Go to Settings > Apps > Translator > Permissions

## Development Notes

- **Expo Go**: Not supported due to WebRTC native module
- **Hot Reload**: Supported for JS changes, native changes need rebuild
- **Debugging**: Press `j` in Metro terminal for debugger

## Learn More

- [Expo Documentation](https://docs.expo.dev/)
- [React Native WebRTC](https://github.com/react-native-webrtc/react-native-webrtc)
- [Expo Router](https://expo.github.io/router/docs/)
