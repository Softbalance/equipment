# Usage

TODO describe

# Integration

### How to add as a gradle dependency:

>repositories {
><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;maven { url "https://raw.githubusercontent.com/mike-stetsenko/equipment/master/maven-repo" }
><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
><br> }

>compile 'ru.softbalance:equipment:1.0.0'

### The library contains activities, don't forget about manifest merger!
>\<application
><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;tools:replace="android:theme, android:label, ...">

__Enjoy it!__ :+1:
