# hello_gemini# hello_gemini

## Project Description

This application fetches and parses US Visa Bulletins. It takes user input for Priority Date, Eligibility Category, and Country (All or one of the restricted countries). The output indicates if the date is current and estimates the expected wait period until it becomes current. The app caches old Visa Bulletin data, regularly checks for new bulletins, and updates when a new one is released.

## Current Status

The application is currently in development. The initial parsing logic has been implemented and is being tested.

## Implementation Plan

- [x] **Setup & Initial Debugging:** Fix build environment and initial parsing logic.
    - [x] Fix Gradle permission issues.
    - [x] Switch from `BasicExtractionAlgorithm` to `SpreadsheetExtractionAlgorithm` for better table parsing.
    - [x] Refactor `VisaBulletinParser` to handle the new table structure.
- [ ] **Verify Core Functionality:** Confirm that the application can successfully download and parse a visa bulletin.
    - [ ] Run the application and check the output.
    - [ ] Adjust table indexing in `App.kt` if necessary.
    - [ ] Clean up raw data output and present parsed data clearly.
- [ ] **Implement Caching Mechanism:** Store parsed visa bulletin data to avoid re-downloading.
    - [ ] Create a caching directory.
    - [ ] Implement logic to check for cached data before downloading.
    - [ ] Save parsed data to a file (e.g., JSON) after a successful download.
    - [ ] Use a JSON serialization library like `kotlinx.serialization`.
- [ ] **Enhance User Interaction:** Add functionality to take user input.
    - [ ] Prompt user for Priority Date, Category, and Country.
    - [ ] Implement logic to compare user's priority date with the bulletin.
    - [ ] Display a clear result to the user.
- [ ] **Improve Bulletin Fetching:** Automatically find the latest visa bulletin URL.
    - [ ] Scrape the State Department website to find the link to the latest bulletin instead of using a hardcoded URL.
- [ ] **Add Unit Tests:** Ensure the application is robust.
    - [ ] Write tests for the parser.
    - [ ] Write tests for the caching logic.

## Instructions for AI-Assisted Development

When using AI tools like Grok, Gemini, or Claude for further development of this project, follow these guidelines to maximize effectiveness:

1. **Provide Detailed Context**: Include the project structure, relevant file paths (e.g., `app/src/main/kotlin/org/example/App.kt`), code snippets, error messages, and specific goals in your queries.

2. **Debugging**: Share stack traces, logs, or symptoms of issues (e.g., "The VisaBulletinFetcher is failing to download the PDF"). Ask the AI to trace code execution, identify bugs, and suggest fixes.

3. **Feature Addition**: Clearly describe the desired feature, inputs/outputs, and integration points. For example: "Implement a caching system for visa bulletins using Kotlin and a local database."

4. **Code Review and Optimization**: Request reviews of specific classes or functions for best practices, performance improvements, or refactoring. E.g., "Review VisaBulletinParser.kt for efficiency in parsing PDF tables."

5. **Testing**: Ask for help in writing or improving unit tests, e.g., in `AppTest.kt`. Provide existing test code if available.

6. **Library Integration**: Inquire about adding dependencies via Gradle, ensuring compatibility. E.g., "How to integrate a PDF parsing library into this Kotlin project?"

7. **Prompt Best Practices**: Use structured prompts like "As a senior Kotlin developer, explain how to..." or "Step-by-step plan to debug...". Break complex tasks into smaller sub-tasks.

8. **Version Control**: After AI suggestions, test changes locally and commit with descriptive messages.

These instructions aim to streamline collaboration with AI for efficient development and debugging.