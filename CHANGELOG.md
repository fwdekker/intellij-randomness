# Changelog
## 9.9.9-unreleased
Delete this sentence. And improve these logs.

### Changed
* Date-time inputs are now slightly wider, to ensure the entire input is visible while typing.
* Invalid date-time inputs are now not thrown away in favour of the old input. Instead, the invalid input is kept, and a warning is shown. This way, when you make a mistake, you can just correct it instead of having to try it again.
* Entering the settings of a template from the popup will now automatically jump to that template's first scheme, since that's what you're most likely looking for anyway.

### Fixed
* Pressing <kbd>Enter</kbd> while writing a date-time without losing focus first now correctly saves the newly entered date-time; and validates it if required.


## 3.3.6 -- 2025-02-15
### Fixed
* Fixed exception when submitting bug report without Internet connection. ([#R40](https://github.com/FWDekkerBot/intellij-randomness-issues/issues/40)) ([#560](https://github.com/FWDekker/intellij-randomness/pull/560))


## 3.3.5 -- 2025-01-27
### Fixed
* Fixed corrupted settings resulting in error messages after enabling "Array", "Fixed Length", or "Surround With" in any template or scheme. With this update, corrupted settings are patched back into a working state automatically. To prevent similar bugs in the future, unit tests have been added that automatically detect bugs resulting in corrupt settings. ([#R37](https://github.com/FWDekkerBot/intellij-randomness-issues/issues/37)) ([#558](https://github.com/FWDekker/intellij-randomness/pull/558))


## 3.3.4 -- 2025-01-17
### Changed
* Refreshed (encrypted) bug reporter token. ([#548](https://github.com/FWDekker/intellij-randomness/issues/548))

### Fixed
* Fixed specific settings resetting after IDE restart. Specifically, for any scheme, "Array", "Fixed Length", and "Surround With" would reset. ([#549](https://github.com/FWDekker/intellij-randomness/issues/549))


## 3.3.3 -- 2024-10-16
### Changed
* In template icons, change the order of scheme colors to be clockwise starting from the top, instead of counterclockwise starting on the right.

### Fixed
* (Hopefully) fix "Must be not computed before that call" bug by deferring icon validation to painting. ([#R1](https://github.com/FWDekkerBot/intellij-randomness-issues/issues/1)) ([#R13](https://github.com/FWDekkerBot/intellij-randomness-issues/issues/13)) ([IJPL-163887](https://youtrack.jetbrains.com/issue/IJPL-163887/))
* Fixed bug reporter to also check closed issues when checking for duplicates.


## 3.3.2 -- 2024-09-28
### Added
* Added even more debug info for future reports relating to issue [#R1](https://github.com/FWDekkerBot/intellij-randomness-issues/issues/1).

### Changed
* Changed icon instantiation to be more in line with guidelines, hopefully fixing [#R13](https://github.com/FWDekkerBot/intellij-randomness-issues/issues/13).
* Updated various dependencies, especially those related to tooling.


## 3.3.1 -- 2024-05-06
### Added
* Added additional debug info for future reports relating to issue [#R1](https://github.com/FWDekkerBot/intellij-randomness-issues/issues/1).

### Fixed
* Fixed an exception when a `StringScheme` contains a matching pattern containing `[]{1,3}`. ([#R2](https://github.com/FWDekkerBot/intellij-randomness-issues/issues/2))


## 3.3.0 -- 2024-04-16
### Added
* Added support for [character classes](https://www.regular-expressions.info/unicode.html#category) (e.g. `\p{Letter}`) in strings by updating [RgxGen](https://github.com/curious-odd-man/RgxGen) to v2.0. ([#530](https://github.com/FWDekker/intellij-randomness/issues/530))

### Changed
* Rewrote error reporter from scratch, now ensuring all relevant data is included, and allowing non-GitHub users to report issues. ([#455](https://github.com/FWDekker/intellij-randomness/issues/455))
* Significantly reduced plugin size by removing unnecessary dependencies. ([#526](https://github.com/FWDekker/intellij-randomness/issues/526))

### Removed
* Disabled dynamic reloading of the plugin until a stable fix can be deployed. ([#522](https://github.com/FWDekker/intellij-randomness/pull/522))

### Fixed
* Fixed various incorrect uses of the platform API. ([#526](https://github.com/FWDekker/intellij-randomness/issues/526))


## 3.2.0 -- 2024-01-10
### Added
* Added support for UUID versions 6, 7, and 8. ([#513](https://github.com/FWDekker/intellij-randomness/issues/513))
* Added button to visit template's target. ([#495](https://github.com/FWDekker/intellij-randomness/issues/495))

### Fixed
* Disabled uppercase option for integers when base is 10 or lower.


## 3.1.0 -- 2023-12-08
### Added
* Added ability to generate non-matching strings.
  ([#447](https://github.com/FWDekker/intellij-randomness/issues/447))

### Changed
* When inserting arrays at multiple carets, the number of elements per array is now independently chosen for each array.
  ([#450](https://github.com/FWDekker/intellij-randomness/issues/450))

### Deprecated
* Minimum IDE version has been increased to 2023.1.


## 3.0.0 -- 2023-11-17
This release brings a major overhaul of how data is generated, allowing you to create your own data types such as IP
addresses or entire JSON objects.
At the same time, it remains just as easy to generate plain numbers.
Check the plugin description for more details and animated usage examples.

### Added
* Each time you insert an array your demands will be slightly different, so when you insert an array a dialog is shown
  in which you can quickly vary the array's settings.
* In addition to a list of standard separators, you can now also choose your own separator for all data types, including
  for arrays.
* You can automatically pad (or truncate) integers to a specific length.
* A notification is shown after upgrading to Randomness 3 to inform the user of incompatibilities with settings from
  older versions.
* Future backwards compatibility ensures that your settings can always be imported into future versions.

### Changed
* Randomness now uses templates to generate data.
  A template consists of a list of "primitive" data types which are concatenated together.
  Data types include the old data types (integer, decimal, string, word, UUID), but also the new date-time and the
  template reference.
* Strings no longer consist of customisable symbol sets, but are specified using a regex.
* Words are no longer read from dictionary files, but are stored directly in the settings window. To reuse words in
  multiple templates, consider using template references.
* The preview pane now looks more beautiful :-)
* Icons for templates and data types are dynamically generated based on the involved data types.
* Invalid settings are now easier to correct with more specific error messages.
* All strings have been internationalised, to make future translation easier.
* Changelogs are now kept in [keep a changelog](https://keepachangelog.com/en/1.0.0/) style.

### Deprecated
* Minimum IDE version has been increased to 2022.3.

### Fixed
* The settings-only popup is now also shown when editing a read-only file.


## 2.7.7 -- 2023-06-30
### Breaking changes
Minimum IDE version has been increased to 2022.2.

### Fixes
Resolve compatibility issues with upcoming IDE versions.
([#459](https://github.com/FWDekker/intellij-randomness/issues/459))
([#460](https://github.com/FWDekker/intellij-randomness/issues/460))


## 2.7.6 -- 2022-12-14
### Breaking changes
Minimum IDE version has been increased to 2022.1.

### Fixes
Resolve compatibility issues with upcoming IDE versions.


## 2.7.5 -- 2022-05-15
### Breaking changes
Minimum IDE version has been increased to 2021.2.

### Fixes
* Add prefix and suffix options for strings.
* Ensure consistent capitalisation between previews.


## 2.7.4 -- 2021-12-10
### Fixes
Custom action shortcuts should use current configuration.
([#423](https://github.com/FWDekker/intellij-randomness/issues/423))


## 2.7.3 -- 2021-10-01
### Breaking changes
Minimum IDE version has been increased to 2020.3.
([#358](https://github.com/FWDekker/intellij-randomness/issues/358))
([#386](https://github.com/FWDekker/intellij-randomness/issues/386))

### Fixes
* Shorter error messages in preview window.
* Resolved critical UI error in upcoming 2021.3 IDEs as result of using incorrect factory.
  ([#418](https://github.com/FWDekker/intellij-randomness/issues/418))


## 2.7.2 -- 2021-07-07
### Fixes
Prevent symbol set settings from being truncated after restarting IDE.
([#382](https://github.com/FWDekker/intellij-randomness/issues/382))


## 2.7.1 -- 2021-07-05
### Breaking changes
Minimum IDE version has been increased to 2020.2.  (#375)

### New features
* Remove limit on difference between minimum and maximum integer.
  ([#367](https://github.com/FWDekker/intellij-randomness/issues/367))
* Add "byte" integer type to generate integers from -127 to 128.
  ([#368](https://github.com/FWDekker/intellij-randomness/issues/368))
* Input field widths now reflect the expected input sizes.
  ([#374](https://github.com/FWDekker/intellij-randomness/issues/374))
* Significantly improved performance when generating long strings.
  ([#373](https://github.com/FWDekker/intellij-randomness/issues/373))
* Generator timeout prevents IntelliJ from freezing when using excessively complex inputs.
  ([#373](https://github.com/FWDekker/intellij-randomness/issues/373))

### Fixes
Prevent overflows when using a large range with integers.
([#370](https://github.com/FWDekker/intellij-randomness/issues/370))


## 2.7.0 -- 2020-12-30
This plugin is also available on the [plugin repository](https://plugins.jetbrains.com/plugin/9836-randomness).

### Breaking changes
* Minimum IDE version has been increased to 2020.1.
  ([#209](https://github.com/FWDekker/intellij-randomness/issues/209),
  [#345](https://github.com/FWDekker/intellij-randomness/issues/345),
  [#361](https://github.com/FWDekker/intellij-randomness/issues/361))

* Extended English dictionary (<tt>english_extended.dic</tt>) has been removed to improve performance and reduce plugin
  size.
  ([#352](https://github.com/FWDekker/intellij-randomness/issues/352))

### New features
* **🔠 Integer capitalization**.\
  Option to change capitalization of Integers with base greater than 10.
  For example, generate <tt>0xFF</tt> instead of <tt>0xff</tt>.
  ([#346](https://github.com/FWDekker/intellij-randomness/issues/346))
* Short inline explanation of how dictionaries work in the Words settings dialog.
  ([#347](https://github.com/FWDekker/intellij-randomness/issues/347))
* Improved support for drag-and-drop of dictionary files into dictionary table.
  ([#350](https://github.com/FWDekker/intellij-randomness/issues/350))
* Improved error messages for IO failures with dictionaries.
  ([#354](https://github.com/FWDekker/intellij-randomness/issues/354))
* Natural column widths in dictionary table.
  ([#354](https://github.com/FWDekker/intellij-randomness/issues/354))
* Changes to dictionary contents are detected directly while in the Words settings dialog.
  ([#354](https://github.com/FWDekker/intellij-randomness/issues/354))
* Invalid settings are marked as modified even if settings have not been changed.
  ([#354](https://github.com/FWDekker/intellij-randomness/issues/354))

### Fixes
Listing of data types in Randomness settings dialog has been fixed.
([#341](https://github.com/FWDekker/intellij-randomness/issues/341))


## 2.6.1 -- 2020-06-24
### New features
* The error report dialogue will now inform you of the privacy policy applicable to the reporting.

### Fixes
* Excessively long error reports are now partially truncated to prevent HTTP 414 errors on GitHub.
* Reserved URI characters in error reports are now truncated to prevent URI misinterpretations.


## 2.6.0 -- 2020-05-14
### New features
* **💫 Prefixes and suffixes**.\
  Prepend or append strings to your integers and decimals
* Check the list of look-alike symbols by hovering over the option in the string settings.

### Fixes
* Schemes can now be saved and loaded correctly.
* Deleting a scheme no longer loads that scheme into the default scheme.
* Mnemonics for spinners now actually work.


## 2.5.1 -- 2020-02-08
### New features
* **🚨 Error reporter**.\
  Easily report fatal Randomness errors to GitHub by clicking the report button in IntelliJ's
  [event log](https://www.jetbrains.com/help/idea/event-log-tool-window.html).

### Fixes
* Time-based UUID previews now use fixed seed until preview is refreshed.
* Radio buttons are no longer in a grid, giving the corresponding labels more natural sizes.
* The layout of UUID options has been altered slightly.
* Holding modifier keys in settings-only popup now triggers normal behavior of opening settings window.


## 2.5.0 -- 2020-01-24
**Symbol set settings will be reset when updating to v2.5.0 because of changes in how settings are stored.**
You can safely re-add your custom symbol sets after updating.

### New features
* **♻️ Scheme switcher**\
  Quickly switch between your schemes by holding <kbd>Alt + Ctrl</kbd> while selecting a data type.
* **😀 Emoji**\
  Generate random strings of emoji by adding them to symbol sets.
* **🕵 Look-alike characters**\
  Exclude characters that look like each other (e.g. `1`, `l`, `I`) in generated strings to prevent confusion.
* Change your settings using the Randomness popup even when you don't have an editor opened.
* Slightly friendlier and more accurate error messages for tables.

### Fixes
* Input fields now correctly resize when shrinking the dialog.
* Symbol set table now makes full use of vertical space.
* Dictionary table no longer exceeds dialog width.
* Adding a new entry to a table using the "Add" button will now allow you to immediately edit the new entry.
* Leading and trailing whitespace characters no longer disappear when expanding a symbol set field.
* Pressing <kbd>Enter</kbd> in a table now activates the editor for the currently-selected cell.
* Duplicate symbols in single symbol sets are now ignored.
* Arrow keys now work correctly for all radio buttons.


## 2.4.1 -- 2020-01-14
### Fixes
Resolves an issue causing plugin incompatibility with IDE versions 2018.1 through 2018.3.


## 2.4.0 -- 2020-01-07
**All Randomness settings will be reset when updating to v2.4.0 because of changes in how settings are stored.**
You can safely reconfigure Randomness after updating.

### New features
* **🗃️ Schemes**\
  Save your Randomness configurations using schemes. Simply create some new schemes to your liking, and you can change
  back and forth between your schemes without any typing.
* **🖼️ Icons**\
  In addition to Randomness' new logo, all Randomness actions can now be identified by their unique icons.
* **♻️ Repeat**\
  Hold <kbd>Alt</kbd> (<kbd>⌥</kbd>) while selecting a data type to insert will insert the same value at all
  [carets](https://www.jetbrains.com/help/idea/working-with-source-code.html#multiple_cursor).
  Like all other Randomness actions, the repeat action can be assigned a shortcut if you want.
* **⌨️ Modifier keys**\
  Hold multiple modifier keys (<kbd>Alt</kbd>, <kbd>Ctrl</kbd>, <kbd>Shift</kbd>) to combine their effects.
  For example, hold <kbd>Ctrl + Shift</kbd> to change the array settings, or hold <kbd>Alt + Shift</kbd> to insert
  repeated arrays.
* **📏 Expandable fields**\
  View large symbol sets in a single glance using expandable text fields.
  No more horizontal scrolling back and forth.
* Empty tables now contain clickable links for adding new values.
* Array previews now contain multiple random numbers instead of repeating the number 17.
* Groups of settings are separated by horizontal lines instead of being surrounded by borders.

### Fixes
* The Randomness popup now appears at your caret instead of in the middle of the screen.
* The Randomness popup no longer appears when the editor isn't selected.
* The Randomness popup is now wide enough to display the full header text.
* Invalid number inputs result in preciser error messages.
* Previews that exceed the dialog width now wrap around.
* Adding a new symbol set or dictionary will move focus to the first editable column so you can start typing right away.


## 2.3.0 -- 2019-12-09
### New features
* **Previews**\
  To help you decide what settings to choose, a preview of the data that is generated with your current settings is
  shown at the bottom of the settings window.
* **New UUID settings**\
  You can now choose between time-based (version 1) and random (version 4) UUIDs.
  Additionally, you can change the capitalisation and remove the dashes if you want.
* **Layout**\
  The improved layout adds vertical space in between groups to make it easier to find settings quickly.
* **Easier range controls**\
  When you set the minimum value to be higher than the maximum value, the maximum value will automatically be increased
  as well---and vice versa for changing the maximum value.
* Randomness can now also be found in the
  [<kbd>Generate</kbd> menu](https://www.jetbrains.com/help/idea/generating-code.html).
* The "space after separator" option in arrays is now disabled if the newline separator is set.
* Random capitalisation mode is now available for words as well.

### Bug fixes
* The copy button is now disabled when a bundled dictionary is selected in the word settings.
* Invalid controls are now disabled when you open the settings dialog.
* Exceptions caused by corrupted settings are now handled properly.


## 2.2.0 -- 2019-08-06
**Notice**\
Please note that symbol set settings will be reset when updating to v2.2.0 because of changes in how settings are
stored.
You can re-add your custom symbol sets after updating.

### New features
* Custom symbol sets for generating strings.
* Inline editing of symbol sets and dictionaries.
* Persistent order of symbol sets and dictionaries.
* Improved error messages.
* More consistent settings layouts.


## 2.1.0 -- 2019-07-10
### New features
* Settings have been moved to the native settings window.
* Retain trailing zeroes in decimals.

### Bug fixes
* Bundled dictionaries no longer disappear if they are not active when word settings are saved.


## 2.0.0 -- 2019-05-16
**Notice**
Please note that dictionary settings will be reset when updating to v2.0.0 because of changes in how settings are
stored.
You can re-add your custom dictionaries after updating.

### New features
* Inserting multiple words is now much faster.
* An additional bundled dictionary with simpler English words.
* Some setting defaults have been changed.
* All-around more descriptive error messages.

### Bug fixes
* Dictionaries are now re-loaded before validating settings.


## 1.6.1 -- 2018-09-12
### New features
* Allow changing quotation marks around generated UUIDs.


## 1.6.0 -- 2018-09-11
### New features
* Generating type 4 UUIDs.
* Generating hexadecimal strings.
* Different capitalisation modes for strings.

### Bug fixes
* Newline separator for arrays is now an actual newline instead of the text "\n".


## 1.5.2 -- 2018-05-27
### New features
* Dictionary contents are now refreshed when word settings are saved.


## 1.5.1 -- 2018-05-15
### Bug fixes
* Dictionary validity is now also checked when a custom dictionary is added.


## 1.5.0 -- 2018-05-13
### New features
* Add newline character as separator when inserting arrays.
* Add two new capitalistion modes when inserting words:
    - `Retain`: Does not change capitalisation w.r.t. dictionary file.
    - `First Letter`: Capitalises the first letter of each word.

### Bug fixes
* Deletion, renaming, and emptiness of custom dictionaries are detected when configuring and inserting random words.


## 1.4.0 -- 2018-04-30
### New features
* Mnemonics for some buttons.
* Integers can be generated in customisable radix.


## 1.3.2 -- 2018-01-21
### New features
* All actions can now be given keybinds separately.
* The Randomness action can now also be found in the Tools menu.


## 1.3.1 -- 2018-01-15
### New features
* Added option to add no brackets around arrays.

### Bug fixes
* Adjusted default random string length.


## 1.3.0 -- 2018-01-09
### New features
* Added ability to generate arrays/lists/vectors of data.
* Overhauled action popup. Use modifier keys to change behaviour.


## 1.2.0 -- 2018-01-05
### New features
* Added ability to generate random words.\
  Words are selected from dictionary files.
  Custom dictionary files can be added by the user.

### Bug fixes
* Minor typo fixes.


## 1.1.0 -- 2017-07-30
### New features
* Added option to change decimal separator for decimals.
* Added option to change grouping separator for integers and decimals.

### Bug fixes
* Input is no longer validated while typing.


## 1.0.1 -- 2017-07-20
### New features
* Random integers can now go from `-2^63` to `2^63`.
* More precise input validation messages.

### Bug fixes
* Excessively large ranges are now rejected.


## 1.0.0 -- 2017-07-16
### New features
* Generated values are now cryptographically safe.\
  (_Note: This is no longer true starting in v1.0.1._)
* Key binding was changed to `Ctrl + R` (or `⌥R`).

### Bug fixes
* Multiple insertions can now be undone together.
* Miscellaneous performance fixes.


## 0.5.0 -- 2017-07-13
### New features
* All actions have been grouped together under `Insert Random Data`.
* The keyboard shortcut `Ctrl + Alt + R` opens a list of all actions.
* Symbols for string generation can now be changed in the settings.


## 0.4.0 -- 2017-07-12
### New features
* Settings are now saved over multiple IDE sessions.
* Different quotation marks can be chosen for string insertion.
* Settings dialogs are now slightly wider to make input easier to read.


## 0.3.0 -- 2017-07-11
### New features
* Data can be inserted at multiple carets simultaneously.
* Inserted data is highlighted after it is inserted.
* Dialogs now have mnemonics.

### Bug fixes
* Plugin works again on IntelliJ versions above 2016.1.


## 0.2.0 -- 2017-07-10
**Because of a configuration error, this release only works for IntelliJ 2016.1.**

### New features
* Random decimals can now be inserted.
* Quotation marks around strings can now be disabled.

### Bug fixes
* Non-numerical input is no longer accepted where numerical input is expected.
* The minimum value input can no longer exceed the maximum value input.


## 0.1.0 -- 2017-07-10
This first release of the Randomness plugin provides some basic features.

### Features
* Insert random numbers at caret.
* Insert random strings at caret.
* Change range of generated numbers.
* Change length of generated strings.
