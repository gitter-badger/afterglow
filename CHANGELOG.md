# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased][unreleased]
### Added
- Support for inverted dimmers (where lower DMX values are brighter).
- Scenes, which allow multiple effects to be grouped into one.
- New ability to fade between effects, with sensible semantics for
  colors, aim, directions, and functions.
- A new mechanism for extending the rendering loop to support effects
  which do not result in DMX values to send to the show universes.
- Support for (and examples of) integration with laser shows being run
  by Pangolin Beyond software, using the extension mechanism.
- New conditional effects and variable-setting effects, using the
  extension mechanism.
- A composable effect which can transform the colors being created by
  other effects to build layered looks. The default transformation
  causes the colors to range from fully saturated at the start of each
  beat to pure gray by the end of the beat, but it is very easy to
  swap in other transformations using oscillated parameters.
- Holding down the Shift key while turning the encoder allows the BPM
  to be changed more rapidly (in whole beat increments, rather than
  tenths) on the Ableton Push.
- Fixture definitions for Chauvet LED Techno Strobe, LED Techno Strobe
  RGB, ColorStrip, Spot LED 150, Kinta X, Scorpion Storm FX RGB,
  Scorpion Storm RGX, Q-Spot 160, Intimidator Scan LED 300, Geyser RGB
  fogger, and Hurricane 1800 Flex fogger.
- Example effect which desaturates a rainbow over the course of a
  beat.

### Changed
- Improved readability and parallelism of core rendering loop.
- The default frame rate was raised from 30Hz to 40Hz.
- Ableton Push now uses SysEx message to specify the exact RGB color
  to light up a pad, rather than choosing from the limited set
  available through MIDI velocity.
- Ableton Push now makes sure the pads are put in poly-pressure mode,
  and sets the sensitivity level to reduce the chance of stuck pads.
- The stability of MIDI clock sync was greatly improved, in order to
  facilitate the Beyond integration.
- The refresh rates of the Push and web interfaces were reduced to put
  less load on the CPU.
- The tempo buttons on the Push and web interfaces are now always
  flashed at least once per beat, even if the reduced refresh rate
  causes the normal "on" window to be missed.
- Improved content and format of command-line usage help.

### Fixed
- The Ableton Push binding now ends cues when it receives an afertouch
  value of 0, since the hardware is not reliably sending a note-end
  message, especially when multiple pads are being pressed at once.
- Fail gracefully when trying to bind to an Ableton Push when none can
  be found.
- Some small errors in the documentation were corrected.
  

## [0.1.3] - 2015-08-16
### Added
- Ability to
  [translate](https://github.com/brunchboy/afterglow/blob/master/doc/fixture_definitions.adoc#translating-qlc-fixture-definitions)
  fixture definitions from the format used by
  [QLC+](http://www.qlcplus.org/) to help people get started on
  defining fixtures.

### Changed
- Separated OLA communication into its own project,
  [ola-clojure](https://github.com/brunchboy/ola-clojure#ola-clojure).

## [0.1.2] - 2015-08-09
### Added
- Allow configuration of an alternate host for the OLA daemon
  (primarily for Windows users, since there is not yet a Windows port
  of OLA).
- Flesh out the command-line arguments when running as an executable
  jar.
- Allow a list of files to be loaded at startup when running as an
  executable jar, in order to configure fixtures, shows, effects, and
  cues.
- Support syncing to Traktor’s beat grid with the help of a new custom
  controller mapping.

### Changed
- MIDI sync sources are now always watched for, and can be offered to
  the user without pausing first.

## [0.1.1] - 2015-08-02
### Added
- Ability to register for notification about changes in status of cues
  and values of show variables.
- Creating a show can optionally register it with the web interface by
  passing a description with `:description`.
- Now cleans up thread-local bindings stored by the web REPL when
  sessions time out.

### Changed
- Forked Protobuf related libraries to make them build Java 6
  compatible clases, so
  [afterglow-max](https://github.com/brunchboy/afterglow-max) can run
  inside the Java environment provided by
  [Cycling ‘74’s Max](https://cycling74.com/).
- Made the meaning of the `:start` attribute of cue variables simpler
  and more consistent.
- Cue variables which respond to aftertouch now also respond to
  initial velocity, and the related configuration attributes have been
  renamed to `:velocity` to reflect this increased generality.
- Improved the detection of project name and version number so they
  work for afterglow-max builds too.

### Fixed
- Eliminated crashes in the Ableton Push interface when trying to
  adjust the value of a cue variable which had not yet been set to
  anything.

## 0.1.0 - 2015-07-19
### Added
- Initial Public Release


[unreleased]: https://github.com/brunchboy/afterglow/compare/v0.1.3...HEAD
[0.1.3]: https://github.com/brunchboy/afterglow/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/brunchboy/afterglow/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/brunchboy/afterglow/compare/v0.1.0...v0.1.1
