# How to Run the Project
1. Clone the project from GitHub.
2. Navigate into the frontend directory: `cd ./meshenger/frontend`
3. Install dependencies: `npm install`
4. Run Expo prebuild (only once): `npx expo prebuild --clean`
5. Update the following files to match the content from GitHub: `meshenger/frontend/android/settings.gradle` and `meshenger/frontend/android/app/build.gradle`
6. Build and run the Android app: `npx expo run:android`
## Notes
- Run `npx expo prebuild --clean` **only once.** Running it multiple times will reset the content in `settings.gradle` and `app/build.gradle`.
- For backend changes, use: `npx expo run:android`
- For frontend changes, use: `npx expo start`
