Simple Strength Log
=================

Minimalistic Android appl for weight lifting and strength training. Allows you to add exercises, filter them by which muscles they work, and log sets. When logging, displays previous workout for a particular exercise to assist with improvement. Also includes a rest timer.

This app is intentionally minimal in feature set to avoid forcing users to plan out detailed workouts with specific numbers of sets on different days. This app is meant to substitute for a clipboard, paper, pencil, and a stopwatch, nothing more.

It requires very few permissions, and doesn't use internet at all, so it works offline or in Airplane mode.
Logs are stored in a simple, readable JSON format, which can be synchronized across devices using something like DropSync/Dropbox. Logs are stored in `/sdcard/SimpleHealthSuite/Strength/Logs`, split by day.

This app is free and open source, code can be found at https://github.com/rodhilton/SimpleStrengthLog