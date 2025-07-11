# Visa Bulletin Checker

This is a command-line application, built in Kotlin, that checks your US Visa Priority Date against the latest official Visa Bulletin from the US Department of State.

## Features

*   **Automatic Updates**: Automatically fetches the latest visa bulletin URL so you're always checking against the most recent data.
*   **Caching**: Caches the bulletin data locally to speed up subsequent checks and reduce network requests.
*   **Employment-Based Categories**: Parses both "Final Action Dates" and "Dates for Filing" for employment-based visa categories.
*   **Interactive Check**: Allows you to interactively enter your priority date, category, and region to see your status.

## How to Run

1.  **Build the application:**
    ```bash
    ./gradlew build
    ```

2.  **Run the application:**
    ```bash
    ./gradlew :app:run
    ```

3.  **Follow the prompts:**
    *   **Priority Date**: You will be prompted to enter your priority date. You must use the `dd-MM-yyyy` format (e.g., `26-10-2023`).
    *   **Employment Category**: Enter your category (e.g., `1st`, `2nd`, `3rd`).
    *   **Region/Country**: Enter your region or country of chargeability (e.g., `INDIA`, `CHINA- MAINLAND BORN`).

The application will then tell you if your priority date is current and provide an estimated wait time if it is not.

## Development

This project uses Gradle for dependency management. The main application logic is in `app/src/main/kotlin/org/example/App.kt`.
Unit tests are located in `app/src/test/kotlin/org/example/`. You can run them with:
```bash
./gradlew test
```