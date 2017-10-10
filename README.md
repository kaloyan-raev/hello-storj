# Hello Storj

Hello Storj is a demo app for integrating the [libstorj](https://github.com/Storj/libstorj) native library in Android. The focus is entirely on the integration and it is not intended to provide any meaningful usability and UX. Future user-focused apps may be based on this project.

If you want to build a similar Android app, you can use the [android-libstorj](https://github.com/Storj/android-libstorj) Gradle library. It provides everything you need for working with the Storj network: a Java API and pre-build native libraries for libstorj and all its dependencies. See the [android-libstorj](https://github.com/Storj/android-libstorj) repo for instructions.

[![Downloading files video](http://img.youtube.com/vi/1082cipNheo/0.jpg)](http://www.youtube.com/watch?v=1082cipNheo)
[![Uploading files video](http://img.youtube.com/vi/7h3rB0eByrU/0.jpg)](http://www.youtube.com/watch?v=7h3rB0eByrU)

## Installation

The app can be installed from Google Play.

<a href='https://play.google.com/store/apps/details?id=name.raev.kaloyan.hellostorj&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height=75/></a>

Requrirements:
- Android 5.0 Lollipop or newer
- armeabi-v7a compatible device (most phones and tablets)


## Features

* [x] Call simple native function from libstorj (`storj_util_timestamp()`)
* [x] Call native function and convert types (`storj_mnemonic_generate()`)
* [x] Call native function with networking (`storj_bridge_get_info()`)
* [x] Import account keys
* [x] List buckets
* [x] List files in the buckets
* [x] Download files
* [x] Create bucket
* [x] Upload files
* [x] Register new user

## License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Some parts of the code (FileUtils) are licensed under the Apache License as published by the Apache Software Foundation. You can redistribute it and/or modify it under the terms version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.

