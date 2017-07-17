# Hello Storj
Hello Storj is a demo app for integrating the [libstorj](https://github.com/Storj/libstorj) native library in Android. The focus is entirely on the integration and it is not intended to provide any meaningful usability and UX. Future user-focused apps may be based on this project.

## Installation

The app can be installed from Google Play: https://play.google.com/store/apps/details?id=name.raev.kaloyan.hellostorj

## Features

* [x] Build the libstorj native library and its dependencies for Android (armeabi-v7a only)
* [x] `storj_util_timestamp()`
* [x] `storj_mnemonic_generate()`

## TODO

* [ ] `storj_bridge_get_info()`
* [ ] `storj_bridge_get_buckets()`
* [ ] `storj_bridge_list_files()`
* [ ] `storj_bridge_create_bucket()`
* [ ] `storj_bridge_resolve_file()`
* [ ] `storj_bridge_store_file()`
* [ ] Build the libstorj native library and its dependencies for all CPU architectures

## JNI wrapper for libstorj

The essence of this project is the JNI wrapper of the libstorj native library. The following are the files of interest:
- [app/src/main/cpp/native-lib.cpp](https://github.com/kaloyan-raev/hello-storj/blob/master/app/src/main/cpp/native-lib.cpp)
- [app/src/main/java/name/raev/kaloyan/hellostorj/jni/Storj.java](https://github.com/kaloyan-raev/hello-storj/blob/master/app/src/main/java/name/raev/kaloyan/hellostorj/jni/Storj.java)
- [app/src/main/java/name/raev/kaloyan/hellostorj/jni/NativeLibraries.java](https://github.com/kaloyan-raev/hello-storj/blob/master/app/src/main/java/name/raev/kaloyan/hellostorj/jni/NativeLibraries.java)

## License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Some parts of the code (JNI wrapper) are licensed under the GNU Lesser General Public License as published by the Free Software Foundation. You can redistribute it and/or modify it under the terms either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU General Public License and the GNU Lesser General Public License along with this program. If not, see http://www.gnu.org/licenses/.
