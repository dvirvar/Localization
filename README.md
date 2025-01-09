# Localization

## Overview

**Localization** is a desktop application designed to help developers and teams manage the translation process for their projects. With this tool, you can send strings that need translation to your translators, and they can return the translated content back to you. The app supports exporting/importing translation files in various formats suitable for Android, iOS, and Web applications, including:

- **XML format** (for Android/KMP)
- **Apple Ecosystem format** (for iOS/macOS)
- **i18n format** (for web applications)

Whether you're localizing an app for multiple languages or handling diverse platforms, **Localization** simplifies the process of managing translations.

## Features

- **Send Text for Translation:** Easily send strings from your project to translators.
- **Receive Translations:** Translators will translate with the app and send the translated file back to you.
- **Multiple Export Formats:** Export translated content into:
  - **XML format** for Android/KMP applications (e.g., .xml files)
  - **Apple Ecosystem format** for iOS/macOS projects (e.g., `.strings` files)
  - **i18n format** for web applications (e.g., JSON files)
- **Supports Multiple Languages and Platforms:** Localize your app into multiple languages and platforms with ease.

## Installation

1. **Download** the latest version of the Localization app from the repository.
2. **Run the installer**
3. **Follow the installation prompts** to complete the setup.
4. **Once installed** open the app, create a project, and you're ready to begin localizing your project.

## How to Use
## For developers
### 1. Create a project
- Open the app.
- Click on **"Create project"** choose a name and path to make your project.
- Follow the steps of the wizard (Add the languages that your project supports, then the platforms and lastly how you would like to export your translations).

### 2. Add translations
- You can manually add translations in the localization tab.
- If you already have translation files, you can import them in the export/import tab (e.g., Android/KMP xml, iOS/macOS .strings, i18n JSON).

### 3. Export to Translators
- You can **export** the strings in the export/import tab under **"Export to translator"** and send them to your translators.
- Translators will work in the Localization app and return the completed translations.

### 4. Receive Translations
- Once the translations are complete, **import** the returned files into the app in the export/import tab under **"Import from translator"**.

### 5. Export Translations
- You can **export** your translations in the export/import tab under **"Export & overwrite"** which will export and overwrite all your localization files in the path you chose.
- Or you can export as ZIP under **"Export as ZIP"**.

## For translators
- Open the app.
- Click on **"Import for translator"** select the file that was sent to you.
- Translate the strings and when you are finished click on **Save**.
- Send back the same file you got.

## Contributing

If you'd like to contribute to the development of Localization, feel free to submit issues or pull requests. All contributions are welcome!.

## License

Localization is licensed under the [MIT License](LICENSE).