# Mapbox Demo 2 - Japanese Map Application

An Android application that displays a Japanese map with location tracking, search functionality, and navigation controls.

## Features

1. **Japanese Map Display**
   - Shows a Mapbox map with Japanese labels
   - Uses the Mapbox Streets style with Japanese language support

2. **Zoom & Location Controls**
   - Three vertical FloatingActionButtons in the bottom-left corner:
     - Zoom In
     - Zoom Out
     - Move to Current Location
   - Material Design compliant UI

3. **Place & POI Search**
   - Search bar at the top of the screen
   - Uses Mapbox Search Box API
   - Enables searching for POIs or place names (e.g., "cafe", "Lawson", "post office")
   - Search centered around user's current location or map center
   - Displays search results on the map

4. **Current Location Marker with Heading**
   - Displays user's current location on the map
   - Shows heading (bearing) of the user
   - Uses LocationComponentPlugin from Mapbox

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox (2021.3.1) or newer
- Android SDK 24 or higher
- Mapbox account with access tokens

### Configuration

1. **Create a local.properties file**
   - Copy the `local.properties.template` file to `local.properties`
   - Add your Android SDK path
   - Add your Mapbox access tokens:
     ```
     sdk.dir=/path/to/your/android/sdk
     MAPBOX_ACCESS_TOKEN=your_mapbox_access_token_here
     MAPBOX_DOWNLOADS_TOKEN=your_mapbox_downloads_token_here
     ```

2. **Obtaining Mapbox Tokens**
   - Create a Mapbox account at [mapbox.com](https://www.mapbox.com/)
   - Generate a public access token for the app
   - Generate a private downloads token for Maven repository access
   - Add both tokens to your `local.properties` file

### Building the Project
1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run the application on an emulator or physical device

## Technical Details

### Dependencies
- Mapbox Maps SDK for Android (10.16.1)
- Mapbox Search SDK (1.0.0-rc.5)
- AndroidX and Material Design components

### Permissions
The app requires the following permissions:
- `INTERNET`: For map loading and search functionality
- `ACCESS_FINE_LOCATION`: For precise location tracking
- `ACCESS_COARSE_LOCATION`: For approximate location tracking

## Project Structure

- `MainActivity.kt`: Main activity that handles map display, location tracking, and search
- `activity_main.xml`: Layout file defining the UI components
- `strings.xml`: String resources (with Japanese translations in `values-ja/strings.xml`)

## Notes
- The app uses the latest stable version of Mapbox SDK (10.x series)
- All API tokens are managed securely through local.properties
- Location permissions are requested and handled properly
- All UI components follow Material Design guidelines
