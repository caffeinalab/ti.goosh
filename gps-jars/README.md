**If you want to use Ti.Goosh with other Android modules, please read this**


### Dealing with multidex

This directory contains some combinations of Google Play Services JARs files to use the module with other Titanium modules that uses Google Play Services too.

To clarify, read this blog post: [https://medium.com/all-titanium/deal-with-titanium-modules-and-its-missing-support-for-android-multidex-546de5486d13#.wfo7v4akx](https://medium.com/all-titanium/deal-with-titanium-modules-and-its-missing-support-for-android-multidex-546de5486d13#.wfo7v4akx)

For example, if you are using **Ti.Goosh** with **Ti.Map**, you have to download the `google-play-services-gcm+map.jar` and **replace** (so delete all others Google Play services JARs) the GPS JAR file in the *lib* directory in both modules.

The structure of your `module/android` must be something like this:

```
.
├── ti.ga
│   └── VERSION
│       ├── lib
│       │   └── google-play-services-XXX.jar  <-- the downloaded library
│       ├── manifest
│       └── timodule.xml
└── ti.goosh
    └── VERSION
        ├── LICENSE
        ├── lib
        │   ├── google-play-services-XXX.jar  <-- the downloaded library
        │   └── gson.jar
        └── timodule.xml
```

The second thing to do is to make sure that all modules reference the right version of the SDK.

To do that, for each module, edit the `[MODULE]/platform/android/res/values/version.xml` file and be sure that the version is `8487000`.

```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <integer name="google_play_services_version">8487000</integer>
</resources>
```
