# Hacking Guide

## Debugging process

Start the app. The tray icon will be shown on the menu bar.
And so, it also starts the backend server.

    MEETNOTE3_PORT=9090 MEETNOTE3_CORS=localhost:8080 ./gradlew :meetnote3:runDebugExecutableNative

Following command will start the debugging frontend server.

    ./gradlew :frontend:jsRun

Then open the browser and access to `http://localhost:8080/`.

## Environment Variable for :meetnote3 module

- `MEETNOTE3_CORS=localhost:8080` to allow the request from the external host for debugging.
- `MEETNOTE3_PORT=9090` to specify the port number for the backend server.
