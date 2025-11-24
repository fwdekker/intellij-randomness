## Documentation
* [ ] The version number has been bumped according to [Semantic Versioning](https://semver.org/).  
  * [ ] Relevant settings upgrade converters have been implemented and tested.
* [ ] Dependency version numbers have been updated in multiple locations where appropriate. Re-read and re-check all the relevant notes.
* [ ] `README.md` is up to date.
  * [ ] The acknowledgements are up to date.
* [ ] `CHANGELOG.md` is up to date.
  * [ ] The version number and date for the upcoming release are correct.
  * [ ] Contributors who have opened issues or pull requests are acknowledged explicitly where appropriate.
  * [ ] The change notes look fine when viewed in the IDE after loading the plugin.
  * [ ] All subsections use bullet points, even those with only one entry. (Otherwise, the subsection does not show up when viewed in the IDE.)
* [ ] `description.html` (in `src/main/resources/META-INF/`) is up to date.
* [ ] Screenshots and GIFs in `.github/img/` are up to date.
* [ ] Screenshots and GIFs on the plugin repository are up to date.
  <!--
  How to update screenshots:
  * Set the global UI scale to 200% before recording/screenshotting to ensure high-resolution images.
  * Use the project in `src/test/resources/screenshots/` to store code snippets in.
    Do not store `.idea/`, `.gradle`, and similar build files in this project.
  * Hide (inlay) hints and set font size to 20.
  * Distance between bottom of "Refresh" button and top of button bar at bottom is 50 pixels, or the original distance, whichever is smaller.
  * On Linux, the screen can be recorded using [peek](https://github.com/phw/peek) or
    [SimpleScreenRecorder](https://www.maartenbaert.be/simplescreenrecorder/).
  * Reducing GIF size is a difficult process.
    The following seems to work fine:
    1. `for f in ./*.webm; do ffmpeg -y -i "$f" -vf "fps=10,scale=768:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse" -loop 0 "${f%.*}.gif"; done`
    2. Go to [ezgif](https://ezgif.com/optimize) and upload the GIF to reduce in size.
    3. Apply the following optimisations in question; after each result, you can click "Optimize" to apply another filter:
       1. "Color Reduction" to 64 colours
       2. "Optimize Transparency" with 2% fuzz
       3. "Lossy GIF" with compression level 30
  -->
* [ ] `./gradlew dokkaGenerateHtml` runs without warnings or errors.  
  There is no need to update the GitHub pages, this is done automatically during the `cd.yml` workflow.
  Check that the build action does not fail!

## Verification
* [ ] All tests and static analysis pass locally.
* [ ] Old and new features have been checked to work manually.
* [ ] The plugin verifier completes successfully.
* [ ] Settings from the previous version have been manually verified to load into the new version without errors.
* [ ] During the manual tests above, the logs do not contain exceptions with Randomness as the "Plugin to blame". You must actually check the log, not just the in-IDE exception notifications.
* [ ] The `cd.yml` workflow completes without issues in dry-run mode.

## How to deploy
Trigger the [`cd.yml` workflow](https://github.com/fwdekker/intellij-randomness/actions/workflows/cd.yml).
See the `README.md` for more details on what it does and how it works.
